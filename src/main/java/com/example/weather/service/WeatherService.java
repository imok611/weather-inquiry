package com.example.weather.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.example.weather.dto.DailyForecast;
import com.example.weather.dto.ForecastResponse;
import com.example.weather.dto.GeocodingResponse;
import com.example.weather.dto.GeoResult;
import com.example.weather.exception.CityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {

    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=zh";

    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s"
                    + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                    + "&timezone=auto&forecast_days=7";

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.create();
    }

    /**
     * 城市名 → 经纬度。查无此城市时抛 CityNotFoundException。
     */
    public GeoResult geocode(String city) {
        // 必须用 uri(URI)：传 String 时 RestClient 会先解码再按 JVM 默认字符集（本机 GBK）
        // 重新编码，导致中文城市名乱码查不到；URL 已由 buildGeocodingUrl 完成 UTF-8 编码，直接传 URI 原样发送
        GeocodingResponse response = restClient.get()
                .uri(URI.create(buildGeocodingUrl(city)))
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

    /**
     * 经纬度 → 完整预报响应（current + 列式 daily）。
     */
    public ForecastResponse fetchForecast(double latitude, double longitude) {
        return restClient.get()
                .uri(URI.create(buildForecastUrl(latitude, longitude)))
                .retrieve()
                .body(ForecastResponse.class);
    }

    static String buildForecastUrl(double latitude, double longitude) {
        return String.format(FORECAST_URL, latitude, longitude);
    }

    /**
     * 上游 daily 是列式平行数组，按下标对齐转成逐日对象；null 安全。
     */
    static List<DailyForecast> extractDailyForecasts(ForecastResponse.Daily daily) {
        if (daily == null || daily.time() == null || daily.time().isEmpty()) {
            return List.of();
        }
        List<DailyForecast> result = new ArrayList<>();
        for (int i = 0; i < daily.time().size(); i++) {
            result.add(new DailyForecast(
                    daily.time().get(i),
                    daily.weatherCode().get(i),
                    daily.temperature2mMax().get(i),
                    daily.temperature2mMin().get(i),
                    daily.precipitationProbabilityMax().get(i)));
        }
        return result;
    }
}
