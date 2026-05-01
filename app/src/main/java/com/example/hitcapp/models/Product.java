package com.example.hitcapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Product {
    private String id;
    private String name;
    private long price;
    private long originalPrice;
    private int discountPercent;
    private String imageUrl;
    private String category;
    private String description;
    private boolean isFeatured;
    private boolean isNew;
    private boolean isSale;
    private Timestamp createdAt;

    public Product() {} // Required for Firebase

    public Product(String name, long price, String imageUrl) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.createdAt = Timestamp.now();
    }

    public Product(String id, String name, long price, long originalPrice, int discountPercent, 
                   String imageUrl, String category, String description, 
                   boolean isFeatured, boolean isNew, boolean isSale) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent;
        this.imageUrl = imageUrl;
        this.category = category;
        this.description = description;
        this.isFeatured = isFeatured;
        this.isNew = isNew;
        this.isSale = isSale;
        this.createdAt = Timestamp.now();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public long getPrice() { return price; }
    public long getOriginalPrice() { return originalPrice; }
    public int getDiscountPercent() { return discountPercent; }
    public String getImageUrl() { return imageUrl; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }

    @PropertyName("isFeatured")
    public boolean isFeatured() { return isFeatured; }
    @PropertyName("isNew")
    public boolean isNew() { return isNew; }
    @PropertyName("isSale")
    public boolean isSale() { return isSale; }
    
    public Timestamp getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(long price) { this.price = price; }
    public void setOriginalPrice(long originalPrice) { this.originalPrice = originalPrice; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("isFeatured")
    public void setFeatured(boolean featured) { isFeatured = featured; }
    @PropertyName("isNew")
    public void setNew(boolean aNew) { isNew = aNew; }
    @PropertyName("isSale")
    public void setSale(boolean sale) { isSale = sale; }

    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
