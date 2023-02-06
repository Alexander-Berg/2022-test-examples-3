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
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.aggregatedstatuses.logic.EffectiveStatusCalculators.adEffectiveStatus;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.PAUSE_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_HAS_ASOCIAL_FLAG;
import static ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.BOTH;
import static ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.CONTEXT;
import static ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.SEARCH;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@RunWith(Parameterized.class)
public class EffectiveStatusCalculatorsAdsTest {
    private SelfStatus effectiveStatus;

    @Parameterized.Parameter
    public AggregatedStatusAdData adStatusData;

    @Parameterized.Parameter(1)
    public AggregatedStatusAdGroupData adgroupStatusData;

    @Parameterized.Parameter(2)
    public GdSelfStatusEnum expectedStatus;

    @Parameterized.Parameter(3)
    public List<GdSelfStatusReason> expectedReasons;

    @Parameterized.Parameters(name = "{2}: {3}")
    public static Object[][] params() {
        // not all variants a production possible
        return new Object[][]{
                // no parent statuses -> returns child
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(null, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(RUN_WARN, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, BOTH), adgroup(null, null),
                        RUN_OK, List.of(GdSelfStatusReason.ACTIVE)},

                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, CONTEXT), adgroup(RUN_OK, null),
                        RUN_WARN, List.of(GdSelfStatusReason.ACTIVE, AD_HAS_ASOCIAL_FLAG)},

                // child archived, masks parent status
                {ad(ARCHIVED, GdSelfStatusReason.ARCHIVED, SEARCH), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},
                {ad(ARCHIVED, GdSelfStatusReason.ARCHIVED, CONTEXT), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},

                // parent archived
                {ad(STOP_CRIT, GdSelfStatusReason.REJECTED_ON_MODERATION, SEARCH),
                        adgroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},
                {ad(STOP_CRIT, GdSelfStatusReason.REJECTED_ON_MODERATION, BOTH),
                        adgroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},

                // DRAFT wont inherit parent status
                {ad(DRAFT, GdSelfStatusReason.AD_ON_MODERATION, SEARCH),
                        adgroup(STOP_CRIT, GdSelfStatusReason.ADGROUP_HAS_NO_SHOW_CONDITIONS_ELIGIBLE_FOR_SERVING),
                        DRAFT, List.of(GdSelfStatusReason.AD_ON_MODERATION)},
                {ad(DRAFT, GdSelfStatusReason.AD_ON_MODERATION, SEARCH), adgroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},
                {ad(DRAFT, GdSelfStatusReason.AD_ON_MODERATION, BOTH), adgroup(ARCHIVED, GdSelfStatusReason.ARCHIVED),
                        ARCHIVED, List.of(GdSelfStatusReason.ARCHIVED)},


