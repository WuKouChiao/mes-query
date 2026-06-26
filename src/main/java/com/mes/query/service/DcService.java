package com.mes.query.service;

import com.mes.query.common.SqlTimer;
import com.mes.query.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 数采数据查询服务
 */
@Slf4j
@Service
public class DcService {

    private static final String DATABASE = "ddcoreprd";
    private static final int MAX_PAGE_SIZE = 500;

    @Autowired
    private DataSource dataSource;

    /**
     * 查询数采数据（电流/电压），强制时间范围走 DC_TIME 索引
     *
     * @param tableName 表名（dc_station_to_electricity / dc_station_to_voltage）
     * @param valueColumn 值列名（ELECTRICITY / VOLTAGE）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param station   工位（可选）
     */
    public PageVO<Map<String, Object>> queryDc(String tableName, String valueColumn,
                                                String startTime, String endTime,
                                                String station, int page, int size) throws SQLException {
        long startMs = System.currentTimeMillis();
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page < 1) page = 1;

        StringBuilder where = new StringBuilder(" WHERE DC_TIME >= ? AND DC_TIME <= ?");
        List<String> params = new ArrayList<>();
        params.add(startTime);
        params.add(endTime);

        if (station != null && !station.isEmpty()) {
            where.append(" AND STATION = ?");
            params.add(station);
        }

        // count
        String countSql = "SELECT COUNT(*) FROM " + DATABASE + "." + tableName + where;
        long total;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            rs.next();
            total = rs.getLong(1);
        }

        // data
        String sql = "SELECT STATION, " + valueColumn + ", DC_TIME, PLC FROM " + DATABASE + "." + tableName
                + where + " ORDER BY DC_TIME DESC LIMIT " + size + " OFFSET " + ((page - 1) * size);

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
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

        SqlTimer.logQuery(tableName, sql, params, startMs);
        return new PageVO<>(total, page, size, rows);
    }
}
