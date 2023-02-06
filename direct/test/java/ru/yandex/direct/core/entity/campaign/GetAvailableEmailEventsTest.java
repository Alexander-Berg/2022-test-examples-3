package ru.yandex.direct.core.entity.campaign;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignEmailEvent;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignEmailEvent.CHECK_POSITION;
import static ru.yandex.direct.core.entity.campaign.model.CampaignEmailEvent.SEND_ACCOUNT_NEWS;
import static ru.yandex.direct.core.entity.campaign.model.CampaignEmailEvent.STOP_BY_REACH_DAILY_BUDGET;
import static ru.yandex.direct.core.entity.campaign.model.CampaignEmailEvent.WARNING_BALANCE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignEmailEvent.XLS_READY;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class GetAvailableEmailEventsTest {

    private static final Long EMPTY_ID = 0L;
    private static final Long SOME_WALLET_ID = 123L;
    private static final Long SOME_MANAGER_UID = 34L;

    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData() {
        // NB порядок элементов в expectedEmailEvents должен быть один и тот же между запусками,
        // потому что из него делается описание тест-кейса в CI, на которое описание
        // потом что-то завязывается
        return new Object[][]{
                {SOME_WALLET_ID, CampaignType.TEXT, SOME_MANAGER_UID,
                        EnumSet.of(CHECK_POSITION, STOP_BY_REACH_DAILY_BUDGET, SEND_ACCOUNT_NEWS, XLS_READY)},
                {EMPTY_ID, CampaignType.TEXT, SOME_MANAGER_UID,
                        EnumSet.of(WARNING_BALANCE, CHECK_POSITION, STOP_BY_REACH_DAILY_BUDGET, SEND_ACCOUNT_NEWS,
                                XLS_READY)},

                {EMPTY_ID, CampaignType.CONTENT_PROMOTION, SOME_MANAGER_UID,
                        EnumSet.of(WARNING_BALANCE, STOP_BY_REACH_DAILY_BUDGET, SEND_ACCOUNT_NEWS)},

                {SOME_WALLET_ID, CampaignType.TEXT, EMPTY_ID,
                        EnumSet.of(CHECK_POSITION, STOP_BY_REACH_DAILY_BUDGET, XLS_READY)},
                {SOME_WALLET_ID, CampaignType.TEXT, null,
                        EnumSet.of(CHECK_POSITION, STOP_BY_REACH_DAILY_BUDGET, XLS_READY)},

                {EMPTY_ID, CampaignType.INTERNAL_AUTOBUDGET, null,
                        EnumSet.of(XLS_READY, WARNING_BALANCE, CHECK_POSITION, STOP_BY_REACH_DAILY_BUDGET)},
                {SOME_WALLET_ID, CampaignType.INTERNAL_AUTOBUDGET, null,
                        EnumSet.of(XLS_READY, WARNING_BALANCE, CHECK_POSITION, STOP_BY_REACH_DAILY_BUDGET)},

                {EMPTY_ID, CampaignType.INTERNAL_DISTRIB, null, EnumSet.of(XLS_READY, CHECK_POSITION)},
                {EMPTY_ID, CampaignType.INTERNAL_FREE, null, EnumSet.of(XLS_READY, CHECK_POSITION)},
        };
    }


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("walletId = {0}, campaignType = {1}, managerUserId = {2}, expectedEmailEvents = {3}")
    public void checkGetAvailableEmailEvents(Long walletId, CampaignType campaignType,
                                             @Nullable Long managerUserId,
                                             Set<CampaignEmailEvent> expectedEmailEvents) {
        Set<CampaignEmailEvent> availableEmailEvents =
                CampaignNotificationUtils.getAvailableEmailEvents(walletId, campaignType, managerUserId);

        // TODO перейти на containsExactlyInAnyOrderElementsOf после апгрейда assertj
        assertThat(availableEmailEvents)
                .containsExactlyInAnyOrder(expectedEmailEvents.toArray(new CampaignEmailEvent[0]));
    }

}
