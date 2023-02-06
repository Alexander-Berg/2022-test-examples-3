package ru.yandex.market.ir.ui.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.http.MatcherService;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.ir.ui.services.data.CategoryInfo;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.GlobalVendorsService;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.robot.shared.models.robot.data.MatchResult;
import ru.yandex.market.robot.shared.models.robot.data.MatcherRequest;
import ru.yandex.market.robot.shared.models.robot.data.MatcherType;
import ru.yandex.market.robot.shared.models.robot.data.YmlParam;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class MatcherServiceProxyTest {
    private static final int CATEGORY_ID = 91491;
    private static final int GURU_CATEGORY_ID = 83901;
    private static final String CATEGORY_NAME = "Mobilnie telephoni";

    private static final String LOCALE = "ru";
    private static final String TITLE = "Apple iphone 12 pro";
    private static final int PARAM_ID = 318902;
    private static final String PARAM_NAME = "Color";
    private static final int VALUE_ID = 5;
    private static final String PARAM_VALUE = "Deep ocean blue";
    private static final List<YmlParam> YML_PARAMS;
    static {
        YmlParam ymlParam = new YmlParam();
        ymlParam.setName(PARAM_NAME);
        ymlParam.setValue(PARAM_VALUE);
        YML_PARAMS = Collections.singletonList(ymlParam);
    }

    private static final int MODEL_ID = 381920;
    private static final String MODEL_NAME = "Iphone 12 pro";
    private static final int PARENT_MODEL_ID = 391;
    private static final int MODIFICATION_ID = 371923;
    private static final String MODIFICATION_NAME = "Iphone 12 pro Deep ocean blue";
    private static final int VENDOR_ID = 9310;
    private static final String VENDOR = "Apple";

    private final MatcherType matcherType;

    private MatcherService currentMatcherService;
    private MatcherServiceProxy matcherServiceProxy;

    public MatcherServiceProxyTest(MatcherType matcherType) {
        this.matcherType = matcherType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][]{
                {MatcherType.MARKET},
                {MatcherType.MBO}
            }
        );
    }

    @Before
    public void setup() {
        MatcherService matcherService = Mockito.mock(MatcherService.class);
        MatcherService mboMatcherService = Mockito.mock(MatcherService.class);
        currentMatcherService = matcherType == MatcherType.MBO ? mboMatcherService : matcherService;
        mockMatcherServiceResponse(getDefaultMatchResponse());

        CategoryInfoProviderService categoryInfoProviderService = Mockito.mock(CategoryInfoProviderService.class);
        mockCategoryInfoResponse(categoryInfoProviderService);

        GlobalVendorsService globalVendorsService = Mockito.mock(GlobalVendorsService.class);
        mockGlobalVendorsResponse(globalVendorsService);

        FormalizerServiceProxy formalizerServiceProxy = Mockito.mock(FormalizerServiceProxy.class);
        mockFormalizerResponse(formalizerServiceProxy);

        matcherServiceProxy = new MatcherServiceProxy(matcherService, mboMatcherService,
            formalizerServiceProxy, categoryInfoProviderService, new VendorInfoProviderService(globalVendorsService));
    }

    @Test
    public void testMatch() {
        MatcherRequest matcherRequest = createDefaultMatchRequest();
        List<MatchResult> matchResults = matcherServiceProxy.match(matcherRequest);
        verifyMatchCalled();

        MatchResult matchResult = matchResults.get(0);
        Assert.assertEquals(MODEL_ID, matchResult.getModelId());
        Assert.assertEquals(MODEL_NAME, matchResult.getModelName());
        Assert.assertEquals(MODIFICATION_ID, matchResult.getModificationId());
        Assert.assertEquals(MODIFICATION_NAME, matchResult.getModificationName());
        Assert.assertEquals(VENDOR_ID, matchResult.getVendorId());
        Assert.assertEquals(VENDOR, matchResult.getVendorName());
        Assert.assertEquals(CATEGORY_ID, matchResult.getCategoryId());
        Assert.assertEquals(CATEGORY_NAME, matchResult.getCategory());
    }

    private Matcher.MatchResponse getDefaultMatchResponse() {
        Matcher.MatchResult matchResult = Matcher.MatchResult.newBuilder()
            .setHid(CATEGORY_ID)
            .setMatchedId(MODEL_ID)
            .addMatchedHierarchy(Matcher.MatchLevel.newBuilder().setMatchedId(PARENT_MODEL_ID).build())
            .addMatchedHierarchy(Matcher.MatchLevel.newBuilder()
                .setMatchedId(MODEL_ID)
                .setName(MODEL_NAME)
                .build())
            .addMatchedHierarchy(Matcher.MatchLevel.newBuilder()
                .setMatchedId(MODIFICATION_ID)
                .setName(MODIFICATION_NAME)
                .build())
            .setGlobalVendorId(VENDOR_ID)
            .build();
        return Matcher.MatchResponse.newBuilder()
            .addResult(matchResult)
            .build();
    }

    private void mockMatcherServiceResponse(Matcher.MatchResponse matchResponse) {
        Mockito.when(currentMatcherService.multiMatch(Mockito.any()))
            .thenReturn(matchResponse);
    }

    private void mockCategoryInfoResponse(CategoryInfoProviderService categoryInfoProviderService) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setHid(CATEGORY_ID);
        categoryInfo.setGuruId(GURU_CATEGORY_ID);
        categoryInfo.setName(CATEGORY_NAME);
        Mockito.when(categoryInfoProviderService.getCategoryInfo(CATEGORY_ID))
            .thenReturn(categoryInfo);
    }

    private void mockGlobalVendorsResponse(GlobalVendorsService globalVendorsService) {
        MboVendors.GlobalVendor globalVendor = MboVendors.GlobalVendor.newBuilder()
            .setId(VENDOR_ID)
            .addName(MboParameters.Word.newBuilder().setName(VENDOR).build())
            .build();
        MboVendors.SearchVendorsResponse searchVendorsResponse = MboVendors.SearchVendorsResponse.newBuilder()
            .addVendors(globalVendor)
            .build();
        Mockito.when(globalVendorsService.searchVendors(Mockito.any())).thenReturn(searchVendorsResponse);
    }

    private void mockFormalizerResponse(FormalizerServiceProxy formalizerServiceProxy) {
        Formalizer.FormalizedOffer formalizedOffer = Formalizer.FormalizedOffer.newBuilder()
            .addPosition(FormalizerParam.FormalizedParamPosition.newBuilder()
                .setParamId(PARAM_ID)
                .setParamName(PARAM_NAME)
                .setValueId(VALUE_ID)
                .setValueName(PARAM_VALUE)
                .build())
            .build();

        Formalizer.FormalizerResponse formalizerResponse = Formalizer.FormalizerResponse.newBuilder()
            .addOffer(formalizedOffer)
            .build();

        Mockito.when(formalizerServiceProxy.doFormalize(Mockito.any()))
            .thenReturn(formalizerResponse);
    }

    private MatcherRequest createDefaultMatchRequest() {
        MatcherRequest matcherRequest = new MatcherRequest();
        matcherRequest.setMatcherType(matcherType);
        matcherRequest.setCategoryId(CATEGORY_ID);
        matcherRequest.setLocale(LOCALE);
        matcherRequest.setText(TITLE);
        matcherRequest.setYmlParams(YML_PARAMS);
        matcherRequest.setUseFormalizerMatcher(true);
        return matcherRequest;
    }

    private void verifyMatchCalled() {
        ArgumentCaptor<Matcher.Offer> requestCaptor = ArgumentCaptor.forClass(Matcher.Offer.class);
        Mockito.verify(currentMatcherService).multiMatch(requestCaptor.capture());
        Matcher.Offer request = requestCaptor.getValue();
        Assert.assertEquals(GURU_CATEGORY_ID, request.getGuruCategoryId());
        Assert.assertEquals(LOCALE, request.getLocale());
        Assert.assertEquals(TITLE, request.getTitle());
        Offer.YmlParam ymlParam = request.getYmlParam(0);
        Assert.assertEquals(PARAM_NAME, ymlParam.getName());
        Assert.assertEquals(PARAM_VALUE, ymlParam.getValue());
        FormalizerParam.FormalizedParamPosition formalizerParamPosition = request.getFormalizedParam(0);
        Assert.assertEquals(PARAM_ID, formalizerParamPosition.getParamId());
        Assert.assertEquals(PARAM_NAME, formalizerParamPosition.getParamName());
        Assert.assertEquals(VALUE_ID, formalizerParamPosition.getValueId());
        Assert.assertEquals(PARAM_VALUE, formalizerParamPosition.getValueName());
    }
}
