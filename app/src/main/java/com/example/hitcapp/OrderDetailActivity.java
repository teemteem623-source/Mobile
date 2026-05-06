package com.example.hitcapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.hitcapp.models.CartItem;
import com.example.hitcapp.models.Order;
import com.example.hitcapp.models.OrderItem;
import com.example.hitcapp.models.Voucher;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private String orderId;
    private Order currentOrder;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration orderListener;
    private SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private TextView tvHeaderStatus, tvDetailStatusTitle, tvDeliveryNote, tvDetailTime, tvDetailStatusDesc;
    private TextView tvDetailBuyerName, tvDetailAddress, tvDetailOrderId, tvDetailPaymentMethod;
    private TextView tvDetailSubtotal, tvDetailShipping, tvDetailTotal, tvDetailOriginalTotal;
    private ImageView imgDetailStatusIcon;
    private LinearLayout containerOrderItems, layoutStatusBanner, layoutBottomAction;
    private MaterialButton btnCancelOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_detail);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        orderId = getIntent().getStringExtra("ORDER_ID");

        if (orderId == null) {
            Toast.makeText(this, "Không tìm thấy mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupInsets();
        fetchOrderDetails();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) orderListener.remove();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        tvHeaderStatus = findViewById(R.id.tvHeaderStatus);
        tvDetailStatusTitle = findViewById(R.id.tvDetailStatusTitle);
        tvDeliveryNote = findViewById(R.id.tvDeliveryNote);
        tvDetailTime = findViewById(R.id.tvDetailTime);
        tvDetailStatusDesc = findViewById(R.id.tvDetailStatusDesc);
        imgDetailStatusIcon = findViewById(R.id.imgDetailStatusIcon);
        layoutStatusBanner = findViewById(R.id.layoutStatusBanner);
        layoutBottomAction = findViewById(R.id.layoutBottomAction);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        
        tvDetailBuyerName = findViewById(R.id.tvDetailBuyerName);
        tvDetailAddress = findViewById(R.id.tvDetailAddress);
        tvDetailOrderId = findViewById(R.id.tvDetailOrderId);
        tvDetailPaymentMethod = findViewById(R.id.tvDetailPaymentMethod);
        tvDetailSubtotal = findViewById(R.id.tvDetailSubtotal);
        tvDetailShipping = findViewById(R.id.tvDetailShipping);
        tvDetailTotal = findViewById(R.id.tvDetailTotal);
        tvDetailOriginalTotal = findViewById(R.id.tvDetailOriginalTotal);
        containerOrderItems = findViewById(R.id.containerOrderItems);

        findViewById(R.id.btnAddOrderToCart).setOnClickListener(v -> addAllToCart());
        findViewById(R.id.btnRepurchase).setOnClickListener(v -> buyAgainNow());
        btnCancelOrder.setOnClickListener(v -> confirmCancelOrder());
    }

    private void confirmCancelOrder() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> cancelOrder())
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void cancelOrder() {
        if (orderId == null) return;
        db.collection("orders").document(orderId)
                .update("status", "Đã hủy")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    if (currentOrder != null) {
                        String displayOrderId = currentOrder.getOrderId();
                        sendNotification("Hủy đơn hàng thành công", 
                            "Bạn đã hủy đơn hàng #" + displayOrderId + " thành công.", 
                            "Đơn hàng", orderId);
                        
                        // Logic thu hồi voucher hoặc mã voucher may mắn
                        revokeVouchersAndCodes(orderId, displayOrderId);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(OrderDetailActivity.this, "Lỗi khi hủy đơn hàng", Toast.LENGTH_SHORT).show());
    }

    private void revokeVouchersAndCodes(String firestoreOrderId, String displayOrderId) {
        String uid = auth.getUid();
        if (uid == null) return;

        // 1. Tìm voucher người dùng đã nhận từ mã (nếu đã nhập mã)
        db.collection("vouchers")
                .whereEqualTo("userId", uid)
                .whereEqualTo("orderId", firestoreOrderId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Voucher v = doc.toObject(Voucher.class);
                            // Cập nhật trạng thái thành CANCELLED thay vì xóa
                            db.collection("vouchers").document(doc.getId()).update("status", "CANCELLED");
                            
                            // Đánh dấu mã gốc là đã bị thu hồi để không nhập lại được
                            if (v.getOriginCode() != null) {
                                db.collection("users").document(uid).update("revokedLuckyCodes", FieldValue.arrayUnion(v.getOriginCode()));
                            }

                            String voucherInfo = (v.getOriginCode() != null) ? "được nhận từ mã " + v.getOriginCode() + " " : "";
                            sendNotification("Thu hồi Voucher", 
                                "Voucher " + v.getTitle() + " " + voucherInfo + "đã bị thu hồi vì bạn đã hủy đơn hàng #" + displayOrderId, 
                                "Hệ thống", firestoreOrderId);
                        }
                    } else {
                        // 2. Nếu chưa nhập mã (chưa có voucher), tìm thông báo mã quà tặng để báo thu hồi mã
                        db.collection("notifications")
                                .whereEqualTo("userId", uid)
                                .whereEqualTo("oderId", firestoreOrderId)
                                .whereEqualTo("title", "Nhận mã Voucher may mắn 🎁")
                                .get()
                                .addOnSuccessListener(notices -> {
                                    if (!notices.isEmpty()) {
                                        String code = notices.getDocuments().get(0).getString("voucherCode");
                                        // Thêm mã vào danh sách thu hồi của user
                                        db.collection("users").document(uid).update("revokedLuckyCodes", FieldValue.arrayUnion(code));

                                        // Tạo một Voucher giả với trạng thái CANCELLED để hiển thị trong lịch sử
                                        String dummyVId = uid + "_REVOKED_" + System.currentTimeMillis();
                                        Voucher dummy = new Voucher(dummyVId, "REVOKED", "Mã quà tặng bị thu hồi",
                                                "Mã " + code + " đã bị vô hiệu hóa do hủy đơn #" + displayOrderId,
                                                uid, 0L, Voucher.TYPE_DISCOUNT, System.currentTimeMillis(), false);
                                        dummy.setStatus("CANCELLED");
                                        dummy.setOrderId(firestoreOrderId);
                                        dummy.setOriginCode(code);
                                        db.collection("vouchers").document(dummyVId).set(dummy);

                                        sendNotification("Thu hồi mã quà tặng", 
                                            "Đã thu hồi mã " + code + " nhận voucher quà tặng vì hủy đơn hàng #" + displayOrderId, 
                                            "Hệ thống", firestoreOrderId);
                                    }
                                });
                    }
                });
    }

    private void sendNotification(String title, String content, String type, String oderId) {
        Map<String, Object> notice = new HashMap<>();
        notice.put("userId", auth.getUid());
        notice.put("title", title);
        notice.put("content", content);
        notice.put("type", type);
        notice.put("oderId", oderId);
        notice.put("timestamp", FieldValue.serverTimestamp());
        db.collection("notifications").add(notice);
    }

    private void addAllToCart() {
        if (currentOrder == null || auth.getCurrentUser() == null) return;
        String userId = auth.getUid();
        for (OrderItem item : currentOrder.getItems()) {
            CartItem cartItem = new CartItem(
                    userId, item.getProductId(), item.getName(), "", 
                    item.getPrice(), item.getPrice(), item.getQuantity(), item.getImageUrl()
            );
            db.collection("carts").add(cartItem);
        }
        Toast.makeText(this, "TT shop đã thêm sản phẩm vào giỏ hàng cho bạn!", Toast.LENGTH_SHORT).show();
    }

    private void buyAgainNow() {
        if (currentOrder == null || auth.getCurrentUser() == null) return;
        ArrayList<CartItem> selectedItems = new ArrayList<>();
        for (OrderItem item : currentOrder.getItems()) {
            selectedItems.add(new CartItem(
                    auth.getUid(), item.getProductId(), item.getName(), "",
                    item.getPrice(), item.getPrice(), item.getQuantity(), item.getImageUrl()
            ));
        }
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("SELECTED_ITEMS_DATA", selectedItems);
        startActivity(intent);
    }

    private void setupInsets() {
        View rootView = findViewById(R.id.main_order_detail);
        View topBar = findViewById(R.id.topBar);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                topBar.setPadding(topBar.getPaddingLeft(), systemBars.top, topBar.getPaddingRight(), topBar.getPaddingBottom());
                layoutBottomAction.setPadding(layoutBottomAction.getPaddingLeft(), layoutBottomAction.getPaddingTop(), 
                                           layoutBottomAction.getPaddingRight(), navBars.bottom);
                return insets;
            });
        }
    }

    private void fetchOrderDetails() {
        if (orderListener != null) orderListener.remove();
        // Lắng nghe thay đổi thời gian thực từ Firebase Console
        orderListener = db.collection("orders").document(orderId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null || documentSnapshot == null || !documentSnapshot.exists()) return;
                    try {
                        currentOrder = documentSnapshot.toObject(Order.class);
                        if (currentOrder != null) {
                            currentOrder.setId(documentSnapshot.getId());
                            displayData(currentOrder);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
    }

    private void displayData(Order order) {
        String status = order.getStatus() != null ? order.getStatus().trim() : "Chờ xác nhận";
        String s = status.toLowerCase();
        tvHeaderStatus.setText("Chi tiết đơn hàng");
        tvDetailStatusTitle.setText(status);

        btnCancelOrder.setVisibility("Chờ xác nhận".equalsIgnoreCase(status) ? View.VISIBLE : View.GONE);

        // Đồng bộ icon và màu sắc với NoticeActivity
        int iconRes = R.drawable.logistic;
        String tintColor = "#1E3A8A";

        if (s.contains("chờ") || s.contains("pending")) {
            tvDeliveryNote.setText("Đơn hàng đang chờ nhân viên TT shop tiếp nhận.");
            tvDetailStatusDesc.setText("Hệ thống đang kiểm tra đơn hàng của bạn.");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#E0F2FE"));
            iconRes = R.drawable.booking;
        } else if (s.contains("xác nhận") || s.contains("confirmed")) {
            tvDeliveryNote.setText("TT shop đã xác nhận. Đang chuẩn bị đóng gói hàng.");
            tvDetailStatusDesc.setText("Sản phẩm của bạn sẽ sớm được bàn giao cho đơn vị vận chuyển.");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#DBEAFE"));
            iconRes = R.drawable.checklist;
        } else if (s.contains("vận chuyển") || s.contains("shipping")) {
            tvDeliveryNote.setText("Đơn hàng đang trên đường tới trung tâm bưu cục.");
            tvDetailStatusDesc.setText("Kiện hàng đã rời khỏi kho TT shop.");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#F0F9FF"));
            iconRes = R.drawable.tracking;
        } else if (s.contains("đang giao") || s.contains("delivering")) {
            tvDeliveryNote.setText("Shipper đang mang kiện hàng đến địa chỉ của bạn.");
            tvDetailStatusDesc.setText("Hãy chuẩn bị nhận cuộc gọi và kiểm tra hàng nhé.");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#FEF3C7"));
            iconRes = R.drawable.shop;
        } else if (s.contains("giao") && (s.contains("thành công") || s.contains("xong") || s.contains("delivered") || s.contains("tất"))) {
            tvDeliveryNote.setText("Giao hàng thành công. Cảm ơn bạn đã ủng hộ TT shop!");
            tvDetailStatusDesc.setText("Chúc bạn có những trải nghiệm tuyệt vời với sản phẩm.");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#DCFCE7"));
            iconRes = R.drawable.product;
        } else if (s.contains("hủy") || s.contains("cancelled")) {
            tvDeliveryNote.setText("Đơn hàng này đã được hủy bỏ.");
            tvDetailStatusDesc.setText("Rất tiếc! Hy vọng TT shop sẽ được phục vụ bạn ở lần tới.");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#F1F5F9"));
            iconRes = R.drawable.reject;
            tintColor = "#EF4444";
        } else {
            tvDeliveryNote.setText("Kiện hàng đang được xử lý");
            tvDetailStatusDesc.setText("Cảm ơn bạn đã mua sắm tại TT Shop");
            layoutStatusBanner.setBackgroundColor(Color.parseColor("#F8FAFC"));
        }

        if (imgDetailStatusIcon != null) {
            imgDetailStatusIcon.setImageResource(iconRes);
            imgDetailStatusIcon.setColorFilter(Color.parseColor(tintColor));
        }

        if (order.getTimestamp() != null) {
            String timeStr = sdfTime.format(order.getTimestamp().toDate());
            String dateStr = sdfDate.format(order.getTimestamp().toDate());
            tvDetailTime.setText("Lần cập nhật mới nhất vào lúc: " + timeStr + " ngày " + dateStr);
        }

        tvDetailBuyerName.setText(order.getBuyerName() + " (" + order.getBuyerPhone() + ")");
        tvDetailAddress.setText(order.getShippingAddress());
        tvDetailOrderId.setText(order.getOrderId());
        tvDetailPaymentMethod.setText(order.getPaymentMethod());
        
        long subtotal = 0;
        if (order.getItems() != null) {
            containerOrderItems.removeAllViews();
            for (OrderItem item : order.getItems()) {
                subtotal += item.getPrice() * item.getQuantity();
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_payment_product, containerOrderItems, false);
                Glide.with(this).load(item.getImageUrl()).placeholder(R.drawable.phone_mockup).into((ImageView) itemView.findViewById(R.id.imgProduct));
                ((TextView) itemView.findViewById(R.id.tvProductName)).setText(item.getName());
                ((TextView) itemView.findViewById(R.id.tvProductPrice)).setText(String.format(Locale.getDefault(), "%,dđ", item.getPrice()));
                ((TextView) itemView.findViewById(R.id.tvQuantity)).setText("x" + item.getQuantity());
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, DetailActivity.class);
                    intent.putExtra("PRODUCT_ID", item.getProductId());
                    startActivity(intent);
                });
                containerOrderItems.addView(itemView);
            }
        }
        tvDetailSubtotal.setText(String.format(Locale.getDefault(), "%,dđ", subtotal));
        tvDetailShipping.setText(String.format(Locale.getDefault(), "%,dđ", order.getShippingFee()));
        
        // Hiển thị giá gốc bị gạch và giá mua 0đ
        long originalTotal = subtotal + order.getShippingFee();
        long finalPrice = order.getFinalPrice();
        if (finalPrice < 0) finalPrice = 0;

        if (finalPrice < originalTotal) {
            tvDetailOriginalTotal.setVisibility(View.VISIBLE);
            tvDetailOriginalTotal.setText(String.format(Locale.getDefault(), "%,dđ", originalTotal));
            tvDetailOriginalTotal.setPaintFlags(tvDetailOriginalTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvDetailTotal.setText(String.format(Locale.getDefault(), "%,dđ", finalPrice));
        } else {
            tvDetailOriginalTotal.setVisibility(View.GONE);
            tvDetailTotal.setText(String.format(Locale.getDefault(), "%,dđ", originalTotal));
        }
    }
}
