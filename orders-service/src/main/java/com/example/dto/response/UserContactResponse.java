package com.example.dto.response;

import lombok.Data;

@Data
public class UserContactResponse {
    private String id;
    private String email;
    private String phone;    // có thể null nếu user chưa điền
    private String fullName;
}