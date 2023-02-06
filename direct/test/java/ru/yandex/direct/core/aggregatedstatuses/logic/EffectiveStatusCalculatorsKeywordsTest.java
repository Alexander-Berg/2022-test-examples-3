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
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.aggregatedstatuses.logic.EffectiveStatusCalculators.keywordEffectiveStatus;
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
public class EffectiveStatusCalculatorsKeywordsTest {
    private SelfStatus effectiveStatus;

    @Parameterized.Parameter
    public AggregatedStatusKeywordData keywordStatusData;

    @Parameterized.Parameter(1)
    public AggregatedStatusAdGroupData adgroupStatusData;

    @Parameterized.Parameter(2)
    public GdSelfStatusEnum expectedStatus;

    @Parameterized.Parameter(3)
    public List<GdSelfStatusReason> expectedReasons;

    @Parameterized.Parameters(name = "{2}: {3}")
    public static Object[][] params() {
        return new Object[][] {
                // no parent statuses -> returns child
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(null, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(RUN_WARN, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                // child archived, masks parent status
                {keyword(ARCHIVED, GdSelfStatusReason.ARCHIVED), adgroup(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},

                // if statuses the same, merging works, and meaningless statuses dropped
                {keyword(STOP_CRIT, GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING), adgroup(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING, GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {keyword(DRAFT, GdSelfStatusReason.ON_MODERATION), adgroup(DRAFT, GdSelfStatusReason.DRAFT),
                        DRAFT, List.of(GdSelfStatusReason.ON_MODERATION)},


                // DRAFT and ARCHIVED on parent masks child statuses
                {keyword(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER), adgroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},
                {keyword(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER), adgroup(DRAFT, GdSelfStatusReason.DRAFT),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},

                // DRAFT wont inherit parent status
                {keyword(DRAFT, null),
                        adgroup(STOP_CRIT, GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING),
                        DRAFT, null},
                {keyword(DRAFT, null), adgroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},

                // all stop on parent mask child status
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(STOP_WARN, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_WARN, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(STOP_OK, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {keyword(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER), adgroup(STOP_CRIT, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER, GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},


                // all pause on parent mask child status
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_OK, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(PAUSE_WARN, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_WARN, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(PAUSE_CRIT, GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},

                {keyword(DRAFT, GdSelfStatusReason.DRAFT), adgroup(RUN_OK, GdSelfStatusReason.ACTIVE),
                        DRAFT, List.of(GdSelfStatusReason.DRAFT)},

                {keyword(RUN_OK, GdSelfStatusReason.ACTIVE), adgroup(DRAFT, GdSelfStatusReason.DRAFT),
                        DRAFT, List.of(GdSelfStatusReason.DRAFT)},

                {keyword(STOP_OK, GdSelfStatusReason.KEYWORD_SUSPENDED_BY_USER),
                        adgroup(STOP_OK, GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER),
                        STOP_OK, List.of(GdSelfStatusReason.KEYWORD_SUSPENDED_BY_USER)},

                // Для STOP_* не пишем ризон про остановку по ТТ
                {keyword(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {keyword(STOP_WARN, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_WARN, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {keyword(STOP_CRIT, GdSelfStatusReason.SUSPENDED_BY_USER),
                        adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
        };
    }

    @Before
    public void getEffectiveStatus() {
        effectiveStatus = keywordEffectiveStatus(keywordStatusData, adgroupStatusData);
    }

    @Test
    public void reasonsMatch() {
        if (expectedReasons == null) {
            assumeThat("reasons amount match", effectiveStatus.getReasons(), empty());
        } else {
            assumeThat("reasons amount match", effectiveStatus.getReasons(), hasSize(expectedReasons.size()));
            assertThat("reason match", effectiveStatus.getReasons(), containsInAnyOrder(expectedReasons.toArray()));
        }
    }

    @Test
    public void statusMatch() {
        assertEquals("status match", expectedStatus, effectiveStatus.getStatus());
    }

    private static AggregatedStatusKeywordData keyword(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusKeywordData(status, reason)
                : new AggregatedStatusKeywordData(status);
    }

    private static AggregatedStatusAdGroupData adgroup(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusAdGroupData(status, reason)
                : new AggregatedStatusAdGroupData(status);
    }
}
