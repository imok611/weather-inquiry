package com.example.weather.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleCityNotFound(CityNotFoundException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(UpstreamApiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleUpstream(UpstreamApiException e) {
        return Map.of("error", "上游天气服务暂不可用，请稍后重试");
    }
}
