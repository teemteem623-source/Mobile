package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.auth.FirebaseAuth;

public class VerifyAccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnVerify;
    private ImageView btnBack;
    private String targetEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_account);

        mAuth = FirebaseAuth.getInstance();
        targetEmail = getIntent().getStringExtra("email");

        initViews();
        setupInsets();

        if (TextUtils.isEmpty(targetEmail)) {
            finish();
            return;
        }

        edtEmail.setText(targetEmail);

        btnBack.setOnClickListener(v -> finish());
        btnVerify.setOnClickListener(v -> performVerification());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnVerify = findViewById(R.id.btnVerify);
        edtEmail = findViewById(R.id.edtVerifyEmail);
        edtPassword = findViewById(R.id.edtVerifyPassword);
    }

    private void performVerification() {
        String password = edtPassword.getText().toString().trim();

        if (password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnVerify.setEnabled(false);
        btnVerify.setText("ĐANG XÁC MINH...");

        mAuth.signInWithEmailAndPassword(targetEmail, password)
                .addOnCompleteListener(this, task -> {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("XÁC MINH TÀI KHOẢN");
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Xác minh thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBar);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }
}
