package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private TextView tvUserName, tvMembership;
    private ImageView imgUserAvatar;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.my_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupInsets();
        initCustomBottomNav();
        setupMenuLinks();
        
        loadUserData();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvMembership = findViewById(R.id.tvMembership);
        imgUserAvatar = findViewById(R.id.imgUserAvatar);

        findViewById(R.id.imgWallet).setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));
        findViewById(R.id.imgSettings).setOnClickListener(v -> Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        DocumentReference docRef = mFirestore.collection("profiles").document(userId);
        docRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("USER_ACTIVITY", "Listen failed.", error);
                return;
            }

            if (value != null && value.exists()) {
                String fullname = value.getString("fullname");
                String memberRank = value.getString("memberRank");
                
                if (!TextUtils.isEmpty(fullname)) {
                    tvUserName.setText(fullname);
                }
                
                if (!TextUtils.isEmpty(memberRank)) {
                    tvMembership.setText(memberRank);
                }

                calculateAndUpdateMembership();
            }
        });
    }

    private void calculateAndUpdateMembership() {
        mFirestore.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("USER_ACTIVITY", "Error fetching orders", error);
                        return;
                    }

                    if (value != null) {
                        long totalSpent = 0;
                        for (QueryDocumentSnapshot doc : value) {
                            String status = doc.getString("status");
                            if (!"Đã hủy".equals(status) && !"Cancelled".equals(status)) {
                                Long price = doc.getLong("totalPrice");
                                if (price != null) {
                                    totalSpent += price;
                                }
                            }
                        }
                        updateMembershipUI(totalSpent);
                    }
                });
    }

    private void updateMembershipUI(long totalSpent) {
        String level;
        if (totalSpent > 40000000) {
            level = "Thành viên Kim cương";
        } else if (totalSpent >= 40000000) {
            level = "Thành viên Vàng";
        } else if (totalSpent >= 20000000) {
            level = "Thành viên Bạc";
        } else {
            level = "Thành viên Đồng";
        }

        tvMembership.setText(level);
        
        Map<String, Object> update = new HashMap<>();
        update.put("memberRank", level);
        mFirestore.collection("profiles").document(userId).update(update);
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBar);
        View bottomNav = findViewById(R.id.bottomNavigationCustom);

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

        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }
    }

    private void initCustomBottomNav() {
        tabHome = findViewById(R.id.tabHome);
        tabProduct = findViewById(R.id.tabProduct);
        tabNotification = findViewById(R.id.tabNotification);
        tabProfile = findViewById(R.id.tabProfile);

        if (tabHome != null) {
            tabHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        }
        if (tabProduct != null) {
            tabProduct.setOnClickListener(v -> {
                startActivity(new Intent(this, ProductActivity.class));
                finish();
            });
        }
        if (tabNotification != null) {
            tabNotification.setOnClickListener(v -> {
                startActivity(new Intent(this, NoticeActivity.class));
                finish();
            });
        }
        
        selectTab(tabProfile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabProfile != null) selectTab(tabProfile);
    }

    private void selectTab(LinearLayout selected) {
        LinearLayout[] tabs = {tabHome, tabProduct, tabNotification, tabProfile};
        int[] iconIds = {R.id.iconHome, R.id.iconProduct, R.id.iconNotification, R.id.iconProfile};
        int[] textIds = {R.id.textHome, R.id.textProduct, R.id.textNotification, R.id.textProfile};

        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
            boolean isSelected = (tabs[i] == selected);
            ImageView icon = findViewById(iconIds[i]);
            TextView text = findViewById(textIds[i]);
            int color = isSelected ? Color.parseColor("#1E40AF") : Color.parseColor("#94A3B8");
            if (icon != null) icon.setColorFilter(color);
            if (text != null) {
                text.setTextColor(color);
                text.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void setupMenuLinks() {
        findViewById(R.id.menuProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.menuOrders).setOnClickListener(v -> startActivity(new Intent(this, OderActivity.class)));
        findViewById(R.id.menuManageAccount).setOnClickListener(v -> startActivity(new Intent(this, AccountManagementActivity.class)));
        findViewById(R.id.menuLogout).setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(UserActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(UserActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
