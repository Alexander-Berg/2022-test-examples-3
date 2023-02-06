package ru.yandex.direct.core.entity.campaign;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.ACTIVE_ORDERS_MONEY_OUT_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.CAMP_FINISHED_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.MODERATE_RESULT_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_METRICA_CONTROL_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_ORDER_MONEY_IN_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.PAUSED_BY_DAY_BUDGET_SMS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class GetAvailableSmsFlagsTest {

    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData() {
        return new Object[][]{
                {true, false, List.of(CAMP_FINISHED_SMS, MODERATE_RESULT_SMS)},
                {true, true, List.of(CAMP_FINISHED_SMS, MODERATE_RESULT_SMS, NOTIFY_METRICA_CONTROL_SMS)},
                {false, false, List.of(CAMP_FINISHED_SMS, MODERATE_RESULT_SMS, ACTIVE_ORDERS_MONEY_OUT_SMS,
                        NOTIFY_ORDER_MONEY_IN_SMS, PAUSED_BY_DAY_BUDGET_SMS)},
                {false, true, List.of(CAMP_FINISHED_SMS, MODERATE_RESULT_SMS, NOTIFY_METRICA_CONTROL_SMS,
                        ACTIVE_ORDERS_MONEY_OUT_SMS, NOTIFY_ORDER_MONEY_IN_SMS, PAUSED_BY_DAY_BUDGET_SMS)},
        };
    }


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("isCampaignUnderWallet = {0}, isCampaignWithSiteMonitoringSmsEvent = {1}, expectedSmsEvents = {2}")
    public void checkGetAvailableSmsFlags(boolean isCampaignUnderWallet,
                                          boolean isCampaignWithSiteMonitoringSmsEvent,
                                          List<SmsFlag> expectedSmsEvents) {
        Set<SmsFlag> availableSmsFlags = CampaignNotificationUtils
                .getAvailableSmsFlags(isCampaignUnderWallet, isCampaignWithSiteMonitoringSmsEvent);

        assertThat(availableSmsFlags)
                .is(matchedBy(beanDiffer(Set.copyOf(expectedSmsEvents))));
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("isCampaignUnderWallet = {0}, isCampaignWithSiteMonitoringSmsEvent = {1}, expectedSmsEvents = {2}")
    public void checkGetAvailableSmsFlags_withCampaignTypeParameter(boolean isCampaignUnderWallet,
                                                                    boolean isCampaignWithSiteMonitoringSmsEvent,
                                                                    List<SmsFlag> expectedSmsEvents) {
        CampaignType campaignType = isCampaignWithSiteMonitoringSmsEvent
                ? CampaignType.TEXT
                : CampaignType.CONTENT_PROMOTION;
        Set<SmsFlag> availableSmsFlags = CampaignNotificationUtils
                .getAvailableSmsFlags(isCampaignUnderWallet, campaignType);

        assertThat(availableSmsFlags)
                .is(matchedBy(beanDiffer(Set.copyOf(expectedSmsEvents))));
    }
}
