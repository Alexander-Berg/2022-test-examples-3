package ru.yandex.direct.api.v5.units;

import java.lang.annotation.Annotation;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.ws.context.MessageContext;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.context.units.UnitsContext;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.units.exception.LackOfUnitsException;
import ru.yandex.direct.api.v5.ws.annotation.ApiMethod;
import ru.yandex.direct.core.units.OperationCosts;
import ru.yandex.direct.core.units.api.UnitsBalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UnitsInterceptorTest {

    private static final int BALANCE_LIMIT = 200_000;
    private static final int OPERATION_COST = 1_000;
    private static final int COSTS_MIN_DAILY_LIMIT = 100_000;

    @Mock
    private ApiUnitsService apiUnitsService;

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ApiContext apiContext;

    @Mock
    private UnitsContext unitsContext;

    @Mock
    private UnitsContext failedUnitsContext;

    @Mock
    private UnitsBalance unitsBalance;

    @Mock
    private OperationCosts operationCosts;

    @Mock
    private DirectApiPreAuthentication preAuth;

    @Mock
    private UnitsContextFactory unitsContextFactory;

    @InjectMocks
    private TestableUnitsInterceptor unitsInterceptor;

    @Before
    public void before() {
        initMocks(this);

        when(apiContextHolder.get()).thenReturn(apiContext);
        when(apiContext.getPreAuthentication()).thenReturn(preAuth);
        when(apiUnitsService.getCostsForOperation(anyString(), anyString())).thenReturn(operationCosts);

        when(unitsContextFactory.createUnitsContext(same(preAuth), anyInt())).thenReturn(unitsContext);
        when(unitsContextFactory.createUnitsContext(eq(null), anyInt())).thenReturn(failedUnitsContext);

        when(unitsContext.getUnitsBalance()).thenReturn(unitsBalance);
        when(unitsContext.getUnitsBalanceOrFail()).thenReturn(unitsBalance);
        when(failedUnitsContext.getUnitsBalance()).thenReturn(unitsBalance);
        when(failedUnitsContext.getUnitsBalanceOrFail()).thenReturn(unitsBalance);

        when(unitsBalance.getLimit()).thenReturn(BALANCE_LIMIT);
        when(operationCosts.getCostForOperation()).thenReturn(OPERATION_COST);
        when(operationCosts.getMinDailyLimit()).thenReturn(COSTS_MIN_DAILY_LIMIT);
    }

    @Test
    public void handleRequest_ShouldSetCorrectUnitsContext() throws Exception {
        when(apiContext.getPreAuthentication()).thenReturn(preAuth);
        when(unitsBalance.isAvailable(anyInt())).thenReturn(true);

        runHandleRequest();
        assertThat(apiContext.getUnitsContext()).isSameAs(unitsContext);
    }

    @Test
    public void handleRequest_AuthenticationIsNull_StillShouldSetUnitsContext() throws Exception {
        when(apiContext.getPreAuthentication()).thenReturn(null);
        when(unitsBalance.isAvailable(anyInt())).thenReturn(true);

        runHandleRequest();
        assertThat(apiContext.getUnitsContext()).isSameAs(failedUnitsContext);
    }

    @Test(expected = LackOfUnitsException.class)
    public void handleRequest_BalanceLimitIsTooSmallToCallOperation_ThrowLackOfUnitsException() throws Exception {
        when(unitsBalance.getLimit()).thenReturn(COSTS_MIN_DAILY_LIMIT - 1);

        runHandleRequest();
    }

    @Test(expected = RuntimeException.class)
    public void handleRequest_UnitsBalanceIsNull_ShouldRethrowRuntimeException() throws Exception {
        when(unitsContext.getUnitsBalanceOrFail()).thenThrow(new RuntimeException());

        runHandleRequest();
    }

    @Test(expected = LackOfUnitsException.class)
    public void handleRequest_NotEnoughUnits_ShouldThrowLackOfUnitsException() throws Exception {
        when(unitsBalance.isAvailable(anyInt())).thenReturn(false);

        runHandleRequest();
    }

    @Test
    public void handleRequest_EverythingIsOk_HandleRequestReturnsTrue() throws Exception {
        when(unitsBalance.isAvailable(anyInt())).thenReturn(true);

        assertThat(runHandleRequest()).isTrue();
    }

    @Test
    public void afterCompletion_ShouldSetCorrectUnitsHeaders() throws Exception {
        apiContext.setUnitsContext(unitsContext);
        runAfterCompletion();

        verify(apiUnitsService).setUnitsHeaders(same(unitsInterceptor.getHttpServletResponse()));
    }

    @Test
    public void afterCompletion_NoUnitsBalanceInTheContext_NoFail() throws Exception {
        apiContext.setUnitsContext(unitsContext);
        when(unitsContext.getUnitsBalance()).thenReturn(null);

        runAfterCompletion();

        verify(apiUnitsService, never()).setUnitsHeaders(any());
    }

    private boolean runHandleRequest() throws Exception {
        return unitsInterceptor.handleRequest(mock(MessageContext.class), new Object());
    }

    private void runAfterCompletion() throws Exception {
        unitsInterceptor.afterCompletion(mock(MessageContext.class), new Object(), null);
    }

    private static class TestableUnitsInterceptor extends UnitsInterceptor {

        private static HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

        public TestableUnitsInterceptor(ApiUnitsService apiUnitsService,
                                        ApiContextHolder apiContextHolder, UnitsContextFactory unitsContextFactory) {
            super(apiUnitsService, apiContextHolder, unitsContextFactory);
        }

        @Override
        ApiMethod getEndpointMethodMeta(Object endpoint) {
            return new ApiMethod() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return ApiMethod.class;
                }

                @Override
                public String service() {
                    return "service";
                }

                @Override
                public String operation() {
                    return "operation";
                }
            };
        }

        @Override
        HttpServletResponse getHttpServletResponse() {
            return httpServletResponse;
        }

    }

}
