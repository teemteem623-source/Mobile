package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBarCard);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                
                if (topBar != null) {
                    topBar.setPadding(0, Math.max(0, systemBars.top - 20), 0, 0);
                }
                
                if (bottomNavigation != null) {
                    bottomNavigation.setPadding(0, 0, 0, Math.max(0, systemBars.bottom - 20));
                }
                
                return insets;
            });
        }

        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_products) {
                    startActivity(new Intent(this, ProductActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(this, NoticeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, UserActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }

        ImageView imgCart = findViewById(R.id.imgCart);
        if (imgCart != null) {
            imgCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }

        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        if (viewPagerBanner != null) {
            List<Integer> bannerImages = new ArrayList<>();
            bannerImages.add(R.drawable.phone_mockup);
            bannerImages.add(R.drawable.phone_mockup);
            bannerImages.add(R.drawable.phone_mockup);
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

        setupProductClicks();
    }

    private void setupProductClicks() {
        int[] ids = {R.id.cardProduct1, R.id.cardProduct2, R.id.cardProduct3, R.id.cardProduct4};
        for (int id : ids) {
            View card = findViewById(id);
            if (card != null) {
                card.setOnClickListener(v -> startActivity(new Intent(this, DetailActivity.class)));
            }
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
