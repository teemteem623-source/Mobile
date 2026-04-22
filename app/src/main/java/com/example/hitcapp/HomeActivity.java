package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ViewPager2 viewPagerBanner;
    private Handler bannerHandler = new Handler();
    private Runnable bannerRunnable;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // --- XỬ LÝ SAFE AREA (Insets) ---
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

        // --- XỬ LÝ TÌM KIẾM TỪ TRANG CHỦ ---
        TextInputLayout tilSearch = findViewById(R.id.tilSearch);
        TextInputEditText edtSearch = findViewById(R.id.edtSearch);
        
        if (edtSearch != null) {
            // Xử lý khi nhấn nút Search trên bàn phím ảo
            edtSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(edtSearch.getText().toString());
                    return true;
                }
                return false;
            });
        }

        if (tilSearch != null) {
            // Xử lý khi nhấn vào icon Tìm kiếm ở cuối khung nhập
            tilSearch.setEndIconOnClickListener(v -> {
                if (edtSearch != null) {
                    performSearch(edtSearch.getText().toString());
                }
            });
        }

        // Bottom Navigation logic
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) return true;
                else if (itemId == R.id.nav_products) {
                    startActivity(new Intent(this, ProductActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(this, NoticeActivity.class));
                    finish(); return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, UserActivity.class));
                    finish(); return true;
                }
                return false;
            });
        }

        // Cart Icon
        ImageView imgCart = findViewById(R.id.imgCart);
        if (imgCart != null) {
            imgCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        // Banners
        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        if (viewPagerBanner != null) {
            List<Integer> bannerImages = new ArrayList<>();
            bannerImages.add(R.drawable.banner);
            bannerImages.add(R.drawable.banner1);
            bannerImages.add(R.drawable.banner2);
            viewPagerBanner.setAdapter(new BannerAdapter(bannerImages));

            bannerRunnable = () -> {
                if (viewPagerBanner != null) {
                    int nextItem = (viewPagerBanner.getCurrentItem() + 1) % bannerImages.size();
                    viewPagerBanner.setCurrentItem(nextItem, true);
                    bannerHandler.postDelayed(bannerRunnable, 4000);
                }
            };
            bannerHandler.postDelayed(bannerRunnable, 4000);
        }

        // Sản phẩm nổi bật
        setupFeaturedProduct(R.id.cardProduct1, "iPhone 15 Pro", "28.990.000đ", R.drawable.iphone15pro);
        setupFeaturedProduct(R.id.cardProduct2, "Samsung S24", "25.490.000đ", R.drawable.samsungs24);
        setupFeaturedProduct(R.id.cardProduct3, "Xiaomi 14", "19.990.000đ", R.drawable.xiaomi14);
        setupFeaturedProduct(R.id.cardProduct4, "Oppo Find X7", "18.500.000đ", R.drawable.oppofindx7);
    }

    private void performSearch(String query) {
        Intent intent = new Intent(HomeActivity.this, ProductActivity.class);
        intent.putExtra("SEARCH_QUERY", query.trim());
        startActivity(intent);
    }

    private void setupFeaturedProduct(int cardId, String name, String price, int imageRes) {
        View card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, DetailActivity.class);
                intent.putExtra("PRODUCT_NAME", name);
                intent.putExtra("PRODUCT_PRICE", price);
                intent.putExtra("PRODUCT_IMAGE", imageRes);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    private static class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
        private List<Integer> images;
        BannerAdapter(List<Integer> images) { this.images = images; }
        @NonNull @Override public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_banner, p, false);
            return new BannerViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull BannerViewHolder h, int pos) {
            h.img.setImageResource(images.get(pos));
        }
        @Override public int getItemCount() { return images.size(); }
        static class BannerViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            BannerViewHolder(View v) { super(v); img = v.findViewById(R.id.imgBannerItem); }
        }
    }
}
