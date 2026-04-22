package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.material.button.MaterialButton;

public class DetailActivity extends AppCompatActivity {

    private int quantity = 1;
    private TextView tvQuantity, tvName, tvPrice;
    private ImageView imgMain;

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

        // --- ÁNH XẠ ---
        imgMain = findViewById(R.id.imgMainProduct);
        tvName = findViewById(R.id.tvDetailName);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvQuantity = findViewById(R.id.tvQuantity);

        // --- NHẬN DỮ LIỆU TỪ INTENT ---
        Intent intent = getIntent();
        if (intent != null) {
            String name = intent.getStringExtra("PRODUCT_NAME");
            String price = intent.getStringExtra("PRODUCT_PRICE");
            int imageRes = intent.getIntExtra("PRODUCT_IMAGE", R.drawable.phone_mockup);

            if (name != null) tvName.setText(name);
            if (price != null) tvPrice.setText(price);
            imgMain.setImageResource(imageRes);
        }

        // --- QUANTITY LOGIC ---
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
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageView imgHeaderCart = findViewById(R.id.imgHeaderCart);
        if (imgHeaderCart != null) {
            imgHeaderCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        // --- BOTTOM ACTION LOGIC ---
        MaterialButton btnAddToCart = findViewById(R.id.btnAddToCart);
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                Toast.makeText(this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DetailActivity.this, CartActivity.class));
            });
        }

        MaterialButton btnBuyNow = findViewById(R.id.btnBuyNow);
        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                Intent payIntent = new Intent(DetailActivity.this, PaymentActivity.class);
                // Truyền dữ liệu sang trang thanh toán nếu cần
                startActivity(payIntent);
            });
        }

        setupRelatedProducts();
    }

    private void setupRelatedProducts() {
        LinearLayout layout = findViewById(R.id.layoutRelatedProducts);
        if (layout != null) {
            int[] images = {R.drawable.iphone14, R.drawable.samsungs24, R.drawable.samsunga54, R.drawable.xiaomi14, R.drawable.oppofindx7, R.drawable.opporeno11};
            String[] names = {"iPhone 14", "Samsung S24", "Samsung A54", "Xiaomi 14", "Oppo Find X7", "Oppo Reno 11"};
            String[] prices = {"16.490.000đ", "25.490.000đ", "8.990.000đ", "19.990.000đ", "18.500.000đ", "10.990.000đ"};

            for (int i = 0; i < layout.getChildCount() && i < images.length; i++) {
                View child = layout.getChildAt(i);
                ImageView img = child.findViewById(R.id.imgRelated);
                TextView tvRName = child.findViewById(R.id.tvRelatedName);
                TextView tvRPrice = child.findViewById(R.id.tvRelatedPrice);

                if (img != null) img.setImageResource(images[i]);
                if (tvRName != null) tvRName.setText(names[i]);
                if (tvRPrice != null) tvRPrice.setText(prices[i]);

                final int index = i;
                child.setOnClickListener(v -> {
                    Intent intent = new Intent(DetailActivity.this, DetailActivity.class);
                    intent.putExtra("PRODUCT_NAME", names[index]);
                    intent.putExtra("PRODUCT_PRICE", prices[index]);
                    intent.putExtra("PRODUCT_IMAGE", images[index]);
                    startActivity(intent);
                    finish();
                });
            }
        }
    }
}
