package ru.yandex.market.ir.autogeneration.common.rating;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta.StringValue;
import Market.DataCamp.DataCampOfferPictures.MarketPicture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_1;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_2;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_3;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_IMPORTANT_1;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_IMPORTANT_2;
import static ru.yandex.market.ir.autogeneration.common.rating.SkuRatingTestUtils.PARAM_ID_IMPORTANT_3;

public class SkuRatingEvaluatorTest {

    private DefaultRatingEvaluator skuRatingEvaluator;
    private static final long DEFAULT_TEST_CATEGORY_ID_1 = 1; // thresholds - 50, 50
    private static final long DEFAULT_TEST_CATEGORY_ID_2 = 2; // thresholds - 50, 50 / 1 param in each group
    private static final long DEFAULT_TEST_CATEGORY_ID_3 = 3; // thresholds - 50, 50 / 2 params in each group
    private static final long CUSTOM_THRESHOLD_CATEGORY_ID_1 = 90690L; // thresholds - 40, 20
    private static final long CUSTOM_THRESHOLD_CATEGORY_ID_2 = 90701L; // thresholds - 50, 30 / 2 params in each group

    @Before
    public void before() {
        CategoryData categoryData1 = Mockito.mock(CategoryData.class);
        Mockito.when(categoryData1.getSkuRatingFormula())
                .thenReturn(SkuRatingTestUtils.buildFormula(DEFAULT_TEST_CATEGORY_ID_1,
                        SkuRatingTestUtils.ALL_PARAMS.keySet()));

        CategoryData categoryData2 = Mockito.mock(CategoryData.class);
        Mockito.when(categoryData2.getSkuRatingFormula())
                .thenReturn(SkuRatingTestUtils.buildFormula(DEFAULT_TEST_CATEGORY_ID_2, Set.of(PARAM_ID_IMPORTANT_1,
                        PARAM_ID_1)));

        CategoryData categoryData3 = Mockito.mock(CategoryData.class);
        Mockito.when(categoryData3.getSkuRatingFormula())
                .thenReturn(SkuRatingTestUtils.buildFormula(DEFAULT_TEST_CATEGORY_ID_3, Set.of(PARAM_ID_IMPORTANT_1,
                        PARAM_ID_IMPORTANT_2, PARAM_ID_1, PARAM_ID_2)));

        CategoryData categoryDataWithCustomThresholds1 = Mockito.mock(CategoryData.class);
        Mockito.when(categoryDataWithCustomThresholds1.getSkuRatingFormula())
                .thenReturn(SkuRatingTestUtils.buildFormula(CUSTOM_THRESHOLD_CATEGORY_ID_1,
                        SkuRatingTestUtils.ALL_PARAMS.keySet()));

        CategoryData categoryDataWithCustomThresholds2 = Mockito.mock(CategoryData.class);
        Mockito.when(categoryDataWithCustomThresholds2.getSkuRatingFormula())
                .thenReturn(SkuRatingTestUtils.buildFormula(CUSTOM_THRESHOLD_CATEGORY_ID_2, Set.of(PARAM_ID_IMPORTANT_1,
                        PARAM_ID_IMPORTANT_2, PARAM_ID_1, PARAM_ID_2)));

        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.addCategoryData(DEFAULT_TEST_CATEGORY_ID_1, categoryData1);
        categoryDataKnowledge.addCategoryData(DEFAULT_TEST_CATEGORY_ID_2, categoryData2);
        categoryDataKnowledge.addCategoryData(DEFAULT_TEST_CATEGORY_ID_3, categoryData3);
        categoryDataKnowledge.addCategoryData(CUSTOM_THRESHOLD_CATEGORY_ID_1, categoryDataWithCustomThresholds1);
        categoryDataKnowledge.addCategoryData(CUSTOM_THRESHOLD_CATEGORY_ID_2, categoryDataWithCustomThresholds2);
        skuRatingEvaluator = new DefaultRatingEvaluator(categoryDataKnowledge);
    }

    @Test
    public void testForFastCard() {
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();

        offerBuilder.getContentBuilder()
                .getBindingBuilder()
                .getApprovedBuilder()
                .setMarketCategoryId((int) DEFAULT_TEST_CATEGORY_ID_1);

        offerBuilder.getContentBuilder()
                .getPartnerBuilder()
                .getActualBuilder()
                .setTitle(StringValue.newBuilder().setValue("FastCardTitle").build())
                .setDescription(StringValue.newBuilder().setValue("FastCardDescription").build())
                .setVendor(StringValue.newBuilder().setValue("Vendor").build());


        offerBuilder.getPicturesBuilder()
                .getPartnerBuilder()
                .putActual(
                        "picture-key",
                        MarketPicture.newBuilder()
                                .setOriginal(
                                        MarketPicture.Picture.newBuilder()
                                                .setUrl("picture-url")
                                                .build()
                                )
                                .build()
                );

        DataCampOffer.Offer offer = offerBuilder.build();

        int rating = skuRatingEvaluator.evaluate(
                new FastRatingEvaluationTicketData(offer)
        );

        Assert.assertEquals(rating, 15); // 3 - name, 3- vendor, 4 - description, 5 - picture
    }

    @Test
    public void testForEmptyFastCard() {
        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();

        offerBuilder.getContentBuilder()
                .getBindingBuilder()
                .getApprovedBuilder()
                .setMarketCategoryId((int) DEFAULT_TEST_CATEGORY_ID_1);

        offerBuilder.getContentBuilder()
                .getPartnerBuilder()
                .getActualBuilder();

        DataCampOffer.Offer offer = offerBuilder.build();

        int rating = skuRatingEvaluator.evaluate(
                new FastRatingEvaluationTicketData(offer)
        );

        Assert.assertEquals(rating, 0);
    }

