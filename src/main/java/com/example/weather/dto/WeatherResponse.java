package com.example.weather.dto;

import java.util.List;

/**
 * GET /api/weather 的统一响应结构。
 * cached=true 表示本次命中内存缓存（Day 4 实现）。
 */
public record WeatherResponse(
        String city,
        String country,
        CurrentWeather current,
        List<DailyForecast> daily,
        boolean cached) {
}
