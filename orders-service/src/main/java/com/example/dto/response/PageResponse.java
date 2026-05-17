package com.example.dto.response;

import java.util.List;

public class PageResponse<T> {
    public List<T> data;
    public int page;
    public int size;
    public long total;
    public int totalPages;

    public PageResponse(List<T> data, int page, int size, long total) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
}
