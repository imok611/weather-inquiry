package com.example.weather.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Geocoding API 顶层响应。城市不存在时 results 可能为 null 或缺失。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(List<GeoResult> results) {
}
