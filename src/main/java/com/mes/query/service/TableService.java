package com.mes.query.service;

import com.mes.query.common.BusinessException;
import com.mes.query.vo.ColumnVO;
import com.mes.query.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 表查询服务——只读，ddcoreprd 库
 */
@Slf4j
@Service
public class TableService {

    private static final String DATABASE = "ddcoreprd";
    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private DataSource dataSource;

    /** 缓存的表名白名单，项目启动后延迟加载 */
    private volatile Set<String> tableCache;

    /**
     * 列出所有表名
     */
    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(DATABASE, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        tables.sort(String::compareTo);
        return tables;
    }

    /**
     * 获取表列信息
     *
     * @param tableName 表名（需通过白名单校验）
     */
    public List<ColumnVO> getColumns(String tableName) throws SQLException {
        validateTable(tableName);
        List<ColumnVO> columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(DATABASE, null, tableName, null)) {
            while (rs.next()) {
                ColumnVO col = new ColumnVO();
                col.setName(rs.getString("COLUMN_NAME"));
                col.setType(rs.getString("TYPE_NAME"));
                col.setSize(rs.getInt("COLUMN_SIZE"));
                col.setNullable(rs.getInt("NULLABLE") == 1);
                col.setRemarks(rs.getString("REMARKS"));
                columns.add(col);
            }
        }
        return columns;
    }

    /**
     * 分页查询表数据（支持等值过滤）
     *
     * @param tableName 表名（需通过白名单校验）
     * @param page      页码，从 1 开始
     * @param size      每页条数，最大 500
     * @param filter    等值过滤，格式 "col1:val1,col2:val2"
     * @return 分页结果，每行数据为 Map（key 为列名，value 为列值）
     */
    public PageVO<Map<String, Object>> queryTable(String tableName, int page, int size, String filter) throws SQLException {
        validateTable(tableName);
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page < 1) page = 1;

        // 解析过滤条件
        Map<String, String> filters = parseFilter(filter, tableName);

        // 构建 SQL
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(DATABASE).append(".").append(tableName)
                .append(" WHERE 1=1");
        List<String> paramValues = new ArrayList<>();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            sql.append(" AND ").append(entry.getKey()).append(" = ?");
            paramValues.add(entry.getValue());
        }

        // 查总数
        String countSql = "SELECT COUNT(*) FROM " + DATABASE + "." + tableName + " WHERE 1=1";
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            countSql += " AND " + entry.getKey() + " = ?";
        }
        long total;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < paramValues.size(); i++) {
                ps.setString(i + 1, paramValues.get(i));
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            total = rs.getLong(1);
        }

        // 查数据（分页）
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

        log.info("查询表 {} 完成, total={}, page={}/{}, filters={}", tableName, total, page,
                (int) Math.ceil((double) total / size), filters.keySet());
        return new PageVO<>(total, page, size, rows);
    }

    // ---- 私有方法 ----

    /**
     * 表名白名单校验
     */
    private void validateTable(String tableName) throws SQLException {
        if (tableCache == null) {
            synchronized (this) {
                if (tableCache == null) {
                    tableCache = new HashSet<>(listTables());
                }
            }
        }
        if (!tableCache.contains(tableName)) {
            throw new BusinessException(400, "表不存在: " + tableName);
        }
    }

    /**
     * 列名白名单校验 + 过滤条件解析
     */
    private Map<String, String> parseFilter(String filter, String tableName) throws SQLException {
        Map<String, String> result = new LinkedHashMap<>();
        if (filter == null || filter.isEmpty()) {
            return result;
        }

        // 获取表的所有列名
        Set<String> validColumns = new HashSet<>();
        for (ColumnVO col : getColumns(tableName)) {
            validColumns.add(col.getName());
        }

        for (String kv : filter.split(",")) {
            String[] parts = kv.split(":", 2);
            if (parts.length != 2) {
                throw new BusinessException(400, "过滤格式错误，应为 col1:val1,col2:val2");
            }
            String col = parts[0].trim();
            if (!validColumns.contains(col)) {
                throw new BusinessException(400, "表中不存在列: " + col);
            }
            result.put(col, parts[1].trim());
        }
        return result;
    }
}
