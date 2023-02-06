package ru.yandex.calendar.util.color;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class ColorTest {

    @Test
    public void rgbToRgbaStr() {
        rgbToRgbaStr("#ff0000ff", new java.awt.Color(255, 0, 0));
        rgbToRgbaStr("#00ff00ff", new java.awt.Color(0, 255, 0));
        rgbToRgbaStr("#0000ffff", new java.awt.Color(0, 0, 255));
        rgbToRgbaStr("#ffffffff", new java.awt.Color(255, 255, 255));
        rgbToRgbaStr("#7f7f7fff", new java.awt.Color(127, 127, 127));
    }

    private void rgbToRgbaStr(String colorStr, java.awt.Color color) {
        Assert.A.equals(colorStr.toLowerCase(), Color.fromRgb(color.getRGB() & 0xffffff).printRgba().toLowerCase());
    }

    @Test
    public void rgbaStrToRgb() {
        rgbaStrToRgb(new java.awt.Color(255, 0, 0), "#ff0000ff");
        rgbaStrToRgb(new java.awt.Color(0, 255, 0), "#00ff00ff");
        rgbaStrToRgb(new java.awt.Color(0, 0, 255), "#0000ffff");
        rgbaStrToRgb(new java.awt.Color(0, 0, 255), "#0000ffff");
        rgbaStrToRgb(new java.awt.Color(255, 255, 255), "#ffffffff");
        rgbaStrToRgb(new java.awt.Color(202, 254, 186), "#cafebabe");
    }

    private void rgbaStrToRgb(java.awt.Color color, String colorStr) {
        Assert.A.equals(color.getRGB() & 0xffffff, Color.parseRgba(colorStr).getRgb());
    }

}
