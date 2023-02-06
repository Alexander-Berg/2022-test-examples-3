package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.AggregatedStatusRetargetingData;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.aggregatedstatuses.logic.EffectiveStatusCalculators.retargetingEffectiveStatus;
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
public class EffectiveStatusCalculatorsRetargetingsTest {
    private SelfStatus effectiveStatus;

    @Parameterized.Parameter
    public AggregatedStatusRetargetingData retargetingStatusData;

    @Parameterized.Parameter(1)
    public AggregatedStatusAdGroupData adGroupStatusData;

    @Parameterized.Parameter(2)
    public GdSelfStatusEnum expectedStatus;

    @Parameterized.Parameter(3)
    public List<GdSelfStatusReason> expectedReasons;

    @Parameterized.Parameters(name = "{2}: {3}")
    public static Object[][] params() {
        return new Object[][]{
                // no parent statuses -> returns child
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(null, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(RUN_WARN, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                // if statuses are the same, merging works, and meaningless statuses are dropped
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(RUN_OK, GdSelfStatusReason.ACTIVE),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                // DRAFT and ARCHIVED on parent masks child statuses
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},
                {retargeting(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(DRAFT, GdSelfStatusReason.DRAFT),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},

                // any stop on parent masks child status
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(STOP_WARN, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_WARN, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {retargeting(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(STOP_OK, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER, GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {retargeting(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},

                // any pause on parent masks child status
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_OK, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {retargeting(RUN_OK, GdSelfStatusReason.ACTIVE),
                        adGroup(PAUSE_WARN, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_WARN, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {retargeting(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(PAUSE_CRIT, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER,
                        GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},

                // Для STOP_* не пишем ризон про остановку по ТТ
                {retargeting(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {retargeting(STOP_WARN, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_WARN, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {retargeting(STOP_CRIT, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adGroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
        };
    }

    @Before
    public void getEffectiveStatus() {
        effectiveStatus = retargetingEffectiveStatus(retargetingStatusData, adGroupStatusData);
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

    private static AggregatedStatusRetargetingData retargeting(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusRetargetingData(status, reason)
                : new AggregatedStatusRetargetingData(status);
    }

    private static AggregatedStatusAdGroupData adGroup(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusAdGroupData(status, reason)
                : new AggregatedStatusAdGroupData(status);
    }
}
