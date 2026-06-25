package com.mes.query.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 订单详情聚合 VO — 一次返回订单+客户+明细
 */
@Data
public class OrderDetailVO {
    /** 订单主表数据 */
    private Map<String, Object> order;
    /** 客户信息 */
    private Map<String, Object> customer;
    /** 订单明细列表 */
    private List<Map<String, Object>> details;
}
