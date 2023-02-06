package ru.yandex.canvas.controllers;

import java.util.Locale;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.steps.ResourceHelpers;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPresetsCommonTagControllerTest {

    public static final String TEST_VALID_JSON =
            "/ru/yandex/canvas/controllers/videoPresetsCommonTagControllerTest/valid.json";

    public static String validJson;

    @Autowired
    private MockMvc mockMvc;

    @BeforeClass
    public static void setUp() throws Exception {
        validJson = ResourceHelpers.getResource(TEST_VALID_JSON);
    }

    @Test
    @Ignore
    public void getPresetInJson() throws Exception {
//http://172.17.42.1:11362/video/presets?cpc_video_banner=1&cpm_banner=&mobile_content_banner=&user_id=734948687
// &client_id=103997791
        mockMvc.perform(
                get("/video/presets?user_id=734948687&client_id=103997791")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(e -> System.err.println("!!!" + e.getResponse().getContentAsString()))
                .andExpect(
                        json().when(Option.IGNORING_ARRAY_ORDER).isEqualTo(validJson))
                .andExpect(status().is(200));
    }

}
