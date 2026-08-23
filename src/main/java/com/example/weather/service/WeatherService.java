package com.example.weather.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import com.example.weather.dto.CurrentWeather;
import com.example.weather.dto.DailyForecast;
import com.example.weather.dto.ForecastResponse;
import com.example.weather.dto.GeocodingResponse;
import com.example.weather.dto.GeoResult;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.exception.CityNotFoundException;
import com.example.weather.exception.UpstreamApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

    private static final long TTL_MILLIS = 10 * 60 * 1000; // 10 分钟

    /** 缓存条目：组装好的最终响应 + 写入时间（epoch millis） */
    record CacheEntry(WeatherResponse data, long createdAt) {
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public WeatherService() {
        this(System::currentTimeMillis);
    }

    /** 包私有构造：供单测注入可控时钟 */
    WeatherService(LongSupplier clock) {
        this.restClient = RestClient.create();
        this.clock = clock;
    }

    /**
     * 城市名 → 经纬度。查无此城市时抛 CityNotFoundException。
     */
    public GeoResult geocode(String city) {
        // 必须用 uri(URI)：传 String 时 RestClient 会先解码再按 JVM 默认字符集（本机 GBK）
        // 重新编码，导致中文城市名乱码查不到；URL 已由 buildGeocodingUrl 完成 UTF-8 编码，直接传 URI 原样发送
        GeocodingResponse response;
        try {
            response = restClient.get()
                    .uri(URI.create(buildGeocodingUrl(city)))
                    .retrieve()
                    .body(GeocodingResponse.class);
        } catch (RestClientException e) {
            throw new UpstreamApiException("调用 Geocoding API 失败: " + city, e);
        }
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
     * 主流程：城市名 → 查缓存 → geocode → forecast → 组装。
     * 缓存套在组装好的最终响应上，惰性淘汰（读时检查过期），命中返回 cached=true。
     */
    public WeatherResponse getWeather(String city) {
        // 校验在查缓存之前：空白输入永远不进缓存
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("请输入城市名称");
        }
        String key = normalize(city);
        CacheEntry entry = cache.get(key);
        if (entry != null && clock.getAsLong() - entry.createdAt() < TTL_MILLIS) {
            return withCachedFlag(entry.data());
        }
        GeoResult geo = geocode(city);
        ForecastResponse forecast = fetchForecast(geo.latitude(), geo.longitude());
        WeatherResponse fresh = assemble(geo, forecast);
        cache.put(key, new CacheEntry(fresh, clock.getAsLong()));
        return fresh;
    }

    /** 归一化缓存 key：trim + 小写，"Beijing" / "beijing" / " Beijing " 视为同一 key */
    static String normalize(String city) {
        return city.trim().toLowerCase(Locale.ROOT);
    }

    /** 缓存命中时返回带 cached=true 的副本，不污染缓存里的原始对象 */
    static WeatherResponse withCachedFlag(WeatherResponse response) {
        return new WeatherResponse(response.city(), response.country(),
                response.current(), response.daily(), true);
    }

    private WeatherResponse assemble(GeoResult geo, ForecastResponse forecast) {
        ForecastResponse.Current current = forecast.current();
        return new WeatherResponse(
                geo.name(),
                geo.country(),
                new CurrentWeather(
                        current.temperature2m(),
                        current.apparentTemperature(),
                        current.weatherCode(),
                        current.windSpeed10m(),
                        current.relativeHumidity2m()),
                extractDailyForecasts(forecast.daily()),
                false);
    }

    /**
     * 经纬度 → 完整预报响应（current + 列式 daily）。
     */
    public ForecastResponse fetchForecast(double latitude, double longitude) {
        try {
            return restClient.get()
                    .uri(URI.create(buildForecastUrl(latitude, longitude)))
                    .retrieve()
                    .body(ForecastResponse.class);
        } catch (RestClientException e) {
            throw new UpstreamApiException("调用 Forecast API 失败: " + latitude + "," + longitude, e);
        }
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
