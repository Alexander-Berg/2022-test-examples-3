package ru.yandex.market.mbo.recipe.url;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.UrlRecipeFilterSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestUrlRecipeFilterSource {

    @Test
    public void testOptionalEmptyString() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "");
        Assert.assertFalse(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());
        Assert.assertNull(source.getValueIds());
    }

    @Test
    public void testOptionalMalformedString() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "12~14 ,20~20,40");
        Assert.assertFalse(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());
        Assert.assertNull(source.getValueIds());
    }

    @Test
    public void testOptionalMalformedString2() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "12~14, 20~20,40");
        Assert.assertFalse(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());
        Assert.assertNull(source.getValueIds());
    }

    @Test
    public void testOptionalMalformedString3() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            " 12~14");
        Assert.assertFalse(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());
        Assert.assertNull(source.getValueIds());
    }

    @Test
    public void testOptionalSingle() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "10");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());

        Assert.assertEquals(10L, source.getValueIds().iterator().next().longValue());
    }

    @Test
    public void testOptionalRangeSingle() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "96~96");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());

        Assert.assertEquals(96L, source.getValueIds().iterator().next().longValue());
    }

    @Test
    public void testOptionalRange() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
                "111",
                "10~100~13");
        Assert.assertFalse(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());
        Assert.assertNull(source.getValueIds());
    }

    @Test
    public void testOptionalMixedCase1() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "12~14,20~20,40");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());

        List<Long> ids = new ArrayList<>(source.getValueIds());
        Collections.sort(ids);
        Assert.assertEquals(12L, ids.get(0).longValue());
        Assert.assertEquals(13L, ids.get(1).longValue());
        Assert.assertEquals(14L, ids.get(2).longValue());

        Assert.assertEquals(20L, ids.get(3).longValue());

        Assert.assertEquals(40L, ids.get(4).longValue());
    }

    @Test
    public void testOptionalMixedCase2() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "8,12~12,15,20~21,40");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC_ENUM));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());

        List<Long> ids = new ArrayList<>(source.getValueIds());
        Collections.sort(ids);
        Iterator<Long> it = ids.iterator();

        Assert.assertEquals(8L, it.next().longValue());

        Assert.assertEquals(12L, it.next().longValue());

        Assert.assertEquals(15L, it.next().longValue());

        Assert.assertEquals(20L, it.next().longValue());
        Assert.assertEquals(21L, it.next().longValue());

        Assert.assertEquals(40L, it.next().longValue());
    }

    @Test
    public void testNumericEmptyString() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "");
        Assert.assertFalse(source.setParamType(Param.Type.NUMERIC));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getMaxValue());
        Assert.assertNull(source.getMinValue());
        Assert.assertNull(source.getValueIds());
    }

    @Test
    public void testNumericSingleInteger() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "10");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getValueIds());
        Assert.assertEquals(new BigDecimal("10"), source.getMinValue());
        Assert.assertEquals(new BigDecimal("10"), source.getMaxValue());
    }

    @Test
    public void testNumericSingleDecimal() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "10.1");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getValueIds());
        Assert.assertEquals(new BigDecimal("10.1"), source.getMinValue());
        Assert.assertEquals(new BigDecimal("10.1"), source.getMaxValue());
    }

    @Test
    public void testNumericSingleDecimalRange() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "12.1~12.1");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getValueIds());
        Assert.assertEquals(new BigDecimal("12.1"), source.getMinValue());
        Assert.assertEquals(new BigDecimal("12.1"), source.getMaxValue());
    }

    @Test
    public void testNumericSingleMixedRange() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "12~12.1");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getValueIds());
        Assert.assertEquals(new BigDecimal("12"), source.getMinValue());
        Assert.assertEquals(new BigDecimal("12.1"), source.getMaxValue());
    }

    @Test
    public void testNumericMixedCase1() {
        UrlRecipeFilterSource source = new UrlRecipeFilterSource(
            "111",
            "12.2~12.2,15.3,20.4~21,40");
        Assert.assertTrue(source.setParamType(Param.Type.NUMERIC));

        Assert.assertNull(source.getBooleanValue());
        Assert.assertNull(source.getValueIds());
        Assert.assertEquals(new BigDecimal("12.2"), source.getMinValue());
        Assert.assertEquals(new BigDecimal("40"), source.getMaxValue());
    }

}
