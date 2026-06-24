package com.mes.query.controller;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class TableController {

    @Autowired
    private DataSource dataSource;

    /**
     * 列出 ddcoreprd 库所有表
     */
    @GetMapping("/tables")
    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables("ddcoreprd", null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        tables.sort(String::compareTo);
        return tables;
    }

    /**
     * 查询单表结构（列信息）
     */
    @GetMapping("/tables/{tableName}/columns")
    public List<Map<String, Object>> getColumns(@PathVariable String tableName) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns("ddcoreprd", null, tableName, null)) {
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name", rs.getString("COLUMN_NAME"));
                col.put("type", rs.getString("TYPE_NAME"));
                col.put("size", rs.getInt("COLUMN_SIZE"));
                col.put("nullable", rs.getInt("NULLABLE") == 1);
                col.put("remarks", rs.getString("REMARKS"));
                columns.add(col);
            }
        }
        return columns;
    }

    /**
     * 查询单表数据
     * @param tableName 表名
     * @param page 页码（从1开始）
     * @param size 每页条数（默认50，最大500）
     * @param filter 等值过滤条件，JSON格式 {"column":"value", ...}
     */
    @GetMapping("/tables/{tableName}")
    public Map<String, Object> queryTable(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String filter) throws SQLException {

        if (size > 500) size = 500;
        if (page < 1) page = 1;

        // 解析 filter JSON
        Map<String, String> filters = new LinkedHashMap<>();
        if (filter != null && !filter.isEmpty()) {
            // 简单的 key:value 解析，格式 "col1:val1,col2:val2"
            for (String kv : filter.split(",")) {
                String[] parts = kv.split(":", 2);
                if (parts.length == 2) {
                    filters.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        // 构建 SQL
        StringBuilder sql = new StringBuilder("SELECT * FROM ddcoreprd." + tableName + " WHERE 1=1");
        List<String> paramValues = new ArrayList<>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            sql.append(" AND ").append(entry.getKey()).append(" = ?");
            paramValues.add(entry.getValue());
        }

        // 查询总数
        String countSql = sql.toString().replace("SELECT *", "SELECT COUNT(*)");
        long total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < paramValues.size(); i++) {
                ps.setString(i + 1, paramValues.get(i));
            }
            ResultSet rs = ps.executeQuery();
            if (rs.next()) total = rs.getLong(1);
        }

        // 分页查询数据
        sql.append(" LIMIT ").append(size).append(" OFFSET ").append((page - 1) * size);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < paramValues.size(); i++) {
                ps.setString(i + 1, paramValues.get(i));
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));
        result.put("data", rows);
        return result;
    }
}
