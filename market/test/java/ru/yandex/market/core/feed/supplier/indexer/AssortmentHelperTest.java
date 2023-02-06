package ru.yandex.market.core.feed.supplier.indexer;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.core.supplier.model.SupplierOffer;

public class AssortmentHelperTest {

    @ParameterizedTest
    @MethodSource("dimensionsParams")
    public void testCreateDImensions(String dimensions, boolean result) {
        Assertions.assertEquals(AssortmentHelper.createDimensions(dimensions).isPresent(), result);
    }

    private static Stream<Arguments> dimensionsParams() {
        return Stream.of(
                Arguments.of("20/12/2", true),
                Arguments.of("2,1/0,1/2", true),
                Arguments.of("1.1/1/2,5;", false)
        );
    }

    @ParameterizedTest
    @MethodSource("weightParams")
    public void testCreateWeight(String dimensions, String result) {
        AssortmentHelper.createWeight(dimensions)
                .ifPresent(bigDecimal ->
                        Assertions.assertEquals(new BigDecimal(result), bigDecimal));
    }

    private static Stream<Arguments> weightParams() {
        return Stream.of(
                Arguments.of("1002", "1.002"),
                Arguments.of("10", "0.010"),
                Arguments.of("1", "0.001")
        );
    }

    @Test
    public void testPopulateParam() {
        SupplierOffer.Builder builder = new SupplierOffer.Builder();
        AssortmentHelper.populateParam(builder, "1", AssortmentHelper.Dimension.HEIGHT, MarketTemplate.OZON_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "2", AssortmentHelper.Dimension.WIDTH, MarketTemplate.OZON_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "3", AssortmentHelper.Dimension.LENGTH, MarketTemplate.OZON_ASSORTMENT);
        Assert.assertEquals("0.3/0.2/0.1", builder.getDimensions());
        builder = new SupplierOffer.Builder();
        AssortmentHelper.populateParam(builder, "1", AssortmentHelper.Dimension.HEIGHT, MarketTemplate.WLB_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "2", AssortmentHelper.Dimension.WIDTH, MarketTemplate.WLB_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "3", AssortmentHelper.Dimension.LENGTH, MarketTemplate.WLB_ASSORTMENT);
        Assert.assertEquals("3/2/1", builder.getDimensions());
        builder = new SupplierOffer.Builder();
        AssortmentHelper.populateParam(builder, "1", AssortmentHelper.Dimension.HEIGHT, MarketTemplate.KUPIVIP_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "2", AssortmentHelper.Dimension.WIDTH, MarketTemplate.KUPIVIP_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "3", AssortmentHelper.Dimension.LENGTH, MarketTemplate.KUPIVIP_ASSORTMENT);
        Assert.assertEquals("_/_/_", builder.getDimensions());
        builder = new SupplierOffer.Builder();
        AssortmentHelper.populateParam(builder, "", AssortmentHelper.Dimension.HEIGHT, MarketTemplate.OZON_ASSORTMENT);
        AssortmentHelper.populateParam(builder, " ", AssortmentHelper.Dimension.WIDTH, MarketTemplate.OZON_ASSORTMENT);
        AssortmentHelper.populateParam(builder, "3", AssortmentHelper.Dimension.LENGTH, MarketTemplate.OZON_ASSORTMENT);
        Assert.assertEquals("0.3/_/_", builder.getDimensions());
    }

    @Test
    public void testCreateCount() {
        Assert.assertEquals(Optional.of(1L), AssortmentHelper.createStocksCount("1"));
        Assert.assertEquals(Optional.empty(), AssortmentHelper.createStocksCount(""));
        Assert.assertEquals(Optional.empty(), AssortmentHelper.createStocksCount("e"));
        Assert.assertEquals(Optional.of(121L), AssortmentHelper.createStocksCount("121"));
        Assert.assertEquals(Optional.empty(), AssortmentHelper.createStocksCount("121.3"));
    }
}
