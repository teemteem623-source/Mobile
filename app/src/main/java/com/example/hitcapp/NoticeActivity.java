package com.example.hitcapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class NoticeActivity extends AppCompatActivity {

    private RecyclerView rvNotices;
    private BottomNavigationView bottomNavigation;
    private List<NoticeItem> fullNoticeList = new ArrayList<>();
    private NoticeAdapter adapter;
    private ChipGroup chipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notice);

        // Xử lý Safe Area
        View mainView = findViewById(R.id.main);
        View topBar = findViewById(R.id.topBarCard);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if (topBar != null) topBar.setPadding(0, systemBars.top, 0, 0);
                if (bottomNavigation != null) bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }

        // Khởi tạo dữ liệu mẫu phong phú
        initData();

        // Setup RecyclerView
        rvNotices = findViewById(R.id.rvNotices);
        rvNotices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoticeAdapter(new ArrayList<>(fullNoticeList));
        rvNotices.setAdapter(adapter);

        // Xử lý sự kiện nhấn Chip để lọc (Chọn 1 cái duy nhất)
        setupChips();

        // Thanh điều hướng
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_notifications);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_products) {
                    startActivity(new Intent(this, ProductActivity.class));
                    finish();
                    return true;
                } else if (itemId == R.id.nav_notifications) {
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, UserActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void initData() {
        // 1. Ưu đãi
        fullNoticeList.add(new NoticeItem("Ưu đãi iPhone 15 Pro Max", "Siêu phẩm iPhone 15 Pro Max đang được giảm giá kịch sàn tại TT SHOP. Hãy nhanh tay sở hữu ngay hôm nay để nhận thêm bộ quà tặng 2 triệu đồng và gói bảo hành rơi vỡ 12 tháng. Chương trình chỉ áp dụng đến hết tuần này hoặc cho đến khi hết quà tặng. Đừng bỏ lỡ cơ hội vàng này để sở hữu chiếc điện thoại mơ ước với mức giá tốt nhất thị trường!", android.R.drawable.ic_menu_send, "#E11D48", "Ưu đãi"));
        fullNoticeList.add(new NoticeItem("Mã giảm giá 50% cho phụ kiện", "Nhận ngay mã giảm giá 50% khi mua kèm ốp lưng và dán cường lực cho tất cả các dòng iPhone. Nhập mã: ACC50. Ưu đãi áp dụng tại cửa hàng và online.", android.R.drawable.ic_menu_send, "#E11D48", "Ưu đãi"));
        fullNoticeList.add(new NoticeItem("Flash Sale Giờ Vàng", "Duy nhất từ 12h - 14h hôm nay, tai nghe Airpods 3 giảm chỉ còn 2.990.000đ. Số lượng cực kỳ có hạn, mỗi khách hàng chỉ được mua tối đa 1 sản phẩm. Hãy đặt báo thức để không bỏ lỡ!", android.R.drawable.ic_menu_send, "#E11D48", "Ưu đãi"));

        // 2. Giao dịch
        fullNoticeList.add(new NoticeItem("Giao dịch thành công", "Bạn đã thanh toán thành công số tiền 15.000.000đ cho đơn hàng #TT12345. Sản phẩm đang được đóng gói và sẽ sớm bàn giao cho đơn vị vận chuyển. Cảm ơn bạn đã tin tưởng mua sắm tại TT SHOP. Bạn có thể theo dõi tiến trình đơn hàng trong mục Đơn hàng của tôi.", android.R.drawable.ic_menu_save, "#059669", "Giao dịch"));
        fullNoticeList.add(new NoticeItem("Đơn hàng đang được giao", "Đơn hàng #TT67890 của bạn đang trên đường vận chuyển và dự kiến sẽ đến tay bạn trong vòng 2-3 ngày tới. Vui lòng giữ điện thoại để shipper liên hệ giao hàng nhé.", android.R.drawable.ic_menu_save, "#059669", "Giao dịch"));
        fullNoticeList.add(new NoticeItem("Hoàn tiền hoàn tất", "Yêu cầu hoàn tiền cho đơn hàng #TT00112 đã được xử lý thành công. Số tiền 500.000đ đã được chuyển về ví của bạn. Vui lòng kiểm tra lại tài khoản.", android.R.drawable.ic_menu_save, "#059669", "Giao dịch"));

        // 3. Nhắc nhở
        fullNoticeList.add(new NoticeItem("Thông báo Bảo trì Hệ thống", "Để nâng cao trải nghiệm khách hàng, ứng dụng sẽ tiến hành bảo trì định kỳ từ 0h00 đến 2h00 sáng ngày mai. Trong thời gian này, một số tính năng thanh toán có thể bị gián đoạn. Rất xin lỗi vì sự bất tiện này và cảm ơn sự thông cảm của bạn.", android.R.drawable.ic_lock_idle_alarm, "#F59E0B", "Nhắc nhở"));
        fullNoticeList.add(new NoticeItem("Nhắc nhở đánh giá", "Bạn đã nhận được đơn hàng #TT55443 chưa? Hãy dành 1 phút để đánh giá chất lượng sản phẩm và dịch vụ để giúp chúng tôi hoàn thiện hơn nhé. Mỗi đánh giá sẽ được tặng ngay 10 điểm thưởng tích lũy.", android.R.drawable.ic_lock_idle_alarm, "#F59E0B", "Nhắc nhở"));
        fullNoticeList.add(new NoticeItem("Cập nhật phiên bản mới", "Đã có phiên bản 2.1.0 mới nhất với tính năng tìm kiếm bằng hình ảnh cực kỳ tiện lợi. Hãy cập nhật ngay để có trải nghiệm mua sắm mượt mà nhất!", android.R.drawable.ic_lock_idle_alarm, "#F59E0B", "Nhắc nhở"));

        // 4. Tài khoản
        fullNoticeList.add(new NoticeItem("Cảnh báo Đăng nhập Lạ", "Tài khoản của bạn vừa được đăng nhập từ một thiết bị Chrome trên Windows tại khu vực Hà Nội. Nếu không phải bạn thực hiện thao tác này, vui lòng nhấn vào đây để đổi mật khẩu ngay lập tức và bảo vệ tài khoản của mình. Đừng chia sẻ mã OTP cho bất kỳ ai!", android.R.drawable.ic_lock_lock, "#DC2626", "Tài khoản"));
        fullNoticeList.add(new NoticeItem("Chúc mừng sinh nhật", "Chúc mừng sinh nhật khách hàng thân thiết! TT SHOP gửi tặng bạn món quà bất ngờ là mã giảm giá 200k cho đơn hàng từ 1 triệu đồng. Mã: HBD_USER. Chúc bạn có một ngày sinh nhật thật ý nghĩa và hạnh phúc bên gia đình!", android.R.drawable.ic_menu_myplaces, "#8B5CF6", "Tài khoản"));
        fullNoticeList.add(new NoticeItem("Nâng hạng thành viên", "Chúc mừng bạn đã đạt hạng VÀNG! Với thứ hạng này, bạn sẽ nhận được ưu đãi miễn phí vận chuyển trọn đời cho mọi đơn hàng và giảm thêm 2% khi mua trực tiếp tại cửa hàng.", android.R.drawable.ic_menu_myplaces, "#8B5CF6", "Tài khoản"));
    }

    private void setupChips() {
        chipGroup = findViewById(R.id.chipGroup);
        
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            String category = "";
            
            if (checkedId == R.id.chipGiaoDich) category = "Giao dịch";
            else if (checkedId == R.id.chipUuDai) category = "Ưu đãi";
            else if (checkedId == R.id.chipNhacNho) category = "Nhắc nhở";
            else if (checkedId == R.id.chipTaiKhoan) category = "Tài khoản";
            
            filterNotices(category);
        });
    }

    private void filterNotices(String category) {
        List<NoticeItem> filtered = new ArrayList<>();
        if (category.isEmpty()) {
            filtered.addAll(fullNoticeList);
        } else {
            for (NoticeItem item : fullNoticeList) {
                if (item.category.equals(category)) {
                    filtered.add(item);
                }
            }
        }
        adapter.updateList(filtered);
    }

    private static class NoticeItem {
        String title, content, colorHex, category;
        int iconRes;
        boolean isExpanded = false;
        NoticeItem(String t, String c, int icon, String color, String cat) { 
            this.title = t; 
            this.content = c; 
            this.iconRes = icon;
            this.colorHex = color;
            this.category = cat;
        }
    }

    private static class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {
        private List<NoticeItem> list;
        NoticeAdapter(List<NoticeItem> list) { this.list = list; }

        public void updateList(List<NoticeItem> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NoticeItem item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvContent.setText(item.content);
            holder.imgIcon.setImageResource(item.iconRes);
            
            try {
                holder.imgIcon.setColorFilter(android.graphics.Color.parseColor(item.colorHex));
                holder.imgIcon.getBackground().setTint(android.graphics.Color.parseColor(item.colorHex));
                holder.imgIcon.getBackground().setAlpha(30);
            } catch (Exception e) {}

            if (item.content.length() > 100) {
                holder.btnDetail.setVisibility(item.isExpanded ? View.GONE : View.VISIBLE);
                holder.tvContent.setMaxLines(item.isExpanded ? Integer.MAX_VALUE : 3);
            } else {
                holder.btnDetail.setVisibility(View.GONE);
                holder.tvContent.setMaxLines(Integer.MAX_VALUE);
            }

            holder.btnDetail.setOnClickListener(v -> {
                item.isExpanded = true;
                notifyItemChanged(position);
            });

            holder.itemView.setOnClickListener(v -> {
                if (item.isExpanded) {
                    item.isExpanded = false;
                    notifyItemChanged(position);
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, btnDetail;
            ImageView imgIcon;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvTitle);
                tvContent = v.findViewById(R.id.tvContent);
                btnDetail = v.findViewById(R.id.btnViewDetail);
                imgIcon = v.findViewById(R.id.imgNoticeIcon);
            }
        }
    }
}
