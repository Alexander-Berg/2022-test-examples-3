package ru.yandex.canvas.controllers;

import java.util.Locale;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.steps.ResourceHelpers;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoGetPresetControllerTest {

    public static final String TEST_VALID_JSON =
            "/ru/yandex/canvas/controllers/videoGetPresetControllerTest/valid.json";

    private static String validJson;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionParams sessionParams;

    @BeforeClass
    public static void setUp() throws Exception {
        validJson = ResourceHelpers.getResource(TEST_VALID_JSON);
    }

    @Test
    public void getPresetInJson() throws Exception {
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPC);

        mockMvc.perform(get("/video/presets/1")
                .locale(Locale.forLanguageTag("ru"))
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().when(Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS, Option.TREATING_NULL_AS_ABSENT).isEqualTo(validJson))
                .andExpect(status().is(200));
    }

}
