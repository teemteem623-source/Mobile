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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class VoucherActivity extends AppCompatActivity {

    private RecyclerView rvShipping, rvProduct, rvExpired;
    private final List<Voucher> shippingList = new ArrayList<>();
    private final List<Voucher> productList = new ArrayList<>();
    private final List<Voucher> expiredList = new ArrayList<>();
    
    private VoucherAdapter shippingAdapter, productAdapter, expiredAdapter;
    
    private Voucher selectedShippingVoucher;
    private Voucher selectedProductVoucher;

    private TextView tvStatus, tvTitleShipping, tvTitleProduct, tvTitleExpired;
    private EditText edtCode;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration voucherListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voucher);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerViews();
        
        if (getIntent().hasExtra("AUTO_FILL_CODE")) {
            String code = getIntent().getStringExtra("AUTO_FILL_CODE");
            if (code != null && !code.isEmpty()) {
                edtCode.setText(code);
                edtCode.postDelayed(this::applyVoucherCode, 500);
            }
        }

        if (getIntent().hasExtra("PRE_SELECTED_SHIPPING")) {
            selectedShippingVoucher = (Voucher) getIntent().getSerializableExtra("PRE_SELECTED_SHIPPING");
        }
        if (getIntent().hasExtra("PRE_SELECTED_PRODUCT")) {
            selectedProductVoucher = (Voucher) getIntent().getSerializableExtra("PRE_SELECTED_PRODUCT");
        }

        startListeningVouchers();
    }

    private void initViews() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        edtCode = findViewById(R.id.edtVoucherCode);
        tvStatus = findViewById(R.id.tvCodeStatus);
        
        tvTitleShipping = findViewById(R.id.tvTitleShipping);
        tvTitleProduct = findViewById(R.id.tvTitleProduct);
        tvTitleExpired = findViewById(R.id.tvTitleExpired);

        findViewById(R.id.btnApplyCode).setOnClickListener(v -> applyVoucherCode());
        
        findViewById(R.id.btnConfirmVoucher).setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_SHIPPING", selectedShippingVoucher);
            resultIntent.putExtra("SELECTED_PRODUCT", selectedProductVoucher);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupRecyclerViews() {
        rvShipping = findViewById(R.id.rvShippingVouchers);
        rvProduct = findViewById(R.id.rvProductVouchers);
        rvExpired = findViewById(R.id.rvExpiredVouchers);

        rvShipping.setLayoutManager(new LinearLayoutManager(this));
        rvProduct.setLayoutManager(new LinearLayoutManager(this));
        rvExpired.setLayoutManager(new LinearLayoutManager(this));

        shippingAdapter = new VoucherAdapter(shippingList, true, false);
        productAdapter = new VoucherAdapter(productList, false, false);
        expiredAdapter = new VoucherAdapter(expiredList, false, true);

        rvShipping.setAdapter(shippingAdapter);
        rvProduct.setAdapter(productAdapter);
        rvExpired.setAdapter(expiredAdapter);
    }

    private void applyVoucherCode() {
        String code = edtCode.getText().toString().trim().toUpperCase();
        if (code.isEmpty()) return;

        String uid = auth.getUid();
        if (uid == null) return;

        if (isLuckyCode(code)) {
            checkAndPerformLuckyDraw(uid, code);
            return;
        }

        db.collection("vouchers")
                .whereEqualTo("code", code)
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Voucher v = doc.toObject(Voucher.class);
                        v.setId(doc.getId());
                        
                        if ("CANCELLED".equals(v.getStatus()) || "EXPIRED".equals(v.getStatus())) {
                            showStatus("Mã này không còn khả dụng", "#EF4444");
                            return;
                        }

                        showStatus("Áp dụng mã thành công!", "#10B981");
                        if (Voucher.TYPE_SHIPPING.equals(v.getType())) selectedShippingVoucher = v;
                        else selectedProductVoucher = v;
                        
                        shippingAdapter.notifyDataSetChanged();
                        productAdapter.notifyDataSetChanged();
                    } else {
                        showStatus("Mã không hợp lệ hoặc đã hết hạn", "#EF4444");
                    }
                });
    }

    private boolean isLuckyCode(String code) {
        return code.startsWith("HITC-") || code.startsWith("LUCKY-") || code.startsWith("GIFT-") ||
               code.equals("LUCKY88") || code.equals("GIFT99") || code.equals("HITCAPP") || 
               code.equals("UUDAI100") || code.equals("LUCKY1") || code.equals("HITC88") || code.equals("PROMO10");
    }

    private void checkAndPerformLuckyDraw(String uid, String code) {
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            List<String> usedCodes = (List<String>) documentSnapshot.get("usedLuckyCodes");
            List<String> revokedCodes = (List<String>) documentSnapshot.get("revokedLuckyCodes");

            if (usedCodes != null && usedCodes.contains(code)) {
                showStatus("Bạn đã sử dụng mã này rồi!", "#EF4444");
            } else if (revokedCodes != null && revokedCodes.contains(code)) {
                showStatus("Mã này đã bị thu hồi do hủy đơn hàng!", "#EF4444");
            } else {
                db.collection("notifications")
                        .whereEqualTo("userId", uid)
                        .whereEqualTo("voucherCode", code)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            String orderId = null;
                            if (!querySnapshot.isEmpty()) {
                                orderId = querySnapshot.getDocuments().get(0).getString("oderId");
                            }
                            performLuckyDraw(uid, code, orderId);
                        })
                        .addOnFailureListener(e -> performLuckyDraw(uid, code, null));
            }
        });
    }

    private void performLuckyDraw(String uid, String code, String orderId) {
        Random r = new Random();
        int chance = r.nextInt(100); 
        Voucher wonVoucher;
        String vId = uid + "_LUCKY_" + System.currentTimeMillis();
        long expiry = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);

        if (chance < 1) wonVoucher = new Voucher(vId, "WIN2M", "Voucher Siêu VIP", "Giảm 2.000.000đ", uid, 2000000L, Voucher.TYPE_DISCOUNT, expiry, false);
        else if (chance < 5) wonVoucher = new Voucher(vId, "WIN1M", "Voucher Cực Đỉnh", "Giảm 1.000.000đ", uid, 1000000L, Voucher.TYPE_DISCOUNT, expiry, false);
        else if (chance < 15) wonVoucher = new Voucher(vId, "WIN500K", "Voucher Khủng", "Giảm 500.000đ", uid, 500000L, Voucher.TYPE_DISCOUNT, expiry, false);
        else if (chance < 30) wonVoucher = new Voucher(vId, "WIN200K", "Voucher May Mắn", "Giảm 200.000đ", uid, 200000L, Voucher.TYPE_DISCOUNT, expiry, false);
        else if (chance < 50) wonVoucher = new Voucher(vId, "WIN100K", "Voucher Quà Tặng", "Giảm 100.000đ", uid, 100000L, Voucher.TYPE_DISCOUNT, expiry, false);
        else wonVoucher = new Voucher(vId, "WINFREE", "Freeship May Mắn", "Miễn phí vận chuyển", uid, 0L, Voucher.TYPE_SHIPPING, expiry, false);

        wonVoucher.setOrderId(orderId);
        wonVoucher.setOriginCode(code);

        db.collection("vouchers").document(vId).set(wonVoucher).addOnSuccessListener(aVoid -> {
            db.collection("users").document(uid).update("usedLuckyCodes", FieldValue.arrayUnion(code));
            showStatus("Chúc mừng! Bạn trúng: " + wonVoucher.getTitle(), "#10B981");
            edtCode.setText("");
        });
    }

    private void showStatus(String msg, String color) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setTextColor(Color.parseColor(color));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voucherListener != null) voucherListener.remove();
    }

    private void startListeningVouchers() {
        String uid = auth.getUid();
        if (uid == null) return;

        voucherListener = db.collection("vouchers")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    shippingList.clear(); productList.clear(); expiredList.clear();
                    
                    for (QueryDocumentSnapshot doc : value) {
                        Voucher v = doc.toObject(Voucher.class);
                        v.setId(doc.getId());
                        
                        if (v.isUsed() || "CANCELLED".equals(v.getStatus()) || "EXPIRED".equals(v.getStatus()) 
                            || (v.getExpiryTimestamp() > 0 && v.getExpiryTimestamp() < System.currentTimeMillis())) {
                            expiredList.add(v);
                        } else {
                            if (Voucher.TYPE_SHIPPING.equals(v.getType())) shippingList.add(v);
                            else productList.add(v);
                        }
                    }
                    
                    updateSectionVisibility();
                    shippingAdapter.notifyDataSetChanged();
                    productAdapter.notifyDataSetChanged();
                    expiredAdapter.notifyDataSetChanged();
                });
    }

    private void updateSectionVisibility() {
        tvTitleShipping.setVisibility(shippingList.isEmpty() ? View.GONE : View.VISIBLE);
        rvShipping.setVisibility(shippingList.isEmpty() ? View.GONE : View.VISIBLE);
        
        tvTitleProduct.setVisibility(productList.isEmpty() ? View.GONE : View.VISIBLE);
        rvProduct.setVisibility(productList.isEmpty() ? View.GONE : View.VISIBLE);
        
        tvTitleExpired.setVisibility(expiredList.isEmpty() ? View.GONE : View.VISIBLE);
        rvExpired.setVisibility(expiredList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {
        private final List<Voucher> list;
        private final boolean isShipping;
        private final boolean isExpired;
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        VoucherAdapter(List<Voucher> list, boolean isShipping, boolean isExpired) { 
            this.list = list; this.isShipping = isShipping; this.isExpired = isExpired; 
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Voucher item = list.get(position);
            holder.tvTitle.setText(item.getTitle());
            holder.tvDesc.setText(item.getDescription());
            
            if ("CANCELLED".equals(item.getStatus())) {
                holder.tvExpiry.setText("Đã bị thu hồi");
                holder.tvExpiry.setTextColor(Color.RED);
            } else {
                holder.tvExpiry.setText(item.getExpiryTimestamp() > 0 ? "HSD: " + sdf.format(item.getExpiryTimestamp()) : "Không thời hạn");
                holder.tvExpiry.setTextColor(isExpired ? Color.GRAY : Color.parseColor("#64748B"));
            }

            holder.imgIcon.setImageResource(isShipping ? R.drawable.logistic : R.drawable.voucher);
            if (isExpired) holder.itemView.setAlpha(0.6f);

            final boolean isCurrentlySelected = isShipping ? 
                (selectedShippingVoucher != null && item.getId().equals(selectedShippingVoucher.getId())) :
                (selectedProductVoucher != null && item.getId().equals(selectedProductVoucher.getId()));
            
            holder.cbSelect.setChecked(isCurrentlySelected);
            holder.cbSelect.setVisibility(isExpired ? View.GONE : View.VISIBLE);
            
            holder.itemView.setOnClickListener(v -> {
                if (isExpired) return;
                if (isShipping) selectedShippingVoucher = isCurrentlySelected ? null : item;
                else selectedProductVoucher = isCurrentlySelected ? null : item;
                notifyDataSetChanged();
            });
        }

        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvExpiry; CheckBox cbSelect; ImageView imgIcon;
            ViewHolder(View v) { super(v);
                tvTitle = v.findViewById(R.id.tvVoucherTitle);
                tvDesc = v.findViewById(R.id.tvVoucherDesc);
                tvExpiry = v.findViewById(R.id.tvExpiry);
                cbSelect = v.findViewById(R.id.cbSelect);
                imgIcon = v.findViewById(R.id.imgVoucherIcon);
            }
        }
    }
}
