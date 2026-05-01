package com.example.hitcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AddUserActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText edtIdentity, edtPassword;
    private MaterialButton btnAddAccount, btnGoogleLogin;
    private ImageView btnBack;
    
    private SharedPreferences sharedPreferences;
    private List<UserAccount> accountList;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_user);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("MultiAccountPrefs", MODE_PRIVATE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.my_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupInsets();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnAddAccount != null) {
            btnAddAccount.setOnClickListener(v -> performAddAccount());
        }

        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        }
        
        loadCurrentAccountList();

        String emailExtra = getIntent().getStringExtra("email");
        if (!TextUtils.isEmpty(emailExtra)) {
            edtIdentity.setText(emailExtra);
            edtPassword.requestFocus();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        edtIdentity = findViewById(R.id.edtAccountIdentity);
        edtPassword = findViewById(R.id.edtAccountPassword);
    }

    private void loadCurrentAccountList() {
        String json = sharedPreferences.getString("accounts", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<UserAccount>>() {}.getType();
        accountList = gson.fromJson(json, type);
        if (accountList == null) accountList = new ArrayList<>();
    }

    private void saveAccounts() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(accountList);
        editor.putString("accounts", json);
        editor.apply();
    }

    private void performAddAccount() {
        String email = edtIdentity.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddAccount.setEnabled(false);
        btnAddAccount.setText("ĐANG KIỂM TRA...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnAddAccount.setEnabled(true);
                    btnAddAccount.setText("ĐĂNG NHẬP");
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        saveAndGo(mAuth.getCurrentUser(), "password");
                    } else {
                        Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                saveAndGo(mAuth.getCurrentUser(), "google.com");
            } else {
                Toast.makeText(this, "Xác thực Firebase thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAndGo(FirebaseUser user, String provider) {
        if (user != null) {
            boolean exists = false;
            for (UserAccount acc : accountList) {
                if (acc.uid.equals(user.getUid())) {
                    acc.provider = provider; 
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                accountList.add(new UserAccount(user.getUid(), user.getEmail(), provider));
                Toast.makeText(this, "Đã thêm tài khoản mới vào danh sách", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tài khoản đã có trong danh sách", Toast.LENGTH_SHORT).show();
            }
            saveAccounts();
            
            Toast.makeText(this, "Đang chuyển đến trang chủ...", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                
                // Đẩy giao diện khi bàn phím hiện (Dùng giá trị lớn nhất giữa systemBar bottom và bàn phím)
                int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
                
                return insets;
            });
        }
    }

    public static class UserAccount {
        public String uid;
        public String email;
        public String provider;
        public UserAccount(String uid, String email, String provider) {
            this.uid = uid;
            this.email = email;
            this.provider = provider;
        }
    }
}
