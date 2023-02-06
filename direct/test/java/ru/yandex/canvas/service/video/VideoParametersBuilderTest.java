package ru.yandex.canvas.service.video;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.TimeDelta;
import ru.yandex.canvas.model.video.PythiaParams;
import ru.yandex.canvas.model.video.addition.AdditionElement;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;
import ru.yandex.canvas.model.video.addition.options.BodyElementOptions;
import ru.yandex.canvas.model.video.files.StreamFormat;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.VideoConstants.VIDEO_AD_SYSTEM_DEFAULT;
import static ru.yandex.canvas.steps.PresetDescriptionsSteps.leastPresetDescription;

@RunWith(SpringJUnit4ClassRunner.class)
public class VideoParametersBuilderTest {
    private static final String VPAID_PCODE_URL = "http://ya.ru";
    private static final String THEME = "theme";
    private static final Double DURATION = 13.0;
    private static final String PACKSHOT_URL = "http://packshot.url";
    private static final long SKIP_DELAY = 10;
    private static final long CREATIVE_ID = 123456L;
    private static final String STRM_PREFIX = "strm_prefix";
    private static final String SKIP_URL = "skip_url";
    private static final String VIDEO_META_ID = "8569468391257906944";
    private static final String PLAYER_ID = "testPlayerId";
    private static final String FIRST_FRAME_URL =
            "https://avatars.mdst.yandex.net/get-vh/3244/2a0000017db33d9266da732a2cc25640f359/orig";

    private static final String SIGNATURE_URL =
            "http://s3.mds.yandex.net/vh-canvas-converted/vod-content/8760510266069838910/a0582770-54062ca5-8c73ef46-9714fe3e/signatures/signatures.json";

    private static final String TEXT_COLOR = "textColor";
    private static final String TEXT = "text";

    private static StreamFormat createMediaFile() {
        return new StreamFormat("delivery", 1024, "http://ya.ru", "mpeg", 768, "id", 1234567L, true);
    }

    private static VideoPreset createPreset() {
        PresetDescription description = leastPresetDescription();
        description.setControlsAllowed(true);
        description.setUseTrackingEvents(true);
        description.setInteractiveVpaid(true);
        description.setAddImpressionPixel(true);
        description.setSkipVpaid(true);
        description.setUseVpaidImpressions(true);
        return new VideoPreset(description);
    }

    private static PythiaParams createPythiaParams() {
        return new PythiaParams()
                .setSlug("slug")
                .setBasePath("base_path")
                .setExtra("extra");
    }

    private static List<AdditionElement> createAdditionElements() {
        return List.of(
                new AdditionElement(AdditionElement.ElementType.ADDITION)
                        .withAvailable(true)
                        .withOptions(new AdditionElementOptions().setAudioId("test_audio1").setVideoId("test_video1")),
                new AdditionElement(AdditionElement.ElementType.BODY)
                        .withAvailable(true)
                        .withOptions(new BodyElementOptions().setTextColor(TEXT_COLOR).setText(TEXT))
        );
    }

    @Test
    public void emptyParamsTest() {
        var builder = new VideoParametersBuilder();
        var parameters = builder.build();

        assertFalse(parameters.hasVpaidPcodeUrl());
        assertFalse(parameters.hasTheme());
        assertFalse(parameters.hasDuration());
        assertEquals(0, parameters.getMediaFilesCount());
        assertFalse(parameters.hasPackshot());
        assertFalse(parameters.getHasAbuseButton());
        assertFalse(parameters.getSocialAdvertisement());
        assertFalse(parameters.hasPlaybackParameters());
        assertFalse(parameters.hasPythiaParams());
        assertFalse(parameters.getUseTrackingEvents());
        assertFalse(parameters.getIsStock());
        assertEquals(0, parameters.getAdditionElementsCount());
        assertFalse(parameters.getInteractiveVpaid());
        assertFalse(parameters.getAddPixelImpression());
        assertFalse(parameters.hasCreativeId());
        assertFalse(parameters.hasStrmPrefix());
        assertTrue(parameters.getShowVpaid());
        assertFalse(parameters.getShowVideoClicks());
        assertFalse(parameters.hasSoundbtnLayout());
        assertFalse(parameters.hasAdlabelLayout());
        assertFalse(parameters.hasCountdownLayout());
        assertFalse(parameters.hasIcon());
        assertFalse(parameters.hasSkipUrl());
        assertFalse(parameters.getUseVpaidImpressions());
        assertFalse(parameters.hasAdSystem());
    }

