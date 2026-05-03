package com.example.hitcapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.hitcapp.models.Voucher;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VoucherService {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Tặng gói quà chào mừng cho thành viên mới (5M + 3 Freeship) - Đã sửa lỗi lặp thông báo
     */
    public void addInitialVouchers(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;

        final Context appContext = context.getApplicationContext();
        String welcomeVoucherId = userId + "_WELCOME_5M_INIT";

        db.collection("vouchers").document(welcomeVoucherId).get().addOnSuccessListener(doc -> {
            // Nếu đã tồn tại voucher chào mừng thì không làm gì cả
            if (doc.exists()) return;

            WriteBatch batch = db.batch();
            long expiry = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);

            // 1. Voucher 5 Triệu (ID cố định theo userId)
            Voucher v5m = new Voucher(welcomeVoucherId, "WELCOME5M", "Voucher 5 Triệu Đồng", 
                "Quà tặng chào mừng - Giảm ngay 5.000.000đ cho mọi đơn hàng", userId, 5000000L, 
                Voucher.TYPE_DISCOUNT, expiry, true);
            batch.set(db.collection("vouchers").document(welcomeVoucherId), v5m);

            // 2. 3 Voucher Freeship (ID cố định theo userId)
            for (int i = 1; i <= 3; i++) {
                String fsId = userId + "_WELCOME_FS_INIT_" + i;
                Voucher fs = new Voucher(fsId, "FREESHIP", "Miễn Phí Vận Chuyển", 
                    "Ưu đãi vận chuyển 0đ dành cho khách hàng mới", userId, 0L, 
                    Voucher.TYPE_SHIPPING, expiry, true);
                batch.set(db.collection("vouchers").document(fsId), fs);
            }

            // 3. Thông báo nhận Voucher chào mừng (SỬ DỤNG ID CỐ ĐỊNH ĐỂ TRÁNH LẶP)
            String noticeId = "NOTICE_WELCOME_" + userId; 
            Map<String, Object> notice = new HashMap<>();
            notice.put("userId", userId);
            notice.put("title", "Bạn nhận được gói quà tặng Voucher 🎫");
            notice.put("content", "Chúc mừng! Bạn nhận được 1 Voucher 5 triệu và 3 mã Freeship. Hãy vào ví voucher để sử dụng ngay!");
            notice.put("type", "Khuyến mãi");
            notice.put("timestamp", FieldValue.serverTimestamp());
            
            // Dùng set với merge để nếu đã có rồi thì không tạo mới bản ghi gây lặp thông báo
            batch.set(db.collection("notifications").document(noticeId), notice, SetOptions.merge());

            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("hasReceivedInitialVouchers", true);
            batch.set(db.collection("users").document(userId), userUpdate, SetOptions.merge());

            batch.commit().addOnSuccessListener(aVoid -> {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(appContext, "🎁 Bạn nhận được quà tặng thành viên mới!", Toast.LENGTH_LONG).show()
                );
            });
        });
    }

    /**
     * Gửi MÃ tri ân mua hàng (Không tặng voucher trực tiếp)
     */
    public void checkAndRewardAfterOrder(String userId, String firestoreOrderId) {
        if (userId == null) return;
        
        String[] codes = {"VANNANG", "TRIAN", "FREE", "HITCAPP"};
        String selectedCode = codes[new Random().nextInt(codes.length)];
        
        Map<String, Object> notice = new HashMap<>();
        notice.put("userId", userId);
        notice.put("title", "Mã tri ân mua hàng từ HITCApp 🎁");
        notice.put("content", "Cảm ơn bạn đã mua sắm! Nhấn vào đây và nhập mã " + selectedCode + " tại mục Ưu đãi để nhận voucher may mắn lên đến 2 triệu đồng.");
        notice.put("type", "Khuyến mãi");
        notice.put("voucherCode", selectedCode);
        notice.put("oderId", firestoreOrderId);
        notice.put("timestamp", FieldValue.serverTimestamp());
        
        db.collection("notifications").add(notice);
    }
    
    public void checkAndRewardAfterOrder(String userId) {
        checkAndRewardAfterOrder(userId, "");
    }
}
