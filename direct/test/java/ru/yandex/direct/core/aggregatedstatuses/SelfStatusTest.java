package ru.yandex.direct.core.aggregatedstatuses;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;

import static org.junit.Assert.assertEquals;

public final class SelfStatusTest {
    @Test
    public void sortReasonsTest() {
        final var selfStatus = SelfStatus.status(GdSelfStatusEnum.RUN_OK, List.of(
                GdSelfStatusReason.CAMPAIGN_ON_MODERATION,
                GdSelfStatusReason.ADGROUP_HAS_RESTRICTED_GEO,
                GdSelfStatusReason.CAMPAIGN_IS_NOT_RECOVERABLE,
                GdSelfStatusReason.CAMPAIGN_ALL_ADGROUPS_SUSPENDED_BY_USER,
                GdSelfStatusReason.CAMPAIGN_IS_WAITING_START,
                GdSelfStatusReason.AD_HAS_UNFAMILY_OR_TRAGIC_FLAG_PLATFORM_BOTH,
                GdSelfStatusReason.ADGROUP_ADS_REJECTED_ON_MODERATION,
                GdSelfStatusReason.AD_MULTICARD_SET_REJECTRED_MODERATION,
                GdSelfStatusReason.CAMPAIGN_UNITS_EXHAUSTED,
                GdSelfStatusReason.CAMPAIGN_BL_NOTHING_GENERATED,
                GdSelfStatusReason.CAMPAIGN_PARTLY_ON_MODERATION,
                GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING
        ));

        assertEquals(selfStatus.getReasons(), List.of(
                GdSelfStatusReason.CAMPAIGN_IS_NOT_RECOVERABLE,
                GdSelfStatusReason.CAMPAIGN_IS_WAITING_START,
                GdSelfStatusReason.CAMPAIGN_ON_MODERATION,
                GdSelfStatusReason.ADGROUP_ADS_REJECTED_ON_MODERATION,
                GdSelfStatusReason.CAMPAIGN_ALL_ADGROUPS_SUSPENDED_BY_USER,
                GdSelfStatusReason.CAMPAIGN_BL_NOTHING_GENERATED,
                GdSelfStatusReason.CAMPAIGN_PARTLY_ON_MODERATION,
                GdSelfStatusReason.ADGROUP_HAS_RESTRICTED_GEO,
                GdSelfStatusReason.CAMPAIGN_UNITS_EXHAUSTED,
                GdSelfStatusReason.AD_HAS_UNFAMILY_OR_TRAGIC_FLAG_PLATFORM_BOTH,
                GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING,
                GdSelfStatusReason.AD_MULTICARD_SET_REJECTRED_MODERATION
        ));
    }
}
