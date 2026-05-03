package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.OrderItem;
import com.example.hitcapp.models.Voucher;
import com.example.hitcapp.utils.VoucherService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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
    private long shippingFee = 0;
    private long shippingDiscount = 0;
    private long productDiscount = 0;
    
    private Voucher selectedShippingVoucher;
    private Voucher selectedProductVoucher;

    private String buyerName = "";
    private String buyerPhone = "";
    private String buyerAddress = "";
    private boolean hasAddress = false;

    private LinearLayout containerProducts, containerSelectedVouchers;
    private TextView tvSubtotal, tvDiscount, tvShippingFee, tvFinalTotal;
    private TextView tvTotalPrice, tvSavingDisplay, tvTotalItems, tvVoucherCount, tvDisplayAddress, tvDisplayName, tvAddressWarning;
    private RadioButton rbBank, rbCod;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private VoucherService voucherService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        voucherService = new VoucherService();
        
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getIntent().hasExtra("SELECTED_ITEMS_DATA")) {
            selectedItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("SELECTED_ITEMS_DATA");
        } else if (getIntent().hasExtra("SELECTED_ITEMS")) {
            selectedItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("SELECTED_ITEMS");
        }

        initViews();
        setupInsets();

        if (selectedItems != null && !selectedItems.isEmpty()) {
            renderProducts();
            updateUI();
        } else {
            Toast.makeText(this, "Không có thông tin sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDefaultAddress();
        autoApplyVouchers();
    }

    private void initViews() {
        containerProducts = findViewById(R.id.containerProducts);
        containerSelectedVouchers = findViewById(R.id.containerSelectedVouchers);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvShippingFee = findViewById(R.id.tvShippingFee);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvSavingDisplay = findViewById(R.id.tvSavingDisplay);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvVoucherCount = findViewById(R.id.tvVoucherCount);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayAddress = findViewById(R.id.tvDisplayAddress);
        tvAddressWarning = findViewById(R.id.tvAddressWarning);

        rbBank = findViewById(R.id.rbBank);
        rbCod = findViewById(R.id.rbCod);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnToAddress).setOnClickListener(v -> 
            startActivityForResult(new Intent(this, AddressActivity.class), REQUEST_CODE_ADDRESS));
        
        findViewById(R.id.btnOpenVoucher).setOnClickListener(v -> {
            Intent intent = new Intent(this, VoucherActivity.class);
            intent.putExtra("PRE_SELECTED_SHIPPING", selectedShippingVoucher);
            intent.putExtra("PRE_SELECTED_PRODUCT", selectedProductVoucher);
            startActivityForResult(intent, REQUEST_CODE_VOUCHER);
        });
        
        findViewById(R.id.btnOrder).setOnClickListener(v -> placeOrder());
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

    private void autoApplyVouchers() {
        db.collection("vouchers")
                .whereEqualTo("userId", auth.getUid())
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean changed = false;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Voucher v = doc.toObject(Voucher.class);
                        v.setId(doc.getId());
                        if (v.isAutoApply()) {
                            if (v.getType().equals(Voucher.TYPE_SHIPPING) && selectedShippingVoucher == null) {
                                selectedShippingVoucher = v;
                                changed = true;
                            } else if (v.getType().equals(Voucher.TYPE_DISCOUNT) && selectedProductVoucher == null) {
                                selectedProductVoucher = v;
                                changed = true;
                            }
                        }
                    }
                    if (changed) updateUI();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_ADDRESS) {
                AddressActivity.AddressItem item = (AddressActivity.AddressItem) data.getSerializableExtra("SELECTED_ADDRESS_OBJ");
                if (item != null) setAddress(item);
            } else if (requestCode == REQUEST_CODE_VOUCHER) {
                selectedShippingVoucher = (Voucher) data.getSerializableExtra("SELECTED_SHIPPING");
                selectedProductVoucher = (Voucher) data.getSerializableExtra("SELECTED_PRODUCT");
                updateUI();
            }
        }
    }

    private void updateUI() {
        long subTotal = 0;
        int totalQty = 0;
        for (CartItem item : selectedItems) {
            subTotal += item.getPrice() * item.getQuantity();
            totalQty += item.getQuantity();
        }

        shippingFee = (long) (subTotal * 0.01);
        if (shippingFee < 30000) shippingFee = 30000;
        
        shippingDiscount = 0;
        if (selectedShippingVoucher != null) shippingDiscount = shippingFee;

        productDiscount = 0;
        if (selectedProductVoucher != null) productDiscount = selectedProductVoucher.getValue();

        long finalTotal = subTotal + shippingFee - shippingDiscount - productDiscount;
        if (finalTotal < 0) finalTotal = 0;

        tvSubtotal.setText(formatMoney(subTotal));
        tvShippingFee.setText(formatMoney(shippingFee));
        tvDiscount.setText("-" + formatMoney(shippingDiscount + productDiscount));
        tvFinalTotal.setText(formatMoney(finalTotal));
        tvTotalPrice.setText(formatMoney(finalTotal));
        tvTotalItems.setText("Tổng (" + totalQty + " mặt hàng)");
        tvSavingDisplay.setText("Tiết kiệm " + formatMoney(shippingDiscount + productDiscount));

        renderSelectedVouchers();
    }

    private void renderSelectedVouchers() {
        containerSelectedVouchers.removeAllViews();
        int count = 0;
        LayoutInflater inflater = LayoutInflater.from(this);
        if (selectedShippingVoucher != null) { addVoucherView(inflater, selectedShippingVoucher); count++; }
        if (selectedProductVoucher != null) { addVoucherView(inflater, selectedProductVoucher); count++; }
        if (count > 0) {
            tvVoucherCount.setText("Đã chọn " + count + " voucher");
            tvVoucherCount.setTextColor(Color.parseColor("#00B5AD"));
        } else {
            tvVoucherCount.setText("Chọn voucher");
            tvVoucherCount.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void addVoucherView(LayoutInflater inflater, Voucher voucher) {
        View v = inflater.inflate(R.layout.item_selected_voucher, containerSelectedVouchers, false);
        ((TextView) v.findViewById(R.id.tvVoucherTitle)).setText(voucher.getTitle());
        ((TextView) v.findViewById(R.id.tvVoucherDesc)).setText(voucher.getDescription());
        containerSelectedVouchers.addView(v);
    }

    private void placeOrder() {
        if (!hasAddress) { Toast.makeText(this, "Vui lòng chọn địa chỉ!", Toast.LENGTH_SHORT).show(); return; }

        String orderIdText = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long totalOriginalPrice = 0;
        List<Map<String, Object>> itemsList = new ArrayList<>();
        
        for (CartItem item : selectedItems) {
            totalOriginalPrice += item.getPrice() * item.getQuantity();
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productId", item.getProductId());
            itemMap.put("name", item.getName());
            itemMap.put("price", item.getPrice());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("imageUrl", item.getImageUrl());
            itemsList.add(itemMap);
        }

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderIdText);
        order.put("userId", auth.getUid());
        order.put("buyerName", buyerName);
        order.put("buyerPhone", buyerPhone);
        order.put("shippingAddress", buyerAddress);
        order.put("status", "Chờ xác nhận");
        order.put("paymentMethod", rbBank.isChecked() ? "Chuyển khoản" : "COD");
        order.put("totalPrice", totalOriginalPrice);
        order.put("shippingFee", shippingFee);
        order.put("finalPrice", totalOriginalPrice + shippingFee - shippingDiscount - productDiscount);
        order.put("createAt", FieldValue.serverTimestamp());
        order.put("items", itemsList);

        if (!selectedItems.isEmpty()) {
            CartItem firstItem = selectedItems.get(0);
            order.put("imageUrl", firstItem.getImageUrl());
            order.put("productName", firstItem.getName());
            order.put("quantity", firstItem.getQuantity());
            order.put("productId", firstItem.getProductId());
        }

        db.collection("orders").add(order).addOnSuccessListener(documentReference -> {
            String firestoreId = documentReference.getId();
            sendNotification("Mua hàng thành công", "Đơn hàng #" + orderIdText + " đã được đặt thành công.", "Đơn hàng", firestoreId);
            clearPurchasedItemsFromCart();
            if (selectedShippingVoucher != null) markVoucherUsed(selectedShippingVoucher.getId());
            if (selectedProductVoucher != null) markVoucherUsed(selectedProductVoucher.getId());
            
            // CẬP NHẬT: Gửi thông báo chứa MÃ quà tặng
            voucherService.checkAndRewardAfterOrder(auth.getUid(), firestoreId);

            Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, OderActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void sendNotification(String title, String content, String type, String oderId) {
        Map<String, Object> notice = new HashMap<>();
        notice.put("userId", auth.getUid());
        notice.put("title", title);
        notice.put("content", content);
        notice.put("type", type);
        notice.put("oderId", oderId);
        notice.put("timestamp", FieldValue.serverTimestamp());
        db.collection("notifications").add(notice);
    }

    private void clearPurchasedItemsFromCart() {
        WriteBatch batch = db.batch();
        for (CartItem item : selectedItems) {
            if (item.getCartItemId() != null) batch.delete(db.collection("carts").document(item.getCartItemId()));
        }
        batch.commit();
    }

    private void markVoucherUsed(String id) { db.collection("vouchers").document(id).update("used", true); }

    private void loadDefaultAddress() {
        db.collection("address").whereEqualTo("userId", auth.getUid()).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        AddressActivity.AddressItem item = new AddressActivity.AddressItem();
                        item.name = doc.getString("fullname");
                        item.phone = doc.getString("phone");
                        item.address = doc.getString("address");
                        setAddress(item);
                    } else tvAddressWarning.setVisibility(View.VISIBLE);
                });
    }

    private void setAddress(AddressActivity.AddressItem item) {
        buyerName = item.name; buyerPhone = item.phone; buyerAddress = item.address;
        hasAddress = true;
        tvDisplayName.setText(buyerName + " (" + buyerPhone + ")");
        tvDisplayAddress.setText(buyerAddress);
        tvAddressWarning.setVisibility(View.GONE);
    }

    private void renderProducts() {
        containerProducts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CartItem item : selectedItems) {
            View v = inflater.inflate(R.layout.item_payment_product, containerProducts, false);
            Glide.with(this).load(item.getImageUrl()).into((ImageView) v.findViewById(R.id.imgProduct));
            ((TextView) v.findViewById(R.id.tvProductName)).setText(item.getName());
            ((TextView) v.findViewById(R.id.tvProductPrice)).setText(formatMoney(item.getPrice()));
            ((TextView) v.findViewById(R.id.tvQuantity)).setText("x" + item.getQuantity());
            containerProducts.addView(v);
        }
    }

    private String formatMoney(long amount) { return String.format(Locale.getDefault(), "%,dđ", amount); }
}
