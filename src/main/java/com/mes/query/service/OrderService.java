package com.mes.query.service;

import com.mes.query.common.BusinessException;
import com.mes.query.vo.OrderDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 订单聚合查询服务
 */
@Slf4j
@Service
public class OrderService {

    @Autowired
    private DataSource dataSource;

    private static final String DATABASE = "ddcoreprd";

    /**
     * 查询订单完整信息：订单主表 + 客户 + 明细
     *
     * @param orderNo 订单号
     * @param site    站点
     */
    public OrderDetailVO getOrderDetail(String orderNo, String site) throws SQLException {
        OrderDetailVO vo = new OrderDetailVO();

        // 1. 查订单主表
        Map<String, Object> order = queryOne(
            "SELECT * FROM " + DATABASE + ".sd_order WHERE ORDER_NO = ? AND SITE = ?",
            orderNo, site
        );
        if (order == null) {
            throw new BusinessException(404, "订单不存在: " + orderNo);
        }
        vo.setOrder(order);

        // 2. 客户信息
        String customBo = (String) order.get("CUSTOM_BO");
        if (customBo != null && !customBo.isEmpty()) {
            Map<String, Object> customer = queryOne(
                "SELECT HANDLE, PARTNER_NO, SHORT_NAME, PARTNER_DESC, PARTNER_CATEGORY FROM " + DATABASE + ".md_partner WHERE HANDLE = ?",
                customBo
            );
            vo.setCustomer(customer);
        }

        // 3. 订单明细
        List<Map<String, Object>> details = queryList(
            "SELECT * FROM " + DATABASE + ".sd_order_detail WHERE ORDER_BO = ?",
            order.get("HANDLE")
        );
        vo.setDetails(details);

        log.info("订单聚合查询完成: {}, 明细数={}", orderNo, details.size());
        return vo;
    }

    private Map<String, Object> queryOne(String sql, Object... params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                return row;
            }
        }
        return null;
    }

    private List<Map<String, Object>> queryList(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
