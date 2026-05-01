package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class ProcessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_process);

        // --- XỬ LÝ SAFE AREA ---
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBar);
        
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (topBar != null) {
                    topBar.setPadding(0, systemBars.top, 0, 0);
                }
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // --- ÁNH XẠ ---
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        MaterialCardView cardProductInfo = findViewById(R.id.cardProductInfo);

        // --- LOGIC ---
        
        // Nút quay lại
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Nhấn vào khung sản phẩm truyền dữ liệu sang trang Chi tiết
        if (cardProductInfo != null) {
            cardProductInfo.setOnClickListener(v -> {
                Intent intent = new Intent(ProcessActivity.this, DetailActivity.class);
                intent.putExtra("PRODUCT_NAME", "iPhone 15 Pro");
                intent.putExtra("PRODUCT_PRICE", "28.990.000đ");
                intent.putExtra("PRODUCT_IMAGE", R.drawable.iphone15pro);
                startActivity(intent);
            });
        }

        // Định dạng số điện thoại (Hiện 4 số đầu, còn lại là *)
        if (tvCustomerPhone != null) {
            String fullPhone = "0901234567"; // Giả lập dữ liệu thật
            if (fullPhone.length() >= 4) {
                String maskedPhone = fullPhone.substring(0, 4) + "******";
                tvCustomerPhone.setText(maskedPhone);
            }
        }
    }
}
