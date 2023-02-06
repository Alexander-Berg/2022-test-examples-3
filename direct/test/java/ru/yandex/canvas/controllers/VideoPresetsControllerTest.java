package ru.yandex.canvas.controllers;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
import ru.yandex.canvas.controllers.video.PresetWrapper;
import ru.yandex.canvas.controllers.video.VideoPresetsController;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.steps.ResourceHelpers;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPresetsControllerTest {

    public static final String TEST_VALID_JSON =
            "/ru/yandex/canvas/controllers/videoPresetsControllerTest/valid.json";

    public static String validJson;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private VideoPresetsController presetsController;
    @Autowired
    private SessionParams sessionParams;

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
                get("/video/presets?cpc_video_banner=1&cpm_banner=&mobile_content_banner=&user_id=734948687&client_id"
                        + "=103997791")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru-RU, ru")
                        .locale(Locale.forLanguageTag("ru"))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(e -> System.err.println("!!!" + e.getResponse().getContentAsString()))
                .andExpect(
                        json().when(Option.IGNORING_ARRAY_ORDER).isEqualTo(validJson))
                .andExpect(status().is(200));
    }


    @Test
    public void test_getList_PresetsFilteringByIds() {
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);

        when(sessionParams.getVideoPresetIds()).thenReturn(List.of(5L, 15L));

        List<PresetWrapper> presets = presetsController.getPresetsByTag().getPresetWrappers();

        Set<Integer> expectedIds = Set.of(5, 15);
        Set<Integer> actualIds =
                presets.stream().map(PresetWrapper::getId).map(Integer::parseInt).collect(Collectors.toSet());
        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void test_getList_PresetsFilteringByIds_noList() {
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);

        when(sessionParams.getVideoPresetIds()).thenReturn(null);

        List<PresetWrapper> presets = presetsController.getPresetsByTag().getPresetWrappers();

        Set<Integer> expectedIds = Set.of(6);
        Set<Integer> actualIds =
                presets.stream().map(PresetWrapper::getId).map(Integer::parseInt).collect(Collectors.toSet());
        assertEquals(expectedIds, actualIds);
    }


    @Test
    public void test_getList_PresetsFilteringByIds_emptyList() {
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getCreativeType()).thenReturn(VideoCreativeType.CPM);

        when(sessionParams.getVideoPresetIds()).thenReturn(List.of());

        List<PresetWrapper> presets = presetsController.getPresetsByTag().getPresetWrappers();

        Set<Integer> actualIds =
                presets.stream().map(PresetWrapper::getId).map(Integer::parseInt).collect(Collectors.toSet());
        assertThat(actualIds).isEmpty();
    }


}
