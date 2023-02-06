package ru.yandex.canvas.service.video;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;

import static org.junit.Assert.assertFalse;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPresetsToCreativeTypeTest {
    @Autowired
    VideoPresetsService videoPresetsService;

    @Test
    public void checkPresetMap() {
        List<VideoCreativeType> creativeTypes = videoPresetsService.getPresetIds()
                .stream()
                .map(e -> {
                    try {
                        return videoPresetsService.fromPresetId(e);
                    } catch (IllegalArgumentException ex) {
                        return VideoCreativeType.UNKNOWN;
                    }
                })
                .collect(Collectors.toList());

        assertFalse(creativeTypes.contains(VideoCreativeType.UNKNOWN));
    }
}
