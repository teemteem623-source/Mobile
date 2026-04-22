package com.example.hitcapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AccountStorage {
    private static final String PREF_NAME = "UserAccounts";
    private static final String KEY_ACCOUNTS = "accounts_list";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";

    public static class User {
        public String username;
        public String email;
        public String password;
        public String fullName;
        public String phone;

        // Constructor mặc định cho đăng ký
        public User(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.fullName = username; // Mặc định tên đầy đủ là username
            this.phone = "";
        }

        public User(String username, String email, String password, String fullName, String phone) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.fullName = fullName;
            this.phone = phone;
        }
    }

    // Lưu danh sách tất cả tài khoản
    public static void saveUser(Context context, User newUser) {
        List<User> accounts = getAccounts(context);
        
        // Kiểm tra xem email hoặc username đã tồn tại chưa để cập nhật hoặc thêm mới
        int index = -1;
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).email.equalsIgnoreCase(newUser.email)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            accounts.set(index, newUser);
        } else {
            accounts.add(newUser);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(accounts);
        prefs.edit().putString(KEY_ACCOUNTS, json).apply();
    }

    // Lấy danh sách tài khoản
    public static List<User> getAccounts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ACCOUNTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<User>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    // Tìm kiếm tài khoản theo Username hoặc Email
    public static User findUser(Context context, String identity) {
        for (User user : getAccounts(context)) {
            if (user.username.equalsIgnoreCase(identity) || user.email.equalsIgnoreCase(identity)) {
                return user;
            }
        }
        return null;
    }

    // --- QUẢN LÝ PHIÊN ĐĂNG NHẬP (Session) ---

    public static void setLoggedInUser(Context context, User user) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(user);
        prefs.edit().putString(KEY_LOGGED_IN_USER, json).apply();
    }

    public static User getLoggedInUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LOGGED_IN_USER, null);
        if (json == null) return null;
        return new Gson().fromJson(json, User.class);
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_LOGGED_IN_USER).apply();
    }
}
