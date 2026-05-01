package com.example.hitcapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AccountManagementActivity extends AppCompatActivity {

    private RecyclerView rvAccounts;
    private LinearLayout layoutEmpty;
    private MaterialButton btnAddAccount;
    private ImageView btnBack;
    private FirebaseAuth mAuth;
    private List<AddUserActivity.UserAccount> accountList;
    private List<AddUserActivity.UserAccount> displayList;
    private AccountAdapter adapter;
    private SharedPreferences sharedPreferences;

    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_management);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("MultiAccountPrefs", MODE_PRIVATE);

        initViews();
        setupInsets();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());
        btnAddAccount.setOnClickListener(v -> {
            Toast.makeText(this, "Đang chuyển sang trang thêm tài khoản...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AddUserActivity.class);
            startActivity(intent);
        });

        loadAccounts();
    }

    private void initViews() {
        rvAccounts = findViewById(R.id.rvAccounts);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnAddAccount = findViewById(R.id.btnAddAccountFooter);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        accountList = new ArrayList<>();
        displayList = new ArrayList<>();
        adapter = new AccountAdapter(displayList, new AccountAdapter.OnAccountActionListener() {
            @Override
            public void onAccountClick(AddUserActivity.UserAccount account) {
                switchAccount(account);
            }

            @Override
            public void onAccountDelete(AddUserActivity.UserAccount account) {
                confirmDeleteLink(account);
            }
        });
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(adapter);
    }

    private void loadAccounts() {
        String json = sharedPreferences.getString("accounts", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<AddUserActivity.UserAccount>>() {}.getType();
        
        accountList.clear();
        List<AddUserActivity.UserAccount> savedAccounts = gson.fromJson(json, type);
        if (savedAccounts != null) {
            for (AddUserActivity.UserAccount acc : savedAccounts) {
                if (acc.provider == null) acc.provider = "password"; 
                accountList.add(acc);
            }
        }

        FirebaseUser current = mAuth.getCurrentUser();
        if (current != null) {
            String provider = "password";
            for (UserInfo userInfo : current.getProviderData()) {
                if (userInfo.getProviderId().equals("google.com")) {
                    provider = "google.com";
                    break;
                }
            }

            boolean exists = false;
            for (AddUserActivity.UserAccount acc : accountList) {
                if (acc.uid.equals(current.getUid())) {
                    acc.provider = provider;
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                accountList.add(new AddUserActivity.UserAccount(current.getUid(), current.getEmail(), provider));
            }
            saveAccounts();
        }

        updateDisplayList();
    }

    private void updateDisplayList() {
        displayList.clear();
        FirebaseUser current = mAuth.getCurrentUser();
        String currentUid = (current != null) ? current.getUid() : "";

        for (AddUserActivity.UserAccount acc : accountList) {
            if (!acc.uid.equals(currentUid)) {
                displayList.add(acc);
            }
        }

        if (displayList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvAccounts.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvAccounts.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void saveAccounts() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(accountList);
        editor.putString("accounts", json);
        editor.apply();
    }

    private void confirmDeleteLink(AddUserActivity.UserAccount account) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa liên kết")
                .setMessage("Bạn có chắc chắn muốn xóa liên kết với tài khoản " + account.email + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    accountList.remove(account);
                    saveAccounts();
                    updateDisplayList();
                    Toast.makeText(this, "Đã xóa liên kết tài khoản thành công!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void switchAccount(AddUserActivity.UserAccount account) {
        if ("google.com".equals(account.provider)) {
            Toast.makeText(this, "Đang chuyển sang tài khoản Google.", Toast.LENGTH_LONG).show();
            signInWithGoogleTargeted(account.email);
        } else {
            Toast.makeText(this, "Để chuyển tài khoản vui lòng xác minh!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, VerifyAccountActivity.class);
            intent.putExtra("email", account.email);
            startActivity(intent);
        }
    }

    private void signInWithGoogleTargeted(String email) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.my_web_client_id))
                .requestEmail()
                .setAccountName(email) 
                .build();
        
        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
        client.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = client.getSignInIntent();
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
                Toast.makeText(this, "Không thể chuyển tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Chuyển tài khoản thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Lỗi khi chuyển tài khoản Google", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBar);
        View footerBtn = findViewById(R.id.btnAddAccountFooter);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Dính sát cạnh trái phải, không set padding bottom ở đây để nút dưới cùng xử lý
                v.setPadding(systemBars.left, 0, systemBars.right, 0); 
                return insets;
            });
        }
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Dính sát thanh trạng thái (StatusBar)
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        if (footerBtn != null) {
            ViewCompat.setOnApplyWindowInsetsListener(footerBtn, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                // Khoảng cách 20dp từ mép dưới hệ thống (NavigationBar)
                lp.bottomMargin = systemBars.bottom + (int) (20 * getResources().getDisplayMetrics().density);
                v.setLayoutParams(lp);
                return insets;
            });
        }
    }

    private static class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {
        private final List<AddUserActivity.UserAccount> accounts;
        private final OnAccountActionListener listener;

        public AccountAdapter(List<AddUserActivity.UserAccount> accounts, OnAccountActionListener listener) {
            this.accounts = accounts;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AddUserActivity.UserAccount acc = accounts.get(position);
            holder.tvEmail.setText(acc.email);
            
            boolean isGoogle = "google.com".equals(acc.provider);
            holder.tvType.setText(isGoogle ? "Tài khoản Google" : "Tài khoản Email");
            holder.imgType.setImageResource(isGoogle ? R.drawable.goole : R.drawable.people);
            
            // XỬ LÝ MÀU ICON: Google giữ nguyên màu, Email dùng màu xanh chủ đạo
            if (!isGoogle) {
                holder.imgType.setColorFilter(Color.parseColor("#1E40AF"));
            } else {
                holder.imgType.clearColorFilter();
            }

            holder.itemView.setOnClickListener(v -> listener.onAccountClick(acc));
            holder.btnDelete.setOnClickListener(v -> listener.onAccountDelete(acc));
        }

        @Override
        public int getItemCount() { return accounts.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmail, tvType;
            ImageView imgType, btnDelete;
            ViewHolder(View v) {
                super(v);
                tvEmail = v.findViewById(R.id.tvAccountEmail);
                tvType = v.findViewById(R.id.tvAccountTypeLabel);
                imgType = v.findViewById(R.id.imgAccountType);
                btnDelete = v.findViewById(R.id.btnDeleteAccount);
            }
        }
        interface OnAccountActionListener { 
            void onAccountClick(AddUserActivity.UserAccount account); 
            void onAccountDelete(AddUserActivity.UserAccount account);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts();
    }
}
