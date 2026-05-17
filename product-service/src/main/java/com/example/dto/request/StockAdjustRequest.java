package com.example.dto.request;

import jakarta.validation.constraints.NotNull;

public class StockAdjustRequest {

    @NotNull(message = "Delta is required")
    public Integer delta;
}
