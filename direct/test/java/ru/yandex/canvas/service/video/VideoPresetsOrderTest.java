package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.junit.Assert.assertEquals;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoPresetsOrderTest {
    @Autowired
    VideoPresetsService videoPresetsService;

    @Test
    public void checkOrder() {
        List<Long> presets = videoPresetsService.getPresetsByCreativeType(VideoCreativeType.TEXT).stream()
                .map(VideoPreset::getId)
                .collect(Collectors.toList());

        assertEquals("Order is correct (as was in python)",  Arrays.asList(1L, 2L, 5L, 4L), presets);
    }
}
