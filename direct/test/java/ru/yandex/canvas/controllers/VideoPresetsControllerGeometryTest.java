package ru.yandex.canvas.controllers;

import java.util.Locale;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.steps.ResourceHelpers;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPresetsControllerGeometryTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SessionParams sessionParams;
    @Autowired
    private DirectService directService;

    @Test
    public void preset_geometry_wide() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("hide_unwanted_preset"));
        String validJson = ResourceHelpers.getResource(
                "/ru/yandex/canvas/controllers/videoPresetsControllerGeometryTest/text.json");

        mockMvc.perform(
                get("/video/presets")
                        .param("client_id", String.valueOf(1L))
                        .param("user_id", String.valueOf(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(json().when(IGNORING_ARRAY_ORDER, IGNORING_EXTRA_FIELDS).isEqualTo(validJson))
                .andExpect(status().is(200));
    }

    @Test
    public void preset_geometry_default() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("hide_unwanted_preset"));
        String validJson = ResourceHelpers.getResource(
                "/ru/yandex/canvas/controllers/videoPresetsControllerGeometryTest/cpc.json");

        mockMvc.perform(
                get("/video/presets")
                        .param("client_id", String.valueOf(1L))
                        .param("user_id", String.valueOf(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                //.andExpect(e -> System.err.println("!!!" + e.getResponse().getContentAsString()))
                .andExpect(json().when(IGNORING_ARRAY_ORDER, IGNORING_EXTRA_FIELDS).isEqualTo(validJson))
                .andExpect(status().is(200));
    }
}
