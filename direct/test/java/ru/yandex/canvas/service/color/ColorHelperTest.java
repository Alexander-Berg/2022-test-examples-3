package ru.yandex.canvas.service.color;

import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.canvas.model.validation.DifferentlyColoredOptions.Metric.WGAG20;

public class ColorHelperTest {

    @Test
    public void blackTest() {
        String borderColorFrom = ColorHelper.getBorderColorFrom("#000000");
        Assert.assertNotNull(borderColorFrom);
        Assert.assertFalse(WGAG20.almostTheSameColors("#000000", borderColorFrom, 2.5f));
    }

    @Test
    public void whiteTest() {

        String borderColorFrom = ColorHelper.getBorderColorFrom("#FFFFFF");
        Assert.assertNotNull(borderColorFrom);
        Assert.assertFalse(WGAG20.almostTheSameColors("#FFFFFF", borderColorFrom, 2.5f));
    }

    @Test
    public void pinkTest() {
        String borderColorFrom = ColorHelper.getBorderColorFrom("#C390D4");
        Assert.assertNotNull(borderColorFrom);
        Assert.assertFalse(WGAG20.almostTheSameColors("#C390D4", borderColorFrom, 2.5f));
    }

    @Test
    public void byLuminanceBlackTest() {
        String adjustedColor = ColorHelper.adjustColorByLuminance("#000000");
        Assert.assertNotEquals(adjustedColor, "#000000");
        Assert.assertTrue(WGAG20Distance.compute(adjustedColor, "#000000") > 1.2d);
        Assert.assertNotEquals(adjustedColor, "#FFFFFF");
    }

    @Test
    public void byLuminanceWhiteTest() {
        String adjustedColor = ColorHelper.adjustColorByLuminance("#FFFFFF");
        Assert.assertNotEquals(adjustedColor, "#000000");
        Assert.assertTrue(WGAG20Distance.compute(adjustedColor, "#000000") > 1.2d);
        Assert.assertNotEquals(adjustedColor, "#FFFFFF");
    }

    @Test
    public void byLuminanceGreyTest() {
        String adjustedColor = ColorHelper.adjustColorByLuminance("#808080");
        Assert.assertNotEquals(adjustedColor, "#000000");
        Assert.assertTrue(WGAG20Distance.compute(adjustedColor, "#000000") > 1.2d);
        Assert.assertNotEquals(adjustedColor, "#FFFFFF");
        Assert.assertNotEquals(adjustedColor, "#080808");
    }
}
