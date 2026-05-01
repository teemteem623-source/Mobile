package com.example.hitcapp.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String cartItemId; // ID of the document in Firestore
    private String userId;
    private String productId;
    private String name;
    private String detail;
    private String imageUrl;
    private long price;
    private long originalPrice;
    private int quantity;
    private boolean selected = true;

    public CartItem() {}

    public CartItem(String userId, String productId, String name, String detail, long price, long originalPrice, int quantity, String imageUrl) {
        this.userId = userId;
        this.productId = productId;
        this.name = name;
        this.detail = detail;
        this.price = price;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.selected = true;
    }

    // Getters and Setters
    public String getCartItemId() { return cartItemId; }
    public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
