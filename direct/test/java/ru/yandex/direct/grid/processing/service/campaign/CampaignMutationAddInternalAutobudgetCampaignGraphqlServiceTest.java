package ru.yandex.direct.grid.processing.service.campaign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.RfCloseByClickType;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.grid.model.campaign.GdInternalCampaignImpressionFrequencyControl;
import ru.yandex.direct.grid.model.campaign.GdRfCloseByClick;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddInternalAutobudgetCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.InternalCampaignWithRotationGoalIdValidator.MOBILE_ROTATION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.campaign.TestInternalAutobudgetCampaigns.INTERNAL_AUTOBUDGET_CAMPAIGN_PRODUCT_ID;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toCampaignAttributionModel;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toMeaningfulGoals;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdAddInternalAutobudgetCampaignRequest;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationAddInternalAutobudgetCampaignGraphqlServiceTest
        extends BaseCampaignMutationAddInternalCampaignGraphqlServiceTest {

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    @Test
    public void addInternalAutobudgetCampaign_GetValidationError_WhenAddMobileRotationGoalId_ForNotMobileCampaign() {
        // тест проверяет что срабатывает валидация, если добавить goalId=3 для немобильной кампании
        // так же проверяется что ошибка валидации будет на поле goalId стратегии, а не на rotationGoalId
//        prepareMetrikaGoalsService();

        var gdAddInternalAutobudgetCampaign = getGdAddInternalAutobudgetCampaignRequest();
        gdAddInternalAutobudgetCampaign.getBiddingStategy().getStrategyData().setGoalId(MOBILE_ROTATION_GOAL_ID);
        var gdAddCampaignUnion = new GdAddCampaignUnion()
                .withInternalAutobudgetCampaign(gdAddInternalAutobudgetCampaign);

        GdAddCampaignPayload gdAddCampaignPayload = runRequestWithValidationErrors(gdAddCampaignUnion);

        Path expectedPath = path(field(GdAddCampaigns.CAMPAIGN_ADD_ITEMS), index(0),
                field(GdAddInternalAutobudgetCampaign.BIDDING_STATEGY),
                field(GdCampaignBiddingStrategy.STRATEGY_DATA),
                field(GdCampaignStrategyData.GOAL_ID));
        GdDefect expectedGdDefect = toGdDefect(expectedPath, inconsistentState());
        assertThat(gdAddCampaignPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);
    }

//    private void prepareMetrikaGoalsService() {
//        // metrikaGoalsService замокан, надо для автобюджетных кампаний возвращать мобильную цель
//        CampaignTypeWithCounterIds campaignTypeWithCounterIds = new CampaignTypeWithCounterIds()
//                .withCampaignType(CampaignType.INTERNAL_AUTOBUDGET)
//                .withCounterIds(Collections.emptySet());
//        doReturn(Map.of(campaignTypeWithCounterIds, Set.of(MOBILE_ROTATION_GOAL_ID)))
//                .when(metrikaGoalsService)
//                .getAvailableStrategyGoalIdsForNewCampaigns(
//                        eq(operator.getUid()), eq(clientInfo.getClientId()), any(), any());
//    }

    @Test
    public void addInternalAutobudgetCampaign() {
        var gdAddInternalAutobudgetCampaign = getGdAddInternalAutobudgetCampaignRequest();
        var gdAddCampaignUnion = new GdAddCampaignUnion()
                .withInternalAutobudgetCampaign(gdAddInternalAutobudgetCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequest(gdAddCampaignUnion);

        InternalAutobudgetCampaign campaign = fetchSingleCampaignFromDb(gdAddCampaignPayload);
        var expectedCampaign = getExpectedCampaign(gdAddInternalAutobudgetCampaign);

        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void addInternalAutobudgetCampaignWithImpressionFrequencyControl() {
        GdInternalCampaignImpressionFrequencyControl impressionFrequencyControl =
                new GdInternalCampaignImpressionFrequencyControl()
                        .withDays(1)
                        .withImpressions(100)
                        .withMaxClicksCount(7)
                        .withMaxClicksPeriod(0)
                        .withMaxStopsCount(5)
                        .withMaxStopsPeriod(1000);
        var gdAddInternalAutobudgetCampaign = getGdAddInternalAutobudgetCampaignRequest()
                .withImpressionFrequencyControl(impressionFrequencyControl)
                .withRfCloseByClick(GdRfCloseByClick.ON_AD_GROUP);

        var gdAddCampaignUnion = new GdAddCampaignUnion()
                .withInternalAutobudgetCampaign(gdAddInternalAutobudgetCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequest(gdAddCampaignUnion);

        InternalAutobudgetCampaign campaign = fetchSingleCampaignFromDb(gdAddCampaignPayload);
        var expectedCampaign = getExpectedCampaign(gdAddInternalAutobudgetCampaign)
                .withImpressionRateCount(impressionFrequencyControl.getImpressions())
                .withImpressionRateIntervalDays(impressionFrequencyControl.getDays())
                .withMaxClicksCount(impressionFrequencyControl.getMaxClicksCount())
                .withMaxClicksPeriod(CampaignConstants.MAX_CLICKS_AND_STOPS_PERIOD_WHOLE_CAMPAIGN_VALUE)
                .withMaxStopsCount(impressionFrequencyControl.getMaxStopsCount())
                .withMaxStopsPeriod(impressionFrequencyControl.getMaxStopsPeriod())
                .withRfCloseByClick(RfCloseByClickType.ADGROUP);

        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void addInternalAutobudgetCampaignWithoutCheckPositionInterval() {
        var gdAddInternalAutobudgetCampaign = getGdAddInternalAutobudgetCampaignRequest();
        gdAddInternalAutobudgetCampaign.getNotification().getEmailSettings()
                .setCheckPositionInterval(null);

        var gdAddCampaignUnion = new GdAddCampaignUnion()
                .withInternalAutobudgetCampaign(gdAddInternalAutobudgetCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequest(gdAddCampaignUnion);
        InternalAutobudgetCampaign campaign = fetchSingleCampaignFromDb(gdAddCampaignPayload);

        var expectedCampaign = getExpectedCampaign(gdAddInternalAutobudgetCampaign)
                .withEnableCheckPositionEvent(false)
                .withCheckPositionIntervalEvent(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL);

        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    private InternalAutobudgetCampaign getExpectedCampaign(GdAddInternalAutobudgetCampaign request) {
        var emailSettings = request.getNotification().getEmailSettings();

        return getCommonExpectedCampaign(request, InternalAutobudgetCampaign::new)
                .withType(CampaignType.INTERNAL_AUTOBUDGET)
                .withProductId(INTERNAL_AUTOBUDGET_CAMPAIGN_PRODUCT_ID)
                .withPlaceId(request.getPlaceId())
                .withIsMobile(request.getIsMobile())
                .withPageId(request.getPageId())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategyName(StrategyName.AUTOBUDGET)
                        .withPlatform(CampaignsPlatform.BOTH)
                        .withAutobudget(CampaignsAutobudget.YES)
                        .withStrategyData(new StrategyData()
                                .withName(StrategyName.AUTOBUDGET.name().toLowerCase())
                                .withSum(request.getBiddingStategy().getStrategyData().getSum())
                                .withVersion(1L)
                                .withUnknownFields(emptyMap())))

                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnableCheckPositionEvent(emailSettings.getCheckPositionInterval() != null)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(emailSettings.getCheckPositionInterval()))
                .withMetrikaCounters(null)
                .withMeaningfulGoals(toMeaningfulGoals(request.getMeaningfulGoals()))
                .withAttributionModel(toCampaignAttributionModel(request.getAttributionModel()))
                .withIsSkadNetworkEnabled(false);
    }

}
