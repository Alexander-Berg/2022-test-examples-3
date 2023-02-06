package ru.yandex.canvas.service.video;

import java.util.Random;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.service.DirectService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.canvas.VideoConstants.NEW_LIVE_PREVIEW_URL_FOR_VIDEO_CREATIVES_ENABLED_FEATURE;
import static ru.yandex.canvas.VideoConstants.NEW_SCREENSHOT_URL_FOR_VIDEO_CREATIVES_ENABLED_FEATURE;
import static ru.yandex.canvas.steps.AdditionsSteps.defaultVideoId;
import static ru.yandex.canvas.steps.AdditionsSteps.leastAddition;

@ParametersAreNonnullByDefault
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPreviewUrlBuilderTest {

    @MockBean
    private DirectService directService;

    @Autowired
    private VideoPreviewUrlBuilder previewUrlBuilder;

    private long clientId;

    @Before
    public void setUp() {
        clientId = new Random().nextLong();
    }

    @Test
    public void getPreviewUrl() {
        var addition = leastAddition(clientId, 1000L, defaultVideoId()).setId("batchId");

        Mockito.when(directService.getFeatures(eq(clientId), any()))
                .thenReturn(Set.of(NEW_LIVE_PREVIEW_URL_FOR_VIDEO_CREATIVES_ENABLED_FEATURE));

        var livePreviewUrl = previewUrlBuilder.getPreviewUrl(clientId, addition, false);

        assertThat(livePreviewUrl).isEqualTo("https://direct.web.url/creative-preview/video/batchId");
    }

    @Test
    public void getOldPreviewUrl() {
        var addition = leastAddition(clientId, 1000L, defaultVideoId()).setId("batchId");

        var livePreviewUrl = previewUrlBuilder.getPreviewUrl(clientId, addition, false);

        assertThat(livePreviewUrl).isEqualTo("https://canvas.preview.host/video-additions/batchId/preview");
    }

    @Test
    public void getScreenshotUrl() {
        Mockito.when(directService.getFeatures(eq(clientId), any()))
                .thenReturn(Set.of(NEW_SCREENSHOT_URL_FOR_VIDEO_CREATIVES_ENABLED_FEATURE));

        var livePreviewUrl = previewUrlBuilder.buildScreenshotUrl(clientId, 42L);

        assertThat(livePreviewUrl).isEqualTo("https://direct.web.url/canvas-api/video/creatives/getScreenshot/42");
    }

    @Test
    public void getOldScreenshotUrl() {
        var getScreenshotUrl = previewUrlBuilder.buildScreenshotUrl(clientId, 42L);

        assertThat(getScreenshotUrl).isEqualTo("https://canvas.preview.host/rest/video/creatives/42/getScreenshot");
    }
}
