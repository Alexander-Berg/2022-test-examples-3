package ru.yandex.direct.api.v5.entity;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.units.OperationSummary;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GenericApiServiceTest {
    private static final Request REQUEST = new Request();
    private static final Response RESPONSE = new Response();
    private static final InternalRequest INTERNAL_REQUEST = new InternalRequest();
    private static final OperationSummary SUCCESSFUL_OPERATION_SUMMARY = OperationSummary.successful(3, 2);
    private static final OperationSummary UNSUCCESSFUL_OPERATION_SUMMARY = OperationSummary.unsuccessful();

    @Mock
    private BaseApiServiceDelegate<Request, Response, InternalRequest, InternalResponseItem> internalDelegate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiContextHolder apiContextHolder;

    @Mock
    private ApiUnitsService apiUnitsService;

    @Mock
    private AccelInfoHeaderSetter accelInfoHeaderSetter;

    @Mock
    private ApiValidationException apiValidationException;

    @Mock
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @InjectMocks
    private GenericApiService genericApiService;

    @Before
    public void before() {
        when(internalDelegate.convertRequest(same(REQUEST))).thenReturn(INTERNAL_REQUEST);
        //noinspection unchecked
        when(internalDelegate.convertResponse(any(ApiResult.class))).thenReturn(RESPONSE);
        when(internalDelegate.createValidationException(any(), anyBoolean(), anyBoolean()))
                .thenReturn(apiValidationException);
        when(internalDelegate.correctOperationSummary(any(), any())).thenCallRealMethod();
    }

    @Test
    public void doAction_Success_CheckCalls() {
        shouldChargeUnits();
        validRequest();
        ApiResult<List<InternalResponseItem>> result = successfulResult();

        Response response = genericApiService.doAction(internalDelegate, REQUEST);

        verify(internalDelegate).validateRequest(same(REQUEST));
        verify(internalDelegate).processRequest(same(INTERNAL_REQUEST));
        verify(internalDelegate).correctOperationSummary(same(INTERNAL_REQUEST), same(result));
        verify(apiUnitsService).withdraw(same(SUCCESSFUL_OPERATION_SUMMARY));
        verify(accelInfoHeaderSetter).setAccelInfoHeaderToHttpResponse();

        verify(internalDelegate, never()).createValidationException(any(), anyBoolean(), anyBoolean());
        verify(apiContextHolder.get(), never()).setAppErrorCode(anyInt());
        verify(requestAccessibleCampaignTypes).setCustom(same(internalDelegate.getCampaignAccessibiltyChecker()));

        assertThat(response).isSameAs(RESPONSE);
    }

    @Test
    public void doAction_NotValidatedRequest_CheckCalls() {
        shouldChargeUnits();
        dontValidateRequest();
        ApiResult<List<InternalResponseItem>> result = successfulResult();

        Response response = genericApiService.doAction(internalDelegate, REQUEST);

        verify(internalDelegate).validateRequest(same(REQUEST));
        verify(internalDelegate).processRequest(same(INTERNAL_REQUEST));
        verify(internalDelegate).correctOperationSummary(same(INTERNAL_REQUEST), same(result));
        verify(apiUnitsService).withdraw(same(SUCCESSFUL_OPERATION_SUMMARY));
        verify(accelInfoHeaderSetter).setAccelInfoHeaderToHttpResponse();

        verify(internalDelegate, never()).createValidationException(any(), anyBoolean(), anyBoolean());
        verify(apiContextHolder.get(), never()).setAppErrorCode(anyInt());

        assertThat(response).isSameAs(RESPONSE);
    }

    @Test(expected = ApiValidationException.class)
    public void doAction_Unsuccessful_ThrowsException() {
        dontValidateRequest();
        unsuccessfulResult();
        genericApiService.doAction(internalDelegate, REQUEST);
    }

    @Test
    public void doAction_Unsuccessful_CheckCalls() {
        shouldChargeUnits();
        dontValidateRequest();
        ApiResult<List<InternalResponseItem>> result = unsuccessfulResult();

        try {
            genericApiService.doAction(internalDelegate, REQUEST);
        } catch (ApiValidationException ignored) {

        }

        verify(internalDelegate).validateRequest(same(REQUEST));
        verify(internalDelegate).processRequest(same(INTERNAL_REQUEST));
        verify(internalDelegate).correctOperationSummary(same(INTERNAL_REQUEST), same(result));
        verify(apiUnitsService).withdraw(eq(UNSUCCESSFUL_OPERATION_SUMMARY));
        verify(internalDelegate).createValidationException(any(), eq(true), eq(true));
        verify(apiContextHolder.get()).setAppErrorCode(anyInt());

        verify(accelInfoHeaderSetter, never()).setAccelInfoHeaderToHttpResponse();
    }

    @Test(expected = ApiValidationException.class)
    public void doAction_InvalidRequest_ThrowsException() {
        invalidRequest();
        genericApiService.doAction(internalDelegate, REQUEST);
    }

    @Test
    public void doAction_InvalidRequest_CheckCalls() {
        shouldChargeUnits();
        invalidRequest();

        try {
            genericApiService.doAction(internalDelegate, REQUEST);
        } catch (ApiValidationException ignored) {

        }

        verify(internalDelegate).validateRequest(same(REQUEST));
        verify(apiUnitsService).withdraw(eq(OperationSummary.unsuccessful()));
        verify(internalDelegate).createValidationException(any(), eq(false), eq(true));
        verify(apiContextHolder.get()).setAppErrorCode(anyInt());

        verify(internalDelegate, never()).processRequest(same(INTERNAL_REQUEST));
        verify(internalDelegate, never()).correctOperationSummary(any(), any());
        verify(accelInfoHeaderSetter, never()).setAccelInfoHeaderToHttpResponse();
    }

    @Test
    public void doAction_Success_ShouldNotChargeUnits() {
        shouldNotChargeUnits();
        validRequest();
        successfulResult();

        genericApiService.doAction(internalDelegate, REQUEST);

        verify(apiUnitsService, never()).withdraw(same(SUCCESSFUL_OPERATION_SUMMARY));
    }

    private void dontValidateRequest() {
        when(internalDelegate.validateRequest(any(Request.class))).thenReturn(null);
    }

    private void validRequest() {
        when(internalDelegate.validateRequest(any(Request.class)))
                .thenAnswer(answer -> ValidationResult.success(answer.getArgument(0)));
    }

    private void invalidRequest() {
        when(internalDelegate.validateRequest(any(Request.class)))
                .thenAnswer(answer -> {
                    ValidationResult<Object, DefectType> vr = ValidationResult.success(answer.getArgument(0));
                    vr.addError(DefectTypes.noRights());
                    return vr;
                });
    }

    private void shouldChargeUnits() {
        when(apiContextHolder.get().shouldChargeUnitsForRequest()).thenReturn(true);
    }

    private void shouldNotChargeUnits() {
        when(apiContextHolder.get().shouldChargeUnitsForRequest()).thenReturn(false);
    }

    private ApiResult<List<InternalResponseItem>> successfulResult() {
        ApiResult<List<InternalResponseItem>> result = mock(ApiResult.class);
        when(result.isSuccessful()).thenReturn(true);
        when(result.getOperationSummary()).thenReturn(SUCCESSFUL_OPERATION_SUMMARY);
        when(internalDelegate.processRequest(same(INTERNAL_REQUEST))).thenReturn(result);
        return result;
    }

    private ApiResult<List<InternalResponseItem>> unsuccessfulResult() {
        ApiResult<List<InternalResponseItem>> result = mock(ApiResult.class);
        when(result.isSuccessful()).thenReturn(false);
        when(result.getOperationSummary()).thenReturn(UNSUCCESSFUL_OPERATION_SUMMARY);
        when(internalDelegate.processRequest(same(INTERNAL_REQUEST))).thenReturn(result);
        return result;
    }

    private static class Request {
    }

    private static class Response {
    }

    private static class InternalRequest {
    }

    private static class InternalResponseItem {
    }
}
