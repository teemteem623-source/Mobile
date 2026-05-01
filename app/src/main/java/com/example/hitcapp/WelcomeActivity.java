package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.hitcapp.utils.FirestoreDataSeeder;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Tự động nạp dữ liệu mẫu lên Firebase Firestore (Chỉ cần chạy 1 lần)
        FirestoreDataSeeder.seedAllData(this);

        MaterialButton btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
        });
    }
}
