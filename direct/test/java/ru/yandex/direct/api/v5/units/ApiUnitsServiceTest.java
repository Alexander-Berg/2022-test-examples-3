package ru.yandex.direct.api.v5.units;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.context.units.UnitsBucket;
import ru.yandex.direct.api.v5.context.units.UnitsContext;
import ru.yandex.direct.api.v5.context.units.UnitsLogData;
import ru.yandex.direct.api.v5.logging.ApiLogRecord;
import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.security.utils.ApiUserMockBuilder;
import ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactory;
import ru.yandex.direct.api.v5.units.logging.UnitsLogWriter;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.units.OperationCosts;
import ru.yandex.direct.core.units.OperationSummary;
import ru.yandex.direct.core.units.api.UnitsBalance;
import ru.yandex.direct.core.units.service.UnitsService;
import ru.yandex.direct.core.units.storage.Storage;
import ru.yandex.direct.core.units.storage.StorageErrorException;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.FALSE;

public class ApiUnitsServiceTest {

    private static final int DAY_LIMIT = 10;
    private static final long OPERATOR_CLIENT_ID = 1L;
    private static final long CLIENT_ID = 2L;
    private static final int SPENT_IN_CURRENT_REQUEST = 10;
    private static final int BALANCE = 140;
    private static final int LIMIT = 1500;
    private static final ApiUser CLIENT = new ApiUserMockBuilder("client", 345, CLIENT_ID, RbacRole.CLIENT).build();
    private static final ApiUser OPERATOR =
            new ApiUserMockBuilder("operator", 346, OPERATOR_CLIENT_ID, RbacRole.AGENCY).build();
    private static final String APPLICATION_ID = "123";

    private static final String UNITS_HEADER = "Units";
    private static final String UNITS_USED_LOGIN_HEADER = "Units-Used-Login";

    private DirectApiPreAuthentication auth;
    private UnitsService unitsService;
    private ApiUnitsService apiUnitsService;

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock
    private UnitsLogDataFactory unitsLogDataFactory;

    @Mock
    private ApiContext apiContext;

    @Mock
    private UnitsContext unitsContext;

    @Mock
    private UnitsLogData unitsLogData;

    @Mock
    private UnitsBalance unitsBalance;

    @Mock
    private UnitsBalance operatorUnitsBalance;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ApiLogRecord apiLogRecord;

    @Mock
    private OperationSummary operationSummary;

    @Mock
    private OperationCosts operationCosts;

    @Mock
    private UnitsLogWriter unitsLogWriter;

    @Before
    public void setUp() {
        initMocks(this);

        DirectApiCredentials credentials = mock(DirectApiCredentials.class);
        when(credentials.getUseOperatorUnitsMode()).thenReturn(FALSE);

        auth = new DirectApiPreAuthentication(credentials, APPLICATION_ID, CLIENT, CLIENT, CLIENT, CLIENT);

        unitsService = spy(new UnitsService(mock(Storage.class)));
        doReturn(DAY_LIMIT).when(unitsService).getLimit(any());

        apiUnitsService = new ApiUnitsService(unitsService, apiContextHolder, unitsLogDataFactory, unitsLogWriter);

        when(apiContextHolder.get()).thenReturn(apiContext);
        when(apiContext.getOperationCosts()).thenReturn(operationCosts);
        when(apiContext.getUnitsContext()).thenReturn(unitsContext);
        when(apiContext.getApiLogRecord()).thenReturn(apiLogRecord);
        when(unitsContext.getUnitsBalance()).thenReturn(unitsBalance);
        when(unitsContext.getOperatorUnitsBalance()).thenReturn(operatorUnitsBalance);
        when(unitsContext.getUnitsUsedUser()).thenReturn(CLIENT);
        when(unitsContext.getOperator()).thenReturn(OPERATOR);
        when(unitsContext.getUnitsLogData()).thenReturn(unitsLogData);
    }

    @Test
    public void getAdjustedUnitsBalance_RedisNotAvailable_FallbackUnitsBalanceReturned() {
        doThrow(new StorageErrorException())
                .when(unitsService)
                .getUnitsBalance(any());

        UnitsBalance unitsBalance = apiUnitsService.getAdjustedUnitsBalance(auth, CLIENT);

        assertNotNull(unitsBalance);
        assertEquals(DAY_LIMIT, unitsBalance.balance());
    }

    @Test
    public void updateSpent_RedisNotAvailable_ReturnsFalse() {
        doThrow(new StorageErrorException())
                .when(unitsService)
                .updateSpent(any());
        assertFalse(apiUnitsService.updateSpent(mock(UnitsBalance.class)));
    }

    @Test
    public void withdrawForRequestError_ShouldWithdrawOperatorUnitsBalance() {
        when(unitsContext.getUnitsLogData())
                .thenReturn(new UnitsLogData().withBucket(new UnitsBucket().withBucketClientId(OPERATOR_CLIENT_ID)));

        when(operatorUnitsBalance.spentInCurrentRequest()).thenReturn(SPENT_IN_CURRENT_REQUEST);
        when(operatorUnitsBalance.balance()).thenReturn(BALANCE);
        when(operatorUnitsBalance.getLimit()).thenReturn(LIMIT);

        apiUnitsService.withdrawForRequestError(0);

        verify(operatorUnitsBalance).withdraw(anyInt());
        verify(unitsBalance, never()).withdraw(anyInt());
        verify(apiLogRecord).withUnitsSpendingUserClientId(same(OPERATOR_CLIENT_ID));
        verify(apiLogRecord).withUnitsStats(anyList());

        List<Integer> unitsStats = apiLogRecord.getUnitsStats();
        assertThat(unitsStats).as("UnitsStats должен содержать 3 значения")
                .hasSize(3)
                .as("В логах должна содержаться информация о юнитах оператора")
                .containsExactly(SPENT_IN_CURRENT_REQUEST, BALANCE, LIMIT);
    }

