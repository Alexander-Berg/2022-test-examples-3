package ru.yandex.canvas.controllers;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.video.files.PackShot;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SequenceService;
import ru.yandex.canvas.service.video.PackshotServiceInterface;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;
import ru.yandex.canvas.service.video.VideoPresetsService;
import ru.yandex.canvas.steps.ResourceHelpers;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.screenshooter.client.model.ScreenShooterScreenshot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoAdditionsCpmPreviewTest {
    public static final String CORRECT_VAST_XML = "/ru/yandex/canvas/controllers/correctCpmVast.xml";

    public static final String CORRECT_VAST_XML_WITHOUT_SKIPOFFSET = "/ru/yandex/canvas/controllers/correctCpmVastWithoutSkipoffset.xml";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SequenceService sequenceService;

    @MockBean
    private ScreenshooterService screenshooterService;

    @MockBean
    private VideoFileUploadServiceInterface videoFileUploadServiceInterface;

    @MockBean
    private PackshotServiceInterface packshotService;

    private VideoPresetsService videoPresetsService;


    @Autowired
    private DirectService directService;

    @Test
    public void getPreviewTest() throws Exception {
        getPreview(Set.of(FeatureName.CANVAS_AUCTION_RENDER_URL_MACROS.getName()));
    }

    @Test
    public void getPreviewMediafilesMacrosTest() throws Exception {
        getPreview(Set.of(FeatureName.CANVAS_AUCTION_RENDER_URL_MACROS.getName(),
                FeatureName.CANVAS_VIDEO_MEDIAFILES_MACROS.getName()));
    }

    public void getPreview(Set<String> features) throws Exception {
        when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Collections.singletonList(12L));
        when(directService.getFeatures(any(), any())).thenReturn(features);

        String correctVast = ResourceHelpers.getResource(CORRECT_VAST_XML);

        ScreenShooterScreenshot screenshot = new ScreenShooterScreenshot()
                .withUrl("http://example.screen.shot")
                .withIsDone(true);

        when(screenshooterService.getScreenshotFromHtml(anyString(), anyLong(), anyLong()))
                .thenReturn(screenshot);

        PackShot packShot = mock(PackShot.class);
        PackShot.ImageFormat format = mock(PackShot.ImageFormat.class);

        when(format.getUrl()).thenReturn("https://youtube.com/dfjdh/r1.avi");
        when(format.getSize()).thenReturn("optimize");

        when(packShot.getFormats()).thenReturn(Collections.singletonList(format));
        when(packShot.getId()).thenReturn("5c862f9c08db4590cc5c42b8");
        when(packShot.getArchive()).thenReturn(false);

        when(packshotService.lookupPackshot("5c862f9c08db4590cc5c42b8", 1L)).thenReturn(packShot);

        final String[] result = new String[1];

        mockMvc.perform(post("/video/additions/preview")
                .locale(Locale.forLanguageTag("ru"))
                .param("client_id", "1")
                .param("user_id", "2")
                .content("{\"data\":{\"bundle\":{\"name\":\"video-banner_theme_empty\"},\"elements\":["
                        + "{\"type\":\"addition\",\"available\":true,"
                        + "\"options\":{\"video_id\":\"new_0_0-077.mov\",\"audio_id\":null,"
                        + "\"packshot_id\":\"5c862f9c08db4590cc5c42b8\"}},"
                        + "{\"type\":\"button\",\"available\":true,\"options\":"
                        + "{\"color\":\"#FFDC00\",\"text_color\":\"#000000\",\"border_color\":\"#000000\", "
                        + "\"position\":\"right-bottom\", \"custom_label\": \"Узнать больше\"}},"
                        + "{\"type\":\"age\",\"available\":true,"
                        + "\"options\":{\"background_color\":\"#000000\",\"text_color\":\"#ffffff\","
                        + "\"text\":\"12\"}},"
                        + "{\"type\":\"legal\",\"available\":true,"
                        + "\"options\":{\"background_color\":\"#000000\",\"text\":\"Привет мир!\","
                        + "\"text_color\":\"#ffffff\"}}]},\"preset_id\":\"6\",\"previewData\":{\"domain\":\"\","
                        + "\"punyDomain\":\"\",\"url\":\"\",\"warning\":\"\"}}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(e -> result[0] = e.getResponse().getContentAsString())
                //.andExpect( jsonPath("$.vast", equalTo(correctVast) ))
                .andExpect(status().is(200));

        Map<String, String> map = new ObjectMapper().readValue(result[0], Map.class);

        assertThat("Vast is present", map, hasKey("vast"));
        String expected = correctVast.replaceAll("\\s+", "");
        String actual = map.get("vast").replaceAll("\\s+", "");
        assertEquals("vast is correct", expected, actual);
    }

    @Test
    public void getPreviewWithoutSkipoffset() throws Exception {
        when(sequenceService.getNextCreativeIdsList(Mockito.anyInt())).thenReturn(Collections.singletonList(12L));
        when(directService.getFeatures(any(), any())).thenReturn(Set.of(FeatureName.CANVAS_AUCTION_RENDER_URL_MACROS.getName()));

        String correctVast = ResourceHelpers.getResource(CORRECT_VAST_XML_WITHOUT_SKIPOFFSET);

        ScreenShooterScreenshot screenshot = new ScreenShooterScreenshot()
                .withUrl("http://example.screen.shot")
                .withIsDone(true);

        when(screenshooterService.getScreenshotFromHtml(anyString(), anyLong(), anyLong()))
                .thenReturn(screenshot);

        PackShot packShot = mock(PackShot.class);
        PackShot.ImageFormat format = mock(PackShot.ImageFormat.class);

        when(format.getUrl()).thenReturn("https://youtube.com/dfjdh/r1.avi");
        when(format.getSize()).thenReturn("optimize");

        when(packShot.getFormats()).thenReturn(Collections.singletonList(format));
        when(packShot.getId()).thenReturn("5c862f9c08db4590cc5c42b8");
        when(packShot.getArchive()).thenReturn(false);

        when(packshotService.lookupPackshot("5c862f9c08db4590cc5c42b8", 1L)).thenReturn(packShot);

        final String[] result = new String[1];

        mockMvc.perform(post("/video/additions/preview")
                        .locale(Locale.forLanguageTag("ru"))
                        .param("client_id", "1")
                        .param("user_id", "2")
                        .content("{\"data\":{\"bundle\":{\"name\":\"video-banner_theme_empty\"},\"elements\":["
                                + "{\"type\":\"addition\",\"available\":true,"
                                + "\"options\":{\"video_id\":\"new_0_0-077.mov\",\"audio_id\":null,"
                                + "\"packshot_id\":\"5c862f9c08db4590cc5c42b8\"}},"
                                + "{\"type\":\"button\",\"available\":true,\"options\":"
                                + "{\"color\":\"#FFDC00\",\"text_color\":\"#000000\",\"border_color\":\"#000000\", "
                                + "\"position\":\"right-bottom\", \"custom_label\": \"Узнать больше\"}},"
                                + "{\"type\":\"age\",\"available\":true,"
                                + "\"options\":{\"background_color\":\"#000000\",\"text_color\":\"#ffffff\","
                                + "\"text\":\"12\"}},"
                                + "{\"type\":\"legal\",\"available\":true,"
                                + "\"options\":{\"background_color\":\"#000000\",\"text\":\"Привет мир!\","
                                + "\"text_color\":\"#ffffff\"}}]},\"preset_id\":\"8\",\"previewData\":{\"domain\":\"\","
                                + "\"punyDomain\":\"\",\"url\":\"\",\"warning\":\"\"}}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(e -> result[0] = e.getResponse().getContentAsString())
                //.andExpect( jsonPath("$.vast", equalTo(correctVast) ))
                .andExpect(status().is(200));

        Map<String, String> map = new ObjectMapper().readValue(result[0], Map.class);

        assertThat("Vast is present", map, hasKey("vast"));
        String expected = correctVast.replaceAll("\\s+", "");
        String actual = map.get("vast").replaceAll("\\s+", "");
        assertEquals("vast is correct", expected, actual);
    }
}
