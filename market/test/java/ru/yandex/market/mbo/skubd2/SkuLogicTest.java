package ru.yandex.market.mbo.skubd2;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.http.SkuBDApi.SkutchType;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.service.CategorySkutcher;
import ru.yandex.market.mbo.skubd2.service.dao.OfferInfo;
import ru.yandex.market.mbo.skubd2.service.dao.OfferResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class SkuLogicTest {
    private static final Consumer<MboParameters.Category.Builder> NOP = builder -> {};
    private static final long GPS_PARAM_ID = 4925683;

    private CategorySkutcher categorySkutcher;
    private CategorySkutcher categorySkutcherWithBoolMandatory;

    @Before
    public void setUp() throws IOException {
        String categoryFileName = getClass().getResource("/proto_json/parameters_91491.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_91491.json").getFile();

        categorySkutcher = CategoryEntityUtils.buildCategorySkutcher(categoryFileName, modelFileName, NOP);
        categorySkutcherWithBoolMandatory = CategoryEntityUtils.buildCategorySkutcher(
                categoryFileName,
                modelFileName,
                builder -> builder.getParameterBuilderList().stream()
                        .filter(p -> p.getId() == GPS_PARAM_ID)
                        .forEach(p -> p.setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING))
        );
    }

    @Test
    public void testHid() {
        Assert.assertEquals(91491, categorySkutcher.getHid());
    }

    @Test
    public void simpleSkutch() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                14206711,
                new ArrayList<>(),
                "iPhone 7 plus 128GB красный",
                Collections.emptyList(),
                -1L));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1748279693, offerResult.getSku().getSkuId());
        Assert.assertEquals("iPhone 7 Plus 128Gb red", offerResult.getSku().getName());
        Assert.assertEquals(1, offerResult.getSku().getParameterValues().size());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());
    }

    @Test
    public void simpleSkutchById() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                0,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                1748279693));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(14206711, offerResult.getSku().getModelId());
        Assert.assertEquals("iPhone 7 Plus 128Gb red", offerResult.getSku().getName());
        Assert.assertEquals(1, offerResult.getSku().getParameterValues().size());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResult.getSku().getSkuType());
        Assert.assertEquals(SkutchType.SKUTCH_BY_SKU_ID, offerResult.getSkutchType());
    }

    @Test
    public void simpleSkutchByBarcode() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                -1, "barcode74823"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149844, offerResult.getSku().getSkuId());
        Assert.assertEquals(14206637, offerResult.getSku().getModelId());
        Assert.assertEquals("SKU with barcode74823", offerResult.getSku().getName());
        Assert.assertEquals(1, offerResult.getSku().getParameterValues().size());
        Assert.assertTrue(offerResult.getSku().isPublished());
        Assert.assertFalse(offerResult.getSku().isPublishedOnMarket());
        Assert.assertTrue(offerResult.getSku().hasPublishedOnBlueMarket());
        Assert.assertFalse(offerResult.getSku().isPublishedOnBlueMarket());
        Assert.assertEquals(SkutchType.BARCODE_SKUTCH, offerResult.getSkutchType());
    }

    @Test
    public void skutchByIdAndBarcode() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                0,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                1752149852, "barcode74823"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1718445328, offerResult.getSku().getModelId());
        Assert.assertEquals("SKU test", offerResult.getSku().getName());
        Assert.assertEquals(3, offerResult.getSku().getParameterValues().size());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResult.getSku().getSkuType());
        Assert.assertEquals(SkutchType.SKUTCH_BY_SKU_ID, offerResult.getSkutchType());
        Assert.assertTrue(offerResult.getSku().isPublished());
        Assert.assertTrue(offerResult.getSku().isPublishedOnMarket());
        Assert.assertTrue(offerResult.getSku().hasPublishedOnBlueMarket());
        Assert.assertTrue(offerResult.getSku().isPublishedOnBlueMarket());
    }

    @Test
    public void skutchByBarcodeAndMain() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "Redmi Note 4 3/32GB gold",
                Collections.emptyList(),
                -1L, "BaRcOdE74823"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149844, offerResult.getSku().getSkuId());
        Assert.assertEquals(14206637, offerResult.getSku().getModelId());
        Assert.assertEquals("SKU with barcode74823", offerResult.getSku().getName());
        Assert.assertEquals(SkutchType.BARCODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1, offerResult.getSku().getParameterValues().size());
        Assert.assertTrue(offerResult.getSku().isPublished());
        Assert.assertFalse(offerResult.getSku().isPublishedOnMarket());
        Assert.assertTrue(offerResult.getSku().hasPublishedOnBlueMarket());
        Assert.assertFalse(offerResult.getSku().isPublishedOnBlueMarket());
    }

    @Test
    public void bestBarcodeTestByFrequency() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                14209841,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                -1L, "BaRcOdE74823|barcode33319|BaRcOdE7000"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149848, offerResult.getSku().getSkuId());
        Assert.assertEquals(14209841, offerResult.getSku().getModelId());
        Assert.assertEquals("SKU with barcodes barcode33319 and barcode7000", offerResult.getSku().getName());
        Assert.assertEquals(SkutchType.BARCODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1, offerResult.getSku().getParameterValues().size());
        Assert.assertTrue(offerResult.getSku().isPublished());
        Assert.assertTrue(offerResult.getSku().isPublishedOnMarket());
    }

    @Test
    public void bestBarcodeTestById() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                1718445328,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                -1L, "barcode31287|barcode548"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149851, offerResult.getSku().getSkuId());
        Assert.assertEquals(1718445328, offerResult.getSku().getModelId());
        Assert.assertEquals("SKU with barcode31287", offerResult.getSku().getName());
        Assert.assertEquals(SkutchType.BARCODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(3, offerResult.getSku().getParameterValues().size());
        Assert.assertTrue(offerResult.getSku().isPublished());
        Assert.assertTrue(offerResult.getSku().isPublishedOnMarket());
    }


    @Test
    public void skutchWithBoolMandatory() {
        OfferResult offerResult = categorySkutcherWithBoolMandatory.skutch(new OfferInfo(
                1718445328,
                Collections.singletonList(ParameterValue.newBooleanValue(GPS_PARAM_ID, true, 12972473)),
                "Redmi Note 4 3/32GB gold",
                Collections.emptyList(),
                -1L));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(1752149852, offerResult.getSku().getSkuId());
        Assert.assertEquals("SKU test", offerResult.getSku().getName());
        Assert.assertEquals(4, offerResult.getSku().getParameterValues().size());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());

        OfferResult noSkuResult = categorySkutcherWithBoolMandatory.skutch(new OfferInfo(
                1718445328,
                Collections.emptyList(),
                "Redmi Note 4 3/32GB gold",
                Collections.emptyList(),
                -1L));
        Assert.assertEquals(SkuBDApi.Status.NO_SKU, noSkuResult.getStatus());
        Assert.assertEquals(SkutchType.NO_SKUTCH, noSkuResult.getSkutchType());
    }

    @Test
    public void skutchOfferWithParamValueOfEmptyMandatoryParam() {
        OfferResult offerResult = categorySkutcherWithBoolMandatory.skutch(new OfferInfo(
                92352002,
                Collections.singletonList(ParameterValue.newBooleanValue(GPS_PARAM_ID, true, 12972473)),
                "Redmi Note 4 3/32GB красный",
                Collections.emptyList(),
                -1L));

        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(100283866240L, offerResult.getSku().getSkuId());
        Assert.assertEquals(3, offerResult.getSku().getParameterValues().size());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());
    }

    @Test
    public void skutchOfferWithNoParamValueOfEmptyMandatoryParam() {
        OfferResult offerResult = categorySkutcherWithBoolMandatory.skutch(new OfferInfo(
                92352002,
                new ArrayList<>(),
                "Redmi Note 4 3/32GB красный",
                Collections.emptyList(),
                -1L));

        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(100283866240L, offerResult.getSku().getSkuId());
        Assert.assertEquals(3, offerResult.getSku().getParameterValues().size());
        Assert.assertEquals(SkutchType.SKUTCH_BY_PARAMETERS, offerResult.getSkutchType());
    }

    @Test
    public void testNullablePublishedOnBlueMarket() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                0,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                1748529021));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResult.getSku().getSkuType());
        Assert.assertEquals(SkutchType.SKUTCH_BY_SKU_ID, offerResult.getSkutchType());
        Assert.assertFalse(offerResult.getSku().hasPublishedOnBlueMarket());
    }

    @Test
    public void skutchModelIsSku() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                17521498,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                0));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(SkutchType.SKUTCH_BY_MODEL_ID, offerResult.getSkutchType());
        Assert.assertEquals(17521498, offerResult.getSku().getSkuId());
        Assert.assertEquals(17521498, offerResult.getSku().getModelId());
    }

    @Test
    public void skutchByVendorCode() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                0, "", "tеst 150 987")); // russian letter "е"
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(SkutchType.VENDOR_CODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1752149845, offerResult.getSku().getSkuId());

        OfferResult moreTokensThanInModelResult = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                0, "", "tеst 150 987 bla bla")); // russian letter "е"
        Assert.assertEquals(SkuBDApi.Status.NO_SKU, moreTokensThanInModelResult.getStatus());
        Assert.assertEquals(SkutchType.NO_SKUTCH, moreTokensThanInModelResult.getSkutchType());

        OfferResult nullResult = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                0, "", "bla 150 987"));
        Assert.assertEquals(SkuBDApi.Status.NO_SKU, nullResult.getStatus());
        Assert.assertEquals(SkutchType.NO_SKUTCH, nullResult.getSkutchType());

        OfferResult onlyInTitleVendorCode = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "iphone 7 test 150 987 bla bla ",
                Collections.emptyList(),
                0, "", "bla bla"
        ));

        Assert.assertEquals(SkuBDApi.Status.OK, onlyInTitleVendorCode.getStatus());
        Assert.assertEquals(SkutchType.VENDOR_CODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1752149845, onlyInTitleVendorCode.getSku().getSkuId());


        ArrayList<Offer.YmlParam> ymlList = new ArrayList<>();
        ymlList.add(Offer.YmlParam.newBuilder().setName("name").setValue("iphone 7 test 150 987 bla bla").build());
        OfferResult onlyInParamsVendorCode = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "iphone 7 test 150 987 bla bla ",
                ymlList,
                0, "", "bla bla"
        ));

        Assert.assertEquals(SkuBDApi.Status.OK, onlyInParamsVendorCode.getStatus());
        Assert.assertEquals(SkutchType.VENDOR_CODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1752149845, onlyInParamsVendorCode.getSku().getSkuId());

        OfferResult inParamsAndTitleSameVendorCode = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "iphone 7 test 150 987 bla bla ",
                ymlList,
                0, "", "bla bla"
        ));

        Assert.assertEquals(SkuBDApi.Status.OK, inParamsAndTitleSameVendorCode.getStatus());
        Assert.assertEquals(SkutchType.VENDOR_CODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1752149845, inParamsAndTitleSameVendorCode.getSku().getSkuId());

        OfferResult inParamsAndTitleDifferentVendorCode = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "iphone 7 art 150 987 bla bla ",
                ymlList,
                0, "", "bla bla"
        ));

        Assert.assertEquals(SkuBDApi.Status.NO_SKU, inParamsAndTitleDifferentVendorCode.getStatus());
        Assert.assertEquals(SkutchType.NO_SKUTCH, inParamsAndTitleDifferentVendorCode.getSkutchType());

        OfferResult vendorCodeWinsAgainstTitle = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "iphone 7 art 150 987 bla bla ",
                Collections.emptyList(),
                0, "", "test 150 987"
        ));

        Assert.assertEquals(SkuBDApi.Status.OK, vendorCodeWinsAgainstTitle.getStatus());
        Assert.assertEquals(SkutchType.VENDOR_CODE_SKUTCH, offerResult.getSkutchType());
        Assert.assertEquals(1752149845, vendorCodeWinsAgainstTitle.getSku().getSkuId());
    }

    @Test
    public void skutchPartnerSku() {
        OfferResult offerResult = categorySkutcher.skutch(new OfferInfo(
                0,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                1752149847, "", ""));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResult.getStatus());
        Assert.assertEquals(SkutchType.SKUTCH_BY_SKU_ID, offerResult.getSkutchType());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.PARTNER_SKU, offerResult.getSku().getSkuType());
        Assert.assertEquals(1752149847, offerResult.getSku().getSkuId());
    }

    @Test
    public void skutchByAlias() {
        OfferResult offerResultByTitle = categorySkutcher.skutch(new OfferInfo(
                14206711,
                new ArrayList<>(),
                "какой-то айфон мама мыла раму",
                Collections.emptyList(),
                0, "", ""));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResultByTitle.getStatus());
        Assert.assertEquals(SkutchType.SKUTCH_BY_ALIAS, offerResultByTitle.getSkutchType());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResultByTitle.getSku().getSkuType());
        Assert.assertEquals(174827346345L, offerResultByTitle.getSku().getSkuId());
        Assert.assertEquals("iPhone 7 Plus 128Gb red мама мыла раму", offerResultByTitle.getSku().getName());

        OfferResult offerResultByParam = categorySkutcher.skutch(new OfferInfo(
                14206711,
                new ArrayList<>(),
                "какой-то айфон",
                Collections.singletonList(Offer.YmlParam.newBuilder()
                        .setName("мама")
                        .setValue("мыла")
                        .setUnit("раму")
                        .build()),
                0, "", ""));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResultByParam.getStatus());
        Assert.assertEquals(SkutchType.SKUTCH_BY_ALIAS, offerResultByParam.getSkutchType());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResultByParam.getSku().getSkuType());
        Assert.assertEquals(174827346345L, offerResultByParam.getSku().getSkuId());
        Assert.assertEquals("iPhone 7 Plus 128Gb red мама мыла раму", offerResultByParam.getSku().getName());

        OfferResult offerResultByDescription = categorySkutcher.skutch(new OfferInfo(
                14206711,
                new ArrayList<>(),
                "какой-то айфон",
                Collections.emptyList(),
                0, "", "", "мама мыла раму"));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResultByDescription.getStatus());
        Assert.assertEquals(SkutchType.SKUTCH_BY_ALIAS, offerResultByDescription.getSkutchType());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResultByDescription.getSku().getSkuType());
        Assert.assertEquals(174827346345L, offerResultByDescription.getSku().getSkuId());
        Assert.assertEquals("iPhone 7 Plus 128Gb red мама мыла раму", offerResultByDescription.getSku().getName());
    }

    @Test
    public void skutchMatchingDisabledSku() {
        OfferResult offerResultById = categorySkutcher.skutch(new OfferInfo(
                0,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                17521498444L, "", ""));
        Assert.assertEquals(SkuBDApi.Status.OK, offerResultById.getStatus());
        Assert.assertEquals(SkutchType.SKUTCH_BY_SKU_ID, offerResultById.getSkutchType());
        Assert.assertEquals(SkuBDApi.SkuOffer.SkuType.SKU, offerResultById.getSku().getSkuType());
        Assert.assertEquals(17521498444L, offerResultById.getSku().getSkuId());
        Assert.assertEquals(0, offerResultById.getSku().getParameterValues().size());

        OfferResult offerResultByBarcode = categorySkutcher.skutch(new OfferInfo(
                14206637,
                new ArrayList<>(),
                "",
                Collections.emptyList(),
                -1L, "BaRcOdE17521498444"));
        Assert.assertEquals(SkuBDApi.Status.NO_SKU, offerResultByBarcode.getStatus());

    }
}
