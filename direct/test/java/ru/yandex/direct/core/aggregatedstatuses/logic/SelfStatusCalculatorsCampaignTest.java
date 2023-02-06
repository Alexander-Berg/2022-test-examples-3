package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_WARN;

@RunWith(Parameterized.class)
public class SelfStatusCalculatorsCampaignTest {
    private static final CampaignCounters EMPTY_COUNTERS = new CampaignCounters();

    private static final CampaignCounters DRAFT_MODERATION_COUNTERS = new CampaignCounters(1,
            Map.of(DRAFT, 1), Map.of(AdGroupStatesEnum.HAS_DRAFT_ON_MODERATION_ADS, 1));

    private static final CampaignCounters SUSPENDED_COUNTERS = new CampaignCounters(1,
            Map.of(STOP_OK, 1), Map.of());

    private static final CampaignCounters ALL_ARCHIVED_COUNTERS = new CampaignCounters(3,
            Map.of(ARCHIVED, 3), Map.of());

    private static final CampaignCounters DRAFT_AND_SUSPENDED_COUNTERS = new CampaignCounters(2,
            Map.of(STOP_OK, 1, DRAFT, 1), Map.of());

    private static final CampaignCounters DRAFT_STOP_AND_ARCHIVED_COUNTERS = new CampaignCounters(5,
            Map.of(STOP_OK, 1, DRAFT, 1, ARCHIVED, 3), Map.of());

    private static final CampaignCounters DRAFT_ON_MODERATION_STOP_AND_ARCHIVED_COUNTERS = new CampaignCounters(5,
            Map.of(STOP_OK, 1, DRAFT, 1, ARCHIVED, 3),
            Map.of(AdGroupStatesEnum.DRAFT, 1, AdGroupStatesEnum.MODERATION, 1,
                    AdGroupStatesEnum.HAS_DRAFT_ON_MODERATION_ADS, 1));

    private static final CampaignCounters MODERATION_COUNTERS = new CampaignCounters(2,
            Map.of(RUN_OK, 1, ON_MODERATION, 1), Map.of());

    private static final CampaignCounters ALL_ADGROUPS_DRAFT_ON_MODERATION = new CampaignCounters(5,
            Map.of(DRAFT, 2, ARCHIVED, 3), Map.of(AdGroupStatesEnum.HAS_DRAFT_ON_MODERATION_ADS, 2));

    private static final CampaignCounters SOME_ADGROUPS_ON_MODERATION = new CampaignCounters(5,
            Map.of(RUN_OK, 1, DRAFT, 2, ARCHIVED, 2), Map.of(AdGroupStatesEnum.HAS_DRAFT_ON_MODERATION_ADS, 2));

    private static final CampaignCounters NO_MODERATION_COUNTERS = new CampaignCounters(1,
            Map.of(STOP_WARN, 1), Map.of(AdGroupStatesEnum.HAS_NO_EFFECTIVE_GEO, 1));

    private static final CampaignCounters RUN_OK_COUNTERS = new CampaignCounters(1,
            Map.of(RUN_OK, 1), Map.of());

    private static final CampaignCounters ACTIVE_COUNTERS = new CampaignCounters(2,
            Map.of(RUN_OK, 1, RUN_WARN, 1), Map.of());

    private static final CampaignCounters DRAFT_RUN_STOP_MODERATION_COUNTERS = new CampaignCounters(4,
            Map.of(DRAFT, 1, RUN_OK, 1, STOP_OK, 1, ON_MODERATION, 1), Map.of());

    private static final CampaignCounters NOT_ALL_ACTIVE_COUNTERS = new CampaignCounters(2,
            Map.of(RUN_OK, 1, STOP_CRIT, 1), Map.of());

    private static final CampaignCounters ACTIVE_AND_STOP_COUNTERS = new CampaignCounters(3,
            Map.of(RUN_OK, 1, STOP_CRIT, 1, RUN_WARN, 1), Map.of());

    private static final CampaignCounters MIXED_COUNTERS = new CampaignCounters(2,
            Map.of(RUN_OK, 1042, STOP_CRIT, 55, STOP_OK, 9, ARCHIVED, 502), Map.of());