    @Test
    public void withdraw_ShouldWithdrawSubclientUnitsBalance() {
        when(unitsContext.getUnitsLogData())
                .thenReturn(new UnitsLogData().withBucket(new UnitsBucket().withBucketClientId(CLIENT_ID)));

        when(unitsBalance.spentInCurrentRequest()).thenReturn(SPENT_IN_CURRENT_REQUEST);
        when(unitsBalance.balance()).thenReturn(BALANCE);
        when(unitsBalance.getLimit()).thenReturn(LIMIT);

        apiUnitsService.withdraw(operationSummary);

        verify(unitsBalance).withdraw(anyInt());
        verify(operatorUnitsBalance, never()).withdraw(anyInt());
        verify(apiLogRecord).withUnitsSpendingUserClientId(same(CLIENT_ID));
        verify(apiLogRecord).withUnitsStats(anyList());

        List<Integer> unitsStats = apiLogRecord.getUnitsStats();
        assertThat(unitsStats).as("UnitsStats должен содержать 3 значения")
                .hasSize(3)
                .as("В логах должна содержаться информация о юнитах субклиента")
                .containsExactly(SPENT_IN_CURRENT_REQUEST, BALANCE, LIMIT);
    }

    @Test
    public void withdrawForRequestError_ShouldCallUnitsLogWriter() {
        apiUnitsService.withdrawForRequestError(0);

        verify(unitsLogDataFactory).addOperatorBucket(same(unitsContext));
        verify(unitsLogWriter).write(same(unitsLogData));
    }

    @Test
    public void withdraw_ShouldCallUnitsLogWriter() {
        apiUnitsService.withdraw(operationSummary);

        verify(unitsLogDataFactory).addUnitsHolderBucket(same(unitsContext));
        verify(unitsLogWriter).write(same(unitsLogData));
    }

    @Test
    public void setUnitsHeader_UnitsAndUnitsUsedLoginHeadersMustBeSet() {
        final String unitsHeaderName = "Units";
        final String unitsUsedLoginHeaderName = "Units-Used-Login";
        HttpServletResponse response = new MockHttpServletResponse();
        Collection<String> headerNames = response.getHeaderNames();

        checkState(!headerNames.contains(unitsHeaderName));
        checkState(!headerNames.contains(unitsUsedLoginHeaderName));

        apiUnitsService.setUnitsHeaders(response);

        assertThat(headerNames).as("Заголовки Units и Units-Used-Login должны быть установлены")
                .containsExactly(unitsHeaderName, unitsUsedLoginHeaderName);
    }

    @Test
    public void setUnitsHeaders_RequestSucceeded_SubclientLoginIsSet() {
        when(unitsBalance.spentInCurrentRequest()).thenReturn(SPENT_IN_CURRENT_REQUEST);
        when(operatorUnitsBalance.spentInCurrentRequest()).thenReturn(0);

        HttpServletResponse response = new MockHttpServletResponse();
        Collection<String> headerNames = response.getHeaderNames();

        checkState(!headerNames.contains(UNITS_HEADER));
        checkState(!headerNames.contains(UNITS_USED_LOGIN_HEADER));

        apiUnitsService.setUnitsHeaders(response);

        assertThat(response.getHeader(UNITS_USED_LOGIN_HEADER))
                .as("В заголовке Units-Used-Login должен быть логин субклиента")
                .isEqualTo(CLIENT.getLogin());
    }

    @Test
    public void setUnitsHeaders_RequestFailed_OperatorLoginIsSet() {
        when(unitsBalance.spentInCurrentRequest()).thenReturn(0);
        when(operatorUnitsBalance.spentInCurrentRequest()).thenReturn(SPENT_IN_CURRENT_REQUEST);

        HttpServletResponse response = new MockHttpServletResponse();
        Collection<String> headerNames = response.getHeaderNames();

        checkState(!headerNames.contains(UNITS_HEADER));
        checkState(!headerNames.contains(UNITS_USED_LOGIN_HEADER));

        apiUnitsService.setUnitsHeaders(response);

        assertThat(response.getHeader(UNITS_USED_LOGIN_HEADER))
                .as("В заголовке Units-Used-Login должен быть логин оператора")
                .isEqualTo(OPERATOR.getLogin());
    }

    @Test
    public void setUnitsHeaders_UnitsContextContainsNullUnitsBalance_NoFail() {
        when(unitsContext.getUnitsBalance()).thenReturn(null);

        apiUnitsService.setUnitsHeaders(new MockHttpServletResponse());
    }

    @Test
    public void setUnitsHeaders_UnitsContextContainsNullUnitsUsedUser_NoFail() {
        when(unitsContext.getUnitsUsedUser()).thenReturn(null);

        apiUnitsService.setUnitsHeaders(new MockHttpServletResponse());
    }

    @Test
    public void setUnitsHeaders_UnitsContextContainsNullOperator_NoFail() {
        when(unitsContext.getOperator()).thenReturn(null);

        apiUnitsService.setUnitsHeaders(new MockHttpServletResponse());
    }
}
