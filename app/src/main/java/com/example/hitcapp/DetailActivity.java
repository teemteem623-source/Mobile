package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private static final String TAG = "DetailActivity";
    private int quantity = 1;
    private TextView tvQuantity, tvName, tvPrice, tvOriginalPrice, tvSaleBadge, tvDesc, tvCartBadge;
    private ImageView imgMain;
    private LinearLayout layoutRelated;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        initViews();
        setupInsets();
        handleIntentData();
        setupQuantityLogic();
        setupTopBar();
        setupBottomActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }

    private void initViews() {
        imgMain = findViewById(R.id.imgMainProduct);
        tvName = findViewById(R.id.tvDetailName);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvOriginalPrice = findViewById(R.id.tvDetailOriginalPrice);
        tvSaleBadge = findViewById(R.id.tvDetailSaleBadge);
        tvDesc = findViewById(R.id.tvDetailDesc);
        tvQuantity = findViewById(R.id.tvQuantity);
        layoutRelated = findViewById(R.id.layoutRelatedProducts);
        tvCartBadge = findViewById(R.id.tvCartBadge);
    }

    private void updateCartBadge() {
        if (tvCartBadge == null || auth.getCurrentUser() == null) return;
        
        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > 0) {
                        tvCartBadge.setVisibility(View.VISIBLE);
                        tvCartBadge.setText(String.valueOf(count));
                    } else {
                        tvCartBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        View bottomBar = findViewById(R.id.bottomNavigationDetail);
        View topBar = findViewById(R.id.topBar);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                
                if (topBar != null) {
                    topBar.setPadding(topBar.getPaddingLeft(), systemBars.top, topBar.getPaddingRight(), topBar.getPaddingBottom());
                }

                if (bottomBar != null) {
                    bottomBar.setPadding(
                        bottomBar.getPaddingLeft(),
                        bottomBar.getPaddingTop(),
                        bottomBar.getPaddingRight(),
                        systemBars.bottom 
                    );
                }
                return insets;
            });
        }
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            productId = intent.getStringExtra("PRODUCT_ID");
            
            if (productId != null) {
                fetchProductDetails(productId);
            } else {
                // Xử lý dữ liệu fallback nếu không có productId
                String name = intent.getStringExtra("PRODUCT_NAME");
                long price = intent.getLongExtra("PRODUCT_PRICE", 0);
                long originalPrice = intent.getLongExtra("PRODUCT_ORIGINAL_PRICE", price);
                String imageUrl = intent.getStringExtra("PRODUCT_IMAGE_URL");
                String desc = intent.getStringExtra("PRODUCT_DESC");

                tvName.setText(name);
                DecimalFormat formatter = new DecimalFormat("#,###");
                tvPrice.setText(formatter.format(price) + "đ");
                tvDesc.setText(desc != null ? desc : "Mô tả đang cập nhật...");
                Glide.with(this).load(imageUrl).placeholder(R.drawable.phone_mockup).into(imgMain);
                
                currentProduct = new Product("temp", name, price, originalPrice, 0, imageUrl, "iphone", desc, false, false, true);
            }
        }
    }

    private void fetchProductDetails(String id) {
        db.collection("products").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentProduct = documentSnapshot.toObject(Product.class);
                    if (currentProduct != null) {
                        currentProduct.setId(documentSnapshot.getId());
                        bindProductData(currentProduct);
                        fetchRelatedProducts(currentProduct.getCategory());
                    }
                });
    }

    private void bindProductData(Product product) {
        tvName.setText(product.getName());
        DecimalFormat formatter = new DecimalFormat("#,###");
        tvPrice.setText(formatter.format(product.getPrice()) + "đ");
        
        if (product.getDiscountPercent() > 0) {
            tvSaleBadge.setVisibility(View.VISIBLE);
            tvSaleBadge.setText("-" + product.getDiscountPercent() + "%");
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(formatter.format(product.getOriginalPrice()) + "đ");
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvSaleBadge.setVisibility(View.GONE);
            tvOriginalPrice.setVisibility(View.GONE);
        }

        tvDesc.setText(product.getDescription() != null ? product.getDescription() : "Mô tả đang cập nhật...");
        Glide.with(this).load(product.getImageUrl()).placeholder(R.drawable.phone_mockup).into(imgMain);
    }

    private void setupQuantityLogic() {
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });
    }

    private void setupTopBar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.layoutCartTop).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
    }

    private void setupBottomActions() {
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            if (currentProduct == null || auth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = auth.getCurrentUser().getUid();
            
            // Kiểm tra sản phẩm đã có trong giỏ hàng chưa
            db.collection("carts")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("productId", currentProduct.getId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Cập nhật số lượng nếu đã có
                            String cartDocId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            int oldQty = queryDocumentSnapshots.getDocuments().get(0).getLong("quantity").intValue();
                            db.collection("carts").document(cartDocId).update("quantity", oldQty + quantity);
                        } else {
                            // Thêm mới nếu chưa có
                            CartItem cartItem = new CartItem(
                                    userId,
                                    currentProduct.getId(),
                                    currentProduct.getName(),
                                    currentProduct.getCategory(),
                                    currentProduct.getPrice(),
                                    currentProduct.getOriginalPrice(),
                                    quantity,
                                    currentProduct.getImageUrl()
                            );
                            db.collection("carts").add(cartItem);
                        }
                        Toast.makeText(this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                        updateCartBadge();
                    });
        });

        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            if (currentProduct == null) return;
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra("PRODUCT_ID", currentProduct.getId());
            intent.putExtra("QUANTITY", quantity);
            startActivity(intent);
        });
    }

    private void fetchRelatedProducts(String category) {
        db.collection("products")
                .whereEqualTo("category", category)
                .limit(8)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> products = queryDocumentSnapshots.toObjects(Product.class);
                    displayRelatedProducts(products);
                });
    }

    private void displayRelatedProducts(List<Product> products) {
        layoutRelated.removeAllViews();
        for (Product p : products) {
            if (p != null && (productId == null || !documentIdEquals(p, productId))) {
                addRelatedProductView(p);
            }
        }
    }
    
    private boolean documentIdEquals(Product p, String id) {
        return p.getId() != null && p.getId().equals(id);
    }

    private void addRelatedProductView(Product product) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_related_small, layoutRelated, false);
        ImageView img = view.findViewById(R.id.imgRelated);
        TextView name = view.findViewById(R.id.tvRelatedName);
        TextView price = view.findViewById(R.id.tvRelatedPrice);

        name.setText(product.getName());
        DecimalFormat formatter = new DecimalFormat("#,###");
        price.setText(formatter.format(product.getPrice()) + "đ");
        Glide.with(this).load(product.getImageUrl()).placeholder(R.drawable.phone_mockup).into(img);
        
        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            startActivity(intent);
            finish();
        });
        layoutRelated.addView(view);
    }
}
