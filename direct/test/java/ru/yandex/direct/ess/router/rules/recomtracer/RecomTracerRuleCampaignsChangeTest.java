package ru.yandex.direct.ess.router.rules.recomtracer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.core.entity.recommendation.RecommendationType;
import ru.yandex.direct.ess.logicobjects.recomtracer.AdditionalColumns;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecommendationKeyIdentifier;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addAdditionItemCallouts;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addBannerDisplayHrefs;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addImageToBanner;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addSiteLinks;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.addTitleExtension;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.changeAdGroupWithLowStat;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.dailyBudget;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.decreaseStrategyTargetROI;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.increaseStrategyTargetCPA;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.increaseStrategyWeeklyBudget;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.removePagesFromBlackListOfACampaign;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesBanks;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesMedServices;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.sendLicensesPharmacy;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.switchOnAutotargeting;
import static ru.yandex.direct.core.entity.recommendation.RecommendationType.weeklyBudget;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;
import static ru.yandex.direct.ess.router.testutils.TestUtils.getRecommendationsTypes;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class RecomTracerRuleCampaignsChangeTest {

    private static final RecommendationType[] CAMPAIGN_BECAME_ARCHIVED_TYPES = new RecommendationType[]{
            addSiteLinks, addImageToBanner, switchOnAutotargeting, sendLicensesPharmacy, changeAdGroupWithLowStat,
            addBannerDisplayHrefs, sendLicensesBanks, removePagesFromBlackListOfACampaign, dailyBudget, weeklyBudget,
            addAdditionItemCallouts, addTitleExtension, sendLicensesMedServices,
            increaseStrategyWeeklyBudget, increaseStrategyTargetCPA, decreaseStrategyTargetROI
    };
    private static final RecommendationType[] CAMPAIGN_CHANGE_STATUS_SHOW_TYPES = new RecommendationType[]{
            sendLicensesBanks, sendLicensesMedServices, sendLicensesPharmacy
    };
    private static final RecommendationType[] CAMPAIGN_CHANGE_PLATFORM_TO_SEARCH = new RecommendationType[]{
            addImageToBanner
    };
    private static final RecommendationType[] CAMPAIGN_CHANGE_STRATEGY_TYPES = new RecommendationType[]{
            dailyBudget, weeklyBudget, increaseStrategyWeeklyBudget, increaseStrategyTargetCPA,
            decreaseStrategyTargetROI
    };
    private static final RecommendationType[] CAMPAIGN_CHANGE_GEO_TYPES = new RecommendationType[]{
            sendLicensesBanks, sendLicensesMedServices, sendLicensesPharmacy
    };
    private static final RecommendationType[] CAMPAIGN_CHANGE_DAY_BUDGET_TYPES = new RecommendationType[]{
            dailyBudget
    };
    private static final RecommendationType[] CAMPAIGN_CHANGE_DONT_SHOW_TYPES = new RecommendationType[]{
            removePagesFromBlackListOfACampaign
    };
    private static final Long CID_VALUE = 50L;
    private static final Long CLIENT_ID_VALUE = 100L;
    @Autowired
    private RecomTracerRule rule;

    @Test
    void campaignBecameArchiveTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.ARCHIVED, "No", "Yes");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_BECAME_ARCHIVED_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_BECAME_ARCHIVED_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void campaignBecameNotArchiveTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.ARCHIVED, "Yes", "No");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        assertThat(got).hasSize(0);
    }


    @Test
    void campaignChangeStatusShowTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.STATUS_SHOW, "No", "Yes");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_CHANGE_STATUS_SHOW_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_CHANGE_STATUS_SHOW_TYPES);
        checkLogicObjectValues(got);
    }


    @Test
    void campaignChangePlatformToSearchTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.PLATFORM, "context", "search");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_CHANGE_PLATFORM_TO_SEARCH.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_CHANGE_PLATFORM_TO_SEARCH);
        checkLogicObjectValues(got);
    }

    @Test
    void campaignChangePlatformToContextTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.PLATFORM, "search", "context");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        assertThat(got).hasSize(0);
    }

    @Test
    void campaignChangeStrategyTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);
        campaignsTableChange.addChangedColumn(CAMPAIGNS.STRATEGY_NAME, "autobudget_max_reach",
                "autobudget_max_impressions");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_CHANGE_STRATEGY_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_CHANGE_STRATEGY_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void campaignChangeGeoTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);

        // geo поле текстовое, а бинлоги у нас включены в режиме NOBLOB, для теста не иммет значение поля
        campaignsTableChange.addChangedColumn(CAMPAIGNS.GEO, "no matter1",
                "no matter2");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_CHANGE_GEO_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_CHANGE_GEO_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void campaignChangeDayBudgetTest() {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CID_VALUE).withClientId(CLIENT_ID_VALUE);

        // geo поле текстовое, а бинлоги у нас включены в режиме NOBLOB, для теста не иммет значение поля
        campaignsTableChange.addChangedColumn(CAMPAIGNS.DAY_BUDGET, new BigDecimal(1),
                new BigDecimal(2));

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_CHANGE_DAY_BUDGET_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_CHANGE_DAY_BUDGET_TYPES);
        checkLogicObjectValues(got);
    }

    @Test
    void campaignChangeDontShowTest() {
        CampaignsTableChange campaignsTableChange = new CampaignsTableChange().withCid(50L).withClientId(100L);

        // dont_show поле текстовое, а бинлоги у нас включены в режиме NOBLOB, для теста не иммет значение поля
        campaignsTableChange.addChangedColumn(CAMPAIGNS.DONT_SHOW, "www.fishki.net",
                "www.fishki.net,www.afisha.ru");

        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);
        binlogEvent.withDb("ppc").withSource("devtest:ppc:1");

        List<RecomTracerLogicObject> got = rule.mapBinlogEvent(binlogEvent);
        List<RecommendationType> appliedRecommendationType = getRecommendationsTypes(got);

        assertThat(appliedRecommendationType).hasSize(CAMPAIGN_CHANGE_DONT_SHOW_TYPES.length);
        assertThat(appliedRecommendationType).containsExactlyInAnyOrder(CAMPAIGN_CHANGE_DONT_SHOW_TYPES);
        checkLogicObjectValues(got);
        long objectsContainsDownShow = got.stream()
                .filter(logicObject -> {
                    AdditionalColumns additionalColumns = logicObject.getAdditionalColumns();
                    return additionalColumns.contains(CAMPAIGNS.DONT_SHOW) &&
                            "www.fishki.net,www.afisha.ru".equals(additionalColumns.get(CAMPAIGNS.DONT_SHOW));
                })
                .count();
        assertThat(objectsContainsDownShow).isEqualTo(got.size());
    }

    private void checkLogicObjectValues(List<RecomTracerLogicObject> got) {
        long suitableObjectsCount = got.stream()
                .filter(logicObject -> !logicObject.isNeedLoad())
                .filter(logicObject -> Objects.isNull(logicObject.getPrimaryKey()))
                .filter(logicObject -> Objects.isNull(logicObject.getTableToLoad()))
                .filter(logicObject ->
                        logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.CLIENT_ID) && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID).equals(CLIENT_ID_VALUE)
                                && logicObject.isRecommendationKeyIdentifierPresent(RecommendationKeyIdentifier.CID) && logicObject.getRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID).equals(CID_VALUE)
                )
                .count();

        assertThat(suitableObjectsCount).

                isEqualTo(got.size());

    }
}
