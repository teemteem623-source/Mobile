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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NoticeActivity extends AppCompatActivity {

    private RecyclerView rvNotices;
    private List<NoticeItem> fullNoticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private SwipeRefreshLayout swipeRefresh;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration noticeListener;
    private String currentCategory = "";

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
        
        syncOrderNotifications();
        startListeningNotices();
        
        swipeRefresh.setOnRefreshListener(() -> {
            syncOrderNotifications();
            startListeningNotices();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void syncOrderNotifications() {
        String currentUserId = auth.getUid();
        if (currentUserId == null) return;

        db.collection("orders")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(orderSnapshots -> {
                    for (QueryDocumentSnapshot orderDoc : orderSnapshots) {
                        String firestoreId = orderDoc.getId();
                        String rawStatus = orderDoc.getString("status");
                        String orderCode = orderDoc.getString("orderId");

                        String status = (rawStatus != null) ? rawStatus.trim() : "";
                        if (status.isEmpty() || status.equalsIgnoreCase("Chờ xác nhận")) continue;

                        String title = "Cập nhật đơn hàng";
                        String content = "";

                        String s = status.toLowerCase();
                        if (s.contains("xác nhận")) {
                            content = "Đơn hàng #" + orderCode + " đã được shop xác nhận và đang chuẩn bị.";
                        } else if (s.contains("vận chuyển")) {
                            content = "Đơn hàng #" + orderCode + " đã được bàn giao cho đơn vị vận chuyển.";
                        } else if (s.contains("đang giao")) {
                            content = "Đơn hàng #" + orderCode + " đang được shipper giao đến bạn.";
                        } else if (s.contains("thành công") || s.contains("hoàn tất") || s.contains("hoàn thành")) {
                            title = "Giao hàng thành công";
                            content = "Đơn hàng #" + orderCode + " đã được giao thành công. Cảm ơn bạn đã ủng hộ!";
                        } else if (s.contains("hủy")) {
                            title = "Hủy đơn hàng thành công";
                            content = "Đơn hàng #" + orderCode + " của bạn đã được cập nhật trạng thái: Đã hủy.";
                        }

                        if (content.isEmpty()) continue;

                        final String fTitle = title;
                        final String fContent = content;

                        // Truy vấn theo oderId (đơn giản, không cần composite index)
                        db.collection("notifications")
                                .whereEqualTo("oderId", firestoreId)
                                .get()
                                .addOnSuccessListener(noticeSnapshots -> {
                                    boolean exists = false;
                                    for (QueryDocumentSnapshot doc : noticeSnapshots) {
                                        String existingContent = doc.getString("content");
                                        if (fContent.equals(existingContent)) {
                                            exists = true;
                                            break;
                                        }
                                    }

                                    if (!exists) {
                                        Map<String, Object> n = new HashMap<>();
                                        n.put("userId", currentUserId);
                                        n.put("title", fTitle);
                                        n.put("content", fContent);
                                        n.put("type", "Đơn hàng");
                                        n.put("oderId", firestoreId);
                                        n.put("timestamp", FieldValue.serverTimestamp());
                                        db.collection("notifications").add(n);
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabNotification != null) selectTab(tabNotification);
        syncOrderNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticeListener != null) noticeListener.remove();
    }

    private void initViews() {
        rvNotices = findViewById(R.id.rvNotices);
        swipeRefresh = findViewById(R.id.swipeRefresh);
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

        if (tabHome != null) tabHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        if (tabProduct != null) tabProduct.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        if (tabProfile != null) tabProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
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
        adapter = new NoticeAdapter(new ArrayList<>(fullNoticeList), this::onNoticeClick);
        rvNotices.setAdapter(adapter);
    }

    private void onNoticeClick(NoticeItem item) {
        String type = item.type;
        String title = (item.title != null) ? item.title.toLowerCase() : "";

        if ("Khuyến mãi".equals(type)) {
            startActivity(new Intent(this, VoucherActivity.class));
        } else if ("Đơn hàng".equals(type)) {
            if (item.oderId != null && !item.oderId.isEmpty()) {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra("ORDER_ID", item.oderId);
                startActivity(intent);
            }
        } else if ("Hệ thống".equals(type)) {
            if (title.contains("thông tin")) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (title.contains("tài khoản")) {
                startActivity(new Intent(this, AccountManagementActivity.class));
            }
        }
    }

    private void startListeningNotices() {
        String currentUserId = auth.getUid();
        if (currentUserId == null) return;

        if (noticeListener != null) noticeListener.remove();

        noticeListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NoticeActivity", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        List<NoticeItem> firestoreItems = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            String title = doc.getString("title");
                            String content = doc.getString("content");
                            String type = doc.getString("type");
                            String oderId = doc.getString("oderId");
                            com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                            
                            Date date = (ts != null) ? ts.toDate() : new Date();
                            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);

                            int icon = android.R.drawable.ic_popup_reminder;
                            String color = "#8B5CF6";
                            if ("Khuyến mãi".equals(type)) {
                                icon = android.R.drawable.ic_menu_send;
                                color = "#E11D48";
                            } else if ("Đơn hàng".equals(type)) {
                                icon = android.R.drawable.ic_menu_save;
                                color = "#3B82F6";
                            } else if ("Hệ thống".equals(type)) {
                                icon = android.R.drawable.ic_menu_add;
                                color = "#10B981";
                            }
                            
                            NoticeItem item = new NoticeItem(title, content, icon, color, type, time, oderId);
                            item.timestampValue = date.getTime();
                            firestoreItems.add(item);
                        }
                        
                        Collections.sort(firestoreItems, (a, b) -> Long.compare(b.timestampValue, a.timestampValue));
                        fullNoticeList.clear();
                        fullNoticeList.addAll(firestoreItems);
                        addDefaultNotices();
                        filterAndDisplay();
                    }
                });
    }

    private void addDefaultNotices() {
        boolean hasVoucher5tr = false;
        boolean hasPersonalInfo = false;
        for (NoticeItem item : fullNoticeList) {
            if (item.title == null) continue;
            if (item.title.contains("5 triệu")) hasVoucher5tr = true;
            if (item.title.toLowerCase().contains("thông tin cá nhân")) hasPersonalInfo = true;
        }
        
        if (!hasVoucher5tr) {
            fullNoticeList.add(new NoticeItem("Voucher 5 triệu cho bạn", "Bạn vừa nhận được voucher giảm giá 5.000.000đ cho đơn hàng tiếp theo.", android.R.drawable.ic_menu_send, "#E11D48", "Khuyến mãi", "Mới", ""));
            fullNoticeList.add(new NoticeItem("3 Voucher miễn phí vận chuyển", "Nhận ngay 3 mã freeship cực hời trong ví voucher của bạn.", android.R.drawable.ic_menu_send, "#E11D48", "Khuyến mãi", "Mới", ""));
        }
        if (!hasPersonalInfo) {
            fullNoticeList.add(new NoticeItem("Thêm thông tin cá nhân", "Cập nhật hồ sơ để nhận thêm nhiều ưu đãi và bảo mật tài khoản tốt hơn.", android.R.drawable.ic_menu_add, "#10B981", "Hệ thống", "Mới", ""));
        }
    }

    private void setupChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroup);
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) { currentCategory = ""; }
            else {
                int id = checkedIds.get(0);
                if (id == R.id.chipAll) currentCategory = "";
                else if (id == R.id.chipUuDai) currentCategory = "Khuyến mãi";
                else if (id == R.id.chipDonHang) currentCategory = "Đơn hàng";
                else if (id == R.id.chipHeThong) currentCategory = "Hệ thống";
            }
            filterAndDisplay();
        });
    }

    private void filterAndDisplay() {
        if (currentCategory.isEmpty()) {
            adapter.updateList(new ArrayList<>(fullNoticeList));
        } else {
            List<NoticeItem> filtered = new ArrayList<>();
            for (NoticeItem item : fullNoticeList) {
                if (currentCategory.equals(item.type)) filtered.add(item);
            }
            adapter.updateList(filtered);
        }
    }

    private static class NoticeItem {
        String title, content, colorHex, type, time, oderId; int iconRes; long timestampValue;
        NoticeItem(String t, String c, int icon, String color, String type, String time, String oderId) { 
            this.title = t; this.content = c; this.iconRes = icon; this.colorHex = color; this.type = type; this.time = time; this.oderId = oderId;
        }
    }

    private interface OnNoticeClickListener { void onNoticeClick(NoticeItem item); }

    private static class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {
        private List<NoticeItem> list;
        private OnNoticeClickListener listener;
        NoticeAdapter(List<NoticeItem> list, OnNoticeClickListener listener) { this.list = list; this.listener = listener; }
        public void updateList(List<NoticeItem> newList) { this.list = newList; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_notice, p, false);
            return new ViewHolder(v);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            NoticeItem item = list.get(pos);
            h.tvTitle.setText(item.title); h.tvContent.setText(item.content); h.tvTime.setText(item.time);
            h.imgIcon.setImageResource(item.iconRes);
            try { h.imgIcon.setColorFilter(Color.parseColor(item.colorHex)); } catch (Exception e) {}
            h.itemView.setOnClickListener(v -> { if (listener != null) listener.onNoticeClick(item); });
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvTime; ImageView imgIcon;
            ViewHolder(View v) { super(v); tvTitle = v.findViewById(R.id.tvTitle); tvContent = v.findViewById(R.id.tvContent); tvTime = v.findViewById(R.id.tvTime); imgIcon = v.findViewById(R.id.imgNoticeIcon); }
        }
    }
}
