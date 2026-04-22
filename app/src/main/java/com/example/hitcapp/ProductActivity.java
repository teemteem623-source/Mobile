package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private List<ProductItem> fullProductList = new ArrayList<>();
    private TextView tvCategoryTitle;
    private String currentCategory = "";
    private String currentQuery = "";

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
                if (topBar != null) {
                    topBar.setPadding(0, systemBars.top + (int)(20 * getResources().getDisplayMetrics().density), 0, 10);
                }
                if (bottomNavigation != null) {
                    bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
                }
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // --- KHỞI TẠO DỮ LIỆU ---
        initData();

        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        
        adapter = new ProductAdapter(new ArrayList<>(fullProductList), item -> {
            Intent intent = new Intent(ProductActivity.this, DetailActivity.class);
            intent.putExtra("PRODUCT_NAME", item.name);
            intent.putExtra("PRODUCT_PRICE", item.price);
            intent.putExtra("PRODUCT_IMAGE", item.imageRes);
            startActivity(intent);
        });
        rvProducts.setAdapter(adapter);

        // --- XỬ LÝ TÌM KIẾM ---
        TextInputLayout tilSearch = findViewById(R.id.tilSearch);
        TextInputEditText edtSearch = findViewById(R.id.edtSearch);
        
        // Nhận dữ liệu từ trang Home gửi qua
        String queryFromHome = getIntent().getStringExtra("SEARCH_QUERY");
        if (queryFromHome != null && !queryFromHome.isEmpty()) {
            edtSearch.setText(queryFromHome);
            currentQuery = queryFromHome.toLowerCase().trim();
            applyFilters();
        }

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Xử lý khi nhấn nút Search trên bàn phím
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilters();
                return true;
            }
            return false;
        });

        // Xử lý khi nhấn vào nút tìm kiếm ở cuối khung (End Icon)
        if (tilSearch != null) {
            tilSearch.setEndIconOnClickListener(v -> applyFilters());
        }

        // --- XỬ LÝ LỌC DANH MỤC ---
        ChipGroup chipGroup = findViewById(R.id.chipGroupProduct);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            currentCategory = "";
            String title = "Tất cả sản phẩm";

            if (checkedId == R.id.chipIphone) {
                currentCategory = "iPhone";
                title = "Điện thoại iPhone";
            } else if (checkedId == R.id.chipSamsung) {
                currentCategory = "Samsung";
                title = "Điện thoại Samsung";
            } else if (checkedId == R.id.chipXiaomi) {
                currentCategory = "Xiaomi";
                title = "Điện thoại Xiaomi";
            } else if (checkedId == R.id.chipOppo) {
                currentCategory = "Oppo";
                title = "Điện thoại Oppo";
            }

            tvCategoryTitle.setText(title);
            applyFilters();
        });

        setupNavigation();
    }

    private void initData() {
        fullProductList.add(new ProductItem("iPhone 15 Pro", "28.990.000đ", "iPhone", R.drawable.iphone15pro));
        fullProductList.add(new ProductItem("iPhone 14", "16.490.000đ", "iPhone", R.drawable.iphone14));
        fullProductList.add(new ProductItem("Samsung S24", "25.490.000đ", "Samsung", R.drawable.samsungs24));
        fullProductList.add(new ProductItem("Samsung A54", "8.990.000đ", "Samsung", R.drawable.samsunga54));
        fullProductList.add(new ProductItem("Xiaomi 14", "19.990.000đ", "Xiaomi", R.drawable.xiaomi14));
        fullProductList.add(new ProductItem("Xiaomi Redmi Note 13", "5.490.000đ", "Xiaomi", R.drawable.xiaomiredminote13));
        fullProductList.add(new ProductItem("Oppo Find X7", "18.500.000đ", "Oppo", R.drawable.oppofindx7));
        fullProductList.add(new ProductItem("Oppo Reno 11", "10.990.000đ", "Oppo", R.drawable.opporeno11));
    }

    private void applyFilters() {
        List<ProductItem> filtered = new ArrayList<>();
        for (ProductItem item : fullProductList) {
            boolean matchesCategory = currentCategory.isEmpty() || item.category.equals(currentCategory);
            boolean matchesQuery = currentQuery.isEmpty() || item.name.toLowerCase().contains(currentQuery);
            
            if (matchesCategory && matchesQuery) {
                filtered.add(item);
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

    private static class ProductItem {
        String name, price, category;
        int imageRes;
        ProductItem(String n, String p, String c, int img) { 
            this.name = n; this.price = p; this.category = c; this.imageRes = img;
        }
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private List<ProductItem> list;
        private OnItemClickListener listener;
        public interface OnItemClickListener { void onItemClick(ProductItem item); }

        ProductAdapter(List<ProductItem> list, OnItemClickListener listener) { 
            this.list = list; this.listener = listener;
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
            holder.imgProduct.setImageResource(item.imageRes);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice;
            ImageView imgProduct;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvProductName);
                tvPrice = v.findViewById(R.id.tvProductPrice);
                imgProduct = v.findViewById(R.id.imgProduct);
            }
        }
    }
}
