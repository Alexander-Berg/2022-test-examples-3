package ru.yandex.canvas.service;

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
import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.steps.Html5BatchSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.canvas.CommonConstants.NEW_LIVE_PREVIEW_URL_FOR_COMMON_CREATIVES_ENABLED_FEATURE;
import static ru.yandex.canvas.CommonConstants.NEW_SCREENSHOT_URL_FOR_COMMON_CREATIVES_ENABLED_FEATURE;
import static ru.yandex.canvas.Html5Constants.NEW_LIVE_PREVIEW_URL_FOR_HTML5_CREATIVES_ENABLED_FEATURE;
import static ru.yandex.canvas.Html5Constants.NEW_SCREENSHOT_URL_FOR_HTML5_CREATIVES_ENABLED_FEATURE;
import static ru.yandex.canvas.steps.CreativeDocumentSteps.createEmptyCreativeDocument;

@ParametersAreNonnullByDefault
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PreviewUrlBuilderTest {
    @Autowired
    PreviewUrlBuilder previewUrlBuilder;

    @MockBean
    private DirectService directService;

    @Autowired
    Html5BatchSteps steps;

    private long clientId;

    @Before
    public void setUp() {
        clientId = new Random().nextLong();
    }

    @Test
    public void buildOldPreviewUrl() {
        var livePreviewUrl = previewUrlBuilder.buildPreviewUrl(clientId, createEmptyCreativeDocument("testBundle"));

        assertThat(livePreviewUrl).isEqualTo("https://canvas.preview.host/creatives/batchId/0/preview");
    }

    @Test
    public void buildPreviewUrl() {
        Mockito.when(directService.getFeatures(eq(clientId), any()))
                .thenReturn(Set.of(NEW_LIVE_PREVIEW_URL_FOR_COMMON_CREATIVES_ENABLED_FEATURE));

        var livePreviewUrl = previewUrlBuilder.buildPreviewUrl(clientId, createEmptyCreativeDocument("testBundle"));

        assertThat(livePreviewUrl).isEqualTo("https://direct.web.url/creative-preview/image/batchId/0");
    }

    @Test
    public void buildHtml5PreviewUrl() {
        Creative creative = steps.defaultCreative().setBatchId("batch-id").setSource(createSource(12345L));

        Mockito.when(directService.getFeatures(eq(clientId), any()))
                .thenReturn(Set.of(NEW_LIVE_PREVIEW_URL_FOR_HTML5_CREATIVES_ENABLED_FEATURE));

        var livePreviewUrl = previewUrlBuilder.buildHtml5PreviewUrl(clientId, creative);

        assertThat(livePreviewUrl)
                .isEqualTo("https://direct.web.url/creative-preview/html5/batch-id/" + creative.getId());
    }

    @Test
    public void buildOldHtml5PreviewUrl() {
        Creative creative = steps.defaultCreative().setBatchId("batch-id").setSource(createSource(12345L));

        var livePreviewUrl = previewUrlBuilder.buildHtml5PreviewUrl(clientId, creative);

        assertThat(livePreviewUrl)
                .isEqualTo("https://canvas.preview.host/html5/batch-id/" + creative.getId() + "/preview");
    }

    private Source createSource(long fileId) {
        var fileInfo = new StillageFileInfo();
        fileInfo.setId(String.valueOf(fileId));
        return new Source().setStillageInfo(new Source.ZipStillageInfo(fileInfo));
    }

    @Test
    public void buildScreenshotUrl() {
        Mockito.when(directService.getFeatures(eq(clientId), any()))
                .thenReturn(Set.of(NEW_SCREENSHOT_URL_FOR_COMMON_CREATIVES_ENABLED_FEATURE));

        var getScreenshotUrl = previewUrlBuilder.buildScreenshotUrl(clientId,
                createEmptyCreativeDocument("testBundle"));

        assertThat(getScreenshotUrl).isEqualTo("https://direct.web.url/canvas-api/creatives/getScreenshot/0");
    }

    @Test
    public void buildOldScreenshotUrl() {
        var getScreenshotUrl = previewUrlBuilder.buildScreenshotUrl(clientId,
                createEmptyCreativeDocument("testBundle"));

        assertThat(getScreenshotUrl).isEqualTo("https://canvas.preview.host/rest/creatives/0/getScreenshot");
    }

    @Test
    public void buildHtml5ScreenshotUrl() {
        Mockito.when(directService.getFeatures(eq(clientId), any()))
                .thenReturn(Set.of(NEW_SCREENSHOT_URL_FOR_HTML5_CREATIVES_ENABLED_FEATURE));

        var getScreenshotUrl = previewUrlBuilder.buildHtml5ScreenshotUrl(clientId, 42L);

        assertThat(getScreenshotUrl).isEqualTo("https://direct.web.url/canvas-api/html5/creative/getScreenshot/42");
    }

    @Test
    public void buildOldHtml5ScreenshotUrl() {
        var getScreenshotUrl = previewUrlBuilder.buildHtml5ScreenshotUrl(clientId, 42L);

        assertThat(getScreenshotUrl)
                .isEqualTo("https://canvas.preview.host/html5/rest/html5/creative/42/getScreenshot");
    }
}
