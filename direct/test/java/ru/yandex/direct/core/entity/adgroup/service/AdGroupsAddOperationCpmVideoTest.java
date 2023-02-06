package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestPricePackages;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PricePackageInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.cpmPriceInvalidPriority;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationCpmVideoTest extends AdGroupsAddOperationTestBase {
    @Autowired
    private FeatureManagingService featureManagingService;
    @Autowired
    private AgencyClientRelationService agencyService;
    @Autowired
    private FeatureSteps featureSteps;
    @Autowired
    private UserSteps userSteps;
    @Autowired
    ClientSteps clientSteps;

    private UserInfo agencyClient;

    @Before
    public void before() {
        super.before();
        addAgencyFeature(FeatureName.DISABLE_CPM_VIDEO);
        agencyClient = createUser(RbacRole.AGENCY);
        bindToAgency(clientInfo, agencyClient);
    }

    @Test
    public void prepareAndApply_Valid() {
        MassResult<Long> result = addCpmGroup();
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_OnUserInDisabledAgency_Invalid() {

        bindFeatureToClient(FeatureName.DISABLE_CPM_VIDEO, agencyClient);

        MassResult<Long> result = addCpmGroup();
        assertThat(result, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_addIntoAdGroupsWithGeoproduct_ValidationError() {
        steps.adGroupSteps().createActiveCpmGeoproductAdGroup(campaignInfo);
        MassResult<Long> result = addCpmGroup();
        assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(adGroupTypeNotSupported().defectId())));
    }

    @Test
    public void prepareAndApply_DefaultPriorityAdGroupInCpmPriceVideoCampaign_ExpectedInvalidPriority() {
        MassResult<Long> result = prepareAndApplyPriorityInCpmPriceCampaign(PRIORITY_DEFAULT);
        assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(cpmPriceInvalidPriority().defectId())));
    }

    @Test
    public void prepareAndApply_SpecificPriorityAdGroupInCpmPriceVideoCampaign_ExpectedInvalidPriority() {
        MassResult<Long> result = prepareAndApplyPriorityInCpmPriceCampaign(PRIORITY_SPECIFIC);
        assertThat(result, isSuccessful(true));
    }

    private MassResult<Long> prepareAndApplyPriorityInCpmPriceCampaign(Long priority) {
        PricePackage defaultPricePackage = defaultPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withIsFrontpage(false)
                .withCurrency(clientInfo.getClient().getWorkCurrency())
                .withStatusApprove(StatusApprove.YES)
                .withClients(mapList(List.of(clientInfo),
                        TestPricePackages::allowedPricePackageClient));
        PricePackage pricePackage = steps.pricePackageSteps().createPricePackage(new PricePackageInfo().withPricePackage(defaultPricePackage)).getPricePackage();

        CpmPriceCampaign cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);

        CpmVideoAdGroup adGroup = clientCpmVideoAdGroup(cpmPriceCampaign.getId())
                .withGeo(pricePackage.getTargetingsFixed().getGeoExpanded())
                .withPriority(priority);

        AdGroupsAddOperation adGroupsAddOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        return adGroupsAddOperation.prepareAndApply();
    }

    private MassResult<Long> addCpmGroup() {
        CpmVideoAdGroup adGroup = clientCpmVideoAdGroup(campaignId);
        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        return addOperation.prepareAndApply();
    }

    private void bindFeatureToClient(FeatureName featureName, UserInfo agencyClient) {
        featureManagingService.enableFeatureForClient(agencyClient.getClientInfo().getClientId(), featureName);
    }

    private void addAgencyFeature(FeatureName featureName) {
        FeatureSettings featureSettings = featureSteps.getDefaultSettings();
        featureSettings.withIsAgencyFeature(true);
        featureSteps.addFeature(featureName.getName(), featureSettings);
    }

    private UserInfo createUser(RbacRole role) {
        return clientSteps.createDefaultClientWithRole(role).getChiefUserInfo();
    }

    private void bindToAgency(ClientInfo userInfo, UserInfo agencyClient) {
        agencyService.bindClients(agencyClient.getClientInfo().getClientId(),
                singleton(userInfo.getClientId()));
    }

    @Override
    protected CampaignInfo createModeratedCampaign() {
        return campaignSteps.createCampaign(new CampaignInfo().withCampaign(
                activeCpmBannerCampaign(null, null)
                        .withStatusModerate(StatusModerate.YES)
        ));
    }

    private static CpmVideoAdGroup clientCpmVideoAdGroup(Long campaignId) {
        return new CpmVideoAdGroup()
                .withType(AdGroupType.CPM_VIDEO)
                .withPriority(PRIORITY_SPECIFIC)
                .withCampaignId(campaignId)
                .withCriterionType(CriterionType.USER_PROFILE)
                .withName("test cpm video group " + randomNumeric(5))
                .withGeo(asList(RUSSIA_REGION_ID));
    }
}
