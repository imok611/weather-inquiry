package com.example.weather.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WeatherController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("msg", "hello");
    }
}
