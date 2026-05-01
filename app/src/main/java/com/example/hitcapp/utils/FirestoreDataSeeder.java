package com.example.hitcapp.utils;

import android.content.Context;
import android.util.Log;
import com.example.hitcapp.models.Product;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreDataSeeder {
    private static final String TAG = "FirestoreDataSeeder";

    public static void seedAllData(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String pkgName = context.getPackageName();
        String resPrefix = "android.resource://" + pkgName + "/drawable/";

        seedBanners(db, resPrefix);
        seedProducts(db, resPrefix);
        seedNotifications(db);
        seedVouchers(db);
        // seedUsers đã bị loại bỏ hoàn toàn để tránh xung đột dữ liệu và lỗi quyền truy cập
    }

    private static void seedBanners(FirebaseFirestore db, String resPrefix) {
        String[][] bannerData = {
            {"b1", resPrefix + "banner", "p1", "1"},
            {"b2", resPrefix + "banner1", "p2", "2"},
            {"b3", resPrefix + "banner2", "p3", "3"},
            {"b4", resPrefix + "banner3", "p4", "4"},
            {"b5", resPrefix + "banner4", "p5", "5"},
            {"b6", resPrefix + "banner5", "p6", "6"}
        };

        for (String[] data : bannerData) {
            Map<String, Object> banner = new HashMap<>();
            banner.put("id", data[0]);
            banner.put("imageUrl", data[1]);
            banner.put("linkToProduct", data[2]);
            banner.put("priority", Integer.parseInt(data[3]));
            db.collection("banners").document(data[0]).set(banner);
        }
    }

    private static void seedProducts(FirebaseFirestore db, String resPrefix) {
        Product[] products = new Product[]{
                new Product("p1","iPhone 15 Pro Max",32990000,34990000,14, resPrefix + "iphone15promax","iphone","A17 Pro mạnh nhất",true,true,false),
                new Product("p2","iPhone 15 Pro",30990000,32990000,6, resPrefix + "iphone15pro","iphone","Titan cao cấp",true,false,true),
                new Product("p3","iPhone 15",22990000,24990000,8, resPrefix + "iphone15","iphone","Dynamic Island",false,true,true),
                new Product("p4","iPhone 14",16990000,19990000,15, resPrefix + "iphone14","iphone","Ổn định",true,false,true),
                new Product("p5","iPhone 13",13990000,16990000,18, resPrefix + "iphone13","iphone","Giá tốt",false,true,true),
                new Product("p6","iPhone 12",10990000,13990000,21, resPrefix + "iphone12","iphone","Vẫn ngon",false,false,true),
                new Product("p7","Samsung S24 Ultra",25990000,33990000,23, resPrefix + "samsungs24ultra","samsung","Camera 200MP",true,false,true),
                new Product("p8","Samsung S24",18990000,22990000,17, resPrefix + "samsungs24","samsung","Hiệu năng mạnh",false,true,true),
                new Product("p9","Samsung S23 FE",13990000,16990000,18, resPrefix + "samsungs23fe","samsung","Flagship giá mềm",true,false,true),
                new Product("p10","Samsung A54",8490000,10490000,19, resPrefix + "samsunga54","samsung","Chống nước",false,false,true),
                new Product("p11","Samsung A34",6990000,8490000,18, resPrefix + "samsunga34","samsung","120Hz",false,true,true),
                new Product("p12","Samsung A14",3990000,4990000,20, resPrefix + "samsunga14","samsung","Giá rẻ",false,true,true),
                new Product("p13","Xiaomi 14",19990000,22990000,13, resPrefix + "xiaomi14","xiaomi","Leica xịn",false,true,true),
                new Product("p14","Xiaomi 13T Pro",14990000,16990000,12, resPrefix + "xiaomi13tpro","xiaomi","Hiệu năng cao",true,false,true),
                new Product("p15","Xiaomi 12T",11990000,13990000,14, resPrefix + "xiaomi12t","xiaomi","Camera 108MP",false,true,true),
                new Product("p16","Redmi Note 13 Pro",6990000,7990000,12, resPrefix + "xiaomiredminote13pro","xiaomi","200MP",false,true,true),
                new Product("p17","Redmi Note 13",4590000,4890000,6, resPrefix + "xiaomiredminote13","xiaomi","AMOLED",false,true,true),
                new Product("p18","Redmi 12",3290000,3990000,17, resPrefix + "xiaomiredmi12","xiaomi","Giá rẻ",false,false,true),
                new Product("p19","Oppo Find X7",18500000,18500000,0, resPrefix + "oppofindx7","oppo","Camera Hasselblad",true,true,false),
                new Product("p20","Oppo Reno11",9990000,10990000,9, resPrefix + "opporeno11","oppo","Chân dung đẹp",false,true,true),
                new Product("p21","Oppo Reno10",8990000,9990000,10, resPrefix + "opporeno10","oppo","Thiết kế đẹp",false,false,true),
                new Product("p22","Oppo A78",5990000,6990000,14, resPrefix + "oppoa78","oppo","Pin trâu",false,true,true),
                new Product("p23","Oppo A58",4990000,5990000,16, resPrefix + "oppoa58","oppo","Ổn định",false,false,true),
                new Product("p24","Oppo A38",3990000,4990000,20, resPrefix + "oppoa38","oppo","Giá rẻ",false,true,true)
        };

        for (Product p : products) {
            db.collection("products").document(p.getId()).set(p);
        }
    }

    private static void seedNotifications(FirebaseFirestore db) {
        String[][] notifyData = {
            {"n1", "Chào mừng bạn!", "Cảm ơn bạn đã gia nhập gia đình TT SHOP. Khám phá các ưu đãi ngay nhé!", "he_thong"},
            {"n2", "Flash Sale Giờ Vàng", "Hàng loạt iPhone giảm giá đến 50% chỉ trong khung giờ 12h-14h hôm nay.", "khuyen_mai"},
            {"n3", "Đơn hàng thành công", "Bạn đã đặt hàng thành công đơn hàng #TT123. Vui lòng theo dõi tiến trình giao hàng.", "don_hang"}
        };

        for (String[] data : notifyData) {
            Map<String, Object> notify = new HashMap<>();
            notify.put("id", data[0]);
            notify.put("title", data[1]);
            notify.put("content", data[2]);
            notify.put("type", data[3]);
            notify.put("imageUrl", "");
            notify.put("timestamp", Timestamp.now());
            notify.put("targetUser", "all");
            db.collection("notifications").document(data[0]).set(notify);
        }
    }

    private static void seedVouchers(FirebaseFirestore db) {
        String[][] voucherData = {
            {"TTSHOP10", "100000", "Giảm 100k cho đơn hàng từ 2 triệu"},
            {"FREE_SHIP", "30000", "Miễn phí vận chuyển toàn quốc"},
            {"IPHONE_NEW", "500000", "Ưu đãi 500k khi mua iPhone 15 series"}
        };

        for (String[] data : voucherData) {
            Map<String, Object> voucher = new HashMap<>();
            voucher.put("code", data[0]);
            voucher.put("value", Long.parseLong(data[1]));
            voucher.put("description", data[2]);
            db.collection("vouchers").document(data[0]).set(voucher);
        }
    }
}
