package com.example.weather.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void geocodingResponseParsesFirstResult() throws Exception {
        String json = """
                {
                  "results": [
                    { "id": 1816670, "name": "北京", "latitude": 39.9075,
                      "longitude": 116.39723, "country": "中华人民共和国",
                      "country_code": "CN", "timezone": "Asia/Shanghai" }
                  ],
                  "generationtime_ms": 0.05
                }
                """;
        GeocodingResponse resp = mapper.readValue(json, GeocodingResponse.class);
        assertEquals(1, resp.results().size());
        GeoResult first = resp.results().get(0);
        assertEquals("北京", first.name());
        assertEquals(39.9075, first.latitude(), 0.001);
        assertEquals(116.39723, first.longitude(), 0.001);
        assertEquals("中华人民共和国", first.country());
    }

    @Test
    void geocodingResponseHandlesEmptyResults() throws Exception {
        String json = """
                { "generationtime_ms": 0.03 }
                """;
        GeocodingResponse resp = mapper.readValue(json, GeocodingResponse.class);
        assertTrue(resp.results() == null || resp.results().isEmpty());
    }

    @Test
    void forecastResponseParsesCurrentAndDailyColumns() throws Exception {
        String json = """
                {
                  "latitude": 39.875, "longitude": 116.375, "timezone": "Asia/Shanghai",
                  "current": {
                    "time": "2026-08-24T10:00", "temperature_2m": 28.5,
                    "apparent_temperature": 30.1, "relative_humidity_2m": 55,
                    "weather_code": 0, "wind_speed_10m": 12.0
                  },
                  "daily": {
                    "time": ["2026-08-24", "2026-08-25"],
                    "weather_code": [1, 61],
                    "temperature_2m_max": [30.2, 27.8],
                    "temperature_2m_min": [22.1, 21.5],
                    "precipitation_probability_max": [10, 70]
                  }
                }
                """;
        ForecastResponse resp = mapper.readValue(json, ForecastResponse.class);
        assertEquals(28.5, resp.current().temperature2m(), 0.001);
        assertEquals(30.1, resp.current().apparentTemperature(), 0.001);
        assertEquals(55, resp.current().relativeHumidity2m());
        assertEquals(0, resp.current().weatherCode());
        assertEquals(12.0, resp.current().windSpeed10m(), 0.001);
        assertEquals(2, resp.daily().time().size());
        assertEquals("2026-08-24", resp.daily().time().get(0));
        assertEquals(61, resp.daily().weatherCode().get(1));
        assertEquals(30.2, resp.daily().temperature2mMax().get(0), 0.001);
        assertEquals(21.5, resp.daily().temperature2mMin().get(1), 0.001);
        assertEquals(70, resp.daily().precipitationProbabilityMax().get(1));
    }
}
