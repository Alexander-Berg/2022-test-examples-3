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
import ru.yandex.market.ir.http.FormalizerService;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.robot.shared.models.robot.data.CategoryChangeInfo;
import ru.yandex.market.robot.shared.models.robot.data.CategoryChangeStatus;
import ru.yandex.market.robot.shared.models.robot.data.FormalizeRequest;
import ru.yandex.market.robot.shared.models.robot.data.FormalizerResult;
import ru.yandex.market.robot.shared.models.robot.data.FormalizerResultParam;
import ru.yandex.market.robot.shared.models.robot.data.FormalizerResultStatus;
import ru.yandex.market.robot.shared.models.robot.data.FormalizerType;
import ru.yandex.market.robot.shared.models.robot.data.YmlParam;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class FormalizerServiceProxyTest {

    private static final int CATEGORY_ID = 90401;
    private static final String TITLE = "Apple Iphone 12 pro";
    private static final String COLOR_PARAM_NAME = "Color";
    private static final String COLOR_PARAM_VALUE = "blue";
    private static final List<YmlParam> YML_PARAMS;
    static {
        YmlParam param = new YmlParam();
        param.setName(COLOR_PARAM_NAME);
        param.setValue(COLOR_PARAM_VALUE);
        YML_PARAMS = Collections.singletonList(param);
    }

    private static final int VENDOR_ID = 9213;

    private static final int PARAM_ID = 3109;
    private static final String PARAM_NAME = "color";
    private static final int VALUE_ID = 5;
    private static final String VALUE_NAME = "blue";
    private static final int SOURCE_INDEX = -2;

    private FormalizerService currentFormalizerService;
    private FormalizerServiceProxy formalizerServiceProxy;

    private final FormalizerType formalizerType;

    public FormalizerServiceProxyTest(FormalizerType formalizerType) {
        this.formalizerType = formalizerType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][]{
                {FormalizerType.FORMALIZER},
                {FormalizerType.MBO_FORMALIZER}
            }
        );
    }

    @Before
    public void setup() {
        FormalizerService formalizerService = Mockito.mock(FormalizerService.class);
        FormalizerService mboFormalizerService = Mockito.mock(FormalizerService.class);

        currentFormalizerService = formalizerType == FormalizerType.FORMALIZER ?
            formalizerService : mboFormalizerService;
        mockFormalizeResponse(currentFormalizerService, createMockedResponseOfferBuilder());

        formalizerServiceProxy = new FormalizerServiceProxy(formalizerService, mboFormalizerService);
    }

    @Test
    public void testFormalize() {
        FormalizerResult formalizerResult = formalizerServiceProxy.formalize(createFormalizerRequest());
        checkResult(formalizerResult);
        checkFormalizeMethodCalled();
    }

    @Test
    public void testUnknownCategory() {
        mockResponseUnknownCategory(currentFormalizerService);

        FormalizerResult formalizerResult = formalizerServiceProxy.formalize(createFormalizerRequest());
        Assert.assertEquals(FormalizerResultStatus.UNKNOWN_CATEGORY, formalizerResult.getStatus());
    }

    @Test
    public void testNumberValue() {
        Formalizer.FormalizedOffer.Builder responseOfferBuilder = createMockedResponseOfferBuilder();

        double numberValue = 26;
        FormalizerParam.FormalizedParamPosition numberPosition = FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(1234)
            .setParamName("abcd")
            .setSourceIndex(0)
            .setNumberValue(numberValue)
            .build();
        responseOfferBuilder.addPosition(numberPosition);

        int valueId = 4321;
        String valueName = "myvalue";
        FormalizerParam.FormalizedParamPosition noNumberPosition = FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(1234)
            .setParamName("abcd")
            .setValueId(valueId)
            .setValueName(valueName)
            .setSourceIndex(0)
            .build();
        responseOfferBuilder.addPosition(noNumberPosition);

        mockFormalizeResponse(currentFormalizerService, responseOfferBuilder);
        FormalizerResult formalizerResult = formalizerServiceProxy.formalize(createFormalizerRequest());

        checkResult(formalizerResult);
        checkFormalizeMethodCalled();

        FormalizerResultParam numberParam = formalizerResult.getParams().get(1);
        Assert.assertEquals(numberValue, numberParam.getNumberValue(), 1e-5);

        FormalizerResultParam noNumberParam = formalizerResult.getParams().get(2);
        Assert.assertEquals(valueId, noNumberParam.getValueId());
        Assert.assertEquals(valueName, noNumberParam.getValueName());
    }

    @Test
    public void testChangeCategoryInfo() {
        Formalizer.FormalizedOffer.Builder responseOfferBuilder = createMockedResponseOfferBuilder();

        int originalCategoryId = 90401;
        int resultCategoryId = 55555;
        int ruleId = 37802;

        responseOfferBuilder.setChangeCategoryInfo(FormalizerParam.ChangeCategoryInfo.newBuilder()
            .setStatus(FormalizerParam.ChangeCategoryInfo.ChangeCategoryStatus.SUCCESS)
            .setOriginalCategoryId(originalCategoryId)
            .setResultCategoryId(resultCategoryId)
            .setRuleId(ruleId)
            .build());
        mockFormalizeResponse(currentFormalizerService, responseOfferBuilder);

        FormalizerResult formalizerResult = formalizerServiceProxy.formalize(createFormalizerRequest());
        checkResult(formalizerResult);
        checkFormalizeMethodCalled();

        CategoryChangeInfo categoryChangeInfo = formalizerResult.getCategoryChangeInfo();
        Assert.assertEquals(CategoryChangeStatus.SUCCESS, categoryChangeInfo.getStatus());

        Assert.assertEquals(Integer.valueOf(originalCategoryId), categoryChangeInfo.getOriginalCategoryId());
        Assert.assertEquals(Integer.valueOf(resultCategoryId), categoryChangeInfo.getResultCategoryId());
        Assert.assertEquals(Integer.valueOf(ruleId), categoryChangeInfo.getRuleId());
    }

    @Test
    public void testPreFormalize() {
        FormalizeRequest formalizeRequest = createFormalizerRequest();
        formalizeRequest.setResolveConflicts(false);

        mockPreFormalizeResponse(currentFormalizerService, createMockedResponseOfferBuilder());
        FormalizerResult formalizerResult = formalizerServiceProxy.formalize(formalizeRequest);

        checkResult(formalizerResult);
        checkPreFormalizeMethodCalled();
    }

    private void mockFormalizeResponse(FormalizerService formalizerService,
                                       Formalizer.FormalizedOffer.Builder responseOfferBuilder) {
        Mockito.when(formalizerService.formalize(Mockito.any()))
            .thenReturn(
                Formalizer.FormalizerResponse.newBuilder()
                    .addOffer(responseOfferBuilder)
                    .build()
            );
    }

    private void mockPreFormalizeResponse(FormalizerService formalizerService,
                                          Formalizer.FormalizedOffer.Builder responseOfferBuilder) {
        Mockito.when(formalizerService.preFormalize(Mockito.any()))
            .thenReturn(
                Formalizer.FormalizerResponse.newBuilder()
                    .addOffer(responseOfferBuilder)
                    .build()
            );
    }

    private Formalizer.FormalizedOffer.Builder createMockedResponseOfferBuilder() {
        FormalizerParam.FormalizedParamPosition position = FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(PARAM_ID)
            .setParamName(PARAM_NAME)
            .setValueId(VALUE_ID)
            .setValueName(VALUE_NAME)
            .setSourceIndex(SOURCE_INDEX)
            .build();

        return Formalizer.FormalizedOffer.newBuilder()
            .setVendorId(VENDOR_ID)
            .addPosition(position)
            .setType(Formalizer.FormalizationType.SUCCESS);
    }

    private FormalizeRequest createFormalizerRequest() {
        FormalizeRequest formalizeRequest = new FormalizeRequest();
        formalizeRequest.setType(formalizerType);
        formalizeRequest.setResolveConflicts(true);
        formalizeRequest.setCategoryId(CATEGORY_ID);
        formalizeRequest.setTitle(TITLE);
        formalizeRequest.setYmlParams(YML_PARAMS);

        return formalizeRequest;
    }

    private void mockResponseUnknownCategory(FormalizerService formalizerService) {
        Formalizer.FormalizedOffer.Builder responseOfferBuilder = Formalizer.FormalizedOffer.newBuilder()
            .setType(Formalizer.FormalizationType.UNKNOWN_CATEGORY);

        mockFormalizeResponse(formalizerService, responseOfferBuilder);
    }

    private void checkResult(FormalizerResult formalizerResult) {
        Assert.assertEquals(FormalizerResultStatus.SUCCESS, formalizerResult.getStatus());
        Assert.assertEquals(VENDOR_ID, formalizerResult.getVendorId());

        FormalizerResultParam resultParam = formalizerResult.getParams().get(0);
        Assert.assertEquals(PARAM_ID, resultParam.getParamId());
        Assert.assertEquals(PARAM_NAME, resultParam.getParamName());
        Assert.assertEquals(VALUE_ID, resultParam.getValueId());
        Assert.assertEquals(VALUE_NAME, resultParam.getValueName());
        Assert.assertEquals(SOURCE_INDEX, resultParam.getSourceIndex());
    }

    private void checkFormalizeMethodCalled() {
        ArgumentCaptor<Formalizer.FormalizerRequest> requestCaptor = ArgumentCaptor.forClass(
            Formalizer.FormalizerRequest.class);
        Mockito.verify(currentFormalizerService).formalize(requestCaptor.capture());
        checkFormalizeRequest(requestCaptor.getValue());
    }

    private void checkPreFormalizeMethodCalled() {
        ArgumentCaptor<Formalizer.FormalizerRequest> requestCaptor = ArgumentCaptor.forClass(
            Formalizer.FormalizerRequest.class);
        Mockito.verify(currentFormalizerService).preFormalize(requestCaptor.capture());
        checkFormalizeRequest(requestCaptor.getValue());
    }

    private void checkFormalizeRequest(Formalizer.FormalizerRequest request) {
        Assert.assertTrue(request.getReturnParamName());
        Assert.assertTrue(request.getReturnValueName());
        Formalizer.Offer requestOffer = request.getOffer(0);
        Assert.assertEquals(CATEGORY_ID, requestOffer.getCategoryId());
        Assert.assertEquals(TITLE, requestOffer.getTitle());
        Assert.assertEquals(
            Collections.singletonList(Offer.YmlParam.newBuilder()
                .setName(COLOR_PARAM_NAME)
                .setValue(COLOR_PARAM_VALUE)
                .build()),
            requestOffer.getYmlParamList());
    }
}
