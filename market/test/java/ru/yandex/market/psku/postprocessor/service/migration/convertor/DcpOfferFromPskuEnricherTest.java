package ru.yandex.market.psku.postprocessor.service.migration.convertor;

import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.MigrationUtils;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMockBuilder;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration.common.util.StringUtil;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ShopBusinessDao;
import ru.yandex.market.psku.postprocessor.service.migration.DcpOfferFromPskuEnricher;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class DcpOfferFromPskuEnricherTest extends BaseDBTest {
    private final long SHOP_ID = 111L;
    private final long OTHER_SHOP_ID = 1111L;
    private final long BUSINESS_ID = 222L;
    private final long PARAM_ID = 12L;

    private final DataCampContentMarketParameterValue.MarketParameterValue.Builder TEST_PARAM =
        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
            .setParamId(PARAM_ID)
            .setValueSource(DataCampContentMarketParameterValue.MarketValueSource.CONTENT_EXCEL)
            .setValue(
                DataCampContentMarketParameterValue.MarketValue.newBuilder()
                    .setStrValue("old")
                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
            );

    private final long SERVICE_PARAM_ID = 12L;
    private final long MULTI_VALUE_PARAM_ID = 11L;
    private final long HID = 13L;

    private final String SHOP_SKU_1 = "some_sku_1";
    private final String SHOP_SKU_2 = "some_sku_2";

    private final String PICTURE_URL_1 = "//avatars.mds.yandex.net/get-mpic/3699263/img_id107390113502556654.jpeg/orig1";
    private final String PICTURE_URL_2 = "//avatars.mds.yandex.net/get-mpic/3699263/img_id107390113502556654.jpeg/orig2";

    private DcpOfferFromPskuEnricher enricher;
    private ModelStorageHelper modelStorageHelper;
    private ModelStorageServiceMock modelStorageServiceMock;
    private CategoryDataKnowledge categoryDataKnowledgeMock;
    @Autowired
    private ShopBusinessDao shopBusinessDao;

    @Before
    public void init() {
        categoryDataKnowledgeMock = CategoryDataKnowledgeMockBuilder
                .builder()
                .startCategory(HID)
                .enumParameterBuilder()
                .setParamId(ParameterValueComposer.VENDOR_ID)
                .setXlsName(ParameterValueComposer.VENDOR)
                .build()
                .stringParameterBuilder(PARAM_ID, String.valueOf(PARAM_ID))
                .build()
                .enumParameterBuilder()
                .setParamId(MULTI_VALUE_PARAM_ID)
                .setXlsName(String.valueOf(MULTI_VALUE_PARAM_ID))
                .setMultivalue(true)
                .build()
                .numericParameterBuilder()
                .setParamId(SERVICE_PARAM_ID)
                .setXlsName(String.valueOf(SERVICE_PARAM_ID))
                .build()
                .build()
                .build();
        BookCategoryHelper bookCategoryHelper = mock(BookCategoryHelper.class);
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledgeMock, bookCategoryHelper);

        modelStorageServiceMock = Mockito.spy(new ModelStorageServiceMock());
        modelStorageHelper = Mockito.spy(new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock));
        SkuRatingEvaluator skuRatingEvaluator = new DefaultRatingEvaluator(categoryDataKnowledgeMock);
        enricher = new DcpOfferFromPskuEnricher(categoryDataHelper, modelStorageHelper, skuRatingEvaluator);

        shopBusinessDao.saveShopBusiness(SHOP_ID, BUSINESS_ID);
    }

    @Test
    public void testUpdateParameter() {
        //check that new business for known shop_id is ignored
        shopBusinessDao.saveShopBusiness(SHOP_ID, 333);

        DataCampOffer.Offer.Builder offerBuilder = makeOffer(SHOP_SKU_1, 1);
        parameterValues(offerBuilder).addParameterValues(TEST_PARAM);
        ModelStorage.Model psku = ModelBuilder.newBuilder(1, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SHOP_ID)
                .withSkuParentRelation(2, 2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue("new")
                                .build()
                        ).build()
                ).build();

        ModelStorage.Model pmodel = ModelBuilder.newBuilder(2, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .withSkuRelations(2, 1)
                .build();

        modelStorageServiceMock.putModels(psku, pmodel);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(offerBuilder)
                .build();
        DataCampUnitedOffer.UnitedOffer result = enricher.enrichOffers(Collections.singletonList(unitedOffer)).get(0);

        assertEquals(1, parameterValues(result.getBasic().toBuilder()).getParameterValuesCount());
    }

    @Test
    public void testUpdateParameterWithMultivalue() {
        DataCampOffer.Offer.Builder offerBuilder = makeOffer(SHOP_SKU_1, 1);
        parameterValues(offerBuilder).addParameterValues(TEST_PARAM);


        ModelStorage.Model psku = ModelBuilder.newBuilder(1, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SHOP_ID)
                .withSkuParentRelation(HID, 2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue("new")
                                .build()
                        ).build()

                )
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MULTI_VALUE_PARAM_ID)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setOptionId(1)
                        .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MigrationUtils.SHOP_SKU_PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(SHOP_SKU_1)
                                .build())
                        .build())
                .build();

        ModelStorage.Model pmodel = ModelBuilder.newBuilder(2, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .withSkuRelations(HID, 1)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MULTI_VALUE_PARAM_ID)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setOptionId(1)
                        .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MULTI_VALUE_PARAM_ID)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setOptionId(2)
                        .build())
                .build();

        modelStorageServiceMock.putModels(psku, pmodel);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(offerBuilder)
                .build();
        DataCampUnitedOffer.UnitedOffer result = enricher.enrichOffers(Collections.singletonList(unitedOffer)).get(0);

        assertEquals(3, parameterValues(result.getBasic().toBuilder()).getParameterValuesCount());
    }

    @Test
    public void testOffersWithDuplicateMapping() {
        DataCampOffer.Offer.Builder offer1Builder = makeOffer(SHOP_SKU_1, 1);
        DataCampOffer.Offer.Builder offer2Builder = makeOffer(SHOP_SKU_2, 1);

        parameterValues(offer1Builder).addParameterValues(TEST_PARAM);
        parameterValues(offer2Builder).addParameterValues(TEST_PARAM);


        ModelStorage.Model psku = ModelBuilder.newBuilder(1, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SHOP_ID)
                .withSkuParentRelation(HID, 2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue("new")
                                .build()
                        ).build()

                )
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MULTI_VALUE_PARAM_ID)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setOptionId(1)
                        .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MigrationUtils.SHOP_SKU_PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(SHOP_SKU_1)
                                .build())
                        .build())
                .build();

        ModelStorage.Model pmodel = ModelBuilder.newBuilder(2, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .withSkuRelations(HID, 1)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MULTI_VALUE_PARAM_ID)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setOptionId(1)
                        .build())
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MULTI_VALUE_PARAM_ID)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .setOptionId(2)
                        .build())
                .build();

        modelStorageServiceMock.putModels(psku, pmodel);

        DataCampUnitedOffer.UnitedOffer unitedOffer1 = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(offer1Builder)
                .build();
        DataCampUnitedOffer.UnitedOffer unitedOffer2 = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(offer2Builder)
                .build();
        List<DataCampUnitedOffer.UnitedOffer> result = enricher.enrichOffers(
                ImmutableList.of(unitedOffer1, unitedOffer2)
        );

        // offer1 should be enriched with model params:
        assertEquals(3, parameterValues(result.get(0).getBasic().toBuilder()).getParameterValuesCount());
        // but offer2 has different shop_sku, so it should be left with initial 1 param:
        assertEquals(1, parameterValues(result.get(1).getBasic().toBuilder()).getParameterValuesCount());
    }

    @Test
    public void testSkuSupplierIdUsedAsBizId() {
        int pskuBizId = 10001;
        int otherBizId = 10002;
        DataCampOffer.Offer.Builder offer1Builder = makeOffer(SHOP_SKU_1, 1, pskuBizId);
        DataCampOffer.Offer.Builder offer2Builder = makeOffer(SHOP_SKU_2, 1, otherBizId);
        parameterValues(offer1Builder).addParameterValues(TEST_PARAM);
        parameterValues(offer2Builder).addParameterValues(TEST_PARAM);

        ModelStorage.Model psku = ModelBuilder.newBuilder(1, HID)
            .currentType(ModelStorage.ModelType.SKU)
            .source(ModelStorage.ModelType.PARTNER_SKU.name())
            .supplierId(pskuBizId)
            .withSkuParentRelation(HID, 2)
            .parameterValue(ModelStorage.ParameterValue.newBuilder()
                .setParamId(PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .addStrValue(ModelStorage.LocalizedString.newBuilder()
                    .setValue("new")
                    .build()
                ).build()
            )
            .parameterValue(ModelStorage.ParameterValue.newBuilder()
                .setParamId(MULTI_VALUE_PARAM_ID)
                .setValueType(MboParameters.ValueType.ENUM)
                .setOptionId(1)
                .build())
            .parameterValue(ModelStorage.ParameterValue.newBuilder()
                .setParamId(MigrationUtils.SHOP_SKU_PARAM_ID)
                .setValueType(MboParameters.ValueType.STRING)
                .addStrValue(ModelStorage.LocalizedString.newBuilder()
                    .setValue(SHOP_SKU_1)
                    .build())
                .build())
            .build();

        ModelStorage.Model pmodel = ModelBuilder.newBuilder(2, HID)
            .currentType(ModelStorage.ModelType.GURU)
            .withSkuRelations(HID, 1)
            .parameterValue(ModelStorage.ParameterValue.newBuilder()
                .setParamId(MULTI_VALUE_PARAM_ID)
                .setValueType(MboParameters.ValueType.ENUM)
                .setOptionId(1)
                .build())
            .parameterValue(ModelStorage.ParameterValue.newBuilder()
                .setParamId(MULTI_VALUE_PARAM_ID)
                .setValueType(MboParameters.ValueType.ENUM)
                .setOptionId(2)
                .build())
            .build();

        modelStorageServiceMock.putModels(psku, pmodel);

        DataCampUnitedOffer.UnitedOffer unitedOffer1 = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offer1Builder)
            .build();
        DataCampUnitedOffer.UnitedOffer unitedOffer2 = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(offer2Builder)
            .build();
        List<DataCampUnitedOffer.UnitedOffer> result = enricher.enrichOffers(
            ImmutableList.of(unitedOffer1, unitedOffer2)
        );

        // offer1 should be enriched with model params:
        assertEquals(3, parameterValues(result.get(0).getBasic().toBuilder()).getParameterValuesCount());
        // but offer2 has different shop_sku, so it should be left with initial 1 param:
        assertEquals(1, parameterValues(result.get(1).getBasic().toBuilder()).getParameterValuesCount());
    }

    @Test
    public void testUpdatePicturesAndRating() {
        DataCampOffer.Offer.Builder offerBuilder = makeOffer(SHOP_SKU_1, 1);
        parameterValues(offerBuilder).addParameterValues(TEST_PARAM);
        ModelStorage.Model psku = ModelBuilder.newBuilder(1, HID)
                .currentType(ModelStorage.ModelType.SKU)
                .source(ModelStorage.ModelType.PARTNER_SKU.name())
                .supplierId(SHOP_ID)
                .picture(ModelStorage.Picture.newBuilder()
                        .setUrlOrig(PICTURE_URL_1))
                .picture(ModelStorage.Picture.newBuilder()
                        .setUrlOrig(PICTURE_URL_2))
                .withSkuParentRelation(2, 2)
                .parameterValue(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(MigrationUtils.SHOP_SKU_PARAM_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                .setValue(SHOP_SKU_1)
                                .build()
                        ).build()
                ).build();

        ModelStorage.Model pmodel = ModelBuilder.newBuilder(2, HID)
                .currentType(ModelStorage.ModelType.GURU)
                .withSkuRelations(2, 1)
                .build();

        modelStorageServiceMock.putModels(psku, pmodel);

        DataCampUnitedOffer.UnitedOffer unitedOffer = DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(offerBuilder)
                .build();
        DataCampUnitedOffer.UnitedOffer result = enricher.enrichOffers(Collections.singletonList(unitedOffer)).get(0);

        Market.DataCamp.DataCampOfferPictures.SourcePictures.Builder originalBuilder = result.getBasic().toBuilder()
                .getPicturesBuilder().getPartnerBuilder()
                .getOriginalBuilder();
        assertEquals(DataCampOfferPictures.PictureSource.MBO, originalBuilder.getSourceBuilder(0).getSource());
        assertEquals(StringUtil.trimLeadSlashes(PICTURE_URL_1), originalBuilder.getSourceBuilder(0).getUrl());
        assertEquals(DataCampOfferPictures.PictureSource.MBO, originalBuilder.getSourceBuilder(1).getSource());
        assertEquals(StringUtil.trimLeadSlashes(PICTURE_URL_2), originalBuilder.getSourceBuilder(1).getUrl());


        DataCampOfferMarketContent.ShopModelRating rating = result.getBasic().getContent()
                .getPartner().getMarketSpecificContent().getRating();
        assertEquals(10, rating.getCurrentRating());
    }

    private DataCampOffer.Offer.Builder makeOffer(String offerId, long approvedSkuId) {
        return makeOffer(offerId, approvedSkuId, BUSINESS_ID);
    }

    private DataCampOffer.Offer.Builder makeOffer(String offerId, long approvedSkuId, long bizId) {
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();
        offerBuilder.getContentBuilder().getBindingBuilder().getApprovedBuilder().setMarketSkuId(approvedSkuId);
        offerBuilder.setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
            .setOfferId(offerId)
            .setBusinessId((int) bizId)
            .setShopId((int) SHOP_ID)
            .build());
        return offerBuilder;
    }


    private DataCampOfferMarketContent.MarketParameterValues.Builder parameterValues(
        DataCampOffer.Offer.Builder offerBuilder
    ) {
        return offerBuilder.getContentBuilder()
            .getPartnerBuilder()
            .getMarketSpecificContentBuilder()
            .getParameterValuesBuilder();
    }
}
