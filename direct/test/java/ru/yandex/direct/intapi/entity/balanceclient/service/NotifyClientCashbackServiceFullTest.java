package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientCashBackParameters;
import ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyClientCashBackValidationService;

import static java.math.RoundingMode.DOWN;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_SERVICE_ID_ERROR_CODE;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyClientCashbackServiceFullTest {
    private static final Integer DIRECT_SERVICE = 7;
    private static final int INVALID_CASH_BACK_BONUS_ERROR_CODE = 1050;
    private static final int INVALID_CLIENT_ID_ERROR_CODE = 1051;
    private static final int INVALID_CURRENCY_ERROR_CODE = 1052;
    private static final int INVALID_CONSUMED_CASH_BACK_BONUS_ERROR_CODE = 1053;

    @Autowired
    private ClientService clientService;

    @Autowired
    private NotifyClientCashBackValidationService validationService;

    @Autowired
    private CashbackNotificationsService cashbackNotificationsService;

    @Autowired
    private Steps steps;

    @Autowired
    private CashbackSteps cashbackSteps;

    private NotifyClientCashBackService service;
    private ClientInfo clientInfo;

    @Before
    public void init() {
        service = new NotifyClientCashBackService(clientService, validationService, cashbackNotificationsService);
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void testNotifyClientCashback_success() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        var clientOptions = clientService.getClientOptions(clientInfo.getClientId());

        assertThat(clientOptions.getConsumedCashbackBonus(), equalTo(BigDecimal.TEN.setScale(2, DOWN)));
        assertThat(clientOptions.getAwaitingCashbackBonus(), equalTo(BigDecimal.TEN.setScale(2, DOWN)));
    }

    @Test
    public void testNotifyClientCashback_successOnlyAwaitingCashbackUpdated() {
        cashbackSteps.updateConsumedCashback(clientInfo.getClientId(), BigDecimal.TEN);
        var awaitingCashback = BigDecimal.valueOf(137);
        var request = new NotifyClientCashBackParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(awaitingCashback)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        var clientOptions = clientService.getClientOptions(clientInfo.getClientId());

        assertThat(clientOptions.getConsumedCashbackBonus(), equalTo(BigDecimal.TEN.setScale(2, DOWN)));
        assertThat(clientOptions.getAwaitingCashbackBonus(), equalTo(awaitingCashback.setScale(2, DOWN)));
    }

    @Test
    public void testNotifyClientCashback_invalidService() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withServiceId(137)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().getResponseCode(), equalTo(INVALID_SERVICE_ID_ERROR_CODE));
    }

    @Test
    public void testNotifyClientCashback_invalidClient() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(null)
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().getResponseCode(), equalTo(INVALID_CLIENT_ID_ERROR_CODE));
    }

    @Test
    public void testNotifyClientCashback_invalidAwaitingCashback() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(null)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().getResponseCode(), equalTo(INVALID_CASH_BACK_BONUS_ERROR_CODE));
    }

    @Test
    public void testNotifyClientCashback_invalidConsumedCashback() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashbackConsumedBonus(null);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().getResponseCode(), equalTo(INVALID_CONSUMED_CASH_BACK_BONUS_ERROR_CODE));
    }

    @Test
    public void testNotifyClientCashback_invalidCurrency() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(null)
                .withCashBackBonus(BigDecimal.TEN)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().getResponseCode(), equalTo(INVALID_CURRENCY_ERROR_CODE));
    }

    @Test
    public void testNotifyClientCashback_unknownClient() {
        var request = new NotifyClientCashBackParameters()
                .withClientId(137L)
                .withServiceId(DIRECT_SERVICE)
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name())
                .withCashBackBonus(BigDecimal.TEN)
                .withCashbackConsumedBonus(BigDecimal.TEN);
        var response = service.notifyClientCashBack(request);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
