package ru.yandex.market.ir.ui.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.OfferProblem;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.robot.shared.models.robot.data.FormalizerResultParam;
import ru.yandex.market.robot.shared.models.robot.data.UltraControllerOfferProblem;
import ru.yandex.market.robot.shared.models.robot.data.UltraControllerRequest;
import ru.yandex.market.robot.shared.models.robot.data.UltraServiceResult;

import java.util.List;

public class UcServiceProxyTest {
    private static final String CLASSIFIER_MAGIC_ID = "280e9odj2308j032";
    private static final String CLASSIFIER_GOOD_ID = "n923y892nd23k";
    private static final String TITLE = "Apple Iphone 12 pro";

    private static final int CATEGORY_ID = 91491;
    private static final String CATEGORY_NAME = "Mobile phones";
    private static final UltraController.EnrichedOffer.ClassificationType CLASSIFICATION_TYPE =
        UltraController.EnrichedOffer.ClassificationType.NORM;
    private static final double FIRST_PROBABILITY = 0.9;
    private static final double CONFIDENT_TOP_PRECISION = 0.85;
    private static final int SECOND_CATEGORY_ID = 91498;
    private static final double SECOND_PROBABILITY = 0.05;
    private static final int TOVAR_CATEGORY_ID = 444;
    private static final int GURU_CATEGORY_ID = 16003;
    private static final int MATCHED_ID = 9128301;
    private static final UltraController.EnrichedOffer.MatchType MATCHED_TYPE =
        UltraController.EnrichedOffer.MatchType.MATCH_OK;
    private static final String MATCH_DETAILS = "apple iphone 12";
    private static final int VENDOR_ID = 3901;
    private static final int MATCHED_VENDOR_ID = 12839;
    private static final String VENDOR_NAME = "Apple";
    private static final long PROCESSED_TIME = 1651246157768L;
    private static final int MARKET_SKU_ID = 389010310;
    private static final String MARKET_MODEL_NAME = "Apple Iphone 12 Pro";
    private static final String MARKET_SKU_NAME = "Apple Iphone 12 Pro deep ocean blue 256Gb";

    private static final String PARAM_NAME = "Производитель";
    private static final int PARAM_ID = 7893318;
    private static final String VALUE_NAME = "Apple";
    private static final int VALUE_ID = 153043;
    private static final int SOURCE_INDEX = -2;

    private static final int NUMBER_PARAM_ID = 1234;
    private static final double NUMBER_PARAM_VALUE = 1770961998511739600d;
    private static final String NUMBER_PARAM_FORMATTED_VALUE_START = "17709619985117396";

    private static final OfferProblem.ProblemType PROBLEM_TYPE = OfferProblem.ProblemType.PRICE_CONFLICT;
    private static final int PROBLEM_MODEL_ID = 0;
    private static final int MIN_PRICE = 29;
    private static final int MAX_PRICE = 400000;

    private UltraControllerService ultraControllerService;
    private UcServiceProxy ucServiceProxy;

    @Before
    public void setup() {
        ultraControllerService = Mockito.mock(UltraControllerService.class);
        mockUcResponse(ultraControllerService);


        VendorInfoProviderService vendorInfoProviderService = Mockito.mock(VendorInfoProviderService.class);
        mockVendorNameResponse(vendorInfoProviderService);

        ucServiceProxy = new UcServiceProxy(ultraControllerService, vendorInfoProviderService);
    }

    @Test
    public void testEnrich() {
        List<UltraServiceResult> enrichResults = ucServiceProxy.enrich(getDefaultUcRequest());
        UltraServiceResult result = enrichResults.get(0);

        ArgumentCaptor<UltraController.Offer> captor = ArgumentCaptor.forClass(UltraController.Offer.class);
        Mockito.verify(ultraControllerService).enrichSingleOffer(captor.capture());
        UltraController.Offer offer = captor.getValue();
        Assert.assertEquals(TITLE, offer.getOffer());
        Assert.assertEquals(CLASSIFIER_MAGIC_ID, offer.getClassifierMagicId());
        Assert.assertEquals(CLASSIFIER_GOOD_ID, offer.getClassifierGoodId());

        Assert.assertEquals(CATEGORY_ID, result.getCategoryId());
        Assert.assertEquals(CATEGORY_ID, result.getClassifierCategoryId());
        Assert.assertEquals(CLASSIFICATION_TYPE.name(), result.getClassificationType());
        Assert.assertEquals(FIRST_PROBABILITY, result.getProbability(), 1e-5);
        Assert.assertEquals(CONFIDENT_TOP_PRECISION, result.getClassifierConfidentTopPrecision(), 1e-5);
        Assert.assertEquals(CATEGORY_ID, result.getOldCategoryId());
        Assert.assertEquals(SECOND_CATEGORY_ID, result.getSecondCategoryId());
        Assert.assertEquals(SECOND_PROBABILITY, result.getSecondProbability(), 1e-5);
        Assert.assertEquals(CLASSIFICATION_TYPE.name(), result.getSecondClassificationType());
        Assert.assertEquals(CATEGORY_ID, result.getMappedId());
        Assert.assertEquals(CATEGORY_ID, result.getMatchedCategoryId());
        Assert.assertEquals(TOVAR_CATEGORY_ID, result.getTovarCategoryId());
        Assert.assertEquals(GURU_CATEGORY_ID, result.getGuruCategoryId());
        Assert.assertEquals(MATCHED_ID, result.getMatchedId());
        Assert.assertEquals(MATCHED_TYPE.name(), result.getMatchedType().name());
        Assert.assertEquals(MATCH_DETAILS, result.getMatchDetails());
        Assert.assertEquals(VENDOR_ID, result.getVendorId());
        Assert.assertEquals(MATCHED_VENDOR_ID, result.getMatchedVendorId());
        Assert.assertEquals(PROCESSED_TIME, result.getProcessedTime());
        Assert.assertEquals(MATCHED_ID, result.getMatchedId());
        Assert.assertEquals(MARKET_SKU_ID, result.getMarketSkuId());
        Assert.assertEquals(CATEGORY_NAME, result.getCategoryName());
        Assert.assertEquals(MARKET_MODEL_NAME, result.getMarketModelName());
        Assert.assertEquals(MARKET_SKU_NAME, result.getMarketSkuName());
        Assert.assertEquals(VENDOR_NAME, result.getVendorName());

        FormalizerResultParam param = result.getFormalizedParams().get(0);
        Assert.assertEquals(PARAM_NAME, param.getParamName());
        Assert.assertEquals(PARAM_ID, param.getParamId());
        Assert.assertEquals(VALUE_NAME, param.getValueName());
        Assert.assertEquals(VALUE_ID, param.getValueId());
        Assert.assertEquals(SOURCE_INDEX, param.getSourceIndex());

        FormalizerResultParam numberParam = result.getFormalizedParams().get(1);
        Assert.assertEquals(NUMBER_PARAM_ID, numberParam.getParamId());
        Assert.assertTrue("Incorrect number format",
            numberParam.getValue().startsWith(NUMBER_PARAM_FORMATTED_VALUE_START));

        UltraControllerOfferProblem problem = result.getProblems().get(0);
        Assert.assertEquals(PROBLEM_TYPE.name(), problem.getProblemTypeStr());
        Assert.assertEquals(PROBLEM_MODEL_ID, problem.getModelId());
        Assert.assertEquals(MIN_PRICE, problem.getMinPrice());
        Assert.assertEquals(MAX_PRICE, problem.getMaxPrice());
    }

