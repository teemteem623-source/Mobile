package com.example.hitcapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddUserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_user);

        // Ánh xạ
        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnAddAccount = findViewById(R.id.btnAddAccount);
        TextInputEditText edtIdentity = findViewById(R.id.edtAccountIdentity);
        TextInputEditText edtPassword = findViewById(R.id.edtAccountPassword);

        // Xử lý Safe Area
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Nút quay lại
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Nút thêm tài khoản
        if (btnAddAccount != null) {
            btnAddAccount.setOnClickListener(v -> {
                String identity = edtIdentity.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (identity.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đã thêm tài khoản: " + identity, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
