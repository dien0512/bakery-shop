package com.example.dto.request;

public class StockAdjustRequest {
    public int delta;

    public StockAdjustRequest() {}
    public StockAdjustRequest(int delta) { this.delta = delta; }
}