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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AddressActivity extends AppCompatActivity {

    private static final int REQ_ADD = 1;
    private static final int REQ_EDIT = 2;

    private RecyclerView rvAddress;
    private View layoutEmpty;
    private List<AddressItem> addressList = new ArrayList<>();
    private AddressAdapter adapter;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_address);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem địa chỉ", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

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

        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvAddress = findViewById(R.id.rvAddress);
        rvAddress.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressAdapter(addressList);
        rvAddress.setAdapter(adapter);

        loadAddresses();

        // Nút Thêm mới
        findViewById(R.id.btnAddAddress).setOnClickListener(v -> {
            startActivityForResult(new Intent(this, Add_AddressActivity.class), REQ_ADD);
        });
    }

    private void loadAddresses() {
        db.collection("address")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addressList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AddressItem item = new AddressItem();
                            item.id = doc.getId();
                            item.name = doc.getString("fullname");
                            item.phone = doc.getString("phone");
                            item.address = doc.getString("address");
                            addressList.add(item);
                        }
                    }

                    if (addressList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvAddress.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvAddress.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    public static class AddressItem implements Serializable {
        public String id, name, phone, address;
        public AddressItem() {}
        public AddressItem(String n, String p, String a) {
            this.name = n; this.phone = p; this.address = a;
        }
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
            holder.tvFull.setText(item.address);

            // Chọn địa chỉ -> Về trang Thanh toán
            holder.itemView.setOnClickListener(v -> {
                Intent res = new Intent();
                res.putExtra("SELECTED_ADDRESS_OBJ", item);
                setResult(RESULT_OK, res);
                finish();
            });

            // Sửa địa chỉ -> Trang Update
            holder.btnEdit.setOnClickListener(v -> {
                Intent i = new Intent(AddressActivity.this, Update_AddressActivity.class);
                i.putExtra("ADDRESS_ITEM", item);
                startActivity(i);
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
