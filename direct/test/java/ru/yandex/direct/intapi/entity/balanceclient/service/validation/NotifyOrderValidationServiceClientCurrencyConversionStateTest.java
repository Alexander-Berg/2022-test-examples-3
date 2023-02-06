package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.service.ClientCurrencyConversionTeaserService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BANANA_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse.errorLocked;
import static ru.yandex.direct.intapi.entity.balanceclient.model.BalanceClientResult.INTERNAL_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.CLIENT_IS_GOING_TO_CURRENCY_CONVERT_SOON_WILL_ACCEPT_NOTIFICATIONS_AFTER_ITS_DONE_MESSAGE;

public class NotifyOrderValidationServiceClientCurrencyConversionStateTest {

    private NotifyOrderValidationService notifyOrderValidationService;
    private Long clientId;

    @Mock
    private ClientCurrencyConversionTeaserService clientCurrencyConversionTeaserService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        notifyOrderValidationService = new NotifyOrderValidationService(clientCurrencyConversionTeaserService,
                DIRECT_SERVICE_ID, BAYAN_SERVICE_ID, BANANA_SERVICE_ID);
        clientId = RandomNumberUtils.nextPositiveLong();
    }


    @Test
    public void checkValidateClientCurrencyConversionStateForConvertingClient() {
        when(clientCurrencyConversionTeaserService.isClientConvertingSoon(eq(ClientId.fromLong(clientId))))
                .thenReturn(true);

        BalanceClientResponse response = notifyOrderValidationService.validateClientCurrencyConversionState(clientId);
        String expectedMessage =
                format(CLIENT_IS_GOING_TO_CURRENCY_CONVERT_SOON_WILL_ACCEPT_NOTIFICATIONS_AFTER_ITS_DONE_MESSAGE,
                        clientId);
        assertThat("получили ожидаемый ответ", response, beanDiffer(errorLocked(INTERNAL_ERROR_CODE, expectedMessage)));
    }

    @Test
    public void checkValidateClientCurrencyConversionStateForNotConvertingClient() {
        when(clientCurrencyConversionTeaserService.isClientConvertingSoon(eq(ClientId.fromLong(clientId))))
                .thenReturn(false);

        BalanceClientResponse response = notifyOrderValidationService.validateClientCurrencyConversionState(clientId);
        assertThat("получили null в ответе", response, nullValue());
    }
}
