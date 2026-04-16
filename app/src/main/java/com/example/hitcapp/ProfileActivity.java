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
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtPhone, edtEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Xử lý vùng an toàn
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- ÁNH XẠ ---
        edtName = findViewById(R.id.edtProfileName);
        edtPhone = findViewById(R.id.edtProfilePhone);
        edtEmail = findViewById(R.id.edtProfileEmail);
        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnUpdate = findViewById(R.id.btnUpdateProfile);
        TextView tvChangePassword = findViewById(R.id.tvChangePassword);

        // --- LOGIC ---
        
        // Quay lại trang trước
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Chuyển đến trang Đổi mật khẩu
        if (tvChangePassword != null) {
            tvChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
                startActivity(intent);
            });
        }

        // Cập nhật thông tin cá nhân
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                String name = edtName.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();

                if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                } else {
                    // Logic cập nhật thực tế sẽ ở đây
                    Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
