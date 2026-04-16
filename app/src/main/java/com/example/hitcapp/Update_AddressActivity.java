package com.example.hitcapp;

import android.content.Context;
import android.content.Intent;
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

public class Update_AddressActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtPhone, edtCity, edtDistrict, edtWard, edtDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_address);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- ÁNH XẠ ---
        edtName = findViewById(R.id.edtName);
        edtPhone = findViewById(R.id.edtPhone);
        edtCity = findViewById(R.id.edtCity);
        edtDistrict = findViewById(R.id.edtDistrict);
        edtWard = findViewById(R.id.edtWard);
        edtDetail = findViewById(R.id.edtDetail);

        // --- NHẬN DỮ LIỆU CŨ ---
        Intent intent = getIntent();
        if (intent != null) {
            edtName.setText(intent.getStringExtra("NAME"));
            edtPhone.setText(intent.getStringExtra("PHONE"));
            edtCity.setText(intent.getStringExtra("CITY"));
            edtDistrict.setText(intent.getStringExtra("DISTRICT"));
            edtWard.setText(intent.getStringExtra("WARD"));
            edtDetail.setText(intent.getStringExtra("DETAIL"));
        }

        // --- TOP BAR LOGIC ---
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // --- UPDATE LOGIC ---
        MaterialButton btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String city = edtCity.getText().toString().trim();
            String district = edtDistrict.getText().toString().trim();
            String ward = edtWard.getText().toString().trim();
            String detail = edtDetail.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || city.isEmpty() || district.isEmpty() || ward.isEmpty() || detail.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ tất cả các trường!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("NAME", name);
            resultIntent.putExtra("PHONE", phone);
            resultIntent.putExtra("CITY", city);
            resultIntent.putExtra("DISTRICT", district);
            resultIntent.putExtra("WARD", ward);
            resultIntent.putExtra("DETAIL", detail);
            
            setResult(RESULT_OK, resultIntent);
            Toast.makeText(this, "Cập nhật địa chỉ thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // --- ẨN BÀN PHÍM VÀ MẤT FOCUS KHI NHẤN RA NGOÀI VÙNG NHẬP LIỆU ---
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
