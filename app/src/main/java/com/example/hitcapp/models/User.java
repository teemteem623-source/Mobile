package com.example.hitcapp.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String username;
    private String fullname;
    private String email;
    private String phone;
    private String avatarUrl;
    private String memberRank;
    private String address;
    private long totalSpent;

    public User() {
        // Required for Firebase
    }

    public User(String uid, String username, String fullname, String email, String phone, String avatarUrl, String memberRank, String address, long totalSpent) {
        this.uid = uid;
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.memberRank = memberRank;
        this.address = address;
        this.totalSpent = totalSpent;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getMemberRank() { return memberRank; }
    public void setMemberRank(String memberRank) { this.memberRank = memberRank; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(long totalSpent) { this.totalSpent = totalSpent; }

    public String getMembershipLevel() {
        if (memberRank != null && !memberRank.isEmpty()) return memberRank;
        if (totalSpent >= 40000000) return "Thành viên Vàng";
        if (totalSpent >= 20000000) return "Thành viên Bạc";
        return "Thành viên Đồng";
    }
}
