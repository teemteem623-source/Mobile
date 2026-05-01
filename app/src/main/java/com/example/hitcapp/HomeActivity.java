package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.example.hitcapp.models.Product;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private ViewPager2 viewPagerBanner;
    private TabLayout tabLayoutIndicator;
    private RecyclerView rvFeatured, rvNew, rvSale;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private TextView tvCartBadge, tvSeeAllSale, tvSeeAllFeatured, tvSeeAllNew;
    
    private ProductAdapter featuredAdapter, newAdapter, saleAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Handler bannerHandler = new Handler();
    private Runnable bannerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        initViews();
        setupInsets();
        initCustomBottomNav();
        setupRecyclerViews();
        setupSearch();
        
        loadAllData();

        swipeRefresh.setOnRefreshListener(this::loadAllData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
        if (tabHome != null) selectTab(tabHome);
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
        
        findViewById(R.id.layoutCart).setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        
        if (tvSeeAllSale != null) {
            tvSeeAllSale.setOnClickListener(v -> openProductList("FLASH_SALE"));
        }
        if (tvSeeAllFeatured != null) {
            tvSeeAllFeatured.setOnClickListener(v -> openProductList("FEATURED"));
        }
        if (tvSeeAllNew != null) {
            tvSeeAllNew.setOnClickListener(v -> openProductList("NEW"));
        }
    }

    private void openProductList(String filterType) {
        Intent intent = new Intent(HomeActivity.this, ProductActivity.class);
        intent.putExtra("FILTER_TYPE", filterType);
        startActivity(intent);
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
        View topBar = findViewById(R.id.topBarCard);
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

    private void setupSearch() {
        View edtSearch = findViewById(R.id.edtSearch);
        if (edtSearch instanceof TextView) {
            ((TextView) edtSearch).setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = ((TextView) edtSearch).getText().toString().trim();
                    Intent intent = new Intent(HomeActivity.this, ProductActivity.class);
                    intent.putExtra("SEARCH_QUERY", query);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
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
                    Log.e(TAG, "Error fetching banners", e);
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
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading filtered products: " + field, e);
                    checkAllLoaded();
                });
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
    protected void onDestroy() {
        super.onDestroy();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    private static class BannerItem {
        String url;
        String title;
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
