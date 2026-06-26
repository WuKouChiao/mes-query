package com.mes.query.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL 慢查询记录工具
 */
public final class SqlTimer {

    private static final Logger log = LoggerFactory.getLogger(SqlTimer.class);

    /** 慢查询阈值（毫秒） */
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000;

    private SqlTimer() {}

    /**
     * 记录普通查询耗时（info 级别）
     *
     * @param tableName 表名
     * @param sql       执行的 SQL
     * @param params    参数列表
     * @param startTime 开始时间戳
     * @return 耗时（毫秒）
     */
    public static long logQuery(String tableName, String sql, Object params, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("查询 {} 完成, elapsed={}ms", tableName, elapsed);
        if (elapsed > SLOW_QUERY_THRESHOLD_MS) {
            log.warn("慢查询 table={} sql={} params={} elapsed={}ms", tableName, sql, params, elapsed);
        }
        return elapsed;
    }
}
