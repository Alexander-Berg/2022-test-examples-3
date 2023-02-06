package ru.yandex.market.mbo.skubd2;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.http.SkuBDApi.SkutchType;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.service.CategorySkutcher;
import ru.yandex.market.mbo.skubd2.service.dao.OfferInfo;
import ru.yandex.market.mbo.skubd2.service.dao.OfferResult;

public class SkuParamValidationLogicTest {
    public static final int PACKS_NUMBER_PARAM_ID = 15772658;
    public static final int VOLUME_PARAM_ID = 15772198;
    private static final Consumer<MboParameters.Category.Builder> NOP = builder -> {
    };
    private static final long GPS_PARAM_ID = 15726400;
    private CategorySkutcher categorySkutcher;
    private CategorySkutcher categorySkutcherWithBoolMandatory;

    @Before
    public void setUp() throws IOException {

        String categoryFileName = getClass().getResource("/proto_json/parameters_15726400.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_15726400.json").getFile();

        categorySkutcher = CategoryEntityUtils.buildCategorySkutcher(categoryFileName, modelFileName, NOP);
        categorySkutcherWithBoolMandatory = CategoryEntityUtils.buildCategorySkutcher(
                categoryFileName,
                modelFileName,
                builder -> builder.getParameterBuilderList().stream()
                        .filter(p -> p.getId() == GPS_PARAM_ID)
                        .forEach(p -> p.setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING))
        );
    }


    // barcode in offer matches with barcode in sku, but packs number param is different
    @Test
    public void skutchByBarcodeRejectedByParam() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                155722677,
                List.of(
                        ParameterValue.newNumericValue(PACKS_NUMBER_PARAM_ID, 2),
                        ParameterValue.newNumericValue(VOLUME_PARAM_ID, 0.25)
                ),
                "",
                Collections.emptyList(),
                -1, "7613036568340"));
        Assert.assertEquals(SkuBDApi.Status.REJECT_BY_PARAMETERS, offerResult.getStatus());
        Assert.assertEquals(100659678844L, offerResult.getSku().getSkuId());
        Assert.assertEquals(155722677, offerResult.getSku().getModelId());
        Assert.assertEquals(SkutchType.BARCODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(PACKS_NUMBER_PARAM_ID, offerResult.getParamDiffs().get(0).getParamId());
    }

    // barcode match rejected, but then found sku by params
    @Test
    public void skutchByBarcodeRejectedByParamAndOkByParam() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                155722677,
                List.of(
                        ParameterValue.newNumericValue(PACKS_NUMBER_PARAM_ID, 6),
                        ParameterValue.newNumericValue(VOLUME_PARAM_ID, 0.25)
                ),
                "",
                Collections.emptyList(),
                -1, "7613036568340"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(100408612125L, offerResult.getSku().getSkuId());
        Assert.assertEquals(155722677, offerResult.getSku().getModelId());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());
        Assert.assertTrue(offerResult.getParamDiffs().isEmpty());
    }

    // offer does not have quantity parameter, but sku has. Check default quantity logic
    @Test
    public void skutchByParametersWithDefaultQuantityInOffer() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                155722677,
                List.of(
                        ParameterValue.newNumericValue(VOLUME_PARAM_ID, 0.25)
                ),
                "",
                Collections.emptyList(),
                -1, ""));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(100408612124L, offerResult.getSku().getSkuId());
        Assert.assertEquals(155722677, offerResult.getSku().getModelId());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());
        Assert.assertTrue(offerResult.getParamDiffs().isEmpty());
    }


    // Offer has quantity parameter, but sku doesn't
    @Test
    public void skutchByParametersWithDefaultQuantityInSku() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                157247092,
                List.of(
                        ParameterValue.newNumericValue(VOLUME_PARAM_ID, 0.75),
                        ParameterValue.newNumericValue(PACKS_NUMBER_PARAM_ID, 1)
                ),
                "",
                Collections.emptyList(),
                -1, ""));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(100462903578L, offerResult.getSku().getSkuId());
        Assert.assertEquals(157247092, offerResult.getSku().getModelId());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());
    }

    @Test
    public void skutchByBarcodeAndParamsBarcoceFirst() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                155722677,
                List.of(
                        ParameterValue.newNumericValue(PACKS_NUMBER_PARAM_ID, 10),
                        ParameterValue.newNumericValue(VOLUME_PARAM_ID, 0.25)
                ),
                "",
                Collections.emptyList(),
                -1, "7613036568340"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(100659678844L, offerResult.getSku().getSkuId());
        Assert.assertEquals(155722677, offerResult.getSku().getModelId());
        Assert.assertEquals(SkutchType.BARCODE_SKUTCH, offerResult.getSkutchType());
    }
}
