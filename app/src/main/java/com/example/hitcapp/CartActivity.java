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

        // --- CẬP NHẬT HÌNH ẢNH SẢN PHẨM TRONG GIỎ HÀNG ---
        cartItemList.add(new CartItem("iPhone 15 Pro", "Titan Tự Nhiên, 256GB", 28990000, 1, R.drawable.iphone15pro));
        cartItemList.add(new CartItem("Samsung S24", "Titanium Black, 256GB", 25490000, 1, R.drawable.samsungs24));

        lvCartItems = findViewById(R.id.lvCartItems);
        adapter = new CartAdapter();
        lvCartItems.setAdapter(adapter);

        chkSelectAll = findViewById(R.id.chkSelectAll);
        chkSelectAll.setChecked(true);
        chkSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CartItem item : cartItemList) item.isSelected = isChecked;
            adapter.notifyDataSetChanged();
        });

        findViewById(R.id.btnDeleteAll).setOnClickListener(v -> {
            cartItemList.clear();
            adapter.notifyDataSetChanged();
            chkSelectAll.setChecked(false);
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
        });

        MaterialButton btnPayment = findViewById(R.id.btnPayment);
        btnPayment.setOnClickListener(v -> {
            if (cartItemList.isEmpty()) return;
            startActivity(new Intent(CartActivity.this, PaymentActivity.class));
        });

        setupRelatedProducts();
    }

    private void setupRelatedProducts() {
        View relatedContainer = findViewById(R.id.gridRelated);
        if (relatedContainer instanceof android.widget.GridLayout) {
            android.widget.GridLayout grid = (android.widget.GridLayout) relatedContainer;
            for (int i = 0; i < grid.getChildCount(); i++) {
                grid.getChildAt(i).setOnClickListener(v -> startActivity(new Intent(CartActivity.this, DetailActivity.class)));
            }
        }
    }

    private static class CartItem {
        String name, detail;
        long price;
        int quantity, imageRes;
        boolean isSelected = true;
        CartItem(String name, String detail, long price, int quantity, int imageRes) {
            this.name = name; this.detail = detail; this.price = price; this.quantity = quantity; this.imageRes = imageRes;
        }
    }

    private class CartAdapter extends BaseAdapter {
        @Override public int getCount() { return cartItemList.size(); }
        @Override public Object getItem(int pos) { return cartItemList.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(CartActivity.this).inflate(R.layout.item_cart, parent, false);
            CartItem item = cartItemList.get(pos);
            ((CheckBox)convertView.findViewById(R.id.chkItem)).setChecked(item.isSelected);
            ((ImageView)convertView.findViewById(R.id.imgProduct)).setImageResource(item.imageRes);
            ((TextView)convertView.findViewById(R.id.tvProductName)).setText(item.name);
            ((TextView)convertView.findViewById(R.id.tvProductDetail)).setText(item.detail);
            ((TextView)convertView.findViewById(R.id.tvProductPrice)).setText(String.format(Locale.getDefault(), "%,dđ", item.price));
            ((TextView)convertView.findViewById(R.id.tvQuantity)).setText(String.valueOf(item.quantity));
            return convertView;
        }
    }
}
