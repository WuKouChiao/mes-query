package com.mes.query.controller;

import com.mes.query.common.ResultVO;
import com.mes.query.service.DcService;
import com.mes.query.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

/**
 * 数采数据查询接口（电流/电压）
 */
@Slf4j
@RestController
@RequestMapping("/api/dc")
public class DcController {

    @Autowired
    private DcService dcService;

    /**
     * 查询电流数据
     *
     * @param startTime 开始时间（必填），格式 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间（必填）
     * @param station   工位（可选）
     * @param page      页码，默认 1
     * @param size      每页条数，默认 50，最大 500
     */
    @GetMapping("/electricity")
    public ResultVO<PageVO<Map<String, Object>>> queryElectricity(
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String station,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) throws SQLException {
        log.info("查询电流数据 startTime={} endTime={} station={} page={} size={}", startTime, endTime, station, page, size);
        return ResultVO.success(dcService.queryDc("dc_station_to_electricity", "ELECTRICITY", startTime, endTime, station, page, size));
    }

    /**
     * 查询电压数据
     */
    @GetMapping("/voltage")
    public ResultVO<PageVO<Map<String, Object>>> queryVoltage(
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String station,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) throws SQLException {
        log.info("查询电压数据 startTime={} endTime={} station={} page={} size={}", startTime, endTime, station, page, size);
        return ResultVO.success(dcService.queryDc("dc_station_to_voltage", "VOLTAGE", startTime, endTime, station, page, size));
    }
}
