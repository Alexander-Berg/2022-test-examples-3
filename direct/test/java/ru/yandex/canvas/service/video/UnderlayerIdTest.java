package ru.yandex.canvas.service.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnderlayerIdTest {
    @Test
    public void checkUnderlayer() {
        assertEquals(456476412L, VideoCreativesService.calculateUnderlayerId("59d734d1a980ee031ba6cc62"));
        assertEquals(35432912L, VideoCreativesService.calculateUnderlayerId("5bfe264e76651d1aee09f4e9"));
        assertEquals(1652201289L, VideoCreativesService.calculateUnderlayerId("5c7788558d9690b70b81a825"));
        assertEquals(0L, VideoCreativesService.calculateUnderlayerId(""));
    }

}
