package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoticeActivity extends AppCompatActivity {

    private RecyclerView rvNotices;
    private List<NoticeItem> fullNoticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private SwipeRefreshLayout swipeRefresh;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupInsets();
        initCustomBottomNav();
        setupRecyclerView();
        setupChips();
        
        loadNoticesFromFirestore();
        swipeRefresh.setOnRefreshListener(this::loadNoticesFromFirestore);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabNotification != null) selectTab(tabNotification);
        loadNoticesFromFirestore();
    }

    private void initViews() {
        rvNotices = findViewById(R.id.rvNotices);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        View imgCart = findViewById(R.id.imgCart);
        if (imgCart != null) {
            imgCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
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
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }
        if (tabProduct != null) {
            tabProduct.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductActivity.class);
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

    private void setupRecyclerView() {
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoticeAdapter(new ArrayList<>(fullNoticeList));
        rvNotices.setAdapter(adapter);
    }

    private void loadNoticesFromFirestore() {
        if (auth.getCurrentUser() == null) {
            if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            return;
        }

        db.collection("notifications")
                .whereEqualTo("userId", auth.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullNoticeList.clear();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String content = doc.getString("content");
                        String category = doc.getString("category");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                        
                        String time = "Vừa xong";
                        if (ts != null) {
                            time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(ts.toDate());
                        }

                        int icon = android.R.drawable.ic_popup_reminder;
                        String color = "#8B5CF6";
                        if ("Khuyến mãi".equals(category)) {
                            icon = android.R.drawable.ic_menu_send;
                            color = "#E11D48";
                        } else if ("Đơn hàng".equals(category)) {
                            icon = android.R.drawable.ic_menu_save;
                            color = "#3B82F6";
                        } else if ("Hệ thống".equals(category)) {
                            icon = android.R.drawable.ic_menu_add;
                            color = "#10B981";
                        }
                        
                        fullNoticeList.add(new NoticeItem(title, content, icon, color, category, time));
                    }
                    
                    if (fullNoticeList.isEmpty()) {
                        addPlaceholderNotices();
                    }
                    
                    adapter.updateList(new ArrayList<>(fullNoticeList));
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("NoticeActivity", "Lỗi tải thông báo: " + e.getMessage());
                    if (fullNoticeList.isEmpty()) addPlaceholderNotices();
                    adapter.updateList(new ArrayList<>(fullNoticeList));
                    if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
    }

    private void addPlaceholderNotices() {
        fullNoticeList.add(new NoticeItem("Siêu ưu đãi cuối tuần", "Giảm giá 50% cho tất cả dòng iPhone tại cửa hàng. Duy nhất Chủ nhật này!", android.R.drawable.ic_menu_send, "#E11D48", "Khuyến mãi", "10:30"));
        fullNoticeList.add(new NoticeItem("Đơn hàng đang đến", "Đơn hàng #TT9988 đang được nhân viên giao hàng vận chuyển đến bạn.", android.R.drawable.ic_menu_save, "#3B82F6", "Đơn hàng", "09:15"));
        fullNoticeList.add(new NoticeItem("Ví của bạn đã được nạp tiền", "Bạn vừa nạp thành công 500.000đ vào ví TT-Pay.", android.R.drawable.ic_menu_add, "#10B981", "Hệ thống", "Hôm qua"));
    }

    private void setupChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroup);
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) { adapter.updateList(new ArrayList<>(fullNoticeList)); return; }
            int id = checkedIds.get(0);
            String cat = "";
            if (id == R.id.chipAll) cat = "";
            else if (id == R.id.chipUuDai) cat = "Khuyến mãi";
            else if (id == R.id.chipDonHang) cat = "Đơn hàng";
            else if (id == R.id.chipHeThong) cat = "Hệ thống";
            
            if (cat.isEmpty()) {
                adapter.updateList(new ArrayList<>(fullNoticeList));
            } else {
                List<NoticeItem> filtered = new ArrayList<>();
                for (NoticeItem item : fullNoticeList) if (item.category.equals(cat)) filtered.add(item);
                adapter.updateList(filtered);
            }
        });
    }

    private static class NoticeItem {
        String title, content, colorHex, category, time; int iconRes;
        NoticeItem(String t, String c, int icon, String color, String cat, String time) { 
            this.title = t; this.content = c; this.iconRes = icon; this.colorHex = color; this.category = cat; this.time = time;
        }
    }

    private static class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {
        private List<NoticeItem> list;
        NoticeAdapter(List<NoticeItem> list) { this.list = list; }
        public void updateList(List<NoticeItem> newList) { this.list = newList; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_notice, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            NoticeItem item = list.get(pos);
            h.tvTitle.setText(item.title); 
            h.tvContent.setText(item.content); 
            h.tvTime.setText(item.time);
            h.imgIcon.setImageResource(item.iconRes);
            try { h.imgIcon.setColorFilter(Color.parseColor(item.colorHex)); } catch (Exception e) {}
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvTime; ImageView imgIcon;
            ViewHolder(View v) { 
                super(v); 
                tvTitle = v.findViewById(R.id.tvTitle); 
                tvContent = v.findViewById(R.id.tvContent); 
                tvTime = v.findViewById(R.id.tvTime);
                imgIcon = v.findViewById(R.id.imgNoticeIcon); 
            }
        }
    }
}
