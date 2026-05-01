package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hitcapp.adapters.ProductAdapter;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private List<CartItem> cartItemList = new ArrayList<>();
    
    private LinearLayout containerCartItems;
    private CheckBox chkSelectAll;
    private TextView tvTotalPrice, tvSavingPrice;
    private LinearLayout layoutEmptyCart, layoutCartContent;
    private RecyclerView rvExploreMore;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ProductAdapter exploreAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupInsets();
        setupExploreMore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCartItems();
    }

    private void initViews() {
        containerCartItems = findViewById(R.id.containerCartItems);
        chkSelectAll = findViewById(R.id.chkSelectAll);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvSavingPrice = findViewById(R.id.tvSavingPrice);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        layoutCartContent = findViewById(R.id.layoutCartContent);
        rvExploreMore = findViewById(R.id.rvExploreMore);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // SỬA TẠI ĐÂY: Đổi MainActivity thành HomeActivity để quay về trang chủ
        findViewById(R.id.btnShopNow).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        if (chkSelectAll != null) {
            chkSelectAll.setChecked(true);
            chkSelectAll.setOnClickListener(v -> {
                boolean isChecked = chkSelectAll.isChecked();
                for (CartItem item : cartItemList) {
                    item.setSelected(isChecked);
                    updateItemSelectionInFirestore(item);
                }
                renderCartItems();
                calculateTotalPrice();
            });
        }

        findViewById(R.id.btnDeleteAll).setOnClickListener(v -> {
            deleteAllCartItems();
        });

        MaterialButton btnPayment = findViewById(R.id.btnPayment);
        if (btnPayment != null) {
            btnPayment.setOnClickListener(v -> {
                ArrayList<CartItem> selectedItems = new ArrayList<>();
                for (CartItem item : cartItemList) {
                    if (item.isSelected()) {
                        selectedItems.add(item);
                    }
                }
                
                if (selectedItems.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn sản phẩm thanh toán", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(CartActivity.this, PaymentActivity.class);
                intent.putExtra("SELECTED_ITEMS_DATA", selectedItems);
                startActivity(intent);
            });
        }
    }

    private void fetchCartItems() {
        if (auth.getCurrentUser() == null) return;

        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cartItemList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        CartItem item = doc.toObject(CartItem.class);
                        item.setCartItemId(doc.getId());
                        cartItemList.add(item);
                    }
                    updateUIState();
                });
    }

    private void updateItemSelectionInFirestore(CartItem item) {
        db.collection("carts").document(item.getCartItemId()).update("selected", item.isSelected());
    }

    private void updateItemQuantityInFirestore(CartItem item) {
        db.collection("carts").document(item.getCartItemId()).update("quantity", item.getQuantity());
    }

    private void deleteItemFromFirestore(CartItem item) {
        db.collection("carts").document(item.getCartItemId()).delete().addOnSuccessListener(aVoid -> fetchCartItems());
    }

    private void deleteAllCartItems() {
        if (auth.getCurrentUser() == null) return;
        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                    cartItemList.clear();
                    updateUIState();
                    Toast.makeText(this, "Đã xóa tất cả sản phẩm", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                
                View topBar = findViewById(R.id.topBar);
                if (topBar != null) {
                    topBar.setPadding(topBar.getPaddingLeft(), systemBars.top, topBar.getPaddingRight(), topBar.getPaddingBottom());
                }
                
                View bottomAction = findViewById(R.id.bottomActionCard);
                if (bottomAction != null) {
                    bottomAction.setPadding(bottomAction.getPaddingLeft(), bottomAction.getPaddingTop(), bottomAction.getPaddingRight(), systemBars.bottom);
                }
                return insets;
            });
        }
    }

    private void updateUIState() {
        if (cartItemList.isEmpty()) {
            layoutEmptyCart.setVisibility(View.VISIBLE);
            layoutCartContent.setVisibility(View.GONE);
            View bottomAction = findViewById(R.id.bottomActionCard);
            if (bottomAction != null) bottomAction.setVisibility(View.GONE);
        } else {
            layoutEmptyCart.setVisibility(View.GONE);
            layoutCartContent.setVisibility(View.VISIBLE);
            View bottomAction = findViewById(R.id.bottomActionCard);
            if (bottomAction != null) bottomAction.setVisibility(View.VISIBLE);
            renderCartItems();
            calculateTotalPrice();
        }
    }

    private void renderCartItems() {
        if (containerCartItems == null) return;
        containerCartItems.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        for (CartItem item : cartItemList) {
            View view = inflater.inflate(R.layout.item_cart, containerCartItems, false);
            
            CheckBox chk = view.findViewById(R.id.chkItem);
            if (chk != null) {
                chk.setChecked(item.isSelected());
                chk.setOnClickListener(v -> {
                    item.setSelected(chk.isChecked());
                    updateItemSelectionInFirestore(item);
                    updateSelectAllStatus();
                    calculateTotalPrice();
                });
            }

            ImageView img = view.findViewById(R.id.imgProduct);
            Glide.with(this).load(item.getImageUrl()).placeholder(R.drawable.phone_mockup).into(img);

            ((TextView)view.findViewById(R.id.tvProductName)).setText(item.getName());
            ((TextView)view.findViewById(R.id.tvProductDetail)).setText(item.getDetail());
            ((TextView)view.findViewById(R.id.tvProductPrice)).setText(String.format(Locale.getDefault(), "%,dđ", item.getPrice()));
            ((TextView)view.findViewById(R.id.tvQuantity)).setText(String.valueOf(item.getQuantity()));
            
            view.findViewById(R.id.btnMinus).setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    ((TextView)view.findViewById(R.id.tvQuantity)).setText(String.valueOf(item.getQuantity()));
                    updateItemQuantityInFirestore(item);
                    calculateTotalPrice();
                }
            });
            
            view.findViewById(R.id.btnPlus).setOnClickListener(v -> {
                item.setQuantity(item.getQuantity() + 1);
                ((TextView)view.findViewById(R.id.tvQuantity)).setText(String.valueOf(item.getQuantity()));
                updateItemQuantityInFirestore(item);
                calculateTotalPrice();
            });

            view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                deleteItemFromFirestore(item);
            });

            view.setOnClickListener(v -> {
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("PRODUCT_ID", item.getProductId());
                startActivity(intent);
            });

            containerCartItems.addView(view);
        }
        updateSelectAllStatus();
    }

    private void updateSelectAllStatus() {
        if (cartItemList.isEmpty()) return;
        boolean allSelected = true;
        for (CartItem item : cartItemList) {
            if (!item.isSelected()) {
                allSelected = false;
                break;
            }
        }
        chkSelectAll.setChecked(allSelected);
    }

    private void calculateTotalPrice() {
        long total = 0;
        long saving = 0;
        for (CartItem item : cartItemList) {
            if (item.isSelected()) {
                total += (item.getPrice() * item.getQuantity());
                if (item.getOriginalPrice() > item.getPrice()) {
                    saving += (item.getOriginalPrice() - item.getPrice()) * item.getQuantity();
                }
            }
        }
        if (tvTotalPrice != null) {
            tvTotalPrice.setText(String.format(Locale.getDefault(), "%,dđ", total));
        }
        if (tvSavingPrice != null) {
            tvSavingPrice.setText(String.format(Locale.getDefault(), "Tiết kiệm %,dđ", saving));
        }
    }

    private void setupExploreMore() {
        if (rvExploreMore == null) return;

        exploreAdapter = new ProductAdapter(product -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            startActivity(intent);
        });
        
        rvExploreMore.setLayoutManager(new GridLayoutManager(this, 2));
        rvExploreMore.setAdapter(exploreAdapter);

        db.collection("products")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        products.add(p);
                    }
                    if (!products.isEmpty()) {
                        exploreAdapter.setData(products);
                    }
                });
    }
}
