package com.mes.query.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 数据类型
 */
@Data
public class PageVO<T> {
    private long total;
    private int page;
    private int size;
    private int pages;
    private List<T> data;

    public PageVO(long total, int page, int size, List<T> data) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
        this.data = data;
    }
}
