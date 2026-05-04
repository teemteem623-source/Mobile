package com.example.hitcapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    private static final String TAG = "VoucherService";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addInitialVouchers(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;

        final Context appContext = context.getApplicationContext();
        String welcomeVoucherId = userId + "_WELCOME_5M_INIT";
        
        // Sử dụng ID cố định để tránh tạo nhiều bản sao thông báo trong Firestore
        String welcomeNoticeId = "WELCOME_NOTICE_" + userId;

        db.collection("vouchers").document(welcomeVoucherId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) return;

            WriteBatch batch = db.batch();
            long expiry = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000);

            // 1. Voucher 5 Triệu
            Voucher v5m = new Voucher(welcomeVoucherId, "WELCOME5M", "Voucher 5 Triệu Đồng", 
                "Quà tặng chào mừng - Giảm ngay 5.000.000đ cho mọi đơn hàng", userId, 5000000L, 
                Voucher.TYPE_DISCOUNT, expiry, true);
            batch.set(db.collection("vouchers").document(welcomeVoucherId), v5m);

            // 2. 3 Voucher Freeship
            for (int i = 1; i <= 3; i++) {
                String fsId = userId + "_WELCOME_FS_INIT_" + i;
                Voucher fs = new Voucher(fsId, "FREESHIP", "Miễn Phí Vận Chuyển", 
                    "Ưu đãi vận chuyển 0đ dành cho khách hàng mới", userId, 0L, 
                    Voucher.TYPE_SHIPPING, expiry, true);
                batch.set(db.collection("vouchers").document(fsId), fs);
            }

            // 3. Thông báo nhận Voucher (Sử dụng ID cố định để không bị lặp)
            Map<String, Object> notice = new HashMap<>();
            notice.put("userId", userId);
            notice.put("title", "Gói quà tặng thành viên mới 🎁");
            notice.put("content", "Chúc mừng! Bạn nhận được 1 Voucher 5 triệu và 3 mã Freeship. Hãy vào ví voucher để sử dụng ngay!");
            notice.put("type", "Khuyến mãi");
            notice.put("timestamp", FieldValue.serverTimestamp());
            batch.set(db.collection("notifications").document(welcomeNoticeId), notice);

            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("hasReceivedInitialVouchers", true);
            batch.set(db.collection("users").document(userId), userUpdate, SetOptions.merge());

            batch.commit().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Initial vouchers and single notice added successfully.");
            });
        });
    }

    public void checkAndRewardAfterOrder(String userId) {
        if (userId == null) return;
        
        String[] codes = {"LUCKY1", "GIFT99", "HITC88", "PROMO10"};
        String luckyCode = codes[new Random().nextInt(codes.length)];
        
        // Dùng ID dựa trên thời gian để cho phép nhiều thông báo đơn hàng khác nhau nhưng tránh spam
        String noticeId = "ORDER_REWARD_" + userId + "_" + (System.currentTimeMillis() / 60000); // 1 mã mỗi phút
        
        Map<String, Object> notice = new HashMap<>();
        notice.put("userId", userId);
        notice.put("title", "Nhận mã Voucher may mắn 🎁");
        notice.put("content", "Cảm ơn bạn đã mua sắm! Hãy dùng mã " + luckyCode + " tại trang Ưu đãi để nhận quà tặng tri ân.");
        notice.put("type", "Khuyến mãi");
        notice.put("voucherCode", luckyCode);
        notice.put("timestamp", FieldValue.serverTimestamp());
        
        db.collection("notifications").document(noticeId).set(notice, SetOptions.merge());
    }
}
