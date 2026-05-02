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

    /**
     * Cấp phát bộ quà chào mừng (1 Voucher 5tr, 3 Voucher Freeship).
     * Dùng ID cố định để đảm bảo chỉ nhận 1 lần duy nhất cho cả acc cũ và mới.
     */
    public void addInitialVouchers(Context context, String userId) {
        if (userId == null || userId.isEmpty()) return;

        final Context appContext = context.getApplicationContext();
        // ID cố định dùng để kiểm tra sự tồn tại của gói quà
        String welcomeVoucherId = userId + "_WELCOME_5M_INIT";

        db.collection("vouchers").document(welcomeVoucherId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Log.d(TAG, "User " + userId + " đã nhận quà trước đó.");
                return;
            }

            Log.d(TAG, "Tiến hành phát quà chào mừng cho: " + userId);
            WriteBatch batch = db.batch();
            long expiry = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000); // Hạn 1 năm

            // 1. Voucher 5 Triệu
            Voucher v5m = new Voucher(welcomeVoucherId, "WELCOME5M", "Voucher 5 Triệu Đồng", 
                "Quà tặng thành viên mới - Giảm ngay 5.000.000đ cho mọi đơn hàng", userId, 5000000L, 
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

            // 3. Thông báo hệ thống (Vào tab Khuyến mãi)
            String noticeId = "NOTICE_WELCOME_" + userId;
            Map<String, Object> notice = new HashMap<>();
            notice.put("title", "Quà tặng chào mừng 🎁");
            notice.put("content", "Chúc mừng! Bạn nhận được gói quà: 1 Voucher 5 triệu và 3 Voucher Freeship. Hãy kiểm tra trong mục Ưu đãi!");
            notice.put("category", "Khuyến mãi");
            notice.put("timestamp", FieldValue.serverTimestamp());
            notice.put("userId", userId);
            notice.put("read", false);
            batch.set(db.collection("notifications").document(noticeId), notice);

            // 4. Đánh dấu trên tài khoản người dùng
            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("hasReceivedInitialVouchers", true);
            batch.set(db.collection("users").document(userId), userUpdate, SetOptions.merge());

            batch.commit().addOnSuccessListener(aVoid -> {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(appContext, "🎁 Bạn vừa nhận được gói quà tặng thành viên mới!", Toast.LENGTH_LONG).show()
                );
            }).addOnFailureListener(e -> Log.e(TAG, "Lỗi khi phát voucher: " + e.getMessage()));
        });
    }

    public void checkAndRewardAfterOrder(String userId) {
        // Tặng thêm voucher sau khi mua hàng (tùy chọn)
    }
}
