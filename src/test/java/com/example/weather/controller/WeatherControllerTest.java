package com.example.weather.controller;

import com.example.weather.dto.GeoResult;
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
    void geocodeReturnsServiceResult() throws Exception {
        when(weatherService.geocode("Beijing"))
                .thenReturn(new GeoResult("北京", "中国", 39.9075, 116.39723));

        mockMvc.perform(get("/api/geocode").param("city", "Beijing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("北京"))
                .andExpect(jsonPath("$.latitude").value(39.9075))
                .andExpect(jsonPath("$.longitude").value(116.39723));
    }
}
