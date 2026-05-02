package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hitcapp.models.Voucher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoucherActivity extends AppCompatActivity {

    private RecyclerView rvShipping, rvProduct;
    private final List<Voucher> shippingList = new ArrayList<>();
    private final List<Voucher> productList = new ArrayList<>();
    private VoucherAdapter shippingAdapter, productAdapter;
    
    private Voucher selectedShippingVoucher;
    private Voucher selectedProductVoucher;

    private TextView tvStatus;
    private EditText edtCode;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voucher);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Nhận voucher đã chọn từ PaymentActivity
        if (getIntent().hasExtra("PRE_SELECTED_SHIPPING")) {
            selectedShippingVoucher = (Voucher) getIntent().getSerializableExtra("PRE_SELECTED_SHIPPING");
        }
        if (getIntent().hasExtra("PRE_SELECTED_PRODUCT")) {
            selectedProductVoucher = (Voucher) getIntent().getSerializableExtra("PRE_SELECTED_PRODUCT");
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        edtCode = findViewById(R.id.edtVoucherCode);
        tvStatus = findViewById(R.id.tvCodeStatus);
        findViewById(R.id.btnApplyCode).setOnClickListener(v -> applyVoucherCode());

        rvShipping = findViewById(R.id.rvShippingVouchers);
        rvProduct = findViewById(R.id.rvProductVouchers);

        rvShipping.setLayoutManager(new LinearLayoutManager(this));
        rvProduct.setLayoutManager(new LinearLayoutManager(this));

        shippingAdapter = new VoucherAdapter(shippingList, true);
        productAdapter = new VoucherAdapter(productList, false);

        rvShipping.setAdapter(shippingAdapter);
        rvProduct.setAdapter(productAdapter);

        findViewById(R.id.btnConfirmVoucher).setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_SHIPPING", selectedShippingVoucher);
            resultIntent.putExtra("SELECTED_PRODUCT", selectedProductVoucher);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        loadVouchers();
    }

    private void applyVoucherCode() {
        String code = edtCode.getText().toString().trim();
        if (code.isEmpty()) return;

        db.collection("vouchers")
                .whereEqualTo("code", code)
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Voucher v = doc.toObject(Voucher.class);
                        v.setId(doc.getId());
                        
                        if (v.getType().equals(Voucher.TYPE_SHIPPING)) {
                            selectedShippingVoucher = v;
                            shippingAdapter.notifyDataSetChanged();
                        } else {
                            selectedProductVoucher = v;
                            productAdapter.notifyDataSetChanged();
                        }
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText("Áp dụng mã thành công!");
                        tvStatus.setTextColor(Color.parseColor("#10B981"));
                    } else {
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText("Mã không hợp lệ hoặc đã hết hạn");
                        tvStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                });
    }

    private void loadVouchers() {
        if (auth.getUid() == null) return;
        db.collection("vouchers")
                .whereEqualTo("userId", auth.getUid())
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    shippingList.clear();
                    productList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Voucher v = doc.toObject(Voucher.class);
                        v.setId(doc.getId());
                        if (v.getType().equals(Voucher.TYPE_SHIPPING)) {
                            shippingList.add(v);
                        } else {
                            productList.add(v);
                        }
                    }
                    shippingAdapter.notifyDataSetChanged();
                    productAdapter.notifyDataSetChanged();
                });
    }

    private class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {
        private final List<Voucher> list;
        private final boolean isShipping;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        VoucherAdapter(List<Voucher> list, boolean isShipping) {
            this.list = list;
            this.isShipping = isShipping;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Voucher item = list.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvDesc.setText(item.getDescription());
            
            if (item.getExpiryTimestamp() > 0) {
                holder.tvExpiry.setText(String.format("HSD: %s", sdf.format(item.getExpiryTimestamp())));
            }

            final boolean isCurrentlySelected;
            if (isShipping) {
                isCurrentlySelected = selectedShippingVoucher != null && selectedShippingVoucher.getId() != null && selectedShippingVoucher.getId().equals(item.getId());
            } else {
                isCurrentlySelected = selectedProductVoucher != null && selectedProductVoucher.getId() != null && selectedProductVoucher.getId().equals(item.getId());
            }
            
            holder.cbSelect.setChecked(isCurrentlySelected);

            holder.itemView.setOnClickListener(v -> {
                if (isShipping) {
                    if (isCurrentlySelected) {
                        selectedShippingVoucher = null;
                    } else {
                        selectedShippingVoucher = item;
                    }
                } else {
                    if (isCurrentlySelected) {
                        selectedProductVoucher = null;
                    } else {
                        selectedProductVoucher = item;
                    }
                }
                notifyDataSetChanged();
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvExpiry;
            CheckBox cbSelect;
            ViewHolder(View v) { super(v);
                tvTitle = v.findViewById(R.id.tvVoucherTitle);
                tvDesc = v.findViewById(R.id.tvVoucherDesc);
                tvExpiry = v.findViewById(R.id.tvExpiry);
                cbSelect = v.findViewById(R.id.cbSelect);
            }
        }
    }
}
