package com.example.hitcapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Region {
    public static class Province implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("code")
        public int code;
        @SerializedName("districts")
        public List<District> districts;

        @Override
        public String toString() {
            return name;
        }
    }

    public static class District implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("code")
        public int code;
        @SerializedName("wards")
        public List<Ward> wards;
    }

    public static class Ward implements Serializable {
        @SerializedName("name")
        public String name;
        @SerializedName("code")
        public int code;

        @Override
        public String toString() {
            return name;
        }
    }
}
