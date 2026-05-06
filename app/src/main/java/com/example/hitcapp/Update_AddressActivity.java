package com.example.hitcapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import com.example.hitcapp.api.RegionApiService;
import com.example.hitcapp.api.RetrofitClient;
import com.example.hitcapp.models.Region;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Update_AddressActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtPhone, edtCity, edtWard, edtDetail;
    private String userId;
    private AddressActivity.AddressItem currentItem;
    private FirebaseFirestore db;
    private RegionApiService apiService;
    private List<Region.Province> provinceList = new ArrayList<>();
    private List<Region.Ward> wardList = new ArrayList<>();
    private ProgressDialog progressDialog;

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

        apiService = RetrofitClient
                .getClient("https://provinces.open-api.vn/api/")
                .create(RegionApiService.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.setCancelable(false);

        initViews();
        setupWindowInsets();
        loadProvinces();

        currentItem = (AddressActivity.AddressItem) getIntent().getSerializableExtra("ADDRESS_ITEM");
        if (currentItem != null) {
            edtName.setText(currentItem.name);
            edtPhone.setText(currentItem.phone);

            String fullAddress = currentItem.address;
            if (fullAddress != null && fullAddress.contains(", ")) {
                String[] parts = fullAddress.split(", ");
                if (parts.length >= 3) {
                    edtCity.setText(parts[parts.length - 1]);
                    edtWard.setText(parts[parts.length - 2]);

                    StringBuilder detailBuilder = new StringBuilder();
                    for(int i = 0; i < parts.length - 2; i++) {
                        detailBuilder.append(parts[i]);
                        if(i < parts.length - 3) detailBuilder.append(", ");
                    }
                    edtDetail.setText(detailBuilder.toString());
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

        edtCity.setFocusable(false);
        edtCity.setClickable(true);
        edtWard.setFocusable(false);
        edtWard.setClickable(true);

        edtCity.setOnClickListener(v -> showProvinceDialog());
        edtWard.setOnClickListener(v -> {
            if (wardList.isEmpty()) {
                if (edtCity.getText().toString().isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố trước", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đang tải dữ liệu Phường/Xã, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                }
            } else {
                showWardDialog();
            }
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        MaterialButton btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(v -> updateAddress());
    }

    private void loadProvinces() {
        apiService.getProvinces().enqueue(new Callback<List<Region.Province>>() {
            @Override
            public void onResponse(Call<List<Region.Province>> call, Response<List<Region.Province>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    provinceList = response.body();

                    // Nếu đang cập nhật, tìm mã code của tỉnh cũ để load xã phường
                    if (edtCity.getText() != null && !edtCity.getText().toString().isEmpty()) {
                        String cityName = edtCity.getText().toString();
                        for (Region.Province p : provinceList) {
                            if (cityName.equals(p.name)) {
                                fetchWards(p.code);
                                break;
                            }
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Region.Province>> call, Throwable t) {
                Toast.makeText(Update_AddressActivity.this, "Lỗi kết nối API địa chính", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProvinceDialog() {
        if (provinceList.isEmpty()) {
            loadProvinces();
            return;
        }
        String[] names = new String[provinceList.size()];
        for (int i = 0; i < provinceList.size(); i++) names[i] = provinceList.get(i).name;

        new AlertDialog.Builder(this)
                .setTitle("Chọn Tỉnh / Thành phố")
                .setItems(names, (dialog, which) -> {
                    Region.Province p = provinceList.get(which);
                    edtCity.setText(p.name);
                    edtWard.setText("");
                    wardList.clear();
                    fetchWards(p.code);
                }).show();
    }

    private void fetchWards(int provinceCode) {
        if (!isFinishing()) progressDialog.show();
        apiService.getProvinceDetail(provinceCode, 3).enqueue(new Callback<Region.Province>() {
            @Override
            public void onResponse(Call<Region.Province> call, Response<Region.Province> response) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && response.body().districts != null) {
                    wardList.clear();
                    for (Region.District d : response.body().districts) {
                        if (d.wards != null) wardList.addAll(d.wards);
                    }
                }
            }
            @Override
            public void onFailure(Call<Region.Province> call, Throwable t) {
                if (progressDialog.isShowing()) progressDialog.dismiss();
                Toast.makeText(Update_AddressActivity.this, "Lỗi tải danh sách Phường/Xã", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWardDialog() {
        if (wardList.isEmpty()) return;
        String[] names = new String[wardList.size()];
        for (int i = 0; i < wardList.size(); i++) names[i] = wardList.get(i).name;

        new AlertDialog.Builder(this)
                .setTitle("Chọn Phường / Xã")
                .setItems(names, (dialog, which) -> {
                    edtWard.setText(wardList.get(which).name);
                }).show();
    }

    private void updateAddress() {
        if (currentItem == null || currentItem.id == null) return;

        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String city = edtCity.getText().toString();
        String ward = edtWard.getText().toString();
        String detail = edtDetail.getText().toString().trim();

        if (name.isEmpty() || city.isEmpty() || ward.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullAddress = detail + ", " + ward + ", " + city;

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullname", name);
        updates.put("phone", phone);
        updates.put("address", fullAddress);
        updates.put("userId", userId);

        db.collection("address").document(currentItem.id).update(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
