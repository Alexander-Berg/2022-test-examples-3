package ru.yandex.canvas.controllers;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.w3c.dom.Document;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.video.files.AudioSource;
import ru.yandex.canvas.model.video.files.PackShot;
import ru.yandex.canvas.service.video.AudioService;
import ru.yandex.canvas.service.video.PackshotServiceInterface;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasXPath;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AudioVastTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PackshotServiceInterface packshotService;

    @MockBean
    private AudioService audioService;

    @Test
    public void testVAST() throws Exception {
        PackShot packShot = mock(PackShot.class);
        PackShot.ImageFormat format = mock(PackShot.ImageFormat.class);
        when(format.getUrl()).thenReturn("https://ya.ru/image.jpeg");
        when(format.getSize()).thenReturn("optimize");
        when(packShot.getFormats()).thenReturn(Arrays.asList(format));
        when(packShot.getId()).thenReturn("5c862f9c08db4590cc5c42b8");
        when(packShot.getArchive()).thenReturn(false);
        when(packshotService.lookupPackshot("5d0bac9faa09c53767df0295", 1L)).thenReturn(packShot);
        AudioSource audioSource = mock(AudioSource.class);
        when(audioSource.getDuration()).thenReturn(21L);
        when(audioSource.getId()).thenReturn("10029");
        when(audioSource.getStillageUrl()).thenReturn(
                "https://storage.mds.yandex.net/get-bstor/63248/fd057e598542.mpga");
        when(audioService.lookupAudio("5d07a40eaa09c53767ac8827", 1L)).thenReturn(audioSource);
        var result = mockMvc.perform(post("/video/additions/preview")
                .param("client_id", "1")
                .param("user_id", "2")
                .content("{\"data\":{\"bundle\":{\"name\":\"video-banner_theme_empty\"},"
                        + "\"elements\":[{\"type\":\"addition\",\"available\":true,"
                        + "\"options\":{\"audio_id\":\"5d07a40eaa09c53767ac8827\","
                        + "\"packshot_id\":\"5d0bac9faa09c53767df0295\"}}]},"
                        + "\"preset_id\":\"301\",\"previewData\":{\"domain\":\"\","
                        + "\"punyDomain\":\"\",\"url\":\"\",\"warning\":\"\"}}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(200))
                .andReturn().getResponse().getContentAsString();
        Map<String, String> map = new ObjectMapper().readValue(result, Map.class);
        assertThat("Vast is present", map, hasKey("vast"));
        var xml = parse(map.get("vast"));
        assertThat(xml, allOf(
                hasXPath("/VAST/Ad/InLine/AdTitle", equalTo("Interactive Direct In Video")),
                hasXPath("/VAST/Ad/InLine/Creatives/Creative/Linear/Duration", equalTo("00:00:21")),
                hasXPath("/VAST/Ad/InLine/Creatives/Creative/Linear/MediaFiles/MediaFile[1]",
                        containsString("https://yastatic.net/pcode/media/vpaid-creative.js"))
        ));
        assertThat(xml,hasXPath("/VAST/Ad/InLine/Creatives/Creative/Linear/AdParameters",
                containsString("https://ya.ru/image.jpeg")));
        assertThat(xml,hasXPath("/VAST/Ad/InLine/Creatives/Creative/Linear/AdParameters",
                containsString("https://storage.mds.yandex.net/get-bstor/63248/fd057e598542.mpga")));
    }
    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
    }
}
