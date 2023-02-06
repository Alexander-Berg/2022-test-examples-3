package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientCashBackParameters;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyClientCashBackValidationService.INVALID_CASH_BACK_BONUS_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyClientCashBackValidationService.INVALID_CLIENT_ID_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyClientCashBackValidationService.INVALID_CONSUMED_CASH_BACK_BONUS_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyClientCashBackValidationService.INVALID_CURRENCY_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_SERVICE_ID_ERROR_CODE;

@RunWith(Parameterized.class)
public class NotifyClientCashBackValidationServiceTest {

    private static final int INVALID_SERVICE_ID = 123;
    private static final int VALID_SERVICE_ID = DIRECT_SERVICE_ID;
    private static final Long INVALID_CLIENT_ID = -123L;
    private static final Long VALID_CLIENT_ID = 123L;

    private static final String VALID_CURRENCY = CurrencyCode.RUB.name();
    private static final String INVALID_CURRENCY = "ABC";

    private static final BigDecimal POSITIVE_BONUS = BigDecimal.TEN;
    private static final BigDecimal NEGATIVE_BONUS = BigDecimal.TEN.negate();
    private static final BigDecimal ZERO_BONUS = BigDecimal.ZERO;


    private final NotifyClientCashBackValidationService validationService =
            new NotifyClientCashBackValidationService(VALID_SERVICE_ID);

    @Parameterized.Parameter()
    public Integer serviceId;

    @Parameterized.Parameter(1)
    public Long clientId;

    @Parameterized.Parameter(2)
    public BigDecimal bonus;

    @Parameterized.Parameter(3)
    public BigDecimal consumedBonus;

    @Parameterized.Parameter(4)
    public String currency;

    @Parameterized.Parameter(5)
    public boolean expectError;

    @Parameterized.Parameter(6)
    public Integer expectErrorCode;

    @Parameterized.Parameters(name = "serviceId={0}, clientId={1}, bonus={2}, consumedBonus={3}, currency={4}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {INVALID_SERVICE_ID, INVALID_CLIENT_ID, POSITIVE_BONUS, POSITIVE_BONUS, VALID_CURRENCY, true,
                        INVALID_SERVICE_ID_ERROR_CODE},
                {INVALID_SERVICE_ID, VALID_CLIENT_ID, POSITIVE_BONUS, POSITIVE_BONUS, VALID_CURRENCY, true,
                        INVALID_SERVICE_ID_ERROR_CODE},
                {VALID_SERVICE_ID, INVALID_CLIENT_ID, POSITIVE_BONUS, POSITIVE_BONUS, VALID_CURRENCY, true,
                        INVALID_CLIENT_ID_ERROR_CODE},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, null, POSITIVE_BONUS, VALID_CURRENCY, true,
                        INVALID_CASH_BACK_BONUS_ERROR_CODE},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, POSITIVE_BONUS, POSITIVE_BONUS, INVALID_CURRENCY, true,
                        INVALID_CURRENCY_ERROR_CODE},

                {VALID_SERVICE_ID, VALID_CLIENT_ID, POSITIVE_BONUS, null, VALID_CURRENCY, true,
                        INVALID_CONSUMED_CASH_BACK_BONUS_ERROR_CODE},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, POSITIVE_BONUS, POSITIVE_BONUS, VALID_CURRENCY, false, null},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, NEGATIVE_BONUS, POSITIVE_BONUS, VALID_CURRENCY, false, null},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, ZERO_BONUS, POSITIVE_BONUS, VALID_CURRENCY, false, null},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, POSITIVE_BONUS, NEGATIVE_BONUS, VALID_CURRENCY, false, null},
                {VALID_SERVICE_ID, VALID_CLIENT_ID, POSITIVE_BONUS, ZERO_BONUS, VALID_CURRENCY, false, null},
        });
    }

    @Test
    public void checkValidateCampaignType() {
        NotifyClientCashBackParameters parameters = new NotifyClientCashBackParameters()
                .withServiceId(serviceId)
                .withClientId(clientId)
                .withBalanceCurrency(currency)
                .withCashBackBonus(bonus)
                .withCashbackConsumedBonus(consumedBonus);

        BalanceClientResponse response = validationService.validateRequest(parameters);

        if (expectError) {
            assertThat("получили ожидаемый код ошибки", response.getBody().getResponseCode(), equalTo(expectErrorCode));
        } else {
            assertThat("получили null в ответе", response, nullValue());
        }
    }

}
