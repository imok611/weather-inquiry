package com.example.weather.controller;

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

    /** 主接口：城市名 → 完整天气数据 */
    @GetMapping("/weather")
    public WeatherResponse getWeather(@RequestParam String city) {
        return weatherService.getWeather(city);
    }

    /** 浏览器定位直查：经纬度 → 完整天气数据 */
    @GetMapping("/weather/location")
    public WeatherResponse getWeatherByLocation(@RequestParam double lat, @RequestParam double lon) {
        return weatherService.getWeatherByLocation(lat, lon);
    }
}
