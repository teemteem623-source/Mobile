package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class OderActivity extends AppCompatActivity {

    private RecyclerView rvRelatedProducts;
    private RelatedProductAdapter adapter;
    private List<ProductItem> relatedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_oder);

        // Xử lý Safe Area
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- ÁNH XẠ ---
        ImageView btnBack = findViewById(R.id.btnBack);
        LinearLayout layoutOrderItem = findViewById(R.id.layoutOrderItem);
        MaterialCardView btnViewProcess = findViewById(R.id.btnViewProcess);
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);

        // --- LOGIC ĐƠN HÀNG ---
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        if (layoutOrderItem != null) {
            layoutOrderItem.setOnClickListener(v -> 
                startActivity(new Intent(OderActivity.this, DetailActivity.class)));
        }

        if (btnViewProcess != null) {
            btnViewProcess.setOnClickListener(v -> 
                startActivity(new Intent(OderActivity.this, ProcessActivity.class)));
        }

        // --- SẢN PHẨM LIÊN QUAN (GRID 2 CỘT) ---
        initData();
        rvRelatedProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new RelatedProductAdapter(relatedList);
        rvRelatedProducts.setAdapter(adapter);
    }

    private void initData() {
        relatedList.add(new ProductItem("Sạc nhanh 20W", "490.000đ"));
        relatedList.add(new ProductItem("Ốp lưng MagSafe", "1.290.000đ"));
        relatedList.add(new ProductItem("AirPods Pro 2", "5.990.000đ"));
        relatedList.add(new ProductItem("Apple Watch S9", "10.490.000đ"));
        relatedList.add(new ProductItem("Cường lực KingKong", "150.000đ"));
        relatedList.add(new ProductItem("Ví MagSafe Da", "1.490.000đ"));
    }

    private static class ProductItem {
        String name, price;
        ProductItem(String n, String p) { this.name = n; this.price = p; }
    }

    private class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.ViewHolder> {
        private List<ProductItem> list;
        RelatedProductAdapter(List<ProductItem> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductItem item = list.get(position);
            holder.tvName.setText(item.name);
            holder.tvPrice.setText(item.price);
            holder.itemView.setOnClickListener(v -> 
                startActivity(new Intent(OderActivity.this, DetailActivity.class)));
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvPrice = v.findViewById(R.id.tvProductPrice);
            }
        }
    }
}
