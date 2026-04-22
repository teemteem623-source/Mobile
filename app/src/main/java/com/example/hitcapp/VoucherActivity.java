package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

public class VoucherActivity extends AppCompatActivity {

    private RecyclerView rvShipping, rvProduct;
    private List<VoucherItem> shippingList = new ArrayList<>();
    private List<VoucherItem> productList = new ArrayList<>();
    private TextView tvStatus;
    private EditText edtCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voucher);

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
        findViewById(R.id.btnApplyCode).setOnClickListener(v -> {
            String code = edtCode.getText().toString().trim();
            if (code.equalsIgnoreCase("TTVIP")) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Áp dụng mã thành công: Giảm 100k");
                tvStatus.setTextColor(Color.parseColor("#10B981")); 
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("VOUCHER_NAME", "Mã TTVIP (-100k)");
                resultIntent.putExtra("VOUCHER_DISCOUNT", 100000L);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Mã không hợp lệ hoặc đã hết hạn sử dụng");
                tvStatus.setTextColor(Color.parseColor("#EF4444")); 
            }
        });

        initData();

        rvShipping = findViewById(R.id.rvShippingVouchers);
        rvProduct = findViewById(R.id.rvProductVouchers);

        rvShipping.setLayoutManager(new LinearLayoutManager(this));
        rvProduct.setLayoutManager(new LinearLayoutManager(this));

        rvShipping.setAdapter(new VoucherAdapter(shippingList));
        rvProduct.setAdapter(new VoucherAdapter(productList));
    }

    private void initData() {
        shippingList.add(new VoucherItem("Freeship 15k", "Đơn từ 50k", android.R.drawable.ic_menu_send, "#3B82F6", 15000));
        shippingList.add(new VoucherItem("Freeship 30k", "Đơn từ 150k", android.R.drawable.ic_menu_send, "#3B82F6", 30000));
        shippingList.add(new VoucherItem("Freeship Extra", "Tối đa 50k cho đơn từ 300k", android.R.drawable.ic_menu_send, "#3B82F6", 50000));

        productList.add(new VoucherItem("Giảm 50k", "Đơn từ 500k", android.R.drawable.ic_menu_save, "#F43F5E", 50000));
        productList.add(new VoucherItem("Giảm 100k", "Đơn từ 2 triệu", android.R.drawable.ic_menu_save, "#F43F5E", 100000));
        productList.add(new VoucherItem("Giảm 10%", "Tối đa 200k cho iPhone", android.R.drawable.ic_menu_save, "#F43F5E", 200000));
        productList.add(new VoucherItem("Giảm 500k", "Đơn từ 10 triệu", android.R.drawable.ic_menu_save, "#F43F5E", 500000));
    }

    private static class VoucherItem {
        String title, desc, colorHex;
        int icon;
        long discountValue;
        VoucherItem(String t, String d, int i, String c, long dv) { title = t; desc = d; icon = i; colorHex = c; discountValue = dv; }
    }

    private class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {
        private List<VoucherItem> list;
        VoucherAdapter(List<VoucherItem> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VoucherItem item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvDesc.setText(item.desc);
            holder.imgIcon.setImageResource(item.icon);
            holder.layoutIcon.setBackgroundColor(Color.parseColor(item.colorHex));
            
            holder.btnApply.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("VOUCHER_NAME", item.title);
                resultIntent.putExtra("VOUCHER_DISCOUNT", item.discountValue);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc, btnApply;
            ImageView imgIcon;
            View layoutIcon;
            ViewHolder(View v) { super(v);
                tvTitle = v.findViewById(R.id.tvVoucherTitle);
                tvDesc = v.findViewById(R.id.tvVoucherDesc);
                imgIcon = v.findViewById(R.id.imgVoucherIcon);
                layoutIcon = v.findViewById(R.id.layoutIcon);
                btnApply = v.findViewById(R.id.btnApply);
            }
        }
    }
}
