package com.example.weather.exception;

/**
 * 包装上游 API 故障（429、网络超时、5xx 等），由全局处理器统一翻译成 503。
 */
public class UpstreamApiException extends RuntimeException {

    public UpstreamApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
