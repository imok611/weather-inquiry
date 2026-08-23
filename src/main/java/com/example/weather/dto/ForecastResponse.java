package com.example.weather.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Forecast API 顶层响应。注意 daily 是列式结构（平行数组），
 * Day 3 组装时按下标对齐转成 List<DailyForecast>。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(Current current, Daily daily) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            @JsonProperty("temperature_2m") double temperature2m,
            @JsonProperty("apparent_temperature") double apparentTemperature,
            @JsonProperty("relative_humidity_2m") int relativeHumidity2m,
            @JsonProperty("weather_code") int weatherCode,
            @JsonProperty("wind_speed_10m") double windSpeed10m) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
            List<String> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_max") List<Double> temperature2mMax,
            @JsonProperty("temperature_2m_min") List<Double> temperature2mMin,
            @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbabilityMax) {
    }
}
