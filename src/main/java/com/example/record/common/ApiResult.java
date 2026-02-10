package com.example.record.common;

import lombok.Data;

@Data
public class ApiResult<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 200;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> error(String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 500;
        r.msg = msg;
        r.data = null;
        return r;
    }
}
