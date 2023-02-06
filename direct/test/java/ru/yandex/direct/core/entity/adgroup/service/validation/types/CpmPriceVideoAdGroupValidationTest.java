package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupPriceSales;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.onlyOneDefaultAdGroupAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.priceSalesDisallowedAdGroupTypes;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultVideoAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CpmPriceVideoAdGroupValidationTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CpmVideoAdGroupValidation validation;

    private PricePackage pricePackage;
    private CpmPriceCampaign campaign;
    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients().getPricePackage();
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
    }

    @Test
    public void validateAdGroups_NoPriority() {
        var newDefaultAdGroup = clientDefaultAdGroup(campaign)
                .withPriority(null);

        var result = validation.validateAdGroups(clientId, List.of(newDefaultAdGroup));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.PRIORITY)),
                notNull()))));
    }

    @Test
    public void validateAdGroups_WrongPriority() {
        var newDefaultAdGroup = clientDefaultAdGroup(campaign)
                .withPriority(11L);

        var result = validation.validateAdGroups(clientId, List.of(newDefaultAdGroup));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.PRIORITY)),
                inCollection()))));
    }

    @Test
    public void validateModelChanges_ChangePriority() {
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        ModelChanges<CpmVideoAdGroup> changes = ModelChanges.build(adGroup, AdGroupPriceSales.PRIORITY, PRIORITY_DEFAULT);

        var result = validation.validateModelChanges(clientId, List.of(changes));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.PRIORITY)),
                forbiddenToChange()))));
    }

    @Test
    public void validateModelChanges_DefaultAdGroup_WrongGeo() {
        var pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);

        var changes = ModelChanges.build(adGroup, CpmYndxFrontpageAdGroup.GEO, List.of(CENTRAL_DISTRICT));

        var result = validation.validateModelChanges(clientId, List.of(changes));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(CpmYndxFrontpageAdGroup.GEO)),
                invalidValue()))));
    }

    @Test
    public void validateAddAdGroups_DefaultAdGroup_AlreadyHasDefaultAdGroup() {
        var existingDefaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        var newDefaultAdGroup = clientDefaultAdGroup(campaign)
                .withPriority(PRIORITY_DEFAULT);

        var result = validation.validateAddAdGroups(clientId, List.of(newDefaultAdGroup));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.PRIORITY)),
                onlyOneDefaultAdGroupAllowed()))));
    }

    @Test
    public void validateAddAdGroups_DefaultAdGroup_AlreadyHasSpecificAdGroup() {
        var existingSpecificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);
        var newDefaultAdGroup = clientDefaultAdGroup(campaign)
                .withPriority(PRIORITY_DEFAULT);

        var result = validation.validateAddAdGroups(clientId, List.of(newDefaultAdGroup));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.TYPE)),
                priceSalesDisallowedAdGroupTypes()
        ))));
    }

    @Test
    public void validateAddAdGroups_TwoDefaultAdGroupsInSameCampaign() {
        var adGroup1 = clientDefaultAdGroup(campaign)
                .withPriority(PRIORITY_DEFAULT);
        var adGroup2 = clientDefaultAdGroup(campaign)
                .withPriority(PRIORITY_DEFAULT);

        var result = validation.validateAddAdGroups(clientId, List.of(adGroup1, adGroup2));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.PRIORITY)),
                onlyOneDefaultAdGroupAllowed()))));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(1), field(AdGroupPriceSales.PRIORITY)),
                onlyOneDefaultAdGroupAllowed()))));
    }

    @Test
    public void validateAddAdGroups_TwoDefaultAdGroupsInDifferentCampaigns() {
        var campaign2 = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);

        var adGroup1 = clientDefaultAdGroup(campaign)
                .withPriority(PRIORITY_DEFAULT);
        var adGroup2 = clientDefaultAdGroup(campaign2)
                .withPriority(PRIORITY_DEFAULT);

        var result = validation.validateAddAdGroups(clientId, List.of(adGroup1, adGroup2));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.TYPE)),
                priceSalesDisallowedAdGroupTypes()
        ))));
    }

    @Test
    public void validateAddAdGroups_DefaultAdGroup_WrongGeo() {
        var pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);

        var adGroup = clientDefaultAdGroup(campaign)
                .withGeo(List.of(CENTRAL_DISTRICT))
                .withPriority(PRIORITY_DEFAULT);

        var result = validation.validateAddAdGroups(clientId, List.of(adGroup));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.GEO)),
                invalidValue()))));
    }

    private CpmVideoAdGroup clientDefaultAdGroup(CpmPriceCampaign campaign) {
        return new CpmVideoAdGroup()
                .withType(AdGroupType.CPM_VIDEO)
                .withCampaignId(campaign.getId())
                .withGeo(campaign.getFlightTargetingsSnapshot().getGeoExpanded())
                .withPriority(PRIORITY_DEFAULT);
    }

    @Test
    public void validateAddAdGroups_AdGroupType_InvalidAdGroupType() {
        var pricePackage = approvedPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var priceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage));
        var adGroup = activeDefaultVideoAdGroupForPriceSales(priceCampaign);

        var result = validation.validateAddAdGroups(clientId, List.of(adGroup));
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroupPriceSales.TYPE)),
                priceSalesDisallowedAdGroupTypes()))));
    }

}
