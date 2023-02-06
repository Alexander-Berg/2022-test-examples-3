package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BANANA_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse.error;
import static ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse.success;
import static ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters.TOTAL_SUM_FIELD_NAME;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.CAMPAIGN_IS_EMPTY_CANT_UPDATE_SUM_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.CAMPAIGN_IS_EMPTY_SKIPPING_ZERO_SUM_BUT_ACCEPTING_NOTIFICATION_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_FIELD_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INVALID_TOTAL_SUM_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.SUM_ON_EMPTY_CAMPAIGN_ID_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.invalidFieldMessage;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.validateCampaignTid;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.validateTotalSum;

public class NotifyOrderValidationServiceMethodTest {

    private NotifyOrderValidationService validationService;

    @Before
    public void before() {
        validationService = new NotifyOrderValidationService(null,
                DIRECT_SERVICE_ID, BAYAN_SERVICE_ID, BANANA_SERVICE_ID);
    }

    @Test
    public void checkInvalidFieldMessageForNotNullValue() {
        BigDecimal someValue = RandomNumberUtils.nextPositiveBigDecimal();
        String someField = "someField";
        String expectedMessage = String.format(INVALID_FIELD_MESSAGE, someField, someValue);

        assertThat("получили ожидаемый текст ошибки",
                invalidFieldMessage(someField, someValue), equalTo(expectedMessage));
    }

    @Test
    public void checkInvalidFieldMessageForNullValue() {
        String someField = "some_field";
        String expectedMessage = String.format(INVALID_FIELD_MESSAGE, someField, "undef");

        assertThat("получили ожидаемый текст ошибки", invalidFieldMessage(someField, null), equalTo(expectedMessage));
    }

    @Test
    public void checkValidateCampaignTidGreaterThanNotifyOrderParameterTid() {
        NotifyOrderParameters notifyOrderParameters = new NotifyOrderParameters()
                .withTid(RandomNumberUtils.nextPositiveLong());
        Long campaignBalanceTid = notifyOrderParameters.getTid() + 1;

        BalanceClientResponse response = validateCampaignTid(notifyOrderParameters, campaignBalanceTid);
        assertThat("получили ожидаемый ответ", response, beanDiffer(BalanceClientResponse.success()));
    }

    @Test
    public void checkValidateCampaignTidEqualNotifyOrderParameterTid() {
        NotifyOrderParameters notifyOrderParameters = new NotifyOrderParameters()
                .withTid(RandomNumberUtils.nextPositiveLong());
        Long campaignBalanceTid = notifyOrderParameters.getTid();

        BalanceClientResponse response = validateCampaignTid(notifyOrderParameters, campaignBalanceTid);
        assertThat("получили null в ответе", response, nullValue());
    }

    @Test
    public void checkValidateCampaignTidLessThanNotifyOrderParameterTid() {
        NotifyOrderParameters notifyOrderParameters = new NotifyOrderParameters()
                .withTid(RandomNumberUtils.nextPositiveLong());
        Long campaignBalanceTid = notifyOrderParameters.getTid() - 1;

        BalanceClientResponse response = validateCampaignTid(notifyOrderParameters, campaignBalanceTid);
        assertThat("получили null в ответе", response, nullValue());
    }

    @Test
    public void checkValidateSumOnEmptyCampaign() {
        Money sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CurrencyCode.RUB);
        Long campaignId = RandomNumberUtils.nextPositiveLong();

        BalanceClientResponse response = validationService.validateSumOnEmptyCampaign(false, sum, campaignId);
        assertThat("получили null в ответе", response, nullValue());
    }

    @Test
    public void checkValidateSumGreaterThanZeroOnNotEmptyCampaign() {
        Money sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CurrencyCode.RUB);
        Long campaignId = RandomNumberUtils.nextPositiveLong();

        BalanceClientResponse response = validationService.validateSumOnEmptyCampaign(true, sum, campaignId);

        String expectedMessage = format(CAMPAIGN_IS_EMPTY_CANT_UPDATE_SUM_MESSAGE, campaignId, sum.bigDecimalValue());
        assertThat("получили ожидаемый ответ", response,
                beanDiffer(error(SUM_ON_EMPTY_CAMPAIGN_ID_ERROR_CODE, expectedMessage)));
    }

    @Test
    public void checkValidateZeroSumOnNotEmptyCampaign() {
        Money sum = Money.valueOf(BigDecimal.ZERO, CurrencyCode.RUB);
        Long campaignId = RandomNumberUtils.nextPositiveLong();

        BalanceClientResponse response = validationService.validateSumOnEmptyCampaign(true, sum, campaignId);

        String expectedMessage =
                format(CAMPAIGN_IS_EMPTY_SKIPPING_ZERO_SUM_BUT_ACCEPTING_NOTIFICATION_MESSAGE, campaignId);
        assertThat("получили ожидаемый ответ", response, beanDiffer(success(expectedMessage)));
    }

    @Test
    public void checkValidateNegativeSumOnNotEmptyCampaign() {
        Money sum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal().negate(), CurrencyCode.RUB);
        Long campaignId = RandomNumberUtils.nextPositiveLong();

        BalanceClientResponse response = validationService.validateSumOnEmptyCampaign(true, sum, campaignId);

        String expectedMessage =
                format(CAMPAIGN_IS_EMPTY_SKIPPING_ZERO_SUM_BUT_ACCEPTING_NOTIFICATION_MESSAGE, campaignId);
        assertThat("получили ожидаемый ответ", response, beanDiffer(success(expectedMessage)));
    }

    @Test
    public void checkValidateTotalSumGreaterThanZero() {
        BigDecimal totalSum = RandomNumberUtils.nextPositiveBigDecimal();

        BalanceClientResponse response = validateTotalSum(totalSum);
        assertThat("получили null в ответе", response, nullValue());
    }

    @Test
    public void checkValidateZeroTotalSum() {
        BigDecimal totalSum = BigDecimal.ZERO;

        BalanceClientResponse response = validateTotalSum(totalSum);
        assertThat("получили null в ответе", response, nullValue());
    }

    @Test
    public void checkValidateTotalSumLessThanZero() {
        BigDecimal totalSum = RandomNumberUtils.nextPositiveBigDecimal().negate();

        BalanceClientResponse response = validateTotalSum(totalSum);
        assertThat("получили ожидаемый ответ", response,
                beanDiffer(error(INVALID_TOTAL_SUM_ERROR_CODE, invalidFieldMessage(TOTAL_SUM_FIELD_NAME, totalSum))));
    }

    @Test
    public void checkValidateNullTotalSum() {
        BigDecimal totalSum = null;
        BalanceClientResponse response = validateTotalSum(totalSum);

        assertThat("получили ожидаемый ответ", response,
                beanDiffer(error(INVALID_TOTAL_SUM_ERROR_CODE, invalidFieldMessage(TOTAL_SUM_FIELD_NAME, totalSum))));
    }
}