                // if statuses the same, mergin works, and meaningless statuses droped
                {ad(RUN_WARN, GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS, SEARCH),
                        adgroup(RUN_WARN, GdSelfStatusReason.ADGROUP_RARELY_SERVED),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS,
                        GdSelfStatusReason.ADGROUP_RARELY_SERVED)},
                {ad(STOP_CRIT, GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING, SEARCH), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {ad(RUN_WARN, GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS, CONTEXT),
                        adgroup(RUN_WARN, GdSelfStatusReason.ADGROUP_RARELY_SERVED),
                        RUN_WARN, List.of(GdSelfStatusReason.AD_HAS_REJECTED_SITELINKS,
                        GdSelfStatusReason.ADGROUP_RARELY_SERVED, GdSelfStatusReason.AD_HAS_ASOCIAL_FLAG)},
                {ad(STOP_CRIT, GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING, BOTH), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_HAS_NO_ADS_ELIGIBLE_FOR_SERVING,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY, GdSelfStatusReason.AD_HAS_ASOCIAL_FLAG)},
                {ad(DRAFT, GdSelfStatusReason.ON_MODERATION, SEARCH), adgroup(DRAFT, GdSelfStatusReason.DRAFT),
                        DRAFT, List.of(GdSelfStatusReason.ON_MODERATION)},

                // all stop on parent mask child status
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(STOP_WARN,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_WARN, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(STOP_OK, GdSelfStatusReason.CAMPAIGN_ADD_MONEY),
                        STOP_OK, List.of(GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {ad(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER, SEARCH), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY), STOP_CRIT,
                        List.of(GdSelfStatusReason.SUSPENDED_BY_USER, GdSelfStatusReason.CAMPAIGN_ADD_MONEY)},
                {ad(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER, BOTH), adgroup(STOP_CRIT,
                        GdSelfStatusReason.CAMPAIGN_ADD_MONEY), STOP_CRIT,
                        List.of(GdSelfStatusReason.SUSPENDED_BY_USER, GdSelfStatusReason.CAMPAIGN_ADD_MONEY,
                                GdSelfStatusReason.AD_HAS_ASOCIAL_FLAG)},


                // all pause on parent mask child status
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(PAUSE_OK,
                        GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_OK, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(PAUSE_WARN,
                        GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_WARN, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(PAUSE_CRIT,
                        GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_CRIT, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, CONTEXT), adgroup(PAUSE_OK,
                        GdSelfStatusReason.CAMPAIGN_IS_WAITING_START),
                        PAUSE_OK, List.of(GdSelfStatusReason.CAMPAIGN_IS_WAITING_START,
                        GdSelfStatusReason.AD_HAS_ASOCIAL_FLAG)},

                // работает костыль https://st.yandex-team.ru/DIRECT-116341#5e82ffcb4e297030712ed421
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH), adgroup(RUN_WARN,
                        GdSelfStatusReason.ADGROUP_RARELY_SERVED),
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_RARELY_SERVED)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, CONTEXT), adgroup(RUN_WARN,
                        GdSelfStatusReason.ADGROUP_RARELY_SERVED),
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_RARELY_SERVED,
                        GdSelfStatusReason.AD_HAS_ASOCIAL_FLAG)},

                {ad(STOP_CRIT, GdSelfStatusReason.REJECTED_ON_MODERATION, SEARCH), adgroup(RUN_WARN,
                        GdSelfStatusReason.ADGROUP_RARELY_SERVED),
                        STOP_CRIT, List.of(GdSelfStatusReason.REJECTED_ON_MODERATION,
                        GdSelfStatusReason.ADGROUP_RARELY_SERVED)},

                {ad(STOP_CRIT, GdSelfStatusReason.REJECTED_ON_MODERATION, SEARCH), adgroup(STOP_WARN,
                        GdSelfStatusReason.AD_REJECTED_BUT_PREVIOUS_VERSION_SHOWN),
                        STOP_CRIT, List.of(GdSelfStatusReason.REJECTED_ON_MODERATION,
                        GdSelfStatusReason.AD_REJECTED_BUT_PREVIOUS_VERSION_SHOWN)},

                {ad(RUN_OK, GdSelfStatusReason.AD_SUSPENDED_BY_USER, SEARCH),
                        adgroup(STOP_OK, GdSelfStatusReason.ADGROUP_ADS_SUSPENDED_BY_USER),
                        STOP_OK, List.of(GdSelfStatusReason.AD_SUSPENDED_BY_USER)},

                {ad(RUN_OK, null, SEARCH),
                        adgroup(RUN_WARN, GdSelfStatusReason.ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN),
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN)},

                {ad(RUN_OK, null, SEARCH),
                        adgroup(DRAFT, GdSelfStatusReason.ADGROUP_BL_PROCESSING),
                        DRAFT, List.of(GdSelfStatusReason.ADGROUP_BL_PROCESSING)},

                {ad(RUN_OK, null, SEARCH),
                        adgroup(RUN_WARN, GdSelfStatusReason.ADGROUP_HAS_RESTRICTED_GEO),
                        RUN_WARN, List.of(GdSelfStatusReason.ADGROUP_HAS_RESTRICTED_GEO)},

                // Для STOP_* не пишем ризон про остановку по ТТ
                {ad(STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER, SEARCH),
                        adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_OK, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {ad(STOP_WARN, GdSelfStatusReason.SUSPENDED_BY_USER, SEARCH),
                        adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_WARN, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},
                {ad(STOP_CRIT, GdSelfStatusReason.SUSPENDED_BY_USER, SEARCH),
                        adgroup(PAUSE_OK, GdSelfStatusReason.CAMPAIGN_IS_PAUSED_BY_TIMETARGETING),
                        STOP_CRIT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER)},

                // RUN_WARN by rejected promoaction lowers from campaign
                {ad(RUN_OK, null, SEARCH),
                        adgroup(RUN_WARN, GdSelfStatusReason.PROMO_EXTENSION_REJECTED),
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED)},
                {ad(RUN_OK, GdSelfStatusReason.ACTIVE, SEARCH),
                        adgroup(RUN_WARN, GdSelfStatusReason.PROMO_EXTENSION_REJECTED),
                        RUN_WARN, List.of(GdSelfStatusReason.PROMO_EXTENSION_REJECTED)},

                // Проверка наличия причины AD_SENT_OR_READY_TO_BS в зависимости от активности группы
                {ad(RUN_PROCESSING, GdSelfStatusReason.AD_SENT_OR_READY_TO_BS, SEARCH),
                        adgroup(RUN_OK, null),
                        RUN_PROCESSING, List.of(GdSelfStatusReason.AD_SENT_OR_READY_TO_BS)},
                {ad(RUN_PROCESSING, GdSelfStatusReason.AD_SENT_OR_READY_TO_BS, SEARCH),
                        adgroup(STOP_CRIT, GdSelfStatusReason.ADGROUP_NEED_DOCUMENTS_FOR_MODERATION),
                        STOP_CRIT, List.of(GdSelfStatusReason.ADGROUP_NEED_DOCUMENTS_FOR_MODERATION)}
        };
    }

    @Before
    public void getEffectiveStatus() {
        effectiveStatus = adEffectiveStatus(adStatusData, adgroupStatusData);
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

    private static AggregatedStatusAdData ad(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason,
                                             CampaignsPlatform platform) {
        return reason != null
                ? new AggregatedStatusAdData(List.of(AdStatesEnum.HAS_ASOCIAL_FLAG), status, reason).withPlatform(platform)
                : new AggregatedStatusAdData(List.of(AdStatesEnum.HAS_ASOCIAL_FLAG), status).withPlatform(platform);
    }

    private static AggregatedStatusAdGroupData adgroup(GdSelfStatusEnum status, @Nullable GdSelfStatusReason reason) {
        return reason != null
                ? new AggregatedStatusAdGroupData(status, reason)
                : new AggregatedStatusAdGroupData(status);
    }
}
