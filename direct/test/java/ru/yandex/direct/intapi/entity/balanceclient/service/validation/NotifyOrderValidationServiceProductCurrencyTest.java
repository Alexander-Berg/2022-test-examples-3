package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BANANA_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse.criticalError;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.NO_PRODUCT_CURRENCY_GIVEN_FOR_DIRECT_CAMPAIGN_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.OUR_PRODUCT_CURRENCY_DOESNT_MATCH_BALANCE_PRODUCT_CURRENCY_FOR_CAMPAIGN_MESSAGE;

@RunWith(Parameterized.class)
public class NotifyOrderValidationServiceProductCurrencyTest {

    private static final Long CAMPAIGN_ID = 42134123446L;

    private NotifyOrderValidationService notifyOrderValidationService;

    @Parameterized.Parameter()
    public Integer serviceId;

    @Parameterized.Parameter(1)
    public String productCurrency;

    @Parameterized.Parameter(2)
    public CurrencyCode dbProductCurrency;

    @Parameterized.Parameter(3)
    public String expectedErrorMessage;

    private static String getNotMatchBalanceProductCurrencyErrorMessage(String productCurrency,
                                                                        CurrencyCode dbProductCurrency) {
        return format(OUR_PRODUCT_CURRENCY_DOESNT_MATCH_BALANCE_PRODUCT_CURRENCY_FOR_CAMPAIGN_MESSAGE,
                dbProductCurrency, productCurrency, CAMPAIGN_ID);
    }

    @Parameterized.Parameters(name = "serviceId={0}, productCurrency={1}, dbProductCurrency={2}, expectedMessage={3}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {DIRECT_SERVICE_ID, null, CurrencyCode.RUB,
                        format(NO_PRODUCT_CURRENCY_GIVEN_FOR_DIRECT_CAMPAIGN_MESSAGE, CAMPAIGN_ID)},

                {DIRECT_SERVICE_ID, "", CurrencyCode.BYN, null},
                {DIRECT_SERVICE_ID, "", CurrencyCode.YND_FIXED, null},

                {DIRECT_SERVICE_ID, "", CurrencyCode.RUB,
                        getNotMatchBalanceProductCurrencyErrorMessage("", CurrencyCode.RUB)},
                {DIRECT_SERVICE_ID, "invalid_currency", CurrencyCode.YND_FIXED,
                        getNotMatchBalanceProductCurrencyErrorMessage("invalid_currency", CurrencyCode.YND_FIXED)},
                {DIRECT_SERVICE_ID, CurrencyCode.RUB.name(), CurrencyCode.YND_FIXED,
                        getNotMatchBalanceProductCurrencyErrorMessage(CurrencyCode.RUB.name(), CurrencyCode.YND_FIXED)},

                {DIRECT_SERVICE_ID, CurrencyCode.KZT.name(), CurrencyCode.KZT, null},
                {DIRECT_SERVICE_ID, "", CurrencyCode.KZT, null},

                //Для баяна всегда возвращаем null
                {BAYAN_SERVICE_ID, null, null, null},
                {BAYAN_SERVICE_ID, CurrencyCode.RUB.name(), CurrencyCode.YND_FIXED, null},
        });
    }

    @Before
    public void before() {
        notifyOrderValidationService = new NotifyOrderValidationService(null,
                DIRECT_SERVICE_ID, BAYAN_SERVICE_ID, BANANA_SERVICE_ID);
    }


    @Test
    public void checkValidateProductCurrency() {
        BalanceClientResponse response = notifyOrderValidationService
                .validateProductCurrency(serviceId, productCurrency, dbProductCurrency, CAMPAIGN_ID);

        if (expectedErrorMessage != null) {
            assertThat("получили ожидаемый ответ", response, beanDiffer(criticalError(expectedErrorMessage)));
        } else {
            assertThat("получили null в ответе", response, nullValue());
        }
    }
}
