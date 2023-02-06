package ru.yandex.canvas.controllers;

import java.util.Collections;
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

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPresetsControllerTallTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SessionParams sessionParams;
    @Autowired
    private DirectService directService;

    @Test
    public void text_show_tall_feature_enabled() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("canvas_range_ratio_cpc"));

        mockMvc.perform(
                get("/video/presets")
                        .param("client_id", String.valueOf(1L))
                        .param("user_id", String.valueOf(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(json().node("total").isEqualTo(4L))
                .andExpect(status().is(200));
    }

    @Test
    public void text_hide_tall_feature_disabled() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Collections.emptySet());

        mockMvc.perform(
                get("/video/presets")
                        .param("client_id", String.valueOf(1L))
                        .param("user_id", String.valueOf(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(json().node("total").isEqualTo(4L))
                .andExpect(status().is(200));
    }

    @Test
    public void cpm_show_tall_feature_enabled() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("canvas_range_ratio"));

        mockMvc.perform(
                get("/video/presets")
                        .param("client_id", String.valueOf(1L))
                        .param("user_id", String.valueOf(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is(200))
                .andExpect(json().node("total").isEqualTo(1L));
    }

    @Test
    public void cpm_hide_tall_feature_disabled() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Collections.emptySet());

        mockMvc.perform(
                get("/video/presets")
                        .param("client_id", String.valueOf(1L))
                        .param("user_id", String.valueOf(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(json().node("total").isEqualTo(1L))
                .andExpect(status().is(200));
    }

    @Test
    public void hide_unwanted_preset() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.TEXT);
        when(sessionParams.getVideoPresetIds()).thenReturn(null);
        when(directService.getFeatures(any(), any())).thenReturn(Set.of("canvas_range_ratio_cpc", "hide_unwanted_preset"));

        mockMvc.perform(
                        get("/video/presets")
                                .param("client_id", String.valueOf(1L))
                                .param("user_id", String.valueOf(2L))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                                .locale(Locale.forLanguageTag("ru"))
                                .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(json().node("total").isEqualTo(1L))
                .andExpect(status().is(200));
    }
}
