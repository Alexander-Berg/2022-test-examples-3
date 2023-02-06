package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.aggrstatus.AggregatedStatusAdGroup;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.aggregatedstatuses.logic.EffectiveStatusCalculators.adGroupEffectiveStatus;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_WARN;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@RunWith(Parameterized.class)
public class EffectiveStatusCalculatorsAdGroupsTest {
    private SelfStatus effectiveStatus;

    @Parameterized.Parameter
    public AggregatedStatusAdGroupData adgroupStatusData;

    @Parameterized.Parameter(1)
    public AggregatedStatusCampaignData campaignStatusData;

    @Parameterized.Parameter(2)
    public GdSelfStatusEnum expectedStatus;

    @Parameterized.Parameter(3)
    public List<GdSelfStatusReason> expectedReasons;

    @Parameterized.Parameters(name = "{2}: {3}")
    public static Object[][] params() {
        // not all variants a production possible
        return new Object[][] {
                // no parent statuses -> returns child
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(null, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(RUN_WARN, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                // child archived, masks parent status
                {adg(ARCHIVED, GdSelfStatusReason.ARCHIVED), campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},

                // if statuses the same, mergin works, and meaningless statuses droped
                {adg(STOP_CRIT, GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING), campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING, GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {adg(DRAFT, GdSelfStatusReason.ON_MODERATION), campaign(DRAFT, GdSelfStatusReason.DRAFT),
                        DRAFT, List.of(GdSelfStatusReason.ON_MODERATION)},


                // DRAFT and ARCHIVED on parent masks child statuses
                {adg(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER), campaign(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},
                {adg(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER), campaign(DRAFT, GdSelfStatusReason.DRAFT),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},

                // DRAFT wont inherit parent status
                {adg(DRAFT, GdSelfStatusReason.ADGROUP_HAS_ADS_ON_MODERATION),
                        campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_HAS_ADS_ON_MODERATION)},
                {adg(DRAFT, GdSelfStatusReason.ADGROUP_HAS_ADS_ON_MODERATION),
                        campaign(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},


                // all stop on parent mask child status
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(STOP_WARN, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_WARN, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(STOP_OK, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {adg(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER), campaign(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER, GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},


                // all pause on parent mask child status
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_OK, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(PAUSE_WARN, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_WARN, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(PAUSE_CRIT, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},

                // Для STOP_* не пишем ризон про остановку по ТТ
                {adg(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        campaign(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {adg(STOP_WARN, GdSelfStatusReason.SUSPENDED_BY_USER),
                        campaign(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_WARN, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {adg(STOP_CRIT, GdSelfStatusReason.SUSPENDED_BY_USER),
                        campaign(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},

                // RUN_WARN by rejected promoaction lowers from campaign
                {adg(RUN_OK, GdSelfStatusReason.ACTIVE), campaign(RUN_WARN, GdSelfStatusReason.PROMO_EXTENSION_REJECTED),
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED)},
                {adg(RUN_OK, null), campaign(RUN_WARN, GdSelfStatusReason.PROMO_EXTENSION_REJECTED),
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED)},
        };
    }

    @Before
    public void getEffectiveStatus() {
        AggregatedStatusAdGroup aggregatedStatusAdGroup =
                new AggregatedStatusAdGroup().withAggregatedStatus(adgroupStatusData);
        effectiveStatus = adGroupEffectiveStatus(Instant.now(), aggregatedStatusAdGroup, campaignStatusData);
    }

    @Test
    public void reasonsMatch() {
        assumeThat("reasons amount match", effectiveStatus.getReasons(), hasSize(expectedReasons.size()));
        assertThat("reason match", effectiveStatus.getReasons(), containsInAnyOrder(expectedReasons.toArray()));
    }

    @Test
    public void statusMatch() {
        assertEquals("status match", expectedStatus, effectiveStatus.getStatus());
    }

    private static AggregatedStatusAdGroupData adg(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusAdGroupData(status, reason)
                : new AggregatedStatusAdGroupData(status);
    }

    private static AggregatedStatusCampaignData campaign(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusCampaignData(status, reason)
                : new AggregatedStatusCampaignData(status);
    }
}
