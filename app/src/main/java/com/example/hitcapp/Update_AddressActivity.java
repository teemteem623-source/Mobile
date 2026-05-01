package com.example.hitcapp;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Update_AddressActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtPhone, edtCity, edtWard, edtDetail;
    private String userId;
    private AddressActivity.AddressItem currentItem;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_address);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();

        currentItem = (AddressActivity.AddressItem) getIntent().getSerializableExtra("ADDRESS_ITEM");
        if (currentItem != null) {
            edtName.setText(currentItem.name);
            edtPhone.setText(currentItem.phone);
            
            // Tách chuỗi địa chỉ cũ để hiển thị lên 3 ô
            String fullAddress = currentItem.address;
            if (fullAddress != null && fullAddress.contains(", ")) {
                String[] parts = fullAddress.split(", ");
                if (parts.length >= 3) {
                    edtDetail.setText(parts[0]);
                    edtWard.setText(parts[1]);
                    edtCity.setText(parts[2]);
                } else {
                    edtDetail.setText(fullAddress);
                }
            } else {
                edtDetail.setText(fullAddress);
            }
        }
    }

    private void initViews() {
        edtName = findViewById(R.id.edtName);
        edtPhone = findViewById(R.id.edtPhone);
        edtCity = findViewById(R.id.edtCity);
        edtWard = findViewById(R.id.edtWard);
        edtDetail = findViewById(R.id.edtDetail);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        MaterialButton btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(v -> updateAddress());
    }

    private void updateAddress() {
        if (currentItem == null || currentItem.id == null) {
            Toast.makeText(this, "Không tìm thấy thông tin địa chỉ để cập nhật!", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String city = edtCity.getText().toString().trim();
        String ward = edtWard.getText().toString().trim();
        String detail = edtDetail.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || city.isEmpty() || ward.isEmpty() || detail.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ghép lại thành chuỗi địa chỉ đầy đủ như Add_AddressActivity
        String fullAddress = detail + ", " + ward + ", " + city;

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullname", name);
        updates.put("phone", phone);
        updates.put("address", fullAddress);
        updates.put("userId", userId);

        db.collection("address").document(currentItem.id).update(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Cập nhật địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Lỗi cập nhật: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();
        if (v != null && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) && 
            v instanceof android.widget.EditText && !v.getClass().getName().startsWith("android.webkit.")) {
            int[] scrcoords = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];
            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom()) {
                hideKeyboard(v);
                v.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