    private static final CampaignCounters ADGROUP_BL_NOTHING_GENERATED = new CampaignCounters(6,
            Map.of(STOP_CRIT, 2, STOP_OK, 1, ARCHIVED, 3),
            Map.of(AdGroupStatesEnum.STATUS_BL_GENERATED_NOTHING_GENERATED, 2));

    private static final CampaignCounters ADGROUP_REJECTED_BL_NOTHING_GENERATED = new CampaignCounters(6,
            Map.of(STOP_CRIT, 3, STOP_OK, 1, ARCHIVED, 3),
            Map.of(AdGroupStatesEnum.STATUS_BL_GENERATED_NOTHING_GENERATED, 2, AdGroupStatesEnum.REJECTED, 1));

    private static final CampaignCounters ADGROUP_ON_MODERATION = new CampaignCounters(1,
            Map.of(ON_MODERATION, 1), Map.of(AdGroupStatesEnum.MODERATION, 1));

    @Parameterized.Parameter()
    public Collection<CampaignStatesEnum> states;

    @Parameterized.Parameter(1)
    public CampaignCounters counters;

    @Parameterized.Parameter(2)
    public GdSelfStatusEnum status;

    @Parameterized.Parameter(3)
    public List<GdSelfStatusReason> reasons;

    @Parameterized.Parameters(name = "states = {0} => status: {2}, reason: {3}")
    public static Object[][] params() {
        return new Object[][]{
                {List.of(CampaignStatesEnum.ARCHIVED), EMPTY_COUNTERS,
                        ARCHIVED, null},

                {List.of(CampaignStatesEnum.ARCHIVING), EMPTY_COUNTERS,
                        ARCHIVED, null},

                {List.of(CampaignStatesEnum.ARCHIVED, CampaignStatesEnum.CANT_BE_UNARCHIVED), EMPTY_COUNTERS,
                        ARCHIVED, List.of(GdSelfStatusReason.CAMPAIGN_IS_NOT_RECOVERABLE)},

                {List.of(CampaignStatesEnum.PAYED), ALL_ARCHIVED_COUNTERS,
                        ARCHIVED, null},

                {List.of(CampaignStatesEnum.DRAFT), DRAFT_MODERATION_COUNTERS,
                        ON_MODERATION, List.of(GdSelfStatusReason.CAMPAIGN_ON_MODERATION)},

                {List.of(CampaignStatesEnum.DRAFT), EMPTY_COUNTERS,
                        DRAFT, null},

                {List.of(CampaignStatesEnum.UNARCHIVING), EMPTY_COUNTERS,
                        DRAFT, null},

                {List.of(CampaignStatesEnum.UNARCHIVING), MIXED_COUNTERS,
                        null, null},

                {List.of(CampaignStatesEnum.SUSPENDED), SUSPENDED_COUNTERS,
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_SUSPENDED_BY_USER)},

                {List.of(CampaignStatesEnum.PAYED), SUSPENDED_COUNTERS,
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_ALL_ADGROUPS_SUSPENDED_BY_USER)},

