package com.example.weather.service;

import java.util.List;

import com.example.weather.dto.GeoResult;
import com.example.weather.dto.GeocodingResponse;
import com.example.weather.exception.CityNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherServiceTest {

    @Test
    void buildGeocodingUrlEncodesChineseCity() {
        String url = WeatherService.buildGeocodingUrl("北京");
        assertTrue(url.contains("name=%E5%8C%97%E4%BA%AC"), "中文城市名必须 URL 编码，实际: " + url);
        assertTrue(url.contains("count=1"));
        assertTrue(url.contains("language=zh"));
    }

    @Test
    void buildGeocodingUrlEncodesSpace() {
        String url = WeatherService.buildGeocodingUrl("New York");
        assertTrue(url.contains("name=New+York") || url.contains("name=New%20York"),
                "空格必须被编码，实际: " + url);
    }

    @Test
    void extractFirstResultReturnsFirstMatch() {
        GeoResult beijing = new GeoResult("北京", "中国", 39.9075, 116.39723);
        GeocodingResponse response = new GeocodingResponse(List.of(beijing));
        GeoResult result = WeatherService.extractFirstResult(response, "Beijing");
        assertEquals(39.9075, result.latitude(), 0.001);
        assertEquals(116.39723, result.longitude(), 0.001);
    }

    @Test
    void extractFirstResultThrowsWhenResultsEmpty() {
        GeocodingResponse response = new GeocodingResponse(List.of());
        assertThrows(CityNotFoundException.class,
                () -> WeatherService.extractFirstResult(response, "asdfg"));
    }

    @Test
    void extractFirstResultThrowsWhenResultsNull() {
        GeocodingResponse response = new GeocodingResponse(null);
        assertThrows(CityNotFoundException.class,
                () -> WeatherService.extractFirstResult(response, "asdfg"));
    }
}
