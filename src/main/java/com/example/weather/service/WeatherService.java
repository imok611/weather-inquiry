package com.example.weather.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.example.weather.dto.GeocodingResponse;
import com.example.weather.dto.GeoResult;
import com.example.weather.exception.CityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=zh";

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.create();
    }

    /**
     * 城市名 → 经纬度。查无此城市时抛 CityNotFoundException。
     */
    public GeoResult geocode(String city) {
        GeocodingResponse response = restClient.get()
                .uri(buildGeocodingUrl(city))
                .retrieve()
                .body(GeocodingResponse.class);
        return extractFirstResult(response, city);
    }

    static String buildGeocodingUrl(String city) {
        return String.format(GEOCODING_URL, URLEncoder.encode(city, StandardCharsets.UTF_8));
    }

    static GeoResult extractFirstResult(GeocodingResponse response, String city) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new CityNotFoundException(city);
        }
        return response.results().get(0);
    }
}
