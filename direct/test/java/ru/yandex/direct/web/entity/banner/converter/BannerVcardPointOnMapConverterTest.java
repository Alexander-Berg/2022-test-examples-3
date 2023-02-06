package ru.yandex.direct.web.entity.banner.converter;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import org.junit.Test;

import ru.yandex.direct.core.entity.vcard.model.PointOnMap;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class BannerVcardPointOnMapConverterTest {

    private static final String VALID_POINT = "37.617530,55.755446   ";
    private static final String VALID_BOUNDS = " 37.614069, 55.752683  ,37.622280,55.757313";

    @Test
    public void toCorePointOnMap_PointAndBoundsBlank_ReturnsNull() {
        assertThat(convert(null, "   ")).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCorePointOnMap_Point_Null_ThrowsException() {
        convertWithPoint(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCorePointOnMap_Bounds_Blank_ThrowsException() {
        convertWithBounds(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCorePointOnMap_Point_NotEnoughNumbers_ThrowsException() {
        convertWithPoint("37.617530");
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCorePointOnMap_Point_TooManyNumbers_ThrowsException() {
        convertWithPoint("37.617530,37.617530,37.617530");
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCorePointOnMap_Bounds_NotEnoughNumbers_ThrowsException() {
        convertWithBounds("37.614069,55.752683,37.622280");
    }

    @Test(expected = IllegalArgumentException.class)
    public void toCorePointOnMap_Bounds_TooManyNumbers_ThrowsException() {
        convertWithBounds("37.614069,55.752683,37.622280,37.622280,37.622280");
    }

    @Test(expected = NumberFormatException.class)
    public void toCorePointOnMap_Point_InvalidNumber_ThrowsException() {
        convertWithPoint("37.614069,50.YTYTYT");
    }

    @Test(expected = NumberFormatException.class)
    public void toCorePointOnMap_Bounds_InvalidNumber_ThrowsException() {
        convertWithBounds("37.614069,55.752683,37.622280,37.ABABAB");
    }

    @Test
    public void toCorePointOnMap_ValidInput_Converted() {
        org.junit.Assert.assertThat(convert(VALID_POINT, VALID_BOUNDS), beanDiffer(new PointOnMap()
                .withX(new BigDecimal("37.617530")).withY(new BigDecimal("55.755446"))
                .withX1(new BigDecimal("37.614069")).withY1(new BigDecimal("55.752683"))
                .withX2(new BigDecimal("37.622280")).withY2(new BigDecimal("55.757313"))));
    }

    private static void convertWithPoint(@Nullable String point) {
        convert(point, VALID_BOUNDS);
    }

    private static void convertWithBounds(@Nullable String bounds) {
        convert(VALID_POINT, bounds);
    }

    private static PointOnMap convert(@Nullable String point, @Nullable String bounds) {
        return BannerVcardPointOnMapConverter.toCorePointOnMap(point, bounds);
    }

}
