package com.example.hitcapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    private TextInputEditText edtIdentity, edtPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Safe Area
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ
        edtIdentity = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Nhận dữ liệu từ trang đăng ký (nếu có)
        String registeredUser = getIntent().getStringExtra("REGISTERED_USER");
        if (registeredUser != null) {
            edtIdentity.setText(registeredUser);
            Toast.makeText(this, "Vui lòng nhập mật khẩu để đăng nhập", Toast.LENGTH_SHORT).show();
        }

        // ===== ĐĂNG NHẬP =====
        btnLogin.setOnClickListener(v -> {
            String identity = edtIdentity.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (identity.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tìm kiếm tài khoản trong bộ nhớ
            AccountStorage.User user = AccountStorage.findUser(this, identity);

            if (user != null && user.password.equals(password)) {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
            }
        });

        // ===== ĐĂNG KÝ =====
        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        // ===== SOCIAL =====
        findViewById(R.id.imageView2).setOnClickListener(v -> openLink("https://www.facebook.com"));
        findViewById(R.id.imageView3).setOnClickListener(v -> openLink("https://www.google.com"));
    }

    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
