package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VOUCHER = 101;
    private static final int REQUEST_CODE_ADDRESS = 102;

    private ArrayList<CartItem> selectedItems = new ArrayList<>();
    private long shippingFee = 30000;
    private long shippingDiscount = 30000; // Freeship
    private long voucherDiscount = 0;
    
    private String buyerName = "";
    private String buyerPhone = "";
    private String buyerAddress = "";
    private boolean hasAddress = false;

    private LinearLayout containerProducts;
    private TextView tvSubtotal, tvProductDiscount, tvDiscount, tvShippingFee, tvShippingFeeDisplay, tvFinalTotal;
    private TextView tvTotalPrice, tvSavingDisplay, tvTotalItems, tvSelectedVoucherName, tvDisplayAddress, tvDisplayName, tvAddressWarning;
    private RadioButton rbBank, rbCod;
    private View layoutCod, layoutBank;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy dữ liệu từ Intent
        if (getIntent().hasExtra("SELECTED_ITEMS_DATA")) {
            selectedItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("SELECTED_ITEMS_DATA");
        } else if (getIntent().hasExtra("SELECTED_ITEMS")) {
            selectedItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("SELECTED_ITEMS");
        } else if (getIntent().hasExtra("PRODUCT_ID")) {
            String pId = getIntent().getStringExtra("PRODUCT_ID");
            int qty = getIntent().getIntExtra("QUANTITY", 1);
            fetchProductForPayment(pId, qty);
        }

        initViews();
        setupInsets();
        renderProducts();
        loadDefaultAddress();
        updateUI();
    }

    private void fetchProductForPayment(String productId, int quantity) {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Product product = documentSnapshot.toObject(Product.class);
                    if (product != null) {
                        CartItem item = new CartItem(
                                auth.getUid(),
                                documentSnapshot.getId(),
                                product.getName(),
                                product.getCategory(),
                                product.getPrice(),
                                product.getOriginalPrice(),
                                quantity,
                                product.getImageUrl()
                        );
                        selectedItems.add(item);
                        renderProducts();
                        updateUI();
                    }
                });
    }

    private void loadDefaultAddress() {
        db.collection("address")
                .whereEqualTo("userId", auth.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        AddressActivity.AddressItem item = new AddressActivity.AddressItem();
                        item.id = doc.getId();
                        item.name = doc.getString("fullname");
                        item.phone = doc.getString("phone");
                        item.address = doc.getString("address");
                        setAddress(item);
                    } else {
                        showAddressWarning(true);
                    }
                })
                .addOnFailureListener(e -> showAddressWarning(true));
    }

    private void setAddress(AddressActivity.AddressItem item) {
        buyerName = item.name;
        buyerPhone = item.phone;
        buyerAddress = item.address;
        hasAddress = true;

        tvDisplayName.setText(buyerName + " (" + buyerPhone + ")");
        tvDisplayAddress.setText(buyerAddress);
        showAddressWarning(false);
    }

    private void showAddressWarning(boolean show) {
        hasAddress = !show;
        if (show) {
            tvAddressWarning.setVisibility(View.VISIBLE);
            tvDisplayName.setText("Chưa có thông tin");
            tvDisplayAddress.setText("Nhấn để chọn hoặc thêm địa chỉ");
        } else {
            tvAddressWarning.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        containerProducts = findViewById(R.id.containerProducts);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvProductDiscount = findViewById(R.id.tvProductDiscount);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvShippingFeeDisplay = findViewById(R.id.tvShippingFeeDisplay);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvSavingDisplay = findViewById(R.id.tvSavingDisplay);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvSelectedVoucherName = findViewById(R.id.tvSelectedVoucherName);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayAddress = findViewById(R.id.tvDisplayAddress);
        tvAddressWarning = findViewById(R.id.tvAddressWarning);

        rbBank = findViewById(R.id.rbBank);
        rbCod = findViewById(R.id.rbCod);
        layoutCod = findViewById(R.id.layoutCod);
        layoutBank = findViewById(R.id.layoutBank);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnToAddress).setOnClickListener(v -> 
            startActivityForResult(new Intent(this, AddressActivity.class), REQUEST_CODE_ADDRESS));
        
        findViewById(R.id.btnOpenVoucher).setOnClickListener(v -> 
            startActivityForResult(new Intent(this, VoucherActivity.class), REQUEST_CODE_VOUCHER));
        
        findViewById(R.id.btnOrder).setOnClickListener(v -> placeOrder());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_ADDRESS) {
                AddressActivity.AddressItem item = (AddressActivity.AddressItem) data.getSerializableExtra("SELECTED_ADDRESS_OBJ");
                if (item != null) {
                    setAddress(item);
                }
            } else if (requestCode == REQUEST_CODE_VOUCHER) {
                // Xử lý voucher nếu có
            }
        }
    }

    private void placeOrder() {
        if (!hasAddress) {
            Toast.makeText(this, "Vui lòng thêm địa chỉ giao hàng trước khi đặt hàng!", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedItems == null || selectedItems.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String orderGroupId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        for (CartItem item : selectedItems) {
            Map<String, Object> order = new HashMap<>();
            order.put("createAt", FieldValue.serverTimestamp());
            order.put("orderId", orderGroupId);
            order.put("productId", item.getProductId());
            order.put("productName", item.getName());
            order.put("imageUrl", item.getImageUrl());
            order.put("quantity", (long) item.getQuantity());
            
            // Lưu thông tin người nhận vào đơn hàng
            order.put("buyerName", buyerName);
            order.put("buyerPhone", buyerPhone);
            order.put("shippingAddress", buyerAddress);
            
            // Lưu phương thức thanh toán
            String method = rbBank.isChecked() ? "Chuyển khoản ngân hàng" : "Thanh toán khi nhận hàng (COD)";
            order.put("paymentMethod", method);
            order.put("shippingFee", shippingFee - shippingDiscount);
            
            order.put("status", "Chờ xác nhận");
            order.put("totalPrice", (long) (item.getPrice() * item.getQuantity()));
            order.put("userId", userId);

            db.collection("orders").add(order);
        }

        Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
        
        // Xóa giỏ hàng sau khi đặt thành công
        for (CartItem item : selectedItems) {
            if (item.getCartItemId() != null) {
                db.collection("carts").document(item.getCartItemId()).delete();
            } else {
                db.collection("carts")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("productId", item.getProductId())
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            for (QueryDocumentSnapshot doc : snapshots) doc.getReference().delete();
                        });
            }
        }

        Intent intent = new Intent(this, OderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void renderProducts() {
        if (containerProducts == null) return;
        containerProducts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CartItem item : selectedItems) {
            View itemView = inflater.inflate(R.layout.item_payment_product, containerProducts, false);
            Glide.with(this).load(item.getImageUrl()).into((ImageView) itemView.findViewById(R.id.imgProduct));
            ((TextView) itemView.findViewById(R.id.tvProductName)).setText(item.getName());
            ((TextView) itemView.findViewById(R.id.tvProductPrice)).setText(formatMoney(item.getPrice()));
            ((TextView) itemView.findViewById(R.id.tvQuantity)).setText("x" + item.getQuantity());
            containerProducts.addView(itemView);
        }
    }

    private void updateUI() {
        long currentTotal = 0;
        int totalQty = 0;
        for (CartItem item : selectedItems) {
            currentTotal += item.getPrice() * item.getQuantity();
            totalQty += item.getQuantity();
        }
        long finalTotal = currentTotal + shippingFee - shippingDiscount - voucherDiscount;
        tvSubtotal.setText(formatMoney(currentTotal));
        tvFinalTotal.setText(formatMoney(finalTotal));
        tvTotalPrice.setText(formatMoney(finalTotal));
        tvTotalItems.setText("Tổng (" + totalQty + " mặt hàng)");
    }

    private String formatMoney(long amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount);
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }
}
