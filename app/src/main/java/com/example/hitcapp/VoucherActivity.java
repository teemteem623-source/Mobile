package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

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
    private ListenerRegistration voucherListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voucher);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getIntent().hasExtra("PRE_SELECTED_SHIPPING")) {
            selectedShippingVoucher = (Voucher) getIntent().getSerializableExtra("PRE_SELECTED_SHIPPING");
        }
        if (getIntent().hasExtra("PRE_SELECTED_PRODUCT")) {
            selectedProductVoucher = (Voucher) getIntent().getSerializableExtra("PRE_SELECTED_PRODUCT");
        }

        initViews();
        setupRecyclerViews();
        startListeningVouchers();

        // Tự động nhập mã nếu đi từ thông báo
        String autoFillCode = getIntent().getStringExtra("AUTO_FILL_CODE");
        if (autoFillCode != null && !autoFillCode.isEmpty()) {
            edtCode.setText(autoFillCode);
        }
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

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        edtCode = findViewById(R.id.edtVoucherCode);
        tvStatus = findViewById(R.id.tvCodeStatus);
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

        rvShipping.setLayoutManager(new LinearLayoutManager(this));
        rvProduct.setLayoutManager(new LinearLayoutManager(this));

        shippingAdapter = new VoucherAdapter(shippingList, true);
        productAdapter = new VoucherAdapter(productList, false);

        rvShipping.setAdapter(shippingAdapter);
        rvProduct.setAdapter(productAdapter);
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
                .whereEqualTo("used", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("VoucherActivity", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        shippingList.clear();
                        productList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Voucher v = doc.toObject(Voucher.class);
                            v.setId(doc.getId());
                            
                            if (Voucher.TYPE_SHIPPING.equals(v.getType())) {
                                shippingList.add(v);
                            } else {
                                productList.add(v);
                            }
                        }
                        shippingAdapter.notifyDataSetChanged();
                        productAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void applyVoucherCode() {
        String code = edtCode.getText().toString().trim().toUpperCase();
        if (code.isEmpty()) return;

        // Kiểm tra xem mã có phải mã tri ân không
        if (isLoyaltyCode(code)) {
            handleLoyaltyCode(code);
            return;
        }

        // Kiểm tra mã voucher bình thường trong DB
        db.collection("vouchers")
                .whereEqualTo("code", code)
                .whereEqualTo("used", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        Voucher v = doc.toObject(Voucher.class);
                        v.setId(doc.getId());
                        
                        if (Voucher.TYPE_SHIPPING.equals(v.getType())) {
                            selectedShippingVoucher = v;
                        } else {
                            selectedProductVoucher = v;
                        }
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText("Áp dụng mã thành công!");
                        tvStatus.setTextColor(Color.parseColor("#10B981"));
                        shippingAdapter.notifyDataSetChanged();
                        productAdapter.notifyDataSetChanged();
                    } else {
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText("Mã không hợp lệ hoặc đã hết hạn");
                        tvStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                });
    }

    private boolean isLoyaltyCode(String code) {
        // Danh sách mã tri ân hệ thống
        String[] loyaltyCodes = {
                "VANNANG", "TRIAN", "FREE", "HITCAPP", 
                "QUATANG", "NHANHAY", "GIAMGIASOC", "VOUCHER100", 
                "VOUCHER500", "VOUCHER2TR", "NEWUSER", "MOBILE2024", 
                "HITCAPPVIP", "TRIANHITC", "SIEUVOUCHER", "MUAHE"
        };
        for (String c : loyaltyCodes) {
            if (c.equals(code)) return true;
        }
        return false;
    }

    private void handleLoyaltyCode(final String code) {
        final String uid = auth.getUid();
        if (uid == null) return;

        // Kiểm tra xem người dùng đã nhận voucher từ mã này chưa (mỗi mã 1 lần/tài khoản)
        db.collection("vouchers")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean alreadyUsed = false;
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String existingCode = doc.getString("code");
                            if (existingCode != null && existingCode.startsWith(code + "_")) {
                                alreadyUsed = true;
                                break;
                            }
                        }
                    }

                    if (alreadyUsed) {
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText("Bạn đã sử dụng mã này rồi!");
                        tvStatus.setTextColor(Color.parseColor("#EF4444"));
                    } else {
                        generateAndAddReward(code, uid);
                    }
                });
    }

    private void generateAndAddReward(String code, String uid) {
        // Tỷ lệ trúng: 1% 2tr, 9% 1tr, 15% 500k, 25% 100k, 50% freeship
        int random = new Random().nextInt(100); 
        String title, desc, type;
        long value;

        if (random < 1) {
            title = "Voucher Tri Ân 2 Triệu";
            desc = "Giảm trực tiếp 2.000.000đ cho đơn hàng";
            value = 2000000;
            type = Voucher.TYPE_DISCOUNT;
        } else if (random < 10) {
            title = "Voucher Tri Ân 1 Triệu";
            desc = "Giảm trực tiếp 1.000.000đ cho đơn hàng";
            value = 1000000;
            type = Voucher.TYPE_DISCOUNT;
        } else if (random < 25) {
            title = "Voucher Tri Ân 500K";
            desc = "Giảm ngay 500.000đ cho đơn hàng";
            value = 500000;
            type = Voucher.TYPE_DISCOUNT;
        } else if (random < 50) {
            title = "Voucher Tri Ân 100K";
            desc = "Giảm ngay 100.000đ cho đơn hàng";
            value = 100000;
            type = Voucher.TYPE_DISCOUNT;
        } else {
            title = "Voucher Freeship Toàn Quốc";
            desc = "Miễn phí vận chuyển cho mọi đơn hàng";
            value = 30000;
            type = Voucher.TYPE_SHIPPING;
        }

        Voucher newVoucher = new Voucher();
        newVoucher.setCode(code + "_" + System.currentTimeMillis());
        newVoucher.setTitle(title);
        newVoucher.setDescription(desc);
        newVoucher.setUserId(uid);
        newVoucher.setValue(value);
        newVoucher.setType(type);
        newVoucher.setUsed(false);
        newVoucher.setExpiryTimestamp(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));

        db.collection("vouchers").add(newVoucher)
                .addOnSuccessListener(documentReference -> {
                    tvStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("Chúc mừng! Bạn nhận được: " + title);
                    tvStatus.setTextColor(Color.parseColor("#10B981"));
                    addRewardNotification(title, desc, type);
                    edtCode.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                });
    }

    private void addRewardNotification(String title, String desc, String voucherType) {
        String uid = auth.getUid();
        if (uid == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", uid);
        notification.put("title", "Bạn vừa nhận được Voucher!");
        notification.put("content", "Chúc mừng bạn đã nhận được " + title + ": " + desc);
        notification.put("type", "Khuyến mãi");
        notification.put("voucherType", voucherType);
        notification.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notifications").add(notification);
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
            
            // Cập nhật icon dựa trên loại voucher
            if (Voucher.TYPE_SHIPPING.equals(item.getType())) {
                holder.imgIcon.setImageResource(R.drawable.logistic);
            } else {
                holder.imgIcon.setImageResource(R.drawable.voucher);
            }
            holder.imgIcon.clearColorFilter(); // Giữ nguyên màu PNG

            if (item.getExpiryTimestamp() > 0) {
                holder.tvExpiry.setText(String.format("HSD: %s", sdf.format(item.getExpiryTimestamp())));
            } else {
                holder.tvExpiry.setText("Không thời hạn");
            }

            final boolean isCurrentlySelected;
            if (isShipping) {
                isCurrentlySelected = selectedShippingVoucher != null && item.getId().equals(selectedShippingVoucher.getId());
            } else {
                isCurrentlySelected = selectedProductVoucher != null && item.getId().equals(selectedProductVoucher.getId());
            }
            
            holder.cbSelect.setChecked(isCurrentlySelected);

            holder.itemView.setOnClickListener(v -> {
                if (isShipping) {
                    selectedShippingVoucher = isCurrentlySelected ? null : item;
                } else {
                    selectedProductVoucher = isCurrentlySelected ? null : item;
                }
                notifyDataSetChanged();
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, tvExpiry;
            ImageView imgIcon;
            CheckBox cbSelect;
            ViewHolder(View v) { super(v);
                tvTitle = v.findViewById(R.id.tvVoucherTitle);
                tvDesc = v.findViewById(R.id.tvVoucherDesc);
                tvExpiry = v.findViewById(R.id.tvExpiry);
                imgIcon = v.findViewById(R.id.imgVoucherIcon);
                cbSelect = v.findViewById(R.id.cbSelect);
            }
        }
    }
}
