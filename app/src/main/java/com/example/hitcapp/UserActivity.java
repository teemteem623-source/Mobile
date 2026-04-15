package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);

        // Xử lý Safe Area (Insets)
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBarCard);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (topBar != null) topBar.setPadding(0, systemBars.top, 0, 0);
                if (bottomNavigation != null) bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }

        // --- XỬ LÝ CÁC NÚT TRÊN TOP BAR ---
        findViewById(R.id.imgWallet).setOnClickListener(v -> 
            Toast.makeText(this, "Mở Ví tiền của bạn", Toast.LENGTH_SHORT).show());
            
        findViewById(R.id.imgSettings).setOnClickListener(v -> 
            Toast.makeText(this, "Cài đặt hệ thống", Toast.LENGTH_SHORT).show());

        // --- XỬ LÝ MENU CÀI ĐẶT ---
        findViewById(R.id.menuProfile).setOnClickListener(v -> 
            Toast.makeText(this, "Xem Hồ sơ cá nhân", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuOrders).setOnClickListener(v -> 
            Toast.makeText(this, "Xem Lịch sử đơn hàng", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuAddAccount).setOnClickListener(v -> 
            Toast.makeText(this, "Thêm tài khoản mới", Toast.LENGTH_SHORT).show());

        findViewById(R.id.menuLogout).setOnClickListener(v -> {
            Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();
            // Thực hiện chuyển về màn hình đăng nhập (ví dụ MainActivity hoặc LoginActivity)
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // --- THANH ĐIỀU HƯỚNG DƯỚI ---
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile); // Đánh dấu đang ở mục Hồ sơ
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_products) {
                    startActivity(new Intent(this, ProductActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(this, NoticeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    return true; // Đang ở đây rồi
                }
                return false;
            });
        }
    }
}
