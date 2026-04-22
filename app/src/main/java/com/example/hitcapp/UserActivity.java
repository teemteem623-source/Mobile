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

        // --- XỬ LÝ SAFE AREA ---
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBarCard);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (topBar != null) {
                    topBar.setPadding(0, systemBars.top + (int)(20 * getResources().getDisplayMetrics().density), 0, 10);
                }
                if (bottomNavigation != null) {
                    bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
                }
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // --- GẮN LINK MENU ---
        setupMenuLinks();

        // --- TOP BAR BUTTONS ---
        findViewById(R.id.imgWallet).setOnClickListener(v -> 
            Toast.makeText(this, "Tính năng ví đang phát triển", Toast.LENGTH_SHORT).show());
            
        findViewById(R.id.imgSettings).setOnClickListener(v -> 
            Toast.makeText(this, "Cài đặt chung", Toast.LENGTH_SHORT).show());

        // --- THANH ĐIỀU HƯỚNG DƯỚI ---
        setupBottomNavigation();
    }

    private void setupMenuLinks() {
        View menuProfile = findViewById(R.id.menuProfile);
        if (menuProfile != null) {
            menuProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        View menuOrders = findViewById(R.id.menuOrders);
        if (menuOrders != null) {
            menuOrders.setOnClickListener(v -> startActivity(new Intent(this, OderActivity.class)));
        }

        View menuAddAccount = findViewById(R.id.menuAddAccount);
        if (menuAddAccount != null) {
            menuAddAccount.setOnClickListener(v -> startActivity(new Intent(this, AddUserActivity.class)));
        }

        View menuLogout = findViewById(R.id.menuLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_products) {
                    startActivity(new Intent(this, ProductActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(this, NoticeActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_profile) {
                    return true;
                }
                return false;
            });
        }
    }
}
