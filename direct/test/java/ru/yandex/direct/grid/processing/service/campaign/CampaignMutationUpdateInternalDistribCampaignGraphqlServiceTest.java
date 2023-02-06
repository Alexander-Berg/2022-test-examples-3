package ru.yandex.direct.grid.processing.service.campaign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateInternalDistribCampaign;
import ru.yandex.direct.validation.result.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toCampaignAttributionModel;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toMeaningfulGoals;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdUpdateInternalDistribCampaignRequest;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationUpdateInternalDistribCampaignGraphqlServiceTest
        extends BaseCampaignMutationUpdateInternalCampaignGraphqlServiceTest {

    protected TypedCampaignInfo createTypedCampaign() {
        return steps.typedCampaignSteps().createDefaultInternalDistribCampaign();
    }

    @Test
    public void updateInternalDistribCampaign() {
        var gaUpdateInternalCampaign = getGdUpdateInternalDistribCampaignRequest(campaignInfo.getId());
        var gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withInternalDistribCampaign(gaUpdateInternalCampaign);
        GdUpdateCampaignPayload gdUpdateCampaignPayload = runRequest(gdUpdateCampaignUnion);

        var expectedCampaign = getExpectedCampaign(gaUpdateInternalCampaign);
        InternalDistribCampaign campaign = fetchSingleCampaignFromDb(gdUpdateCampaignPayload);
        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void updateInternalDistribCampaign_GetValidationError() {
        long invalidValue = -22;
        var gaUpdateInternalCampaign = getGdUpdateInternalDistribCampaignRequest(campaignInfo.getId())
                .withRotationGoalId(invalidValue);
        var gdUpdateCampaignUnion = new GdUpdateCampaignUnion().withInternalDistribCampaign(gaUpdateInternalCampaign);
        GdUpdateCampaignPayload gdUpdateCampaignPayload = runRequestWithValidationErrors(gdUpdateCampaignUnion);

        Path expectedPath = path(field(GdUpdateCampaigns.CAMPAIGN_UPDATE_ITEMS), index(0),
                field(GdUpdateInternalDistribCampaign.ROTATION_GOAL_ID));
        GdDefect expectedGdDefect = toGdDefect(expectedPath,
                greaterThanOrEqualTo(MIN_INTERNAL_CAMPAIGN_ROTATION_GOAL_ID), true);
        assertThat(gdUpdateCampaignPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);
    }

    private InternalDistribCampaign getExpectedCampaign(GdUpdateInternalDistribCampaign request) {
        var emailSettings = request.getNotification().getEmailSettings();

        var campaign = (InternalDistribCampaign) campaignInfo.getCampaign();
        fillCommonExpectedCampaign(campaign, request);

        return campaign
                .withRotationGoalId(request.getRotationGoalId())
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
