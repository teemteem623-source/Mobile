package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText edtIdentity, edtPassword;
    private MaterialButton btnLogin, btnGoogleLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore mFirestore;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mFirestore = FirebaseFirestore.getInstance();

        initViews();
        handleIntent(getIntent());
    }

    private void initViews() {
        edtIdentity = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        // XỬ LÝ ĐẨY GIAO DIỆN KHI HIỆN BÀN PHÍM
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            
            // Nếu bàn phím hiện lên, dùng chiều cao bàn phím, ngược lại dùng chiều cao thanh điều hướng
            int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
            
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
            return insets;
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.my_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("REGISTERED_EMAIL")) {
            String email = intent.getStringExtra("REGISTERED_EMAIL");
            edtIdentity.setText(email);
        }
    }

    private void loginWithEmail() {
        String email = edtIdentity.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("ĐANG ĐĂNG NHẬP...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        syncUserAndGo(mAuth.getCurrentUser());
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("ĐĂNG NHẬP");
                        Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                syncUserAndGo(mAuth.getCurrentUser());
            } else {
                Toast.makeText(this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncUserAndGo(FirebaseUser user) {
        if (user == null) return;

        Toast.makeText(MainActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
        finish();

        String uid = user.getUid();
        mDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> userMap = new HashMap<>();
                    String name = user.getDisplayName();
                    if (TextUtils.isEmpty(name)) name = "Người dùng";

                    userMap.put("uid", uid);
                    userMap.put("email", user.getEmail());
                    userMap.put("username", name);
                    userMap.put("fullname", name);
                    userMap.put("totalSpent", 0);
                    userMap.put("phone", "");
                    userMap.put("memberRank", "Thành viên Đồng");
                    userMap.put("avatarUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

                    mDatabase.child(uid).setValue(userMap);
                    mFirestore.collection("users").document(uid).set(userMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DB_ERROR", "Ghi CSDL thất bại: " + error.getMessage());
            }
        });
    }
}
