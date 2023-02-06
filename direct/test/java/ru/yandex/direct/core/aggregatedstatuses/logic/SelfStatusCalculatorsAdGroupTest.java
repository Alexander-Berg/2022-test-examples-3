package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.aggregatedstatuses.repository.ShowConditionsCounter;
import ru.yandex.direct.core.entity.adgroup.aggrstatus.AggregatedStatusAdGroup;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.KeywordStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.RetargetingStatesEnum;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_BL_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING;

@RunWith(Parameterized.class)
public class SelfStatusCalculatorsAdGroupTest {
    private static final AdGroupCounters EMPTY_COUNTERS = new AdGroupCounters();

    private static final AdGroupCounters RUN_OK_COUNTERS = new AdGroupCounters(1, 0, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters ARCHIVED_COUNTERS = new AdGroupCounters(1, 0, 0,
            Map.of(ARCHIVED, 1), Map.of(AdStatesEnum.ARCHIVED, 1),
            Map.of(), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters DRAFT_ON_MODERATION_COUNTERS = new AdGroupCounters(1, 0, 0,
            Map.of(DRAFT, 1), Map.of(AdStatesEnum.DRAFT_ON_MODERATION, 1),
            Map.of(), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters DRAFT_ON_MODERATION_WITH_KEYWORDS_COUNTERS = new AdGroupCounters(1, 1, 0,
            Map.of(DRAFT, 1), Map.of(AdStatesEnum.DRAFT_ON_MODERATION, 1),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters STOP_OK_COUNTERS_ADS_SUSPENDED = new AdGroupCounters(1, 0, 0,
            Map.of(STOP_OK, 1), Map.of(AdStatesEnum.SUSPENDED, 1),
            Map.of(), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters STOP_OK_COUNTERS_KEYWORDS_SUSPENDED = new AdGroupCounters(1, 1, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(STOP_OK, 1), Map.of(KeywordStatesEnum.SUSPENDED, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters STOP_OK_COUNTERS_RETARGETINGS_SUSPENDED = new AdGroupCounters(1, 0, 1,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(), Map.of(),
            Map.of(STOP_OK, 1), Map.of(RetargetingStatesEnum.SUSPENDED, 1));

    private static final AdGroupCounters OK_AD_AND_REJECTED_KEYWORD = new AdGroupCounters(1, 1, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(STOP_CRIT, 1), Map.of(KeywordStatesEnum.REJECTED, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters OK_AD_AND_ONE_OF_KEYWORD_REJECTED = new AdGroupCounters(1, 2, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(STOP_CRIT, 1, RUN_OK, 1), Map.of(KeywordStatesEnum.REJECTED, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters ONE_AD_AND_ONE_KEYWORD_REJECTED = new AdGroupCounters(2, 2, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_CRIT, 1), Map.of(AdStatesEnum.REJECTED, 1),
            Map.of(STOP_CRIT, 1, RUN_OK, 1), Map.of(KeywordStatesEnum.REJECTED, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters ONE_AD_AND_ONE_KEYWORD_WITH_WARNINGS = new AdGroupCounters(2, 2, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, RUN_WARN, 1), Map.of(AdStatesEnum.HAS_REJECTED_CALLOUTS, 1),
            Map.of(RUN_OK, 1, RUN_WARN, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters ONE_AD_AND_ONE_KEYWORD_WITH_STOP_WARNINGS = new AdGroupCounters(2, 2, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_WARN, 1), Map.of(AdStatesEnum.HAS_REJECTED_CALLOUTS, 1),
            Map.of(RUN_OK, 1, STOP_WARN, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters REJECTED_COUNTERS = new AdGroupCounters(1, 0, 0,
            Map.of(STOP_CRIT, 1), Map.of(AdStatesEnum.REJECTED, 1),
            Map.of(), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters RUN_OK_AD_AND_KEYWORD = new AdGroupCounters(1, 1, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters RUN_OK_AD_AND_RETARGETING = new AdGroupCounters(1, 0, 1,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(), Map.of(),
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of());

    private static final AdGroupCounters RUN_OK_HAS_SUSPENDED_RETARGETING = new AdGroupCounters(1, 0, 2,
            Map.of(GdSelfStatusEnum.RUN_OK, 1), Map.of(),
            Map.of(), Map.of(),
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_OK, 1), Map.of(RetargetingStatesEnum.SUSPENDED, 1));

    private static final AdGroupCounters RUN_WARN_AD_HAS_REJECTED_CALLOUTS = new AdGroupCounters(2, 0, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_WARN, 1), Map.of(AdStatesEnum.HAS_REJECTED_CALLOUTS, 1),
            Map.of(), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters MODERATION_COUNTERS = new AdGroupCounters(2, 2, 1,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_WARN, 1), Map.of(AdStatesEnum.MODERATION, 1),
            Map.of(STOP_CRIT, 1, GdSelfStatusEnum.STOP_OK, 1), Map.of(KeywordStatesEnum.REJECTED, 1,
            KeywordStatesEnum.SUSPENDED, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters PARTLY_MODERATION_WITH_REJECTED_COUNTERS = new AdGroupCounters(2, 3, 1,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_WARN, 1), Map.of(AdStatesEnum.MODERATION, 1),
            Map.of(STOP_CRIT, 1, GdSelfStatusEnum.STOP_OK, 1, DRAFT, 1), Map.of(KeywordStatesEnum.REJECTED, 1,
            KeywordStatesEnum.SUSPENDED, 1, KeywordStatesEnum.DRAFT, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters PARTLY_MODERATION_WITH_ACTIVE_COUNTERS = new AdGroupCounters(2, 3, 1,
            Map.of(GdSelfStatusEnum.RUN_OK, 2), Map.of(AdStatesEnum.MODERATION, 1),
            Map.of(RUN_OK, 1, GdSelfStatusEnum.STOP_OK, 1, DRAFT, 1), Map.of(
            KeywordStatesEnum.SUSPENDED, 1, KeywordStatesEnum.DRAFT, 1),
            Map.of(), Map.of());

    private static final AdGroupCounters HAS_DRAFT_AD_ON_MODERATION_AND_OK_AD = new AdGroupCounters(2, 1, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, DRAFT, 1),
            Map.of(AdStatesEnum.MODERATION, 1, AdStatesEnum.PREACCEPTED, 1, AdStatesEnum.DRAFT_ON_MODERATION, 1),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters HAS_AD_ON_MODERATION_AND_OK_AD = new AdGroupCounters(2, 1, 0,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, ON_MODERATION, 1),
            Map.of(AdStatesEnum.MODERATION, 1, AdStatesEnum.PREACCEPTED, 1, AdStatesEnum.DRAFT_ON_MODERATION, 1),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters ALL_ADS_ON_MODERATION = new AdGroupCounters(2, 1, 0,
            Map.of(ON_MODERATION, 2),
            Map.of(AdStatesEnum.MODERATION, 1, AdStatesEnum.PREACCEPTED, 1, AdStatesEnum.DRAFT_ON_MODERATION, 1),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters HAS_DRAFT_SUSPENDED_AND_ARCHIVED_ADS = new AdGroupCounters(3, 1, 0,
            Map.of(DRAFT, 1, STOP_OK, 1, ARCHIVED, 1),
            Map.of(AdStatesEnum.MODERATION, 1, AdStatesEnum.SUSPENDED, 1, AdStatesEnum.ARCHIVED, 1,
                    AdStatesEnum.DRAFT_ON_MODERATION, 1),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters ALL_ADS_RUN_OK_ACTIVE = new AdGroupCounters(2, 1, 0,
            Map.of(RUN_OK, 2), Map.of(AdStatesEnum.ACTIVE_IN_BS, 2),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters HAS_RUN_PROCESSING_AD_AND_OK_AD = new AdGroupCounters(2, 1, 0,
            Map.of(RUN_OK, 1, RUN_PROCESSING, 1), Map.of(),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters HAS_RUN_PROCESSING_AD_AND_OK_AD_ACTIVE = new AdGroupCounters(2, 1, 0,
            Map.of(RUN_OK, 1, RUN_PROCESSING, 1), Map.of(AdStatesEnum.ACTIVE_IN_BS, 1),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    private static final AdGroupCounters ALL_ADS_RUN_PROCESSING = new AdGroupCounters(2, 1, 0,
            Map.of(RUN_PROCESSING, 2), Map.of(),
            Map.of(RUN_OK, 1), Map.of(),
            Map.of(), Map.of());

    @Parameterized.Parameter(0)
    public Collection<AdGroupStatesEnum> states;

    @Parameterized.Parameter(1)
    public AdGroupCounters counters;

    @Parameterized.Parameter(2)
    public GdSelfStatusEnum status;

    @Parameterized.Parameter(3)
    public List<GdSelfStatusReason> reasons;

    @Parameterized.Parameter(4)
    public boolean hasInterestShowCondition;

    @Parameterized.Parameter(5)
    public boolean hasBannerGeoLegalFlag;

    @Parameterized.Parameters(name = "states = {0} => status: {2}, reason: {3}")
    public static Object[][] params() {
        return new Object[][]{
                {List.of(), ARCHIVED_COUNTERS,
                        ARCHIVED, null, false, true},

                {List.of(AdGroupStatesEnum.DRAFT), EMPTY_COUNTERS,
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING),
                        false, false},

                {List.of(AdGroupStatesEnum.DRAFT), DRAFT_ON_MODERATION_COUNTERS,
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_ADS_ON_MODERATION,
                        GdSelfStatusReason.ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING), false, false},

                {List.of(AdGroupStatesEnum.DRAFT), DRAFT_ON_MODERATION_WITH_KEYWORDS_COUNTERS,
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_ADS_ON_MODERATION), false, false},

                {List.of(AdGroupStatesEnum.REJECTED), EMPTY_COUNTERS,
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING),
                        false, false},

                {List.of(AdGroupStatesEnum.REJECTED), OK_AD_AND_REJECTED_KEYWORD,
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_REJECTED_ON_MODERATION), false, false},

                {List.of(), OK_AD_AND_ONE_OF_KEYWORD_REJECTED,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_SHOW_CONDITIONS_REJECTED_ON_MODERATION),
                        false, false},

                {List.of(), ONE_AD_AND_ONE_KEYWORD_REJECTED,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_ADS_REJECTED_ON_MODERATION,
                        GdSelfStatusReason.ADGROUP_HAS_SHOW_CONDITIONS_REJECTED_ON_MODERATION),
                        false, false},

                // Эти тесты не нужны, т.к. больше нет этих причин
                /*{List.of(), ONE_AD_AND_ONE_KEYWORD_WITH_WARNINGS,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_ADS_WITH_WARNINGS,
                        GdSelfStatusReason.ADGROUP_HAS_SHOW_CONDITIONS_WITH_WARNINGS),
                        false, false},

                {List.of(), ONE_AD_AND_ONE_KEYWORD_WITH_STOP_WARNINGS,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_ADS_WITH_WARNINGS,
                        GdSelfStatusReason.ADGROUP_HAS_SHOW_CONDITIONS_WITH_WARNINGS),
                        false, false},*/

                {List.of(AdGroupStatesEnum.MODERATION), EMPTY_COUNTERS,
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING),
                        false, false},

                {List.of(), STOP_OK_COUNTERS_ADS_SUSPENDED,
                        STOP_OK, List.of(GdSelfStatusReason.ADGROUP_ADS_SUSPENDED_BY_USER), true, false},

                {List.of(), STOP_OK_COUNTERS_KEYWORDS_SUSPENDED,
                        STOP_OK, List.of(GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER), false, false},

                {List.of(), STOP_OK_COUNTERS_RETARGETINGS_SUSPENDED,
                        STOP_OK, List.of(GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER), false, false},

                {List.of(AdGroupStatesEnum.HAS_RESTRICTED_GEO), RUN_OK_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_RESTRICTED_GEO), true, false},

                {List.of(AdGroupStatesEnum.HAS_NO_EFFECTIVE_GEO), RUN_OK_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_EFFECTIVE_GEO), true, false},

                {List.of(AdGroupStatesEnum.HAS_NO_EFFECTIVE_GEO), RUN_OK_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_NEED_DOCUMENTS_FOR_MODERATION), true, true},

                {List.of(), RUN_OK_AD_AND_KEYWORD,
                        RUN_OK, null, false, false},

                {List.of(), RUN_OK_AD_AND_RETARGETING,
                        RUN_OK, null, false, false},

                {List.of(), RUN_OK_HAS_SUSPENDED_RETARGETING,
                        RUN_OK, null, false, false},

                // Возможно, этот тест больше не нужен.
                /*{List.of(), RUN_WARN_AD_HAS_REJECTED_CALLOUTS,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_ADS_WITH_WARNINGS), true, false},*/

                {List.of(AdGroupStatesEnum.MODERATION), MODERATION_COUNTERS,
                        ON_MODERATION, List.of(GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_ON_MODERATION), false, false},

                {List.of(AdGroupStatesEnum.MODERATION), PARTLY_MODERATION_WITH_REJECTED_COUNTERS,
                        ON_MODERATION, List.of(GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_PARTLY_ON_MODERATION), false,
                        false},

                {List.of(AdGroupStatesEnum.MODERATION), PARTLY_MODERATION_WITH_ACTIVE_COUNTERS,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_PARTLY_ON_MODERATION), false,
                        false},

                {List.of(), HAS_DRAFT_AD_ON_MODERATION_AND_OK_AD,
                        RUN_OK, null, false, false},

                {List.of(), HAS_AD_ON_MODERATION_AND_OK_AD,
                        RUN_OK, null, false, false},

                {List.of(), ALL_ADS_ON_MODERATION,
                        ON_MODERATION, List.of(GdSelfStatusReason.ADGROUP_ADS_ON_MODERATION), false, false},

                {List.of(), HAS_DRAFT_SUSPENDED_AND_ARCHIVED_ADS,
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_HAS_ADS_ON_MODERATION), false, false},

                {List.of(), REJECTED_COUNTERS,
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING, GdSelfStatusReason.ADGROUP_ADS_REJECTED_ON_MODERATION), true, false},

                {List.of(AdGroupStatesEnum.STATUS_BL_GENERATED_PROCESSING), EMPTY_COUNTERS,
                        DRAFT, List.of(ADGROUP_BL_PROCESSING, ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING),
                        false, false},

                {List.of(AdGroupStatesEnum.STATUS_BL_GENERATED_NOTHING_GENERATED), EMPTY_COUNTERS,
                        DRAFT, List.of(ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING),
                        false, false},

                {List.of(AdGroupStatesEnum.STATUS_BL_GENERATED_NOTHING_GENERATED), RUN_OK_AD_AND_KEYWORD,
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_BL_NOTHING_GENERATED), false, false},

                // когда условия ретагетинга находятся на модерации, хотим отображать эту информации
                // (возвращать ADGROUP_SHOW_CONDITIONS_ON_MODERATION), чтобы было понятно, почему показы не идут
                // возможно поведение будет изменено в DIRECT-140204
                {List.of(AdGroupStatesEnum.MODERATION), RUN_OK_AD_AND_RETARGETING,
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_ON_MODERATION), false, false},

                {List.of(), ALL_ADS_RUN_OK_ACTIVE,
                        RUN_OK, null, false, false},

                {List.of(AdGroupStatesEnum.STATUS_BL_GENERATED_PROCESSING), ALL_ADS_RUN_OK_ACTIVE,
                        RUN_PROCESSING, List.of(GdSelfStatusReason.ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN),
                        false, false},

                {List.of(), HAS_RUN_PROCESSING_AD_AND_OK_AD,
                        RUN_OK, null, false, false},

                {List.of(AdGroupStatesEnum.STATUS_BL_GENERATED_PROCESSING), HAS_RUN_PROCESSING_AD_AND_OK_AD,
                        RUN_PROCESSING, List.of(GdSelfStatusReason.ADGROUP_BL_PROCESSING),
                        false, false},

                {List.of(), ALL_ADS_RUN_PROCESSING,
                        RUN_OK, null, false, false},

                {List.of(AdGroupStatesEnum.STATUS_BL_GENERATED_PROCESSING), ALL_ADS_RUN_PROCESSING,
                        STOP_PROCESSING, List.of(GdSelfStatusReason.ADGROUP_BL_PROCESSING), false, false},
        };
    }

    @Test
    public void test() {
        AggregatedStatusAdGroup adgroup = new AggregatedStatusAdGroup();
        SelfStatus calculatedStatus = SelfStatusCalculators.calcAdGroupSelfStatus(adgroup, states, Collections.emptyMap(), counters,
                hasInterestShowCondition, hasBannerGeoLegalFlag, new ShowConditionsCounter(), true);

        assertEquals("Status ok", status, calculatedStatus.getStatus());

        if (reasons == null) {
            assertThat("Reason is null", calculatedStatus.getReasons(), Matchers.nullValue());
        } else {
            assertThat("Reason count ok", calculatedStatus.getReasons(), hasSize(reasons.size()));
            assertThat("Reason ok", calculatedStatus.getReasons(), containsInAnyOrder(reasons.toArray()));
        }
    }
}
