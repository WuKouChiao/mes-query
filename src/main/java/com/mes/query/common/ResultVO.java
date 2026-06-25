package com.mes.query.common;

import lombok.Data;

/**
 * 统一返回体
 *
 * @param <T> 数据类型
 */
@Data
public class ResultVO<T> {
    private int code;
    private String message;
    private T data;

    private ResultVO(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(200, "success", data);
    }

    public static <T> ResultVO<T> success() {
        return new ResultVO<>(200, "success", null);
    }

    public static <T> ResultVO<T> error(int code, String message) {
        return new ResultVO<>(code, message, null);
    }
}
