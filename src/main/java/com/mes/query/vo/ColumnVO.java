package com.mes.query.vo;

import lombok.Data;

/**
 * 表列信息
 */
@Data
public class ColumnVO {
    private String name;
    private String type;
    private int size;
    private boolean nullable;
    private String remarks;
}