    @Test
    public void videoParametersTest() {
        var dummyMediaFile = createMediaFile();
        var dummyPreset = createPreset();
        var dummyPythiaParams = createPythiaParams();

        var builder = new VideoParametersBuilder();

        builder.setVpaidPcodeUrl(VPAID_PCODE_URL);
        builder.setTheme(THEME);
        builder.setDuration(DURATION);
        builder.setMediaFiles(List.of(dummyMediaFile));
        builder.setPackshotUrl(PACKSHOT_URL);
        builder.fillPresetParameters(dummyPreset);
        builder.setPlaybackParameters(true, new TimeDelta(SKIP_DELAY));
        builder.setPythiaParams(dummyPythiaParams);
        builder.setIsStock(true);
        builder.setElements(createAdditionElements());
        builder.setCreativeId(CREATIVE_ID);
        builder.setStrmPrefix(STRM_PREFIX);
        builder.setShowVideoClicks(true);
        builder.setSkipUrl(SKIP_URL);
        builder.setVideoMetaId(VIDEO_META_ID);
        builder.setPlayerId(PLAYER_ID);
        builder.setHeight(1080);
        builder.setWidth(1920);
        builder.setFirstFrameUrl(FIRST_FRAME_URL);
        builder.setSignaturesParameters(SIGNATURE_URL);

        var parameters = builder.build();

        assertEquals(VPAID_PCODE_URL, parameters.getVpaidPcodeUrl());
        assertEquals(THEME, parameters.getTheme());
        assertEquals(DURATION, parameters.getDuration(), 1e-6);

        assertEquals(1, parameters.getMediaFilesCount());
        assertEquals(dummyMediaFile.getDelivery(), parameters.getMediaFiles(0).getDelivery());
        assertEquals((int) dummyMediaFile.getWidth(), parameters.getMediaFiles(0).getWidth());
        assertEquals((int) dummyMediaFile.getHeight(), parameters.getMediaFiles(0).getHeight());
        assertEquals(dummyMediaFile.getId(), parameters.getMediaFiles(0).getId());
        assertEquals(dummyMediaFile.getUrl(), parameters.getMediaFiles(0).getUrl());
        assertEquals(dummyMediaFile.getMimeType(), parameters.getMediaFiles(0).getMimeType());
        assertEquals((long) dummyMediaFile.getFileSize(), parameters.getMediaFiles(0).getFileSize());
        assertTrue(dummyMediaFile.getHasAudio());

        assertEquals(PACKSHOT_URL, parameters.getPackshot().getImageUrl());
        assertEquals(3.0, parameters.getPackshot().getDuration(), 1e-6);

        assertTrue(parameters.getHasAbuseButton());
        assertFalse(parameters.getSocialAdvertisement());

        assertTrue(parameters.getPlaybackParameters().getShowSkipButton());
        assertEquals(SKIP_DELAY, parameters.getPlaybackParameters().getSkipDelay());

        assertEquals(dummyPythiaParams.getSlug(), parameters.getPythiaParams().getSlug());
        assertEquals(dummyPythiaParams.getBasePath(), parameters.getPythiaParams().getBasePath());
        assertEquals(dummyPythiaParams.getExtra(), parameters.getPythiaParams().getExtra());

        assertTrue(parameters.getUseTrackingEvents());
        assertTrue(parameters.getIsStock());

        assertEquals(1, parameters.getAdditionElementsCount());
        assertEquals("BODY", parameters.getAdditionElements(0).getType());
        assertEquals(TEXT_COLOR, parameters.getAdditionElements(0).getOptions().getTextColor());
        assertEquals(TEXT, parameters.getAdditionElements(0).getOptions().getText());

        assertTrue(parameters.getInteractiveVpaid());
        assertTrue(parameters.getAddPixelImpression());
        assertEquals(CREATIVE_ID, parameters.getCreativeId());
        assertEquals(STRM_PREFIX, parameters.getStrmPrefix());
        assertFalse(parameters.getShowVpaid());
        assertTrue(parameters.getShowVideoClicks());
        assertEquals(1L, parameters.getSoundbtnLayout());
        assertEquals(1L, parameters.getAdlabelLayout());
        assertEquals(1L, parameters.getCountdownLayout());
        assertFalse(parameters.hasIcon());
        assertEquals(SKIP_URL, parameters.getSkipUrl());
        assertTrue(parameters.getUseVpaidImpressions());
        assertEquals(VIDEO_AD_SYSTEM_DEFAULT, parameters.getAdSystem());
        assertEquals(VIDEO_META_ID, parameters.getVideoMetaId());
        assertEquals(PLAYER_ID, parameters.getPlayerId());
        assertEquals(FIRST_FRAME_URL, parameters.getFirstFrameParameters(0).getUrl());
        assertEquals(SIGNATURE_URL, parameters.getSignatures().getSignaturesUrl());
    }
}
