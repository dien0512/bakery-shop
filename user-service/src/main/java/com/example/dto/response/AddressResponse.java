package com.example.dto.response;

import java.time.LocalDateTime;

public class AddressResponse {
    public String id;
    public String userId;
    public String receiverName;
    public String phone;
    public String addressLine;
    public String ward;
    public String district;
    public String city;
    public Boolean isDefault;
    public LocalDateTime createdAt;
}