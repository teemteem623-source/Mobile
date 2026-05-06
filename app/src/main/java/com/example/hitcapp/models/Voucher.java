package com.example.hitcapp.models;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Voucher implements Serializable {
    public static final String TYPE_DISCOUNT = "DISCOUNT";
    public static final String TYPE_SHIPPING = "SHIPPING";

    private String id;
    private String code;
    private String title;
    private String description;
    private String userId;
    private long value;
    private String type; // DISCOUNT, SHIPPING
    private long expiryTimestamp;
    private boolean used;
    private boolean autoApply;
    private String status; // AVAILABLE, EXPIRED, CANCELLED
    private String originCode; // Mã đã nhập để nhận được voucher này (nếu có)
    private String orderId; // ID đơn hàng mà voucher này được tặng kèm (nếu có)

    public Voucher() {}

    public Voucher(String id, String code, String title, String description, String userId, long value, String type, long expiryTimestamp, boolean autoApply) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.value = value;
        this.type = type;
        this.expiryTimestamp = expiryTimestamp;
        this.autoApply = autoApply;
        this.used = false;
        this.status = "AVAILABLE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public long getExpiryTimestamp() { return expiryTimestamp; }
    public void setExpiryTimestamp(long expiryTimestamp) { this.expiryTimestamp = expiryTimestamp; }
    
    // Alias for getExpiryTimestamp to fix compatibility issues
    public long getExpiryDate() { return expiryTimestamp; }

    @PropertyName("used")
    public boolean isUsed() { return used; }
    @PropertyName("used")
    public void setUsed(boolean used) { this.used = used; }

    @PropertyName("autoApply")
    public boolean isAutoApply() { return autoApply; }
    @PropertyName("autoApply")
    public void setAutoApply(boolean autoApply) { this.autoApply = autoApply; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
