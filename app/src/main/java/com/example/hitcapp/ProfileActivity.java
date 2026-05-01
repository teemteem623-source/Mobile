package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADDRESS = 1001;

    private TextInputEditText edtName, edtPhone, edtEmail, edtAddress;
    private TextView tvMembership;
    private MaterialButton btnUpdate;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = currentUser.getUid();

        initViews();
        setupWindowInsets();
        fetchProfileData();
        calculateAndUpdateMembership(); // Khởi chạy tính toán cấp bậc
    }

    private void initViews() {
        edtName = findViewById(R.id.edtProfileName);
        edtPhone = findViewById(R.id.edtProfilePhone);
        edtEmail = findViewById(R.id.edtProfileEmail);
        edtAddress = findViewById(R.id.edtProfileAddress);
        tvMembership = findViewById(R.id.tvProfileMembership);
        btnUpdate = findViewById(R.id.btnUpdateProfile);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        edtAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADDRESS);
        });

        btnUpdate.setOnClickListener(v -> pushProfileToFirestore());

        findViewById(R.id.tvChangePassword).setOnClickListener(v -> 
            startActivity(new Intent(this, ChangePasswordActivity.class)));
    }

    private void fetchProfileData() {
        mFirestore.collection("profiles").document(userId).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) return;
            if (documentSnapshot != null && documentSnapshot.exists()) {
                edtName.setText(documentSnapshot.getString("fullname"));
                edtPhone.setText(documentSnapshot.getString("phone"));
                edtEmail.setText(documentSnapshot.getString("email"));
                
                String rank = documentSnapshot.getString("memberRank");
                if (rank != null) tvMembership.setText(rank);

                String addr = documentSnapshot.getString("address");
                if (addr != null && !addr.isEmpty()) {
                    edtAddress.setText(addr);
                }
            }
        });
    }

    private void calculateAndUpdateMembership() {
        mFirestore.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        long totalSpent = 0;
                        for (QueryDocumentSnapshot doc : value) {
                            String status = doc.getString("status");
                            // Không tính đơn hàng đã hủy
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
        
        // Cập nhật lại vào bảng profiles để đồng bộ
        Map<String, Object> update = new HashMap<>();
        update.put("memberRank", level);
        mFirestore.collection("profiles").document(userId).update(update);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADDRESS && resultCode == RESULT_OK && data != null) {
            AddressActivity.AddressItem item = (AddressActivity.AddressItem) data.getSerializableExtra("SELECTED_ADDRESS_OBJ");
            if (item != null) {
                edtAddress.setText(item.address);
            }
        }
    }

    private void pushProfileToFirestore() {
        String fullname = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String memberRank = tvMembership.getText().toString().trim();

        if (fullname.isEmpty()) {
            edtName.setError("Vui lòng nhập họ và tên");
            return;
        }

        btnUpdate.setEnabled(false);
        btnUpdate.setText("ĐANG CẬP NHẬT...");

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("email", email);
        profileData.put("fullname", fullname);
        profileData.put("memberRank", memberRank);
        profileData.put("phone", phone);
        profileData.put("userId", userId);
        profileData.put("address", address); 

        mFirestore.collection("profiles").document(userId)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("CẬP NHẬT THÔNG TIN");
                    Toast.makeText(ProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("CẬP NHẬT THÔNG TIN");
                    Toast.makeText(ProfileActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
                return insets;
            });
        }
    }
}
