package com.jingqu.dto;

import lombok.Data;
import java.util.List;

@Data
public class PlaceDetailDTO {
    private String name;
    private String address;
    private String city;
    private Double lat;
    private Double lng;
    private String category;       // 景点/美食/饮品/购物/住宿
    private String categoryTag;    // Amap type e.g. "风景名胜"
    private Double rating;         // 1.0-5.0
    private Integer reviewCount;
    private String openingHours;
    private String phone;
    private String cost;
    private List<String> photos;
    private String aiDescription;  // DeepSeek生成的介绍
    private List<ReviewCard> aiReviews; // DeepSeek生成的评价

    @Data
    public static class ReviewCard {
        private String userName;
        private String avatarUrl;
        private String content;
        private int likeCount;
        private int rating;  // 1-5
    }
}
