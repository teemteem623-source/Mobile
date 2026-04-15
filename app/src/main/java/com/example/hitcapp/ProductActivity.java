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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private List<ProductItem> fullProductList = new ArrayList<>();
    private TextView tvCategoryTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product);

        // --- XỬ LÝ SAFE AREA ---
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBarCard);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (topBar != null) topBar.setPadding(0, systemBars.top, 0, 0);
                if (bottomNavigation != null) bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }

        // --- KHỞI TẠO DỮ LIỆU ---
        initData();

        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(new ArrayList<>(fullProductList), item -> {
            startActivity(new Intent(this, DetailActivity.class));
        });
        rvProducts.setAdapter(adapter);

        // --- XỬ LÝ LỌC DANH MỤC ---
        ChipGroup chipGroup = findViewById(R.id.chipGroupProduct);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            String category = "";
            String title = "Tất cả sản phẩm";

            if (checkedId == R.id.chipIphone) {
                category = "iPhone";
                title = "Điện thoại iPhone";
            } else if (checkedId == R.id.chipSamsung) {
                category = "Samsung";
                title = "Điện thoại Samsung";
            } else if (checkedId == R.id.chipXiaomi) {
                category = "Xiaomi";
                title = "Điện thoại Xiaomi";
            } else if (checkedId == R.id.chipOppo) {
                category = "Oppo";
                title = "Điện thoại Oppo";
            }

            tvCategoryTitle.setText(title);
            filterProducts(category);
        });

        // --- NAVIGATION & CART ---
        setupNavigation();
    }

    private void initData() {
        fullProductList.add(new ProductItem("iPhone 15 Pro", "28.990.000đ", "iPhone"));
        fullProductList.add(new ProductItem("iPhone 14", "16.490.000đ", "iPhone"));
        fullProductList.add(new ProductItem("Samsung S24", "25.490.000đ", "Samsung"));
        fullProductList.add(new ProductItem("Samsung A54", "8.990.000đ", "Samsung"));
        fullProductList.add(new ProductItem("Xiaomi 14", "19.990.000đ", "Xiaomi"));
        fullProductList.add(new ProductItem("Xiaomi Redmi Note 13", "5.490.000đ", "Xiaomi"));
        fullProductList.add(new ProductItem("Oppo Find X7", "18.500.000đ", "Oppo"));
        fullProductList.add(new ProductItem("Oppo Reno 11", "10.990.000đ", "Oppo"));
    }

    private void filterProducts(String category) {
        List<ProductItem> filtered = new ArrayList<>();
        if (category.isEmpty()) {
            filtered.addAll(fullProductList);
        } else {
            for (ProductItem item : fullProductList) {
                if (item.category.equals(category)) {
                    filtered.add(item);
                }
            }
        }
        adapter.updateList(filtered);
    }

    private void setupNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_products);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_products) return true;
                else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(this, NoticeActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, UserActivity.class));
                    finish(); return true;
                }
                return false;
            });
        }

        ImageView imgCart = findViewById(R.id.imgCart);
        if (imgCart != null) {
            imgCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
    }

    // --- ADAPTER & MODEL ---
    private static class ProductItem {
        String name, price, category;
        ProductItem(String n, String p, String c) { this.name = n; this.price = p; this.category = c; }
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private List<ProductItem> list;
        private OnItemClickListener listener;
        public interface OnItemClickListener { void onItemClick(ProductItem item); }

        ProductAdapter(List<ProductItem> list, OnItemClickListener listener) { 
            this.list = list; 
            this.listener = listener;
        }

        public void updateList(List<ProductItem> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

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
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvPrice = v.findViewById(R.id.tvProductPrice);
            }
        }
    }
}
