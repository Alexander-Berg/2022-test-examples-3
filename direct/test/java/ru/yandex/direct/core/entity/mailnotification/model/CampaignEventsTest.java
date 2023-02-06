package ru.yandex.direct.core.entity.mailnotification.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignEventsTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String TEXT_FOR_END_DATE_NULL_VALUE = "<не указана>";

    private Long operatorUid;
    private Long ownerUid;
    private Long campaignId;

    @Before
    public void initTestData() {
        operatorUid = RandomNumberUtils.nextPositiveLong();
        ownerUid = RandomNumberUtils.nextPositiveLong();
        campaignId = RandomNumberUtils.nextPositiveLong();
    }


    @Test
    public void checkChangedStartDateCampaignEvent() {
        LocalDate oldValue = LocalDate.now();
        LocalDate newValue = oldValue.plusDays(3);
        CampaignEvent<String> campaignEvent =
                CampaignEvent.changedStartDateEvent(operatorUid, ownerUid, campaignId, oldValue, newValue);

        var expectedMailNotificationEvent = getExpectedMailNotificationEvent(EventType.C_START,
                oldValue.format(DATE_FORMATTER), newValue.format(DATE_FORMATTER));
        assertThat(campaignEvent.toMailNotificationEvent())
                .is(matchedBy(beanDiffer(expectedMailNotificationEvent)));
    }

    @Test
    @Parameters(method = "parametrizedTestData_forChangedEndDateEvent")
    @TestCaseName("oldValue = {0}, newValue = {1}, expectedOldValue = {2}, expectedNewValue = {3}")
    public void checkChangedEndDateCampaignEvent(@Nullable LocalDate oldValue, @Nullable LocalDate newValue,
                                                 String expectedOldValue, String expectedNewValue) {
        CampaignEvent<String> campaignEvent = CampaignEvent.changedEndDateEvent(operatorUid, ownerUid, campaignId,
                TEXT_FOR_END_DATE_NULL_VALUE, oldValue, newValue);

        var expectedMailNotificationEvent =
                getExpectedMailNotificationEvent(EventType.C_FINISH, expectedOldValue, expectedNewValue);
        assertThat(campaignEvent.toMailNotificationEvent())
                .is(matchedBy(beanDiffer(expectedMailNotificationEvent)));
    }

    @Test
    @Parameters(method = "parametrizedTestData_forChangedDayBudgetEvent")
    @TestCaseName("oldValue = {0}, newValue = {1}, currency = {2}, expectedOldValue = {3}, expectedNewValue = {4}")
    public void checkChangedDayBudgetCampaignEvent(BigDecimal oldValue, BigDecimal newValue, CurrencyCode currency,
                                                   String expectedOldValue, String expectedNewValue) {
        CampaignEvent<String> campaignEvent = CampaignEvent.changedDayBudgetEvent(operatorUid, ownerUid, campaignId,
                currency, oldValue, newValue);

        var expectedMailNotificationEvent = getExpectedMailNotificationEvent(EventType.C_DAY_BUDGET_MULTICURRENCY,
                expectedOldValue, expectedNewValue);
        assertThat(campaignEvent.toMailNotificationEvent())
                .is(matchedBy(beanDiffer(expectedMailNotificationEvent)));
    }


    private MailNotificationEvent getExpectedMailNotificationEvent(EventType eventType,
                                                                   String expectedOldValue, String expectedNewValue) {
        return new MailNotificationEvent()
                .withOperatorUid(operatorUid)
                .withOwnerUid(ownerUid)
                .withCampaignId(campaignId)
                .withObjectId(campaignId)
                .withEventType(eventType)
                .withObjectType(ObjectType.CAMP)
                .withJsonData(format("{\"old_text\":\"%s\",\"new_text\":\"%s\"}", expectedOldValue, expectedNewValue));
    }

    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData_forChangedEndDateEvent() {
        LocalDate someDate = LocalDate.of(2019, 9, 11);
        LocalDate nextDate = someDate.plusDays(1);

        return new Object[][]{
                {null, someDate, TEXT_FOR_END_DATE_NULL_VALUE, someDate.format(DATE_FORMATTER)},
                {someDate, null, someDate.format(DATE_FORMATTER), TEXT_FOR_END_DATE_NULL_VALUE},
                {someDate, nextDate, someDate.format(DATE_FORMATTER), nextDate.format(DATE_FORMATTER)},
        };
    }

    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData_forChangedDayBudgetEvent() {
        BigDecimal someValue = BigDecimal.valueOf(123.45);
        return new Object[][]{
                {BigDecimal.ZERO, BigDecimal.TEN, CurrencyCode.RUB, "0", "10:RUB"},
                {BigDecimal.TEN, someValue, CurrencyCode.RUB, "10:RUB", "123.45:RUB"},
                {someValue, BigDecimal.ZERO, CurrencyCode.KZT, "123.45:KZT", "0"},
        };
    }

}
