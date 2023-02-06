package ru.yandex.market.mbo.skubd2;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.service.CategorySkutcher;
import ru.yandex.market.mbo.skubd2.service.dao.OfferInfo;
import ru.yandex.market.mbo.skubd2.service.dao.OfferResult;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

public class SkuSizesParamTest {
    private static final Consumer<MboParameters.Category.Builder> NOP = builder -> {};

    private CategorySkutcher categorySkutcher;

    @Before
    public void setUp() throws IOException {
        String categoryFileName = getClass().getResource("/proto_json/parameters_7812074.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_7812074.json").getFile();
        categorySkutcher = CategoryEntityUtils.buildCategorySkutcher(categoryFileName, modelFileName, NOP);
    }

    @Test
    public void simpleSkutchSize() {
        // value name is "24"
        OfferResult skutch = categorySkutcher.skutch(new OfferInfo(
            1781987789L,
            ImmutableList.of(
                ParameterValue.newEnumValue(14474354, 14563845),
                ParameterValue.newEnumValue(14474354, 14563663)
            ),
            "",
            Collections.emptyList(),
            -1L));
        Assert.assertEquals(100184765477L, skutch.getSku().getSkuId());
    }

    @Test
    public void skutchRangeSizes() {
        // value name is "24-28"
        OfferResult skutch = categorySkutcher.skutch(new OfferInfo(
            1781987789L,
            ImmutableList.of(
                ParameterValue.newEnumValue(14474354, 14563845),
                ParameterValue.newEnumValue(14474354, 14563852),
                ParameterValue.newEnumValue(14474354, 14563859),

                ParameterValue.newEnumValue(14474354, 14563663)
            ),
            "",
            Collections.emptyList(),
            -1L));
        Assert.assertEquals(100184765497L, skutch.getSku().getSkuId());
    }
}
