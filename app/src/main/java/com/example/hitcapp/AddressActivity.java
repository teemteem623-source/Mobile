package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AddressActivity extends AppCompatActivity {

    private static final int REQ_ADD = 1;
    private static final int REQ_EDIT = 2;

    private RecyclerView rvAddress;
    private List<AddressItem> addressList = new ArrayList<>();
    private AddressAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_address);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Top Bar
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Data mẫu đồng bộ với Payment
        addressList.add(new AddressItem("Nguyễn Văn A", "0901234567", "123 Đường ABC", "Phường 4", "Quận 5", "TP.HCM"));

        // RecyclerView
        rvAddress = findViewById(R.id.rvAddress);
        rvAddress.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressAdapter(addressList);
        rvAddress.setAdapter(adapter);

        // Nút Thêm mới
        findViewById(R.id.btnAddAddress).setOnClickListener(v -> {
            startActivityForResult(new Intent(this, Add_AddressActivity.class), REQ_ADD);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra("NAME");
            String phone = data.getStringExtra("PHONE");
            String detail = data.getStringExtra("DETAIL");
            String ward = data.getStringExtra("WARD");
            String district = data.getStringExtra("DISTRICT");
            String city = data.getStringExtra("CITY");

            AddressItem newItem = new AddressItem(name, phone, detail, ward, district, city);

            if (requestCode == REQ_ADD) {
                addressList.add(newItem);
            } else if (requestCode == REQ_EDIT) {
                // Trong thực tế cần index để sửa, ở đây ta giả lập sửa item đầu tiên
                if (!addressList.isEmpty()) addressList.set(0, newItem);
            }
            adapter.notifyDataSetChanged();
        }
    }

    public static class AddressItem {
        String name, phone, detail, ward, district, city;
        AddressItem(String n, String p, String d, String w, String dis, String c) {
            this.name = n; this.phone = p; this.detail = d; this.ward = w; this.district = dis; this.city = c;
        }
        public String getFullAddress() { return detail + ", " + ward + ", " + district + ", " + city; }
    }

    private class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
        private List<AddressItem> list;
        AddressAdapter(List<AddressItem> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_address, p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AddressItem item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvPhone.setText(item.phone);
            holder.tvFull.setText(item.getFullAddress());

            // Chọn địa chỉ -> Về trang Thanh toán
            holder.itemView.setOnClickListener(v -> {
                Intent res = new Intent();
                res.putExtra("SELECTED_ADDRESS", item.name + " | " + item.phone + "\n" + item.getFullAddress());
                setResult(RESULT_OK, res);
                finish();
            });

            // Sửa địa chỉ -> Trang Update
            holder.btnEdit.setOnClickListener(v -> {
                Intent i = new Intent(AddressActivity.this, Update_AddressActivity.class);
                i.putExtra("NAME", item.name); i.putExtra("PHONE", item.phone);
                i.putExtra("DETAIL", item.detail); i.putExtra("WARD", item.ward);
                i.putExtra("DISTRICT", item.district); i.putExtra("CITY", item.city);
                startActivityForResult(i, REQ_EDIT);
            });
        }

        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvFull, btnEdit;
            ViewHolder(View v) { super(v);
                tvName = v.findViewById(R.id.tvName); tvPhone = v.findViewById(R.id.tvPhone);
                tvFull = v.findViewById(R.id.tvFullAddress); btnEdit = v.findViewById(R.id.btnEdit);
            }
        }
    }
}
