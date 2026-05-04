package com.example.hitcapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.hitcapp.adapters.ProductAdapter;
import com.example.hitcapp.adapters.SearchSuggestionAdapter;
import com.example.hitcapp.models.Product;
import com.example.hitcapp.utils.VoucherService;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final String PREFS_NAME = "SearchPrefs";
    private static final String KEY_HISTORY = "search_history";

    private ViewPager2 viewPagerBanner;
    private TabLayout tabLayoutIndicator;
    private RecyclerView rvFeatured, rvNew, rvSale;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private TextView tvCartBadge, tvSeeAllSale, tvSeeAllFeatured, tvSeeAllNew;
    
    // Search Views
    private View searchCardTrigger;
    private View layoutSearchOverlay;
    private EditText edtSearchOverlay;
    private ImageView btnBackSearch, btnClearSearch;
    private TextView tvBtnSearch;
    private RecyclerView rvSuggestions;
    private SearchSuggestionAdapter suggestionAdapter;
    private List<String> allProductNames = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();
    
    private ProductAdapter featuredAdapter, newAdapter, saleAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private VoucherService voucherService;
    private Handler bannerHandler = new Handler();
    private Runnable bannerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        voucherService = new VoucherService();
        
        if (auth.getCurrentUser() != null) {
            voucherService.addInitialVouchers(this, auth.getUid());
        }
        
        loadSearchHistory();
        initViews();
        setupInsets();
        initCustomBottomNav();
        setupRecyclerViews();
        setupSearchLogic();
        
        loadAllData();
        fetchProductNamesForSuggestions();

        swipeRefresh.setOnRefreshListener(this::loadAllData);
    }

    private void initViews() {
        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        rvFeatured = findViewById(R.id.rvFeatured);
        rvSale = findViewById(R.id.rvSale);
        rvNew = findViewById(R.id.rvNew);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvSeeAllSale = findViewById(R.id.tvSeeAllSale);
        tvSeeAllFeatured = findViewById(R.id.tvSeeAllFeatured);
        tvSeeAllNew = findViewById(R.id.tvSeeAllNew);
        
        // Search Overlay Views
        searchCardTrigger = findViewById(R.id.searchCardTrigger);
        layoutSearchOverlay = findViewById(R.id.layoutSearchOverlay);
        edtSearchOverlay = findViewById(R.id.edtSearchOverlay);
        btnBackSearch = findViewById(R.id.btnBackSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvBtnSearch = findViewById(R.id.tvBtnSearch);
        rvSuggestions = findViewById(R.id.rvSuggestions);
        
        findViewById(R.id.layoutCart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));

        View btnVoucher = findViewById(R.id.btnVoucher);
        if (btnVoucher != null) {
            btnVoucher.setOnClickListener(v -> startActivity(new Intent(this, VoucherActivity.class)));
        }

        View btnOrders = findViewById(R.id.btnOrders);
        if (btnOrders != null) {
            btnOrders.setOnClickListener(v -> startActivity(new Intent(this, OderActivity.class)));
        }
        
        if (tvSeeAllSale != null) tvSeeAllSale.setOnClickListener(v -> openProductList("FLASH_SALE"));
        if (tvSeeAllFeatured != null) tvSeeAllFeatured.setOnClickListener(v -> openProductList("FEATURED"));
        if (tvSeeAllNew != null) tvSeeAllNew.setOnClickListener(v -> openProductList("NEW"));
    }

    private void setupSearchLogic() {
        suggestionAdapter = new SearchSuggestionAdapter(this::performSearch);
        suggestionAdapter.setOnDeleteHistoryListener(this::deleteHistoryItem);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);

        searchCardTrigger.setOnClickListener(v -> {
            layoutSearchOverlay.setVisibility(View.VISIBLE);
            edtSearchOverlay.requestFocus();
            showKeyboard(edtSearchOverlay);
            showSearchHistory();
        });

        btnBackSearch.setOnClickListener(v -> {
            layoutSearchOverlay.setVisibility(View.GONE);
            hideKeyboard(edtSearchOverlay);
            edtSearchOverlay.setText(""); // Clear text on back
        });

        btnClearSearch.setOnClickListener(v -> {
            edtSearchOverlay.setText("");
            showSearchHistory();
        });

        tvBtnSearch.setOnClickListener(v -> performSearch(edtSearchOverlay.getText().toString()));

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
        List<String> filtered = allProductNames.stream()
                .filter(name -> name.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        suggestionAdapter.setData(filtered, query);
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
        
        layoutSearchOverlay.setVisibility(View.GONE);
        hideKeyboard(edtSearchOverlay);
        edtSearchOverlay.setText(""); // Clear text after search
        
        Intent intent = new Intent(this, ProductActivity.class);
        intent.putExtra("SEARCH_QUERY", q);
        startActivity(intent);
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
        if (layoutSearchOverlay.getVisibility() == View.VISIBLE) {
            layoutSearchOverlay.setVisibility(View.GONE);
            edtSearchOverlay.setText(""); // Clear text on back
        } else {
            super.onBackPressed();
        }
    }

    private void openProductList(String filterType) {
        Intent intent = new Intent(this, ProductActivity.class);
        intent.putExtra("FILTER_TYPE", filterType);
        startActivity(intent);
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
                        if (qty != null) totalQuantity += qty.intValue();
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
        View topBar = findViewById(R.id.topBarCard);
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

    private void setupRecyclerViews() {
        saleAdapter = new ProductAdapter(this::openProductDetail);
        saleAdapter.setLimit(4);
        rvSale.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSale.setAdapter(saleAdapter);

        featuredAdapter = new ProductAdapter(this::openProductDetail);
        featuredAdapter.setLimit(4);
        rvFeatured.setLayoutManager(new GridLayoutManager(this, 2));
        rvFeatured.setAdapter(featuredAdapter);
        rvFeatured.setNestedScrollingEnabled(false);

        newAdapter = new ProductAdapter(this::openProductDetail);
        newAdapter.setLimit(4);
        rvNew.setLayoutManager(new GridLayoutManager(this, 2));
        rvNew.setAdapter(newAdapter);
        rvNew.setNestedScrollingEnabled(false);
    }

    private void loadAllData() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        fetchBanners();
        fetchFilteredProducts("isSale", true, saleAdapter, 4);
        fetchFilteredProducts("isFeatured", true, featuredAdapter, 4);
        fetchFilteredProducts("isNew", true, newAdapter, 4);
    }

    private void fetchBanners() {
        db.collection("banners")
                .orderBy("priority", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BannerItem> banners = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String url = doc.getString("imageUrl");
                        String title = doc.getString("title");
                        if (url != null) banners.add(new BannerItem(url, title != null ? title : ""));
                    }
                    if (!banners.isEmpty()) {
                        setupBannerSlider(banners);
                        viewPagerBanner.setVisibility(View.VISIBLE);
                        tabLayoutIndicator.setVisibility(View.VISIBLE);
                    } else {
                        viewPagerBanner.setVisibility(View.GONE);
                        tabLayoutIndicator.setVisibility(View.GONE);
                    }
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> {
                    viewPagerBanner.setVisibility(View.GONE);
                    tabLayoutIndicator.setVisibility(View.GONE);
                    checkAllLoaded();
                });
    }

    private void fetchFilteredProducts(String field, boolean value, ProductAdapter adapter, int limit) {
        db.collection("products")
                .whereEqualTo(field, value)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> products = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            products.add(p);
                        }
                    }
                    adapter.setData(products);
                    checkAllLoaded();
                })
                .addOnFailureListener(e -> checkAllLoaded());
    }

    private void setupBannerSlider(List<BannerItem> banners) {
        viewPagerBanner.setAdapter(new BannerAdapter(banners));
        new TabLayoutMediator(tabLayoutIndicator, viewPagerBanner, (tab, position) -> {}).attach();
        
        bannerHandler.removeCallbacks(bannerRunnable);
        bannerRunnable = () -> {
            int nextItem = (viewPagerBanner.getCurrentItem() + 1) % banners.size();
            viewPagerBanner.setCurrentItem(nextItem, true);
            bannerHandler.postDelayed(bannerRunnable, 5000);
        };
        bannerHandler.postDelayed(bannerRunnable, 5000);
    }

    private void checkAllLoaded() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
    }

    private void openProductDetail(Product product) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getId());
        startActivity(intent);
    }

    private void initCustomBottomNav() {
        tabHome = findViewById(R.id.tabHome);
        tabProduct = findViewById(R.id.tabProduct);
        tabNotification = findViewById(R.id.tabNotification);
        tabProfile = findViewById(R.id.tabProfile);

        if (tabProduct != null) {
            tabProduct.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductActivity.class);
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
            int color = isSelected ? Color.parseColor("#1E3A8A") : Color.parseColor("#94A3B8");
            if (icon != null) icon.setColorFilter(color);
            if (text != null) {
                text.setTextColor(color);
                text.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        loadSearchHistory(); // Refresh history when coming back from ProductActivity
        if (tabHome != null) selectTab(tabHome);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    private static class BannerItem {
        String url, title;
        BannerItem(String url, String title) { this.url = url; this.title = title; }
    }

    private static class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private List<BannerItem> items;
        BannerAdapter(List<BannerItem> items) { this.items = items; }
        @NonNull @Override public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_banner, p, false);
            return new BannerViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull BannerViewHolder h, int pos) {
            BannerItem item = items.get(pos);
            Glide.with(h.itemView.getContext()).load(item.url).into(h.imgBanner);
            h.tvTitle.setText(item.title);
            h.tvTitle.setVisibility(item.title.isEmpty() ? View.GONE : View.VISIBLE);
        }
        @Override public int getItemCount() { return items.size(); }
        class BannerViewHolder extends RecyclerView.ViewHolder {
            ImageView imgBanner;
            TextView tvTitle;
            BannerViewHolder(View v) { 
                super(v); 
                imgBanner = v.findViewById(R.id.imgBannerItem);
                tvTitle = v.findViewById(R.id.tvBannerTitle);
            }
        }
    }
}
