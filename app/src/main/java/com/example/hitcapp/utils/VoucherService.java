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

public class VoucherService {
    private static final String TAG = "VoucherService";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addInitialVouchers(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;

        final Context appContext = context.getApplicationContext();
        String welcomeVoucherId = userId + "_WELCOME_5M_INIT";

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

            // 3. Thông báo nhận Voucher
            String noticeId = "NOTICE_V_INIT_" + userId + "_" + System.currentTimeMillis();
            Map<String, Object> notice = new HashMap<>();
            notice.put("userId", userId);
            notice.put("title", "Bạn nhận được gói quà tặng Voucher 🎫");
            notice.put("content", "Chúc mừng! Bạn nhận được 1 Voucher 5 triệu và 3 mã Freeship. Hãy vào ví voucher để sử dụng ngay!");
            notice.put("type", "Khuyến mãi");
            notice.put("timestamp", FieldValue.serverTimestamp());
            batch.set(db.collection("notifications").document(noticeId), notice);

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

    public void checkAndRewardAfterOrder(String userId) {
        if (userId == null) return;
        
        // Tặng 1 voucher tri ân sau mỗi đơn hàng thành công
        String voucherId = userId + "_REWARD_" + System.currentTimeMillis();
        long expiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // Hạn 30 ngày
        
        Voucher reward = new Voucher(voucherId, "THANKS10", "Voucher Tri Ân", 
            "Cảm ơn bạn đã mua hàng! Tặng bạn voucher giảm giá 100.000đ cho đơn sau.", userId, 100000L, 
            Voucher.TYPE_DISCOUNT, expiry, false);
            
        // LƯU VOUCHER THẬT VÀO DATABASE
        db.collection("vouchers").document(voucherId).set(reward).addOnSuccessListener(aVoid -> {
            // Sau khi lưu voucher thành công mới gửi thông báo
            Map<String, Object> notice = new HashMap<>();
            notice.put("userId", userId);
            notice.put("title", "Nhận thêm Voucher tri ân 🎁");
            notice.put("content", "Cảm ơn bạn đã mua sắm! Một mã giảm giá 100k đã được thêm vào kho voucher của bạn.");
            notice.put("type", "Khuyến mãi");
            notice.put("timestamp", FieldValue.serverTimestamp());
            db.collection("notifications").add(notice);
        });
    }
}
