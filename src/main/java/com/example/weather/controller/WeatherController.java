package com.example.weather.controller;

import java.util.Map;

import com.example.weather.dto.GeoResult;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("msg", "hello");
    }

    /** 主接口：城市名 → 完整天气数据 */
    @GetMapping("/weather")
    public WeatherResponse getWeather(@RequestParam String city) {
        return weatherService.getWeather(city);
    }

    /** 临时验证接口，Day 5 收尾时删除 */
    @GetMapping("/geocode")
    public GeoResult geocode(@RequestParam String city) {
        return weatherService.geocode(city);
    }
}
