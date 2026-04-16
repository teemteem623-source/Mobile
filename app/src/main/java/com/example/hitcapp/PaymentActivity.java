package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VOUCHER = 101;
    private static final int REQUEST_CODE_ADDRESS = 102;

    private int quantity = 1;
    private long unitPrice = 34990000;
    private long shippingFee = 30000;
    private long shippingDiscount = 30000;
    private long voucherDiscount = 0;
    private String selectedVoucherName = "";

    private TextView tvQuantity, tvSubtotal, tvDiscount, tvShipping, tvShippingDiscount, tvTotalPrice, tvFinalTotal, tvTotalItems, tvSelectedVoucherName, tvDisplayAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);

        // Safe Area
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- ÁNH XẠ ---
        tvQuantity = findViewById(R.id.tvQuantity);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvShipping = findViewById(R.id.tvShipping);
        tvShippingDiscount = findViewById(R.id.tvShippingDiscount);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvSelectedVoucherName = findViewById(R.id.tvSelectedVoucherName);
        tvDisplayAddress = findViewById(R.id.tvDisplayAddress);

        // --- TOP BAR ---
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // --- QUANTITY LOGIC ---
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            quantity++;
            updateUI();
        });
        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateUI();
            }
        });

        // --- NAVIGATION ---
        View.OnClickListener toAddress = v -> {
            Intent intent = new Intent(this, AddressActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADDRESS);
        };
        findViewById(R.id.btnToAddress).setOnClickListener(toAddress);
        findViewById(R.id.btnChangeAddress).setOnClickListener(toAddress);

        findViewById(R.id.btnOpenVoucher).setOnClickListener(v -> {
            Intent intent = new Intent(this, VoucherActivity.class);
            startActivityForResult(intent, REQUEST_CODE_VOUCHER);
        });

        // --- ĐẶT HÀNG ---
        findViewById(R.id.btnOrder).setOnClickListener(v -> {
            // Thông báo thành công
            Toast.makeText(this, "Đặt hàng thành công!", Toast.LENGTH_LONG).show();
            
            // Chuyển tới trang Đơn hàng của tôi (OderActivity)
            Intent intent = new Intent(PaymentActivity.this, OderActivity.class);
            // Xóa các activity trước đó để tránh quay lại trang thanh toán
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_VOUCHER) {
                selectedVoucherName = data.getStringExtra("VOUCHER_NAME");
                voucherDiscount = data.getLongExtra("VOUCHER_DISCOUNT", 0);
                
                if (tvSelectedVoucherName != null) {
                    tvSelectedVoucherName.setText(selectedVoucherName != null ? selectedVoucherName : "Chọn mã giảm giá >");
                    tvSelectedVoucherName.setTextColor(0xFF059669); // Green
                }
                updateUI();
            } else if (requestCode == REQUEST_CODE_ADDRESS) {
                String newAddress = data.getStringExtra("SELECTED_ADDRESS");
                if (tvDisplayAddress != null && newAddress != null) {
                    tvDisplayAddress.setText(newAddress);
                }
            }
        }
    }

    private void updateUI() {
        long totalProductPrice = unitPrice * quantity;
        long finalAmount = totalProductPrice + shippingFee - shippingDiscount - voucherDiscount;

        if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
        if (tvSubtotal != null) tvSubtotal.setText(formatMoney(totalProductPrice));
        if (tvDiscount != null) tvDiscount.setText("-" + formatMoney(voucherDiscount));
        if (tvShipping != null) tvShipping.setText(formatMoney(shippingFee));
        if (tvShippingDiscount != null) tvShippingDiscount.setText("-" + formatMoney(shippingDiscount));
        if (tvTotalPrice != null) tvTotalPrice.setText(formatMoney(finalAmount));
        if (tvFinalTotal != null) tvFinalTotal.setText(formatMoney(finalAmount));
        if (tvTotalItems != null) {
            tvTotalItems.setText(String.format(Locale.getDefault(), "Tổng (%d mặt hàng)", quantity));
        }
    }

    private String formatMoney(long amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount);
    }
}
