package ru.yandex.direct.grid.processing.service.campaign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignWithForbiddenStrategyAddOperationSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddInternalFreeCampaign;
import ru.yandex.direct.validation.result.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_INTERNAL_CAMPAIGN_RESTRICTION_VALUE;
import static ru.yandex.direct.core.testing.data.campaign.TestInternalFreeCampaigns.INTERNAL_FREE_CAMPAIGN_PRODUCT_ID;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toCampaignAttributionModel;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toInternalCampaignRestrictionType;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toCampaignWarnPlaceInterval;
import static ru.yandex.direct.grid.processing.service.campaign.converter.CommonCampaignConverter.toMeaningfulGoals;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.getGdAddInternalFreeCampaignRequest;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignMutationAddInternalFreeCampaignGraphqlServiceTest
        extends BaseCampaignMutationAddInternalCampaignGraphqlServiceTest {


    @Test
    public void addInternalFreeCampaign() {
        var gaAddInternalCampaign = getGdAddInternalFreeCampaignRequest();
        var gdAddCampaignUnion = new GdAddCampaignUnion().withInternalFreeCampaign(gaAddInternalCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequest(gdAddCampaignUnion);

        var expectedCampaign = getExpectedCampaign(gaAddInternalCampaign);
        InternalFreeCampaign campaign = fetchSingleCampaignFromDb(gdAddCampaignPayload);
        assertThat(campaign)
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY
                        .forFields(newPath(CampaignWithPackageStrategy.STRATEGY_ID.name())).useMatcher(nullValue()))));
    }

    @Test
    public void addInternalFreeCampaign_GetValidationError() {
        long invalidValue = -11;
        var gaAddInternalCampaign = getGdAddInternalFreeCampaignRequest()
                .withRestrictionValue(invalidValue);
        var gdAddCampaignUnion = new GdAddCampaignUnion().withInternalFreeCampaign(gaAddInternalCampaign);
        GdAddCampaignPayload gdAddCampaignPayload = runRequestWithValidationErrors(gdAddCampaignUnion);

        Path expectedPath = path(field(GdAddCampaigns.CAMPAIGN_ADD_ITEMS), index(0),
                field(GdAddInternalFreeCampaign.RESTRICTION_VALUE));
        GdDefect expectedGdDefect = toGdDefect(expectedPath,
                greaterThanOrEqualTo(MIN_INTERNAL_CAMPAIGN_RESTRICTION_VALUE), true);
        assertThat(gdAddCampaignPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);
    }

    private InternalFreeCampaign getExpectedCampaign(GdAddInternalFreeCampaign request) {
        var emailSettings = request.getNotification().getEmailSettings();

        return getCommonExpectedCampaign(request, InternalFreeCampaign::new)
                .withType(CampaignType.INTERNAL_FREE)
                .withProductId(INTERNAL_FREE_CAMPAIGN_PRODUCT_ID)
                .withRestrictionValue(request.getRestrictionValue())
                .withRestrictionType(toInternalCampaignRestrictionType(request.getRestrictionType()))
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
