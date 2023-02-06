package ru.yandex.direct.grid.processing.service.campaign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithForbiddenStrategyAddOperationSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddInternalDistribCampaign;
import ru.yandex.direct.validation.result.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.InternalCampaignWithRotationGoalIdValidator.MOBILE_ROTATION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.campaign.TestInternalDistribCampaigns.INTERNAL_DISTRIB_CAMPAIGN_PRODUCT_ID;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toCampaignAttributionModel;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toMeaningfulGoals;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdAddInternalDistribCampaignRequest;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationAddInternalDistribCampaignGraphqlServiceTest
        extends BaseCampaignMutationAddInternalCampaignGraphqlServiceTest {

    @Test
    public void addInternalDistribCampaign() {
        var gaAddInternalCampaign = getGdAddInternalDistribCampaignRequest();
        var gdAddCampaignUnion = new GdAddCampaignUnion().withInternalDistribCampaign(gaAddInternalCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequest(gdAddCampaignUnion);

        var expectedCampaign = getExpectedCampaign(gaAddInternalCampaign);
        InternalDistribCampaign campaign = fetchSingleCampaignFromDb(gdAddCampaignPayload);
        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY
                        .forFields(newPath(CampaignWithPackageStrategy.STRATEGY_ID.name())).useMatcher(nullValue()))));
    }

    @Test
    public void addInternalDistribCampaign_GetValidationError() {
        var gaAddInternalCampaign = getGdAddInternalDistribCampaignRequest()
                .withRotationGoalId(MOBILE_ROTATION_GOAL_ID)
                .withIsMobile(false);
        var gdAddCampaignUnion = new GdAddCampaignUnion().withInternalDistribCampaign(gaAddInternalCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequestWithValidationErrors(gdAddCampaignUnion);

        Path expectedPath = path(field(GdAddCampaigns.CAMPAIGN_ADD_ITEMS), index(0),
                field(GdAddInternalDistribCampaign.ROTATION_GOAL_ID));
        GdDefect expectedGdDefect = toGdDefect(expectedPath, inconsistentState());
        assertThat(gdAddCampaignPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);
    }


    private InternalDistribCampaign getExpectedCampaign(GdAddInternalDistribCampaign request) {
        var emailSettings = request.getNotification().getEmailSettings();

        return getCommonExpectedCampaign(request, InternalDistribCampaign::new)
                .withType(CampaignType.INTERNAL_DISTRIB)
                .withProductId(INTERNAL_DISTRIB_CAMPAIGN_PRODUCT_ID)
                .withRotationGoalId(request.getRotationGoalId())
                .withPlaceId(request.getPlaceId())
                .withIsMobile(request.getIsMobile())
                .withPageId(request.getPageId())
                .withStrategy(CampaignWithForbiddenStrategyAddOperationSupport.defaultStrategy())

                .withEnableOfflineStatNotice(emailSettings.getXlsReady())
                .withEnableCheckPositionEvent(emailSettings.getCheckPositionInterval() != null)
                .withCheckPositionIntervalEvent(toCampaignWarnPlaceInterval(emailSettings.getCheckPositionInterval()))
                .withMetrikaCounters(null)
                .withMeaningfulGoals(toMeaningfulGoals(request.getMeaningfulGoals()))
                .withAttributionModel(toCampaignAttributionModel(request.getAttributionModel()))
                .withIsSkadNetworkEnabled(false);
    }

}
