package com.example.hitcapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hitcapp.adapters.ProductAdapter;
import com.example.hitcapp.adapters.SearchSuggestionAdapter;
import com.example.hitcapp.models.Product;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProductActivity extends AppCompatActivity {

    private static final String TAG = "ProductActivity";
    private static final String PREFS_NAME = "SearchPrefs";
    private static final String KEY_HISTORY = "search_history";

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private TextView tvCategoryTitle, tvCartBadge, tvSearchTrigger;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupProduct;
    
    // Search Overlay Views
    private View searchCardTrigger;
    private View layoutSearchOverlay;
    private EditText edtSearchOverlay;
    private ImageView btnBackSearch, btnClearSearch;
    private TextView tvBtnSearch;
    private RecyclerView rvSuggestions;
    private SearchSuggestionAdapter suggestionAdapter;
    private List<String> allProductNames = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String filterType;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        filterType = getIntent().getStringExtra("FILTER_TYPE");
        currentSearchQuery = getIntent().getStringExtra("SEARCH_QUERY");

        loadSearchHistory();
        initViews();
        setupInsets();
        initCustomBottomNav();
        setupRecyclerView();
        setupCategoryFilter();
        setupSearchLogic();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadInitialData);
        }
        
        loadInitialData();
        fetchProductNamesForSuggestions();
    }

    private void loadInitialData() {
        if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            if (tvSearchTrigger != null) tvSearchTrigger.setText(currentSearchQuery);
            if (chipGroupProduct != null) chipGroupProduct.clearCheck();
            searchProducts(currentSearchQuery);
            return;
        }

        if (filterType == null) {
            if (tvCategoryTitle != null) tvCategoryTitle.setText("Tất cả sản phẩm");
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
        loadSearchHistory(); 
        if (tabProduct != null) selectTab(tabProduct);
    }

    private void initViews() {
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        rvProducts = findViewById(R.id.rvProducts);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        chipGroupProduct = findViewById(R.id.chipGroupProduct);
        tvSearchTrigger = findViewById(R.id.tvSearchTrigger);
        
        // Search Views
        searchCardTrigger = findViewById(R.id.searchCardTrigger);
        layoutSearchOverlay = findViewById(R.id.layoutSearchOverlay);
        edtSearchOverlay = findViewById(R.id.edtSearchOverlay);
        btnBackSearch = findViewById(R.id.btnBackSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvBtnSearch = findViewById(R.id.tvBtnSearch);
        rvSuggestions = findViewById(R.id.rvSuggestions);

        View layoutCart = findViewById(R.id.layoutCart);
        if (layoutCart != null) {
            layoutCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
    }

    private void setupSearchLogic() {
        suggestionAdapter = new SearchSuggestionAdapter(this::performSearch);
        suggestionAdapter.setOnDeleteHistoryListener(this::deleteHistoryItem);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);

        if (searchCardTrigger != null) {
            searchCardTrigger.setOnClickListener(v -> {
                layoutSearchOverlay.setVisibility(View.VISIBLE);
                edtSearchOverlay.requestFocus();
                showKeyboard(edtSearchOverlay);
                showSearchHistory();
            });
        }

        if (btnBackSearch != null) {
            btnBackSearch.setOnClickListener(v -> {
                layoutSearchOverlay.setVisibility(View.GONE);
                hideKeyboard(edtSearchOverlay);
                resetSearch();
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                edtSearchOverlay.setText("");
                showSearchHistory();
            });
        }

        if (tvBtnSearch != null) {
            tvBtnSearch.setOnClickListener(v -> performSearch(edtSearchOverlay.getText().toString()));
        }

        if (edtSearchOverlay != null) {
            edtSearchOverlay.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString();
                    btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    if (query.isEmpty()) {
                        showSearchHistory();
                    } else {
                        updateSuggestions(query);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            edtSearchOverlay.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(edtSearchOverlay.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    private void resetSearch() {
        currentSearchQuery = "";
        edtSearchOverlay.setText("");
        if (tvSearchTrigger != null) tvSearchTrigger.setText("Tìm kiếm sản phẩm...");
        loadInitialData(); 
    }

    private void showSearchHistory() {
        if (searchHistory.isEmpty()) {
            suggestionAdapter.setData(new ArrayList<>(), "");
        } else {
            suggestionAdapter.setData(searchHistory, "");
        }
    }

    private void deleteHistoryItem(String item) {
        searchHistory.remove(item);
        saveSearchHistory();
        showSearchHistory();
    }

    private void updateSuggestions(String query) {
        if (query.isEmpty()) {
            showSearchHistory();
            return;
        }
        String normalizedQuery = removeAccent(query.toLowerCase().trim());
        List<String> filtered = allProductNames.stream()
                .filter(name -> removeAccent(name.toLowerCase()).contains(normalizedQuery))
                .collect(Collectors.toList());
        suggestionAdapter.setData(filtered, query);
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private void fetchProductNamesForSuggestions() {
        db.collection("products").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allProductNames.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                if (name != null) allProductNames.add(name);
            }
        });
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) return;
        String q = query.trim();
        saveToHistory(q);
        currentSearchQuery = q;
        filterType = null;
        
        if (tvSearchTrigger != null) tvSearchTrigger.setText(q);
        if (chipGroupProduct != null) chipGroupProduct.clearCheck();
        
        layoutSearchOverlay.setVisibility(View.GONE);
        hideKeyboard(edtSearchOverlay);
        searchProducts(q);
    }

    private void saveToHistory(String query) {
        searchHistory.remove(query);
        searchHistory.add(0, query);
        if (searchHistory.size() > 10) {
            searchHistory.remove(searchHistory.size() - 1);
        }
        saveSearchHistory();
    }

    private void loadSearchHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, null);
        if (json != null) {
            Gson gson = new Gson();
            searchHistory = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
        } else {
            searchHistory = new ArrayList<>();
        }
    }

    private void saveSearchHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(searchHistory);
        editor.putString(KEY_HISTORY, json);
        editor.apply();
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (layoutSearchOverlay != null && layoutSearchOverlay.getVisibility() == View.VISIBLE) {
            layoutSearchOverlay.setVisibility(View.GONE);
            resetSearch();
        } else if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
            resetSearch();
        } else {
            super.onBackPressed();
        }
    }

    private void updateCartBadge() {
        if (tvCartBadge == null || auth.getCurrentUser() == null) return;
        
        db.collection("carts")
                .whereEqualTo("userId", auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalQuantity = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long qty = doc.getLong("quantity");
                        if (qty != null) {
                            totalQuantity += qty.intValue();
                        }
                    }
                    if (totalQuantity > 0) {
                        tvCartBadge.setVisibility(View.VISIBLE);
                        tvCartBadge.setText(String.valueOf(totalQuantity));
                    } else {
                        tvCartBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void setupInsets() {
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBar);
        View bottomNav = findViewById(R.id.bottomNavigationCustom);
        View searchHeader = findViewById(R.id.searchHeader);

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

        if (searchHeader != null) {
            ViewCompat.setOnApplyWindowInsetsListener(searchHeader, (v, insets) -> {
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
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
        if (tabNotification != null) {
            tabNotification.setOnClickListener(v -> {
                Intent intent = new Intent(this, NoticeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
        if (tabProfile != null) {
            tabProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
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
            filterType = null;
            currentSearchQuery = "";
            if (tvSearchTrigger != null) tvSearchTrigger.setText("Tìm kiếm sản phẩm...");
            
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
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> list = new ArrayList<>();
                    String normalizedQuery = removeAccent(queryText.toLowerCase().trim());
                    String[] queryWords = normalizedQuery.split("\\s+");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        if (p.getName() == null) continue;

                        String normalizedName = removeAccent(p.getName().toLowerCase());
                        
                        boolean match = true;
                        for (String word : queryWords) {
                            if (!normalizedName.contains(word)) {
                                match = false;
                                break;
                            }
                        }

                        if (match) {
                            list.add(p);
                        }
                    }
                    if (adapter != null) adapter.setData(list);
                    if (tvCategoryTitle != null) tvCategoryTitle.setText("Kết quả cho: \"" + queryText + "\"");
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });
    }
}
