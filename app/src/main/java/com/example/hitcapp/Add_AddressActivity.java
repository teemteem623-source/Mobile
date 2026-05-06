package com.example.hitcapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hitcapp.api.RegionApiService;
import com.example.hitcapp.api.RetrofitClient;
import com.example.hitcapp.models.Region;
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

public class Add_AddressActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtPhone, edtCity, edtWard, edtDetail;
    private FirebaseFirestore db;
    private RegionApiService apiService;
    private List<Region.Province> provinceList = new ArrayList<>();
    private List<Region.Ward> wardList = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_address);

        db = FirebaseFirestore.getInstance();
        apiService = RetrofitClient
                .getClient("https://provinces.open-api.vn/api/")
                .create(RegionApiService.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.setCancelable(false);

        initViews();
        loadProvinces();
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

        findViewById(R.id.btnSave).setOnClickListener(v -> saveAddress());
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadProvinces() {
        apiService.getProvinces().enqueue(new Callback<List<Region.Province>>() {
            @Override
            public void onResponse(Call<List<Region.Province>> call, Response<List<Region.Province>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    provinceList = response.body();
                }
            }
            @Override
            public void onFailure(Call<List<Region.Province>> call, Throwable t) {
                Toast.makeText(Add_AddressActivity.this, "Lỗi kết nối API địa chính", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(Add_AddressActivity.this, "Lỗi tải danh sách Phường/Xã", Toast.LENGTH_SHORT).show();
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

    private void saveAddress() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String city = edtCity.getText().toString();
        String ward = edtWard.getText().toString();
        String detail = edtDetail.getText().toString().trim();

        if (name.isEmpty() || city.isEmpty() || ward.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("fullname", name);
        addressMap.put("phone", phone);
        addressMap.put("address", detail + ", " + ward + ", " + city);
        addressMap.put("userId", userId);

        db.collection("address").add(addressMap).addOnSuccessListener(doc -> {
            Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
