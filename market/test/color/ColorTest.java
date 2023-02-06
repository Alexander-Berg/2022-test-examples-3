package ru.yandex.market.jmf.attributes.test.color;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.attributes.color.Color;

public class ColorTest {

    @Test
    public void checkRgba() {
        Color color = new Color("#01234567");

        Assertions.assertEquals("#01234567", color.toString());
    }

    @Test
    public void checkWrongRgba() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Color("#012345678"));
    }
}