                {List.of(CampaignStatesEnum.UNITS_EXHAUSTED), RUN_OK_COUNTERS,
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_UNITS_EXHAUSTED)},

                {List.of(CampaignStatesEnum.NO_MONEY), EMPTY_COUNTERS,
                        DRAFT, null},

                {List.of(CampaignStatesEnum.NO_MONEY), DRAFT_MODERATION_COUNTERS,
                        ON_MODERATION, List.of(GdSelfStatusReason.CAMPAIGN_ON_MODERATION)},

                {List.of(CampaignStatesEnum.SUSPENDED, CampaignStatesEnum.UNARCHIVING), DRAFT_AND_SUSPENDED_COUNTERS,
                        STOP_WARN, List.of(GdSelfStatusReason.CAMPAIGN_UNARCHIVING_IN_PROGRESS)},

                {List.of(CampaignStatesEnum.SUSPENDED), DRAFT_AND_SUSPENDED_COUNTERS,
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_SUSPENDED_BY_USER)},

                {List.of(), DRAFT_AND_SUSPENDED_COUNTERS,
                        DRAFT, null},

                {List.of(), DRAFT_STOP_AND_ARCHIVED_COUNTERS,
                        DRAFT, null},

                {List.of(), DRAFT_ON_MODERATION_STOP_AND_ARCHIVED_COUNTERS,
                        ON_MODERATION, List.of(GdSelfStatusReason.CAMPAIGN_ON_MODERATION)},

                {List.of(CampaignStatesEnum.NO_MONEY), MODERATION_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_PARTLY_ON_MODERATION,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},

                {List.of(CampaignStatesEnum.PAYED), ALL_ADGROUPS_DRAFT_ON_MODERATION,
                        ON_MODERATION, List.of(GdSelfStatusReason.CAMPAIGN_ON_MODERATION)},

                {List.of(CampaignStatesEnum.NO_MONEY), ALL_ADGROUPS_DRAFT_ON_MODERATION,
                        ON_MODERATION, List.of(GdSelfStatusReason.CAMPAIGN_ON_MODERATION)},

                {List.of(), ALL_ADGROUPS_DRAFT_ON_MODERATION,
                        ON_MODERATION, List.of(GdSelfStatusReason.CAMPAIGN_ON_MODERATION)},

                {List.of(CampaignStatesEnum.NO_MONEY), NO_MODERATION_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING)},

                {List.of(CampaignStatesEnum.NO_MONEY, CampaignStatesEnum.AWAIT_PAYMENT), RUN_OK_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_WAIT_PAYMENT)},

                {List.of(CampaignStatesEnum.NO_MONEY), RUN_OK_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},

                {List.of(CampaignStatesEnum.PAYED), RUN_OK_COUNTERS,
                        RUN_OK, null},

                {List.of(CampaignStatesEnum.PAYED), ACTIVE_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS)},

                {List.of(CampaignStatesEnum.PAYED), NOT_ALL_ACTIVE_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.CAMPAIGN_HAS_INACTIVE_BANNERS)},

                {List.of(CampaignStatesEnum.PAYED), ADGROUP_BL_NOTHING_GENERATED,
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_BL_NOTHING_GENERATED)},

                {List.of(CampaignStatesEnum.PAYED), ADGROUP_REJECTED_BL_NOTHING_GENERATED,
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING)},

                {List.of(CampaignStatesEnum.PAYED), ACTIVE_AND_STOP_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS, GdSelfStatusReason.CAMPAIGN_HAS_INACTIVE_BANNERS)},

                // Временно убираем для всех кампаний: DIRECT-140177
//                {List.of(CampaignStatesEnum.PAY_FOR_CONVERSION_CAMPAIGN_HAS_LACK_OF_FUNDS), ACTIVE_COUNTERS,
//                        PAUSE_WARN, List.of(GdSelfStatusReason.PAY_FOR_CONVERSION_CAMPAIGN_HAS_LACK_OF_FUNDS)},

                {List.of(CampaignStatesEnum.PAYED), DRAFT_RUN_STOP_MODERATION_COUNTERS, RUN_OK, null},

                {List.of(CampaignStatesEnum.PROMO_EXTENSION_REJECTED), RUN_OK_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED)},

                {List.of(CampaignStatesEnum.PROMO_EXTENSION_REJECTED), ACTIVE_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED,
                        GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS)},

                {List.of(CampaignStatesEnum.PROMO_EXTENSION_REJECTED), NOT_ALL_ACTIVE_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED,
                        GdSelfStatusReason.CAMPAIGN_HAS_INACTIVE_BANNERS)},

                {List.of(CampaignStatesEnum.SUSPENDED), ADGROUP_ON_MODERATION,
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_SUSPENDED_BY_USER)}
        };
    }

    @Test
    public void test() {
        var calculatedStatus = SelfStatusCalculators.calcCampaignSelfStatus(1L, false, states,
                Collections.emptyMap(), counters, null, true);

        if (status == null) {
            assertThat("Status is null", calculatedStatus, Matchers.nullValue());
            return;
        }

        assertEquals("Status ok", status, calculatedStatus.getStatus());

        if (reasons == null) {
            assertThat("Reason is null", calculatedStatus.getReasons(), Matchers.nullValue());
        } else {
            assertThat("Reason count ok", calculatedStatus.getReasons(), hasSize(reasons.size()));
            assertThat("Reason ok", calculatedStatus.getReasons(), containsInAnyOrder(reasons.toArray()));
        }
    }
}
