package com.example.weather.dto;

/**
 * 实时天气（面向前端）。字段名即最终 JSON 字段名。
 */
public record CurrentWeather(
        double temperature,
        double feelsLike,
        int weatherCode,
        double windSpeed,
        int humidity) {
}