    @Test
    public void testFormulaAllDataFromSku() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(1,
                ParameterValueComposer.NAME_ID, SkuRatingFormula.DESCRIPTION_ID, PARAM_ID_1);

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_1)
        );
        Assert.assertEquals(rating, 27); // 3 (name) + 4 (description) + 15 (1/3 other params) + 5 (1 picture)
    }

    @Test
    public void testFormulaAllDataFromModel() {
        ModelStorage.Model model = buildModel(1,
                ParameterValueComposer.NAME_ID, SkuRatingFormula.DESCRIPTION_ID, PARAM_ID_IMPORTANT_1);
        ModelStorage.Model sku = buildModel(0);

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_1)
        );
        Assert.assertEquals(32, rating); // 3 (name) + 4 (description) + 20 (1/2 important params) + 5 (1 picture)
    }

    @Test
    public void testFormulaFromSkuAndModel() {
        ModelStorage.Model model = buildModel(1,
                ParameterValueComposer.NAME_ID, SkuRatingFormula.DESCRIPTION_ID, PARAM_ID_1);
        ModelStorage.Model sku = buildModel(2,
                ParameterValueComposer.NAME_ID, SkuRatingFormula.DESCRIPTION_ID, PARAM_ID_IMPORTANT_2);

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_1)
        );

        // 3 (name) + 4 (description) + 20 (1/2 important params) + 15 (1/3 other params) + 15 (3 picture)
        Assert.assertEquals(57, rating);
    }

    @Test
    public void testFormulaAllParams() {
        ModelStorage.Model model = buildModel(5,
                ParameterValueComposer.NAME_ID, PARAM_ID_1, PARAM_ID_2, PARAM_ID_3);
        ModelStorage.Model sku = buildModel(2,
                ParameterValueComposer.NAME_ID, ParameterValueComposer.VENDOR_ID, SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_IMPORTANT_1, PARAM_ID_IMPORTANT_2, PARAM_ID_IMPORTANT_3);

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_1)
        );
        Assert.assertEquals(rating, 100);
    }

    @Test
    public void testFormulaEmptySku() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0);

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_1)
        );
        Assert.assertEquals(rating, 0);
    }


    @Test
    public void testFormulaImportantParamsWithThresholds() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0, ParameterValueComposer.NAME_ID,
                SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_IMPORTANT_1
        );

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, CUSTOM_THRESHOLD_CATEGORY_ID_1)
        );
        Assert.assertEquals(27, rating); // 3 (name) + 4 (description) + 20 (1/4 other params < 40% threshold =>
        // count every param)
    }

    @Test
    public void testFormulaImportantParamsWithThresholdsOverMax() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0, ParameterValueComposer.NAME_ID,
                SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_IMPORTANT_1,
                PARAM_ID_IMPORTANT_2
        );

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, CUSTOM_THRESHOLD_CATEGORY_ID_1)
        );
        Assert.assertEquals(47, rating); // 3 (name) + 4 (description) + 20 (1/4 other params < 40% threshold =>
        // count every param)
    }

    @Test
    public void testFormulaCommonParamsWithThresholds() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0, ParameterValueComposer.NAME_ID,
                SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_1
        );

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, CUSTOM_THRESHOLD_CATEGORY_ID_1)
        );
        Assert.assertEquals(37, rating); // 3 (name) + 4 (description) + 30 (1/3 other params > 20% threshold => full
        // rating)
    }

    @Test
    public void testFormulaOneParamInGroup() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0, ParameterValueComposer.NAME_ID,
                SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_IMPORTANT_1
        );

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_2)
        );
        Assert.assertEquals(47, rating); // 3 (name) + 4 (description) + 40 (all important) + 0 (no common)
    }

    @Test
    public void testFormulaDefaultTwoParamsInGroup() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0, ParameterValueComposer.NAME_ID,
                SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_IMPORTANT_1,
                PARAM_ID_1,
                PARAM_ID_2
        );

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, DEFAULT_TEST_CATEGORY_ID_3)
        );
        Assert.assertEquals(57, rating); // 3 (name) + 4 (description) + 20 (1/2 important) + 30 (all common)
    }

    @Test
    public void testFormulaCustomTwoParamsInGroup() {
        ModelStorage.Model model = buildModel(0);
        ModelStorage.Model sku = buildModel(0, ParameterValueComposer.NAME_ID,
                SkuRatingFormula.DESCRIPTION_ID,
                PARAM_ID_IMPORTANT_1,
                PARAM_ID_1
        );

        int rating = skuRatingEvaluator.evaluate(
                new DataCampRatingEvaluationTicketData(modelsToMap(sku, model), 1, CUSTOM_THRESHOLD_CATEGORY_ID_2)
        );
        Assert.assertEquals(57, rating); // 3 (name) + 4 (description) + 20 (1/2 important) + 30 (1/2 common > 30%)
    }

    private Map<Long, ModelStorage.Model> modelsToMap(ModelStorage.Model sku, ModelStorage.Model model) {
        Map<Long, ModelStorage.Model> models = new HashMap<>();
        models.put(0L, model);
        models.put(1L, sku);
        return models;
    }

    private ModelStorage.Model buildModel(int picsCount, long... paramIds) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();

        for (int i = 0; i < picsCount; i++) {
            builder.addPictures(
                    ModelStorage.Picture.newBuilder()
                            .setUrl("http://url_" + i + ".jpg")
                            .build()
            );
        }
        for (long paramId : paramIds) {
            builder.addParameterValues(
                    ModelStorage.ParameterValue.newBuilder()
                            .setParamId(paramId)
                            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                                    .setValue("ttt" + paramId)
                                    .setIsoCode("ru")
                                    .build())
                            .build()
            );
        }
        return builder.build();
    }
}
