package com.campusforum.common;

import lombok.Data;

import java.util.UUID;

@Data
public class R<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
    }

    public static <T> R<T> ok() {
        return new R<>(0, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(0, "ok", data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
