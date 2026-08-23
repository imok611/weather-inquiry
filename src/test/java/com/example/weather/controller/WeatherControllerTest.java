package com.example.weather.controller;

import java.util.List;

import com.example.weather.dto.CurrentWeather;
import com.example.weather.dto.DailyForecast;
import com.example.weather.dto.GeoResult;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.exception.CityNotFoundException;
import com.example.weather.exception.UpstreamApiException;
import com.example.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    @Test
    void pingReturnsHello() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("hello"));
    }

    @Test
    void geocodeReturnsFirstResult() throws Exception {
        when(weatherService.geocode("Hangzhou"))
                .thenReturn(new GeoResult("Hangzhou", "China", 30.2937, 120.1614));

        mockMvc.perform(get("/api/geocode").param("city", "Hangzhou"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hangzhou"))
                .andExpect(jsonPath("$.latitude").value(30.2937));
    }

    @Test
    void weatherReturnsAssembledData() throws Exception {
        WeatherResponse stub = new WeatherResponse("杭州市", "中国",
                new CurrentWeather(26.1, 28.3, 1, 7.4, 61),
                List.of(new DailyForecast("2026-08-24", 1, 31.0, 24.0, 10)),
                false);
        when(weatherService.getWeather("杭州")).thenReturn(stub);

        mockMvc.perform(get("/api/weather").param("city", "杭州"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("杭州市"))
                .andExpect(jsonPath("$.current.temperature").value(26.1))
                .andExpect(jsonPath("$.daily[0].date").value("2026-08-24"))
                .andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    void weatherReturns404WhenCityNotFound() throws Exception {
        when(weatherService.getWeather("火星")).thenThrow(new CityNotFoundException("火星"));

        mockMvc.perform(get("/api/weather").param("city", "火星"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("未找到城市: 火星"));
    }

    @Test
    void weatherReturns503WhenUpstreamFails() throws Exception {
        when(weatherService.getWeather("Beijing"))
                .thenThrow(new UpstreamApiException("调用 Geocoding API 失败: Beijing", new RuntimeException()));

        mockMvc.perform(get("/api/weather").param("city", "Beijing"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("上游天气服务暂不可用，请稍后重试"));
    }
}
