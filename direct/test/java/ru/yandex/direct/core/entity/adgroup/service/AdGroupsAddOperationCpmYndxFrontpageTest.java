package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CRIMEA_PROVINCE;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.UKRAINE;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationCpmYndxFrontpageTest extends AdGroupsAddOperationTestBase {

    @Test
    public void prepareAndApply_PositiveTest() {
        CpmYndxFrontpageAdGroup adGroup = clientCpmYndxFrontpageAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_PriorityInCpmPriceCampaign_ExpectedZero() {
        prepareAndApply_PriorityInCpmPriceCampaign_ExpectedValue(PRIORITY_DEFAULT);
    }

    @Test
    public void prepareAndApply_PriorityInCpmPriceCampaign_ExpectedOne() {
        prepareAndApply_PriorityInCpmPriceCampaign_ExpectedValue(PRIORITY_SPECIFIC);
    }

    private void prepareAndApply_PriorityInCpmPriceCampaign_ExpectedValue(Long priority) {
        CpmPriceCampaign cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);

        CpmYndxFrontpageAdGroup adGroup = clientCpmPriceAdGroup(cpmPriceCampaign)
                .withPriority(priority);

        AdGroupsAddOperation adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        Long resultAdGroupId = adGroupsAddOperation.prepareAndApply().get(0).getResult();

        CpmYndxFrontpageAdGroup actualGroup = (CpmYndxFrontpageAdGroup) adGroupRepository.getAdGroups(shard,
                singletonList(resultAdGroupId)).get(0);

        assertThat("Неверное значение приоритета", actualGroup.getPriority(), is(priority));
    }

    @Test
    public void prepareAndApply_PriorityInCpmPriceCampaign_ErrorIfMissing() {
        CpmPriceCampaign cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);

        CpmYndxFrontpageAdGroup adGroup = clientCpmPriceAdGroup(cpmPriceCampaign)
                .withPriority(null);

        AdGroupsAddOperation adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = adGroupsAddOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(CpmYndxFrontpageAdGroup.PRIORITY)), DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void prepareAndApply_PriorityInNotCpmPriceCampaign_ExpectedNull() {
        CpmYndxFrontpageAdGroup adGroup = clientCpmYndxFrontpageAdGroup(campaignId);

        AdGroupsAddOperation adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        Long resultAdGroupId = adGroupsAddOperation.prepareAndApply().get(0).getResult();

        CpmYndxFrontpageAdGroup actualGroup = (CpmYndxFrontpageAdGroup) adGroupRepository.getAdGroups(shard, singletonList(resultAdGroupId)).get(0);

        assertNull("Приоритет ожидался null", actualGroup.getPriority());
    }

    @Test
    public void prepareAndApply_PriorityInNotCpmPriceCampaign_Error() {
        CpmYndxFrontpageAdGroup adGroup = clientCpmYndxFrontpageAdGroup(campaignId)
                .withPriority(PRIORITY_DEFAULT);

        AdGroupsAddOperation adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = adGroupsAddOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(CpmYndxFrontpageAdGroup.PRIORITY)), DefectIds.MUST_BE_NULL)));
    }

    @Test
    public void prepareAndApply_AddMinusKeywords_ValidationError() {
        CpmYndxFrontpageAdGroup adGroup = clientCpmYndxFrontpageAdGroup(campaignId)
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
        CpmYndxFrontpageAdGroup adGroup = clientCpmYndxFrontpageAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(adGroupTypeNotSupported().defectId())));
    }

    @Test
    public void prepareAndApply_CpmPriceCampaign_RussianGeoTreeUsedForRussianClient() {
        var client = steps.clientSteps().createClient(defaultClient().withCountryRegionId(RUSSIA));
        initClientData(client);
        var pricePackage = createPricePackageWithRussiaGeoForClient(client);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);

        var clientAdGroup = clientCpmPriceAdGroup(campaign)
                .withGeo(List.of(RUSSIA));

        var adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(clientAdGroup));
        var result = adGroupsAddOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        var adGroupId = result.get(0).getResult();
        var adGroup = adGroupRepository.getAdGroups(client.getShard(), List.of(adGroupId)).get(0);
        assertThat(adGroup.getGeo(), equalTo(List.of(RUSSIA, CRIMEA_PROVINCE)));
    }

    @Test
    public void prepareAndApply_CpmPriceCampaign_RussianGeoTreeUsedForNonRussianClient() {
        var client = steps.clientSteps().createClient(defaultClient().withCountryRegionId(UKRAINE));
        initClientData(client);
        var pricePackage = createPricePackageWithRussiaGeoForClient(client);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackage);

        var clientAdGroup = clientCpmPriceAdGroup(campaign)
                .withGeo(List.of(RUSSIA));

        var adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(clientAdGroup));
        var result = adGroupsAddOperation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        var adGroupId = result.get(0).getResult();
        var adGroup = adGroupRepository.getAdGroups(client.getShard(), List.of(adGroupId)).get(0);
        assertThat(adGroup.getGeo(), equalTo(List.of(RUSSIA, CRIMEA_PROVINCE)));
    }

    @Override
    protected CampaignInfo createModeratedCampaign() {
        Campaign campaign = activeCpmYndxFrontpageCampaign(null, null)
                .withStatusModerate(StatusModerate.YES);
        return campaignSteps.createCampaign(new CampaignInfo().withCampaign(campaign));
    }

    private static CpmYndxFrontpageAdGroup clientCpmYndxFrontpageAdGroup(Long campaignId) {
        return new CpmYndxFrontpageAdGroup()
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withCampaignId(campaignId)
                .withName("test cpm frontpage group " + randomNumeric(5))
                .withGeo(singletonList(Region.RUSSIA_REGION_ID));
    }

    private static CpmYndxFrontpageAdGroup clientCpmPriceAdGroup(CpmPriceCampaign campaign) {
        return new CpmYndxFrontpageAdGroup()
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withCampaignId(campaign.getId())
                .withName("test cpm price group " + randomNumeric(5))
                .withGeo(campaign.getFlightTargetingsSnapshot().getGeoExpanded())
                .withPriority(PRIORITY_DEFAULT);
    }

    private PricePackage createPricePackageWithRussiaGeoForClient(ClientInfo client) {
        var pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(client)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return pricePackage;
    }
}