    private UltraControllerRequest getDefaultUcRequest() {
        UltraControllerRequest ultraControllerRequest = new UltraControllerRequest();
        ultraControllerRequest.setText(TITLE);
        ultraControllerRequest.setOfferId(CLASSIFIER_MAGIC_ID);
        ultraControllerRequest.setGoodId(CLASSIFIER_GOOD_ID);
        return ultraControllerRequest;
    }

    private void mockUcResponse(UltraControllerService ultraControllerService) {
        FormalizerParam.FormalizedParamPosition formalizedParamPosition =
            FormalizerParam.FormalizedParamPosition.newBuilder()
                .setParamName(PARAM_NAME)
                .setParamId(PARAM_ID)
                .setValueName(VALUE_NAME)
                .setValueId(VALUE_ID)
                .setSourceIndex(SOURCE_INDEX)
                .build();

        FormalizerParam.FormalizedParamPosition numberParamPosition =
            FormalizerParam.FormalizedParamPosition.newBuilder()
                .setParamId(NUMBER_PARAM_ID)
                .setSourceIndex(SOURCE_INDEX)
                .setNumberValue(NUMBER_PARAM_VALUE)
                .build();

        OfferProblem.Problem problem = OfferProblem.Problem.newBuilder()
            .setProblemType(PROBLEM_TYPE)
            .setModelId(PROBLEM_MODEL_ID)
            .setMinPrice(MIN_PRICE)
            .setMaxPrice(MAX_PRICE)
        .build();

        UltraController.EnrichedOffer enrichedOffer = UltraController.EnrichedOffer.newBuilder()
            .setClassifierCategoryId(CATEGORY_ID)
            .setClassificationTypeValue(CLASSIFICATION_TYPE)
            .setProbability(FIRST_PROBABILITY)
            .setClassifierConfidentTopPrecision(CONFIDENT_TOP_PRECISION)
            .setOldCategoryId(CATEGORY_ID)
            .setSecondCategoryId(SECOND_CATEGORY_ID)
            .setSecondClassificationTypeValue(CLASSIFICATION_TYPE)
            .setSecondProbability(SECOND_PROBABILITY)
            .setMappedId(CATEGORY_ID)
            .setMatchedCategoryId(CATEGORY_ID)
            .setCategoryId(CATEGORY_ID)
            .setTovarCategoryId(TOVAR_CATEGORY_ID)
            .setGuruCategoryId(GURU_CATEGORY_ID)
            .setMatchedId(MATCHED_ID)
            .setMatchedTypeValue(MATCHED_TYPE)
            .setMatchDetails(MATCH_DETAILS)
            .setVendorId(VENDOR_ID)
            .setMatchedVendorId(MATCHED_VENDOR_ID)
            .setProcessedTime(PROCESSED_TIME)
            .setModelId(MATCHED_ID)
            .setMarketSkuId(MARKET_SKU_ID)
            .setMarketCategoryName(CATEGORY_NAME)
            .setMarketVendorName(VENDOR_NAME)
            .setMarketModelName(MARKET_MODEL_NAME)
            .setMarketSkuName(MARKET_SKU_NAME)
            .addParams(formalizedParamPosition)
            .addParams(numberParamPosition)
            .addOfferProblem(problem)
            .build();
        Mockito.when(ultraControllerService.enrichSingleOffer(Mockito.any()))
            .thenReturn(enrichedOffer);
    }

    private void mockVendorNameResponse(VendorInfoProviderService vendorInfoProviderService) {
        Mockito.when(vendorInfoProviderService.getGlobalVendorName(VENDOR_ID)).thenReturn(VENDOR_NAME);
    }
}
