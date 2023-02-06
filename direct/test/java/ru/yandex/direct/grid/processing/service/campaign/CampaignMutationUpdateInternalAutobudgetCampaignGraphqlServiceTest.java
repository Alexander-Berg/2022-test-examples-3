package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateInternalAutobudgetCampaign;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toCampaignAttributionModel;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toMeaningfulGoals;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdUpdateInternalAutobudgetCampaignRequest;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationUpdateInternalAutobudgetCampaignGraphqlServiceTest
        extends BaseCampaignMutationUpdateInternalCampaignGraphqlServiceTest {

    protected TypedCampaignInfo createTypedCampaign() {
        return steps.typedCampaignSteps().createDefaultInternalAutobudgetCampaign();
    }

    @Test
    public void updateInternalAutobudgetCampaign() {
        var gaUpdateInternalCampaign = getGdUpdateInternalAutobudgetCampaignRequest(campaignInfo.getId());
        var gdUpdateCampaignUnion =
                new GdUpdateCampaignUnion().withInternalAutobudgetCampaign(gaUpdateInternalCampaign);
        GdUpdateCampaignPayload gdUpdateCampaignPayload = runRequest(gdUpdateCampaignUnion);

        var expectedCampaign = getExpectedCampaign(gaUpdateInternalCampaign);
        InternalAutobudgetCampaign campaign = fetchSingleCampaignFromDb(gdUpdateCampaignPayload);
        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void updateInternalAutobudgetCampaign_GetValidationError() {
        var gaUpdateInternalCampaign = getGdUpdateInternalAutobudgetCampaignRequest(campaignInfo.getId());
        BigDecimal minSum = Currencies.getCurrency(CurrencyCode.RUB).getMinAutobudget();
        BigDecimal invalidValue = minSum.subtract(BigDecimal.ONE);
        gaUpdateInternalCampaign.getBiddingStategy().getStrategyData()
                .setSum(invalidValue);
        var gdUpdateCampaignUnion =
                new GdUpdateCampaignUnion().withInternalAutobudgetCampaign(gaUpdateInternalCampaign);
        GdUpdateCampaignPayload gdUpdateCampaignPayload = runRequestWithValidationErrors(gdUpdateCampaignUnion);

        Path expectedPath = path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS), index(0),
                field(GdUpdateInternalAutobudgetCampaign.BIDDING_STATEGY.name()),
                field(GdCampaignBiddingStrategy.STRATEGY_DATA),
                field(GdCampaignStrategyData.SUM));
        GdDefect expectedGdDefect = toGdDefect(expectedPath, greaterThanOrEqualTo(minSum), true);
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);
    }

    private InternalAutobudgetCampaign getExpectedCampaign(GdUpdateInternalAutobudgetCampaign request) {
        var emailSettings = request.getNotification().getEmailSettings();

        var campaign = (InternalAutobudgetCampaign) campaignInfo.getCampaign();
        fillCommonExpectedCampaign(campaign, request);

        return campaign
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.AUTOBUDGET_AVG_CPA_PER_FILTER)
                        .withStrategyName(StrategyName.AUTOBUDGET)
                        .withPlatform(CampaignsPlatform.SEARCH)
                        .withAutobudget(CampaignsAutobudget.YES)
                        .withStrategyData(new StrategyData()
                                .withName(StrategyName.AUTOBUDGET.name().toLowerCase())
                                .withSum(request.getBiddingStategy().getStrategyData().getSum())
                                .withVersion(1L)
                                .withUnknownFields(emptyMap())))
                .withPageId(request.getPageId())
                .withImpressionRateCount(null)
                .withImpressionRateIntervalDays(null)
                .withMaxClicksCount(null)
                .withMaxClicksPeriod(null)
                .withMaxStopsCount(null)
                .withMaxStopsPeriod(null)

                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnableCheckPositionEvent(emailSettings.getCheckPositionInterval() != null)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(emailSettings.getCheckPositionInterval()))
                .withMetrikaCounters(null)
                .withMeaningfulGoals(toMeaningfulGoals(request.getMeaningfulGoals()))
                .withAttributionModel(toCampaignAttributionModel(request.getAttributionModel()));
    }

}
