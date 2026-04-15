package com.example.hitcapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top,
                        systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // ===== LOGIN =====
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            EditText edtEmail = findViewById(R.id.edtEmail);
            EditText edtPassword = findViewById(R.id.edtPassword);

            String sEmail = edtEmail.getText().toString().trim();
            String sPassword = edtPassword.getText().toString().trim();

            if (sEmail.equals("Thanh") && sPassword.equals("123")) {
                Intent it = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(it);
            } else {
                Toast.makeText(
                        MainActivity.this,
                        "Email hoặc mật khẩu không đúng!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // ===== REGISTER =====
        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> {
            Intent it = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(it);
        });

        // ===== FACEBOOK ICON =====
        ImageView imgFacebook = findViewById(R.id.imageView2);
        imgFacebook.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.facebook.com"));
            startActivity(intent);
        });

        // ===== GOOGLE ICON =====
        ImageView imgGoogle = findViewById(R.id.imageView3);
        imgGoogle.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.google.com"));
            startActivity(intent);
        });
    }
}