package ru.yandex.canvas.controllers.video;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.service.video.Geometry;
import ru.yandex.canvas.service.video.VideoCreativeType;
import ru.yandex.canvas.service.video.VideoPresetsService;

import static org.assertj.core.api.Assertions.assertThat;

@CanvasTest
@RunWith(Parameterized.class)
public class VideoDefaultControllerTest {

    @Autowired
    private VideoPresetsService videoPresetsService;

    @Parameterized.Parameter
    public Long presetId;

    @Parameterized.Parameter(1)
    public Geometry expectedGeometry;

    @Parameterized.Parameter(2)
    public VideoCreativeType expectedType;

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{
                {
                        VideoDefaultController.CPC_DEFAULT_WIDE_PRESET_ID,
                        Geometry.UNIVERSAL,
                        VideoCreativeType.TEXT,
                },
                {
                        VideoDefaultController.CPC_DEFAULT_SQUARE_PRESET_ID,
                        Geometry.SQUARE,
                        VideoCreativeType.TEXT,
                },
                {
                        VideoDefaultController.CPC_DEFAULT_TALL_PRESET_ID,
                        Geometry.TALL,
                        VideoCreativeType.TEXT,
                },
                {
                        VideoDefaultController.MOBILE_CONTENT_DEFAULT_WIDE_PRESET_ID,
                        Geometry.UNIVERSAL,
                        VideoCreativeType.MOBILE_CONTENT,
                },
                {
                        VideoDefaultController.MOBILE_CONTENT_DEFAULT_SQUARE_PRESET_ID,
                        Geometry.SQUARE,
                        VideoCreativeType.MOBILE_CONTENT,
                },
                {
                        VideoDefaultController.MOBILE_CONTENT_DEFAULT_TALL_PRESET_ID,
                        Geometry.TALL,
                        VideoCreativeType.MOBILE_CONTENT,
                },
        };
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void testPresets() {
        var preset = videoPresetsService.getPreset(presetId);
        assertThat(preset).isNotNull();
        assertThat(preset.getDescription().getGeometry()).isEqualTo(expectedGeometry);
        assertThat(preset.videoCreativeType()).isEqualTo(expectedType);
    }
}
