package ru.yandex.market.mbo.skubd2;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.service.CategorySkutcher;
import ru.yandex.market.mbo.skubd2.service.dao.OfferInfo;
import ru.yandex.market.mbo.skubd2.service.dao.OfferResult;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

/**
 *
 * <pre>
    [
        Sku{skuId=1752149852, modelId=1718445328, name='SKU test', published=true, publishedOnMarket=true,
            parameterValues=[
                ParameterValue{parameterId=15164148, optionId=15164154},
                ParameterValue{parameterId=14871214, optionId=14896254},
                ParameterValue{parameterId=8440238, optionId=13900604}
            ]
        },
        Sku{skuId=17521498521, modelId=1718445328, name='fake from 1752149852', published=true, publishedOnMarket=true,
            parameterValues=[
                ParameterValue{parameterId=15164148, optionId=15164157},
                ParameterValue{parameterId=14871214, optionId=14896254},
                ParameterValue{parameterId=8440238, optionId=12105602}
            ]
        },
        Sku{skuId=17521498522, modelId=1718445328, name='fake from 1752149852', published=true, publishedOnMarket=true,
            parameterValues=[
                ParameterValue{parameterId=15164148, optionId=15164157},
                ParameterValue{parameterId=14871214, optionId=14896254},
                ParameterValue{parameterId=8440238, optionId=12105604}
            ]
        }
    ]
    </pre>

 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class SkuWithNotMandatoryParamTest {
    private static final Consumer<MboParameters.Category.Builder> NOP = builder -> {};
    private static final long VIDEO_PROCESSOR_ID = 8440238;
    private static final long RAM_VOL_ENUM_ID = 15164148;

    private CategorySkutcher categorySkutcher;

    @Before
    public void setUp() throws IOException {
        String categoryFileName = getClass().getResource("/proto_json/parameters_91491.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_91491.json").getFile();
        categorySkutcher = CategoryEntityUtils.buildCategorySkutcher(categoryFileName, modelFileName, NOP);
    }

    @Test
    public void skutchWithAllMandatoryParam() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
            1718445328,
            ImmutableList.of(
                ParameterValue.newEnumValue(VIDEO_PROCESSOR_ID, 13900604),
                ParameterValue.newEnumValue(RAM_VOL_ENUM_ID, 15164154)
            ),
            "Redmi Note 4 3/32GB gold",
            Collections.emptyList(),
            -1L));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149852, offerResult.getSku().getSkuId());
        Assert.assertEquals("SKU test", offerResult.getSku().getName());
        Assert.assertEquals(3, offerResult.getSku().getParameterValues().size());

        OfferResult noSkuResult = categorySkutcher.skutch(new OfferInfo(
            1718445328,
            ImmutableList.of(ParameterValue.newEnumValue(VIDEO_PROCESSOR_ID, 12105601)),
            "Redmi Note 4 3/32GB gold",
            Collections.emptyList(),
            -1L));
        Assert.assertEquals(SkuBDApi.Status.NO_SKU, noSkuResult.getStatus());
    }

    @Test
    public void skutchWithOneMandatoryParam() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
            1718445328,
            ImmutableList.of(
                ParameterValue.newEnumValue(VIDEO_PROCESSOR_ID, 13900604)
            ),
            "Redmi Note 4 3/32GB gold",
            Collections.emptyList(),
            -1L));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149852, offerResult.getSku().getSkuId());
        Assert.assertEquals("SKU test", offerResult.getSku().getName());
        Assert.assertEquals(3, offerResult.getSku().getParameterValues().size());
    }

    @Test
    public void skutchWithConflictMandatoryParam() {
        OfferResult noSkuResult = categorySkutcher.skutch(new OfferInfo(
            1718445328,
            ImmutableList.of(
                ParameterValue.newEnumValue(RAM_VOL_ENUM_ID, 15164157)
            ),
            "Redmi Note 4 3/32GB gold",
            Collections.emptyList(),
            -1L));
        Assert.assertEquals(SkuBDApi.Status.NO_SKU, noSkuResult.getStatus());
    }
}
