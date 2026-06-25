package com.mes.query.controller;

import com.mes.query.common.ResultVO;
import com.mes.query.service.OrderService;
import com.mes.query.vo.OrderDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

/**
 * 订单聚合查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 查询订单完整信息（订单 + 客户 + 明细）
     *
     * @param orderNo 订单号，如 SO-260623-021
     * @param site    站点，默认 3100
     */
    @GetMapping("/{orderNo}")
    public ResultVO<OrderDetailVO> getOrder(
            @PathVariable String orderNo,
            @RequestParam(defaultValue = "3100") String site) throws SQLException {
        log.info("查询订单详情: orderNo={}, site={}", orderNo, site);
        return ResultVO.success(orderService.getOrderDetail(orderNo, site));
    }
}
