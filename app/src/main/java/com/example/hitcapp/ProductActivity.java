package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hitcapp.adapters.ProductAdapter;
import com.example.hitcapp.models.Product;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    private static final String TAG = "ProductActivity";
    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private TextView tvCategoryTitle, tvCartBadge;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private EditText edtSearch;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupProduct;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String filterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        filterType = getIntent().getStringExtra("FILTER_TYPE");

        initViews();
        setupInsets();
        initCustomBottomNav();
        setupRecyclerView();
        setupCategoryFilter();
        setupSearch();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadInitialData);
        }
        loadInitialData();
    }

    private void loadInitialData() {
        if (filterType == null) {
            loadAllProducts();
            return;
        }

        switch (filterType) {
            case "FLASH_SALE":
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Săn Deal Flash Sale");
                loadFilteredProducts("isSale", true);
                break;
            case "FEATURED":
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Sản phẩm nổi bật");
                loadFilteredProducts("isFeatured", true);
                break;
            case "NEW":
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Sản phẩm mới về");
                loadFilteredProducts("isNew", true);
                break;
            default:
                loadAllProducts();
                break;
        }
    }

    private void loadFilteredProducts(String field, boolean value) {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        db.collection("products")
                .whereEqualTo(field, value)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        list.add(p);
                    }
                    if (adapter != null) adapter.setData(list);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading filtered products", e);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        if (tabProduct != null) selectTab(tabProduct);
    }

    private void initViews() {
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        rvProducts = findViewById(R.id.rvProducts);
        edtSearch = findViewById(R.id.edtSearch);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        chipGroupProduct = findViewById(R.id.chipGroupProduct);
        
        View layoutCart = findViewById(R.id.layoutCart);
        if (layoutCart != null) {
            layoutCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
    }

    private void updateCartBadge() {
        if (tvCartBadge == null || auth.getCurrentUser() == null) return;
        
        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > 0) {
                        tvCartBadge.setVisibility(View.VISIBLE);
                        tvCartBadge.setText(String.valueOf(count));
                    } else {
                        tvCartBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBar);
        View bottomNav = findViewById(R.id.bottomNavigationCustom);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }
    }

    private void initCustomBottomNav() {
        tabHome = findViewById(R.id.tabHome);
        tabProduct = findViewById(R.id.tabProduct);
        tabNotification = findViewById(R.id.tabNotification);
        tabProfile = findViewById(R.id.tabProfile);

        if (tabHome != null) {
            tabHome.setOnClickListener(v -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        }
        if (tabNotification != null) {
            tabNotification.setOnClickListener(v -> {
                startActivity(new Intent(this, NoticeActivity.class));
                finish();
            });
        }
        if (tabProfile != null) {
            tabProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, UserActivity.class));
                finish();
            });
        }
    }

    private void selectTab(LinearLayout selected) {
        LinearLayout[] tabs = {tabHome, tabProduct, tabNotification, tabProfile};
        int[] iconIds = {R.id.iconHome, R.id.iconProduct, R.id.iconNotification, R.id.iconProfile};
        int[] textIds = {R.id.textHome, R.id.textProduct, R.id.textNotification, R.id.textProfile};

        for (int i = 0; i < tabs.length; i++) {
            LinearLayout tab = tabs[i];
            if (tab == null) continue;
            
            boolean isSelected = (tab == selected);
            ImageView icon = findViewById(iconIds[i]);
            TextView text = findViewById(textIds[i]);

            int color = isSelected ? Color.parseColor("#1E40AF") : Color.parseColor("#94A3B8");
            if (icon != null) icon.setColorFilter(color);
            if (text != null) {
                text.setTextColor(color);
                text.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void setupRecyclerView() {
        if (rvProducts == null) return;
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductAdapter(product -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            startActivity(intent);
        });
        rvProducts.setAdapter(adapter);
    }

    private void setupCategoryFilter() {
        if (chipGroupProduct == null) return;
        chipGroupProduct.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            filterType = null; // Clear special filter when user clicks a category
            
            if (checkedId == R.id.chipAll) {
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Tất cả sản phẩm");
                loadAllProducts();
            } else if (checkedId == R.id.chipIphone) {
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Điện thoại iPhone");
                loadProductsByCategory("iphone");
            } else if (checkedId == R.id.chipSamsung) {
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Điện thoại Samsung");
                loadProductsByCategory("samsung");
            } else if (checkedId == R.id.chipXiaomi) {
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Điện thoại Xiaomi");
                loadProductsByCategory("xiaomi");
            } else if (checkedId == R.id.chipOppo) {
                if (tvCategoryTitle != null) tvCategoryTitle.setText("Điện thoại Oppo");
                loadProductsByCategory("oppo");
            }
        });
    }

    private void setupSearch() {
        if (edtSearch == null) return;
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String queryText = edtSearch.getText().toString().trim();
                if (!queryText.isEmpty()) {
                    searchProducts(queryText);
                }
                return true;
            }
            return false;
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) loadInitialData();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllProducts() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        list.add(p);
                    }
                    if (adapter != null) adapter.setData(list);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }

    private void loadProductsByCategory(String categoryId) {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        db.collection("products")
                .whereEqualTo("category", categoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        list.add(p);
                    }
                    if (adapter != null) adapter.setData(list);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }

    private void searchProducts(String queryText) {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        db.collection("products")
                .whereGreaterThanOrEqualTo("name", queryText)
                .whereLessThanOrEqualTo("name", queryText + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        list.add(p);
                    }
                    if (adapter != null) adapter.setData(list);
                    if (tvCategoryTitle != null) tvCategoryTitle.setText("Kết quả cho: \"" + queryText + "\"");
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }
}
