package com.example.weather.service;

import java.util.List;

import com.example.weather.dto.DailyForecast;
import com.example.weather.dto.ForecastResponse;
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

    @Test
    void buildForecastUrlContainsCoordinatesAndFields() {
        String url = WeatherService.buildForecastUrl(30.2937, 120.1614);
        assertTrue(url.contains("latitude=30.2937"));
        assertTrue(url.contains("longitude=120.1614"));
        assertTrue(url.contains("current="));
        assertTrue(url.contains("daily="));
        assertTrue(url.contains("timezone=auto"));
        assertTrue(url.contains("forecast_days=7"));
    }

    @Test
    void extractDailyForecastsAlignsColumnArraysByIndex() {
        ForecastResponse.Daily daily = new ForecastResponse.Daily(
                List.of("2026-08-24", "2026-08-25", "2026-08-26"),
                List.of(0, 1, 61),
                List.of(31.0, 29.5, 27.0),
                List.of(24.0, 23.1, 21.0),
                List.of(10, 40, 90));

        List<DailyForecast> forecasts = WeatherService.extractDailyForecasts(daily);

        assertEquals(3, forecasts.size());
        assertEquals("2026-08-25", forecasts.get(1).date());
        assertEquals(1, forecasts.get(1).weatherCode());
        assertEquals(29.5, forecasts.get(1).tempMax());
        assertEquals(23.1, forecasts.get(1).tempMin());
        assertEquals(40, forecasts.get(1).precipProb());
    }

    @Test
    void extractDailyForecastsReturnsEmptyWhenDailyNull() {
        assertTrue(WeatherService.extractDailyForecasts(null).isEmpty());
    }
}
