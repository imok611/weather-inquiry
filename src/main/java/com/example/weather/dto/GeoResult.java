package com.example.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Geocoding API results 数组的单个元素。
 * 只保留本项目用到的字段，其余由 ignoreUnknown 忽略。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoResult(
        String name,
        String country,
        double latitude,
        double longitude) {
}
