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
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class NoticeActivity extends AppCompatActivity {

    private RecyclerView rvNotices;
    private List<NoticeItem> fullNoticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private LinearLayout tabHome, tabProduct, tabNotification, tabProfile;
    private SwipeRefreshLayout swipeRefresh;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration noticeListener;
    private ListenerRegistration ordersListener;
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
        
        // Bắt đầu lắng nghe thông báo và đơn hàng thời gian thực
        startListeningNotices();
        startListeningOrders();
        
        swipeRefresh.setOnRefreshListener(() -> {
            if (auth.getUid() != null) {
                db.collection("orders").whereEqualTo("userId", auth.getUid()).get()
                        .addOnSuccessListener(this::syncOrderNotifications);
            }
            swipeRefresh.setRefreshing(false);
        });
    }

    private void startListeningOrders() {
        String currentUserId = auth.getUid();
        if (currentUserId == null) return;
        
        if (ordersListener != null) ordersListener.remove();
        
        // Lắng nghe sự thay đổi của đơn hàng TRỰC TIẾP từ Firebase Console
        ordersListener = db.collection("orders")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("NoticeActivity", "Lỗi lắng nghe đơn hàng", error);
                        return;
                    }
                    if (value != null) {
                        syncOrderNotifications(value);
                    }
                });
    }

    private void syncOrderNotifications(QuerySnapshot orderSnapshots) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) return;

        // Fetch danh sách thông báo hiện có để đối soát tránh trùng lặp
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(noticesSnapshot -> {
                    Set<String> usedVoucherCodes = new HashSet<>();
                    Map<String, Set<String>> existingOrderTitles = new HashMap<>();

                    for (QueryDocumentSnapshot doc : noticesSnapshot) {
                        String vCode = doc.getString("voucherCode");
                        if (vCode != null) usedVoucherCodes.add(vCode);

                        String oid = doc.getString("oderId");
                        String title = doc.getString("title");
                        if (oid != null && title != null) {
                            if (!existingOrderTitles.containsKey(oid)) {
                                existingOrderTitles.put(oid, new HashSet<>());
                            }
                            existingOrderTitles.get(oid).add(title);
                        }
                    }

                    for (QueryDocumentSnapshot orderDoc : orderSnapshots) {
                        String firestoreId = orderDoc.getId();
                        String rawStatus = orderDoc.getString("status");
                        String orderCode = orderDoc.getString("orderId");
                        String status = (rawStatus != null) ? rawStatus.trim() : "";
                        if (status.isEmpty()) continue;

                        String title = "";
                        String content = "";
                        String s = status.toLowerCase();
                        boolean isNewOrder = false;

                        // Nhận diện trạng thái thông minh (Hỗ trợ nhiều từ khóa)
                        if (s.contains("chờ") || s.contains("đặt") || s.contains("pending")) {
                            title = "Mua hàng thành công";
                            content = "Đơn hàng #" + orderCode + " Đang chờ xác nhận";
                            isNewOrder = true;
                        } else if (s.contains("xác nhận") || s.contains("confirmed")) {
                            title = "Đơn hàng đã xác nhận";
                            content = "Đơn hàng #" + orderCode + " đã được shop xác nhận và đang chuẩn bị hàng.";
                        } else if (s.contains("vận chuyển") || s.contains("shipping")) {
                            title = "Đơn hàng đang vận chuyển";
                            content = "Đơn hàng #" + orderCode + " đã được bàn giao cho đơn vị vận chuyển.";
                        } else if (s.contains("đang giao") || s.contains("delivering")) {
                            title = "Đơn hàng đang giao";
                            content = "Đơn hàng #" + orderCode + " đang được giao đến bạn. Hãy để ý điện thoại nhé!";
                        } else if (s.contains("giao") && (s.contains("thành công") || s.contains("xong") || s.contains("delivered"))) {
                            title = "Giao hàng thành công";
                            content = "Đơn hàng #" + orderCode + " đã được giao thành công. Cảm ơn bạn đã ủng hộ!";
                        } else if (s.contains("hủy") || s.contains("cancelled")) {
                            title = "Đơn hàng đã hủy";
                            content = "Đơn hàng #" + orderCode + " của bạn đã bị hủy.";
                        }

                        if (title.isEmpty()) continue;

                        Set<String> titlesForThisOrder = existingOrderTitles.get(firestoreId);
                        boolean hasStatusNotice = (titlesForThisOrder != null && titlesForThisOrder.contains(title));
                        
                        if (!hasStatusNotice) {
                            Map<String, Object> n = new HashMap<>();
                            n.put("userId", currentUserId);
                            n.put("title", title);
                            n.put("content", content);
                            n.put("type", "Đơn hàng");
                            n.put("oderId", firestoreId);
                            n.put("timestamp", FieldValue.serverTimestamp());
                            
                            // Tạo Document ID duy nhất theo đơn hàng + trạng thái để không bị lặp
                            String statusNoticeId = "STATUS_" + firestoreId + "_" + title.replace(" ", "_");
                            db.collection("notifications").document(statusNoticeId).set(n);
                        }

                        // Cấp mã Voucher may mắn HITC-xx cho đơn hàng mới
                        boolean hasVoucherNotice = (titlesForThisOrder != null && titlesForThisOrder.contains("Nhận mã Voucher may mắn 🎁"));
                        if (isNewOrder && !hasVoucherNotice) {
                            String luckyCode = pickUnusedCode(usedVoucherCodes);
                            if (luckyCode != null) {
                                usedVoucherCodes.add(luckyCode); 
                                Map<String, Object> v = new HashMap<>();
                                v.put("userId", currentUserId);
                                v.put("title", "Nhận mã Voucher may mắn 🎁");
                                v.put("content", "Cảm ơn bạn đã mua sắm! Hãy dùng mã " + luckyCode + " tại trang Ưu đãi để nhận quà tặng tri ân.");
                                v.put("type", "Khuyến mãi");
                                v.put("voucherCode", luckyCode);
                                v.put("oderId", firestoreId);
                                v.put("timestamp", FieldValue.serverTimestamp());
                                
                                db.collection("notifications").document("LUCKY_VOUCHER_" + firestoreId).set(v);
                            }
                        }
                    }
                });
    }

    private String pickUnusedCode(Set<String> usedCodes) {
        List<String> pool = new ArrayList<>();
        String[] priorityCodes = {"LUCKY88", "GIFT99", "HITCAPP", "UUDAI100", "LUCKY1", "HITC88", "PROMO10"};
        for (String c : priorityCodes) pool.add(c);
        for (int i = 1; i <= 50; i++) pool.add(String.format(Locale.getDefault(), "HITC-%02d", i));
        
        List<String> available = new ArrayList<>();
        for (String code : pool) if (!usedCodes.contains(code)) available.add(code);

        if (available.isEmpty()) return null; 
        return available.get(new Random().nextInt(available.size()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabNotification != null) selectTab(tabNotification);
        // Sync một lần khi quay lại app
        if (auth.getUid() != null) {
            db.collection("orders").whereEqualTo("userId", auth.getUid()).get().addOnSuccessListener(this::syncOrderNotifications);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noticeListener != null) noticeListener.remove();
        if (ordersListener != null) ordersListener.remove();
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
        if ("Khuyến mãi".equals(item.type)) {
            Intent intent = new Intent(this, VoucherActivity.class);
            if (item.voucherCode != null) intent.putExtra("AUTO_FILL_CODE", item.voucherCode);
            startActivity(intent);
        } else if ("Đơn hàng".equals(item.type) && item.oderId != null) {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", item.oderId);
            startActivity(intent);
        } else if ("Hệ thống".equals(item.type)) {
            String title = (item.title != null) ? item.title.toLowerCase() : "";
            String content = (item.content != null) ? item.content.toLowerCase() : "";
            
            if (title.contains("thông tin cá nhân") || content.contains("cập nhật hồ sơ") || content.contains("thông tin cá nhân")) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (title.contains("tài khoản") || title.contains("liên kết") || content.contains("tài khoản") || content.contains("liên kết")) {
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
                    if (error != null) return;
                    if (value != null) {
                        List<NoticeItem> firestoreItems = new ArrayList<>();
                        Set<String> seenKeys = new HashSet<>(); 
                        boolean foundWelcomeGift = false;
                        boolean foundPersonalInfo = false;

                        for (QueryDocumentSnapshot doc : value) {
                            String title = (doc.getString("title") != null) ? doc.getString("title").trim() : "";
                            String content = (doc.getString("content") != null) ? doc.getString("content").trim() : "";
                            String type = doc.getString("type");
                            String vCode = doc.getString("voucherCode");

                            String t = title.toLowerCase();
                            String c = content.toLowerCase();

                            boolean isWelcome = t.contains("quà tặng") || t.contains("thành viên mới");
                            boolean isProfile = t.contains("thông tin cá nhân") || t.contains("hồ sơ");

                            if (isWelcome) { if (foundWelcomeGift) continue; foundWelcomeGift = true; }
                            else if (isProfile) { if (foundPersonalInfo) continue; foundPersonalInfo = true; }
                            else {
                                String uniqueKey = (title + "|" + content).toLowerCase();
                                if (seenKeys.contains(uniqueKey)) continue; 
                                seenKeys.add(uniqueKey);
                            }

                            String oderId = doc.getString("oderId");
                            Object ts = doc.get("timestamp");
                            long timeVal = 0;
                            if (ts instanceof Long) timeVal = (Long) ts;
                            else if (ts instanceof com.google.firebase.Timestamp) timeVal = ((com.google.firebase.Timestamp) ts).toDate().getTime();
                            else timeVal = System.currentTimeMillis();

                            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(timeVal));

                            int icon = R.drawable.update; String colorHex = "#10B981"; 

                            if ("Khuyến mãi".equals(type)) {
                                colorHex = "#EF4444"; 
                                if (isWelcome) icon = R.drawable.giftbox;
                                else icon = R.drawable.voucher;
                            } else if ("Đơn hàng".equals(type)) {
                                colorHex = "#3B82F6"; 
                                if (t.contains("mua hàng thành công")) icon = R.drawable.booking;
                                else if (t.contains("xác nhận")) icon = R.drawable.checklist;
                                else if (t.contains("vận chuyển")) icon = R.drawable.tracking;
                                else if (t.contains("đang giao")) icon = R.drawable.shop;
                                else if (t.contains("giao hàng thành công")) icon = R.drawable.product;
                                else if (t.contains("hủy")) icon = R.drawable.reject;
                            } else if ("Hệ thống".equals(type)) {
                                if (t.contains("tài khoản") || t.contains("liên kết")) icon = R.drawable.people;
                                else if (t.contains("thông tin cá nhân")) icon = R.drawable.adduser;
                                else if (t.contains("thu hồi voucher")) { icon = R.drawable.voucherdelete; colorHex = "#EF4444"; }
                                else if (t.contains("thu hồi mã quà tặng")) { icon = R.drawable.giftdelete; colorHex = "#EF4444"; }
                            }
                            
                            NoticeItem item = new NoticeItem(title, content, icon, colorHex, type, time, oderId);
                            item.voucherCode = vCode;
                            item.timestampValue = timeVal;
                            firestoreItems.add(item);
                        }
                        
                        fullNoticeList.clear();
                        fullNoticeList.addAll(firestoreItems);
                        addDefaultNotices(foundWelcomeGift, foundPersonalInfo);
                        Collections.sort(fullNoticeList, (a, b) -> Long.compare(b.timestampValue, a.timestampValue));
                        filterAndDisplay();
                    }
                });
    }

    private void addDefaultNotices(boolean foundWelcomeGift, boolean foundPersonalInfo) {
        if (!foundPersonalInfo) {
            NoticeItem p = new NoticeItem("Thêm thông tin cá nhân", "Cập nhật hồ sơ để nhận thêm nhiều ưu đãi.", R.drawable.adduser, "#10B981", "Hệ thống", "Hệ thống", "");
            p.timestampValue = 0; fullNoticeList.add(p);
        }
        if (!foundWelcomeGift) {
            NoticeItem g = new NoticeItem("Gói quà tặng thành viên mới 🎁", "Chào mừng bạn! Nhận ngay ưu đãi cho lần đầu mua sắm.", R.drawable.giftbox, "#EF4444", "Khuyến mãi", "Hệ thống", "");
            g.timestampValue = 0; fullNoticeList.add(g);
        }
    }

    private void setupChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroup);
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) currentCategory = "";
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
        if (currentCategory.isEmpty()) adapter.updateList(new ArrayList<>(fullNoticeList));
        else {
            List<NoticeItem> filtered = new ArrayList<>();
            for (NoticeItem item : fullNoticeList) if (currentCategory.equals(item.type)) filtered.add(item);
            adapter.updateList(filtered);
        }
    }

    private static class NoticeItem {
        String title, content, colorHex, type, time, oderId, voucherCode; int iconRes; long timestampValue;
        NoticeItem(String t, String c, int icon, String color, String type, String time, String oderId) { 
            this.title = t; this.content = c; this.iconRes = icon; this.colorHex = color; this.type = type; this.time = time; this.oderId = oderId;
        }
    }

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
            if (item.colorHex != null) h.imgIcon.setColorFilter(Color.parseColor(item.colorHex));
            h.itemView.setOnClickListener(v -> listener.onNoticeClick(item));
        }
        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvTime; ImageView imgIcon;
            ViewHolder(View v) { super(v); tvTitle = v.findViewById(R.id.tvTitle); tvContent = v.findViewById(R.id.tvContent); tvTime = v.findViewById(R.id.tvTime); imgIcon = v.findViewById(R.id.imgNoticeIcon); }
        }
    }
    private interface OnNoticeClickListener { void onNoticeClick(NoticeItem item); }
}
