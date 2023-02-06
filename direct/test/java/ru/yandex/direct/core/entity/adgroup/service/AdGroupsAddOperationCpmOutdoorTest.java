package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.clientCpmOutdoorAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationCpmOutdoorTest extends AdGroupsAddOperationTestBase {

    private OutdoorPlacement placement;

    @Before
    public void before() {
        super.before();
        placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        geoTree = geoTreeFactory.getRussianGeoTree();
    }

    @Test
    public void prepareAndApply_Valid() {
        CampaignInfo campaign = campaignSteps.createActiveCpmBannerCampaign(clientInfo);
        CpmOutdoorAdGroup adGroup = clientCpmOutdoorAdGroup(campaign.getCampaignId(), placement);
        AdGroup expectedAdGroup = clientCpmOutdoorAdGroup(campaign.getCampaignId(), placement);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        AdGroup realAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup, beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_UnexistingPage_PageBlocksValidationConnected() {
        steps.placementSteps().clearPlacements();
        CampaignInfo campaign = campaignSteps.createActiveCpmBannerCampaign(clientInfo);
        CpmOutdoorAdGroup adGroup = clientCpmOutdoorAdGroup(campaign.getCampaignId(), null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(1L)
                        .withImpId(10L)));

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();


        Path path = path(index(0), field(CpmOutdoorAdGroup.PAGE_BLOCKS), index(0), field("pageId"));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path, objectNotFound())));
    }

    @Test
    public void prepareAndApply_SaveAsDraft_AdGroupDraft() {
        CpmOutdoorAdGroup adGroup = clientCpmOutdoorAdGroup(campaignId, placement);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, true);

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_SaveAsNotDraft_AdGroupReadyForModeration() {
        CpmOutdoorAdGroup adGroup = clientCpmOutdoorAdGroup(campaignId, placement);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, false);

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_SaveAsDraftInDraftCompany_AdGroupDraft() {
        CampaignInfo draftCampaign = createDraftCampaign();

        AdGroup adGroup = clientCpmOutdoorAdGroup(draftCampaign.getCampaignId(), placement);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, true);

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_SaveAsNotDraftInDraftCompany_AdGroupDraft() {
        CampaignInfo draftCampaign = createDraftCampaign();

        AdGroup adGroup = clientCpmOutdoorAdGroup(draftCampaign.getCampaignId(), placement);
        List<Long> ids = addAndCheckResultIsSuccessful(adGroup, false);

        AdGroup expectedGroup = new CpmOutdoorAdGroup()
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(ids.get(0)));
        assumeThat(adGroups, hasSize(1));
        assertThat(adGroups.get(0), beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_AddMinusKeywords_ValidationError() {
        CpmOutdoorAdGroup adGroup = clientCpmOutdoorAdGroup(campaignId, placement)
                .withMinusKeywords(singletonList("minusword"));

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(CpmYndxFrontpageAdGroup.MINUS_KEYWORDS)), AdGroupDefectIds.Gen.MINUS_KEYWORDS_NOT_ALLOWED)));
    }

    @Test
    public void prepareAndApply_addIntoAdGroupsWithGeoproduct_ValidationError() {
        steps.adGroupSteps().createActiveCpmGeoproductAdGroup(campaignInfo);
        CpmOutdoorAdGroup adGroup = clientCpmOutdoorAdGroup(campaignId, placement);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(adGroupTypeNotSupported().defectId())));
    }

    @Override
    protected CampaignInfo createModeratedCampaign() {
        return campaignSteps.createCampaign(new CampaignInfo().withCampaign(
                activeCpmBannerCampaign(null, null)
                        .withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.YES)
        ));
    }

    @Override
    protected CampaignInfo createDraftCampaign() {
        return campaignSteps.createCampaign(
                activeCpmBannerCampaign(null, null)
                        .withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NEW),
                clientInfo);
    }

    @Override
    protected CampaignInfo createRejectedCampaign() {
        return campaignSteps.createCampaign(
                activeCpmBannerCampaign(null, null)
                        .withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NO),
                clientInfo);
    }
}
