package com.example.weather.dto;

/**
 * 单日预报（面向前端）。weatherCode 保留，供图标/描述映射使用。
 */
public record DailyForecast(
        String date,
        int weatherCode,
        double tempMax,
        double tempMin,
        int precipProb) {
}
