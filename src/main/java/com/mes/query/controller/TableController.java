package com.mes.query.controller;

import com.mes.query.common.ResultVO;
import com.mes.query.service.TableService;
import com.mes.query.vo.ColumnVO;
import com.mes.query.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 表查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/tables")
public class TableController {

    @Autowired
    private TableService tableService;

    /**
     * 列出 ddcoreprd 库所有表
     */
    @GetMapping
    public ResultVO<List<String>> listTables() throws SQLException {
        log.info("查询所有表");
        return ResultVO.success(tableService.listTables());
    }

    /**
     * 查询单表列信息
     */
    @GetMapping("/{tableName}/columns")
    public ResultVO<List<ColumnVO>> getColumns(@PathVariable String tableName) throws SQLException {
        log.info("查询表 {} 列信息", tableName);
        return ResultVO.success(tableService.getColumns(tableName));
    }

    /**
     * 分页查询表数据
     *
     * @param tableName 表名
     * @param page      页码，从 1 开始，默认 1
     * @param size      每页条数，默认 50，最大 500
     * @param filter    等值过滤，格式 "col1:val1,col2:val2"
     */
    @GetMapping("/{tableName}")
    public ResultVO<PageVO<Map<String, Object>>> queryTable(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String filter) throws SQLException {
        log.info("查询表 {}, page={}, size={}, filter={}", tableName, page, size, filter);
        return ResultVO.success(tableService.queryTable(tableName, page, size, filter));
    }
}
