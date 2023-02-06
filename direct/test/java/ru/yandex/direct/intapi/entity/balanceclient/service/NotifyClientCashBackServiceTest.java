package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.currencies.CurrencyUsd;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientCashBackParameters;
import ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyClientCashBackValidationService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyClientCashBackService.WRONG_CURRENCY_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyClientCashBackService.WRONG_CURRENCY_ERROR_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyClientCashBackTestHelper.generateNotifyClientCashBackParameters;

/**
 * Тесты на метод notifyClientCashBack из NotifyClientCashBackService
 *
 * @see NotifyClientCashBackService
 */
public class NotifyClientCashBackServiceTest {

    private static BalanceClientResponse errorResponse;
    private static BalanceClientResponse successResponse;

    @Mock
    private ClientService clientService;

    @Mock
    private NotifyClientCashBackValidationService validationService;

    @Mock
    private CashbackNotificationsService cashbackNotificationsService;

    private NotifyClientCashBackService notifyClientCashBackService;
    private NotifyClientCashBackParameters updateRequest;
    private ClientId clientId;

    @BeforeClass
    public static void initTestData() {
        errorResponse = BalanceClientResponse.criticalError("some error");
        successResponse = BalanceClientResponse.success();
    }


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        notifyClientCashBackService = spy(new NotifyClientCashBackService(
                clientService,
                validationService,
                cashbackNotificationsService));
        updateRequest = generateNotifyClientCashBackParameters();
        clientId = ClientId.fromLong(updateRequest.getClientId());
    }

    @Test
    public void checkReturnSuccessResponse() {
        Client client = new Client().withId(clientId.asLong()).withWorkCurrency(CurrencyCode.RUB);
        doReturn(client).when(clientService).getClient(clientId);

        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
        verify(cashbackNotificationsService, times(1))
                .addSmsNotification(clientId, updateRequest.getCashbackConsumedBonus(), CurrencyCode.RUB);
    }

    @Test
    public void checkReturnSuccessResponse_whenGotNegativeConsumedBonus() {
        Client client = new Client().withId(clientId.asLong()).withWorkCurrency(CurrencyCode.RUB);
        doReturn(client).when(clientService).getClient(clientId);
        updateRequest.withCashbackConsumedBonus(BigDecimal.valueOf(-100));
        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
        verifyZeroInteractions(cashbackNotificationsService);
    }

    @Test
    public void checkReturnSuccessResponse_whenGotMicroMoneyAddedBonus() {
        Client client = new Client()
                .withId(clientId.asLong())
                .withWorkCurrency(CurrencyCode.RUB)
                .withCashBackBonus(BigDecimal.ONE);
        doReturn(client).when(clientService).getClient(clientId);
        updateRequest.withCashbackConsumedBonus(BigDecimal.ONE.add(BigDecimal.valueOf(0.1)));
        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
        verifyZeroInteractions(cashbackNotificationsService);
    }

    @Test
    public void checkReturnSuccessResponse_whenClientDoesNotExistInDb() {
        doReturn(null).when(clientService).getClient(clientId);
        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
        verifyZeroInteractions(cashbackNotificationsService);
    }

    @Test
    public void checkReturnSuccessResponse_whenGotNegativeBonus() {
        Client client = new Client().withId(clientId.asLong()).withWorkCurrency(CurrencyCode.RUB);
        doReturn(client).when(clientService).getClient(clientId);
        updateRequest.withCashBackBonus(BigDecimal.valueOf(-100));
        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);

        assertThat(balanceClientResponse, beanDiffer(successResponse));
    }

    @Test
    public void checkReturnValidationResponse_whenValidateRequestFailed() {
        doReturn(errorResponse).when(validationService).validateRequest(updateRequest);
        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);

        assertThat(balanceClientResponse, equalTo(errorResponse));
        verifyZeroInteractions(cashbackNotificationsService);
    }

    @Test
    public void checkReturnErrorResponse_whenCurrencyDoesNotMatch() {
        Client client = new Client().withId(clientId.asLong()).withWorkCurrency(CurrencyCode.USD);
        doReturn(client).when(clientService).getClient(clientId);
        doReturn(CurrencyUsd.getInstance()).when(clientService).getWorkCurrency(clientId);

        BalanceClientResponse balanceClientResponse = notifyClientCashBackService.notifyClientCashBack(updateRequest);
        String message = String.format(WRONG_CURRENCY_ERROR_MESSAGE, updateRequest.getBalanceCurrency(),
                clientId.asLong(),
                clientService.getWorkCurrency(clientId).getBalanceCurrencyName());

        assertThat(balanceClientResponse, beanDiffer(BalanceClientResponse.error(WRONG_CURRENCY_ERROR_CODE, message)));
        verifyZeroInteractions(cashbackNotificationsService);
    }

}
