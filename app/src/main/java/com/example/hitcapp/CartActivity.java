package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private ListView lvCartItems;
    private CheckBox chkSelectAll;
    private CartAdapter adapter;
    private List<CartItem> cartItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        // Xử lý Safe Area (Status bar & Navigation bar)
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- TOP BAR ---
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // --- DATA GIẢ LẬP ---
        cartItemList.add(new CartItem("iPhone 15 Pro Max", "Titan Tự Nhiên, 256GB", 34990000, 1, R.drawable.phone_mockup));
        cartItemList.add(new CartItem("Samsung S24 Ultra", "Titanium Black, 512GB", 29990000, 1, R.drawable.phone_mockup));

        // --- SETUP LISTVIEW ---
        lvCartItems = findViewById(R.id.lvCartItems);
        adapter = new CartAdapter();
        lvCartItems.setAdapter(adapter);

        // --- CHK SELECT ALL ---
        chkSelectAll = findViewById(R.id.chkSelectAll);
        chkSelectAll.setChecked(true); // Mặc định chọn tất cả
        chkSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CartItem item : cartItemList) {
                item.isSelected = isChecked;
            }
            adapter.notifyDataSetChanged();
        });

        // --- XÓA TẤT CẢ ---
        findViewById(R.id.btnDeleteAll).setOnClickListener(v -> {
            cartItemList.clear();
            adapter.notifyDataSetChanged();
            chkSelectAll.setChecked(false);
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
        });

        // --- THANH TOÁN ---
        MaterialButton btnPayment = findViewById(R.id.btnPayment);
        btnPayment.setOnClickListener(v -> {
            boolean hasSelection = false;
            for (CartItem item : cartItemList) {
                if (item.isSelected) {
                    hasSelection = true;
                    break;
                }
            }
            if (hasSelection) {
                startActivity(new Intent(CartActivity.this, PaymentActivity.class));
            } else {
                Toast.makeText(this, "Vui lòng chọn sản phẩm muốn mua", Toast.LENGTH_SHORT).show();
            }
        });

        // --- SẢN PHẨM LIÊN QUAN ---
        setupRelatedProducts();
    }

    private void setupRelatedProducts() {
        View relatedContainer = findViewById(R.id.gridRelated);
        if (relatedContainer instanceof android.widget.GridLayout) {
            android.widget.GridLayout grid = (android.widget.GridLayout) relatedContainer;
            for (int i = 0; i < grid.getChildCount(); i++) {
                grid.getChildAt(i).setOnClickListener(v -> {
                    startActivity(new Intent(CartActivity.this, DetailActivity.class));
                });
            }
        }
    }

    // --- MODEL ---
    private static class CartItem {
        String name, detail;
        long price;
        int quantity;
        int imageRes;
        boolean isSelected = true;

        CartItem(String name, String detail, long price, int quantity, int imageRes) {
            this.name = name;
            this.detail = detail;
            this.price = price;
            this.quantity = quantity;
            this.imageRes = imageRes;
        }
    }

    // --- ADAPTER ---
    private class CartAdapter extends BaseAdapter {
        @Override
        public int getCount() { return cartItemList.size(); }
        @Override
        public Object getItem(int position) { return cartItemList.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CartActivity.this).inflate(R.layout.item_cart, parent, false);
            }

            CartItem item = cartItemList.get(position);

            CheckBox chkItem = convertView.findViewById(R.id.chkItem);
            ImageView imgProduct = convertView.findViewById(R.id.imgProduct);
            TextView tvName = convertView.findViewById(R.id.tvProductName);
            TextView tvDetail = convertView.findViewById(R.id.tvProductDetail);
            TextView tvPrice = convertView.findViewById(R.id.tvProductPrice);
            TextView tvQty = convertView.findViewById(R.id.tvQuantity);
            TextView btnPlus = convertView.findViewById(R.id.btnPlus);
            TextView btnMinus = convertView.findViewById(R.id.btnMinus);
            ImageView btnDelete = convertView.findViewById(R.id.btnDelete);

            chkItem.setChecked(item.isSelected);
            imgProduct.setImageResource(item.imageRes);
            tvName.setText(item.name);
            tvDetail.setText(item.detail);
            tvPrice.setText(String.format(Locale.getDefault(), "%,dđ", item.price));
            tvQty.setText(String.valueOf(item.quantity));

            // Tăng giảm
            btnPlus.setOnClickListener(v -> { item.quantity++; notifyDataSetChanged(); });
            btnMinus.setOnClickListener(v -> { if (item.quantity > 1) { item.quantity--; notifyDataSetChanged(); } });

            // Xóa món này
            btnDelete.setOnClickListener(v -> {
                cartItemList.remove(position);
                notifyDataSetChanged();
            });

            // Checkbox logic ràng buộc
            chkItem.setOnClickListener(v -> {
                item.isSelected = chkItem.isChecked();
                if (!item.isSelected) {
                    chkSelectAll.setOnCheckedChangeListener(null);
                    chkSelectAll.setChecked(false);
                    chkSelectAll.setOnCheckedChangeListener((bv, checked) -> {
                        for (CartItem i : cartItemList) i.isSelected = checked;
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    boolean allChecked = true;
                    for (CartItem i : cartItemList) {
                        if (!i.isSelected) { allChecked = false; break; }
                    }
                    if (allChecked) chkSelectAll.setChecked(true);
                }
            });

            // Về chi tiết
            View.OnClickListener toDetail = v -> startActivity(new Intent(CartActivity.this, DetailActivity.class));
            imgProduct.setOnClickListener(toDetail);
            tvName.setOnClickListener(toDetail);

            return convertView;
        }
    }
}
