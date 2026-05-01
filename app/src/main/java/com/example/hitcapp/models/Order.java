package com.example.hitcapp.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Order implements Serializable {
    private String id;
    private String orderId;
    private String userId;
    private String productId;
    private String productName;
    private String imageUrl;
    private int quantity;
    private String shippingAddress;
    private String status;
    private long totalPrice;
    private Timestamp createAt;
    
    // Các trường bổ sung để khớp với OrderDetailActivity và PaymentActivity
    private String buyerName;
    private String buyerPhone;
    private String paymentMethod;
    private long shippingFee;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(long totalPrice) { this.totalPrice = totalPrice; }

    public Timestamp getCreateAt() { return createAt; }
    public void setCreateAt(Timestamp createAt) { this.createAt = createAt; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerPhone() { return buyerPhone; }
    public void setBuyerPhone(String buyerPhone) { this.buyerPhone = buyerPhone; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getShippingFee() { return shippingFee; }
    public void setShippingFee(long shippingFee) { this.shippingFee = shippingFee; }

    // Các phương thức trợ giúp để tương thích với Activity
    public Timestamp getTimestamp() { return createAt; }
    
    public long getTotalAmount() { return totalPrice + shippingFee; }
    
    public String getAddress() { return shippingAddress; }

    public List<OrderItem> getItems() {
        List<OrderItem> items = new ArrayList<>();
        if (productId != null && !productId.isEmpty()) {
            OrderItem item = new OrderItem();
            item.setProductId(this.productId);
            item.setName(this.productName != null ? this.productName : "Sản phẩm");
            item.setImageUrl(this.imageUrl);
            item.setQuantity(this.quantity);
            // Giả định đơn giá = tổng giá / số lượng nếu không lưu riêng
            item.setPrice(this.totalPrice / (quantity > 0 ? quantity : 1));
            items.add(item);
        }
        return items;
    }
}
