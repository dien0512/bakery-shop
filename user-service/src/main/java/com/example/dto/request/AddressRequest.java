package com.example.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AddressRequest {

    @NotBlank
    public String receiverName;

    @NotBlank
    public String phone;

    @NotBlank
    public String addressLine;

    public String ward;
    public String district;
    public String city;
    public Boolean isDefault = false;
}