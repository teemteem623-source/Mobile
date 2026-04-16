package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class DetailActivity extends AppCompatActivity {

    private int quantity = 1;
    private TextView tvQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        // Xử lý Safe Area
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- QUANTITY LOGIC ---
        tvQuantity = findViewById(R.id.tvQuantity);
        TextView btnPlus = findViewById(R.id.btnPlus);
        TextView btnMinus = findViewById(R.id.btnMinus);

        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                quantity++;
                tvQuantity.setText(String.valueOf(quantity));
            });
        }

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    tvQuantity.setText(String.valueOf(quantity));
                }
            });
        }

        // --- TOP BAR LOGIC ---
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish()); // Quay lại trang cũ
        }

        ImageView imgHeaderCart = findViewById(R.id.imgHeaderCart);
        if (imgHeaderCart != null) {
            imgHeaderCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        // --- BOTTOM ACTION LOGIC ---
        MaterialButton btnAddToCart = findViewById(R.id.btnAddToCart);
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                Toast.makeText(this, "Đã thêm " + quantity + " sản phẩm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DetailActivity.this, CartActivity.class));
            });
        }

        MaterialButton btnBuyNow = findViewById(R.id.btnBuyNow);
        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                startActivity(new Intent(DetailActivity.this, PaymentActivity.class));
            });
        }

        // --- RELATED PRODUCTS LOGIC ---
        setupRelatedProducts();
    }

    private void setupRelatedProducts() {
        View relatedContainer = findViewById(R.id.layoutRelatedProducts);
        if (relatedContainer instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout layout = (android.widget.LinearLayout) relatedContainer;
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                child.setOnClickListener(v -> {
                    // Load lại chính Activity để xem sản phẩm mới
                    Intent intent = new Intent(DetailActivity.this, DetailActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
        }
    }
}
