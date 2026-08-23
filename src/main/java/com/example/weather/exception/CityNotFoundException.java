package com.example.weather.exception;

/**
 * Geocoding 查无此城市时抛出，Day 3 由 GlobalExceptionHandler 翻译为 404。
 */
public class CityNotFoundException extends RuntimeException {

    public CityNotFoundException(String city) {
        super("未找到城市: " + city);
    }
}
