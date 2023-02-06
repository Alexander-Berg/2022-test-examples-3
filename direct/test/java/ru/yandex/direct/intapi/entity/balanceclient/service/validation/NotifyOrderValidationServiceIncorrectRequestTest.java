package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.client.service.ClientCurrencyConversionTeaserService;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BANANA_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse.error;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.CAMPAIGN_ID_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.CHIPS_COST_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.CHIPS_SPENT_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.SUM_REAL_MONEY_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.SUM_UNITS_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.TID_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderTestHelper.generateNotifyOrderParameters;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_CAMPAIGN_ID_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_CAMPAIGN_ID_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_CHIPS_COST_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_CHIPS_SPENT_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_SERVICE_ID_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_SERVICE_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_SUM_REAL_MONEY_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_SUM_UNITS_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_TID_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.NO_VALID_TID_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.invalidFieldMessage;

@RunWith(Parameterized.class)
public class NotifyOrderValidationServiceIncorrectRequestTest {

    private NotifyOrderValidationService notifyOrderValidationService;
    private NotifyOrderParameters notifyOrderParameters;

    @Parameterized.Parameter()
    public Consumer<NotifyOrderParameters> parametersConsumer;

    @Parameterized.Parameter(1)
    public int expectedErrorCode;

    @Parameterized.Parameter(2)
    public String expectedMessage;

    @Parameterized.Parameters(name = "expectedErrorCode = {1}, expectedMessage = {2}")
    public static Collection<Object[]> data() {
        Integer invalidServiceId = 123;
        // random vals
        BigDecimal negativeBigDecimalValue = new BigDecimal("-41235243.12387984");
        Long negativeLongValue = -234354223455L;
        Long zeroCampaignId = 0L;

        return asList(new Object[][]{
                //Проверка serviceId
                {(Consumer<NotifyOrderParameters>) p -> p.withServiceId(null),
                        INVALID_SERVICE_ID_ERROR_CODE,
                        format(INVALID_SERVICE_MESSAGE, "null")},
                {(Consumer<NotifyOrderParameters>) p -> p.withServiceId(invalidServiceId),
                        INVALID_SERVICE_ID_ERROR_CODE,
                        format(INVALID_SERVICE_MESSAGE, invalidServiceId)},

                //Проверка sumUnits
                {(Consumer<NotifyOrderParameters>) p -> p.withSumUnits(null),
                        INVALID_SUM_UNITS_ERROR_CODE,
                        invalidFieldMessage(SUM_UNITS_FIELD_NAME, null)},
                {(Consumer<NotifyOrderParameters>) p -> p.withSumUnits(negativeBigDecimalValue),
                        INVALID_SUM_UNITS_ERROR_CODE,
                        invalidFieldMessage(SUM_UNITS_FIELD_NAME, negativeBigDecimalValue)},

                //Проверка chipsCost
                {(Consumer<NotifyOrderParameters>) p -> p.withChipsCost(negativeBigDecimalValue),
                        INVALID_CHIPS_COST_ERROR_CODE,
                        invalidFieldMessage(CHIPS_COST_FIELD_NAME, negativeBigDecimalValue)},

                //Проверка sumRealMoney
                {(Consumer<NotifyOrderParameters>) p -> p.withSumRealMoney(negativeBigDecimalValue),
                        INVALID_SUM_REAL_MONEY_ERROR_CODE,
                        invalidFieldMessage(SUM_REAL_MONEY_FIELD_NAME, negativeBigDecimalValue)},

                //Проверка chipsSpent
                {(Consumer<NotifyOrderParameters>) p -> p.withChipsSpent(negativeBigDecimalValue),
                        INVALID_CHIPS_SPENT_ERROR_CODE,
                        invalidFieldMessage(CHIPS_SPENT_FIELD_NAME, negativeBigDecimalValue)},

                //Проверка tid
                {(Consumer<NotifyOrderParameters>) p -> p.withTid(null),
                        INVALID_TID_ERROR_CODE,
                        format(NO_VALID_TID_MESSAGE, TID_FIELD_NAME)},

                //Проверка campaignId
                {(Consumer<NotifyOrderParameters>) p -> p.withCampaignId(null),
                        INVALID_CAMPAIGN_ID_ERROR_CODE,
                        format(INVALID_CAMPAIGN_ID_MESSAGE, CAMPAIGN_ID_FIELD_NAME, null)},
                {(Consumer<NotifyOrderParameters>) p -> p.withCampaignId(negativeLongValue),
                        INVALID_CAMPAIGN_ID_ERROR_CODE,
                        format(INVALID_CAMPAIGN_ID_MESSAGE, CAMPAIGN_ID_FIELD_NAME, negativeLongValue)},
                {(Consumer<NotifyOrderParameters>) p -> p.withCampaignId(zeroCampaignId),
                        INVALID_CAMPAIGN_ID_ERROR_CODE,
                        format(INVALID_CAMPAIGN_ID_MESSAGE, CAMPAIGN_ID_FIELD_NAME, zeroCampaignId)},
        });
    }

    @Before
    public void before() {
        notifyOrderValidationService = new NotifyOrderValidationService(
                mock(ClientCurrencyConversionTeaserService.class),
                DIRECT_SERVICE_ID, BAYAN_SERVICE_ID, BANANA_SERVICE_ID);

        notifyOrderParameters = generateNotifyOrderParameters();
    }


    @Test
    public void checkValidateRequest() {
        parametersConsumer.accept(notifyOrderParameters);
        BalanceClientResponse response = notifyOrderValidationService.validateRequest(notifyOrderParameters);
        assertThat("получили ожидаемый ответ", response, beanDiffer(error(expectedErrorCode, expectedMessage)));
    }
}
