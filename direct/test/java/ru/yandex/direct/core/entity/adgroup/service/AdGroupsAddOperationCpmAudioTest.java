package ru.yandex.direct.core.entity.adgroup.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmAudioAdGroup;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationCpmAudioTest extends AdGroupsAddOperationTestBase {
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
        addAgencyFeature(FeatureName.DISABLE_CPM_AUDIO);
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
        bindFeatureToClient(FeatureName.DISABLE_CPM_AUDIO, agencyClient);

        MassResult<Long> result = addCpmGroup();
        assertThat(result, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_OnClientWithGBP_Invalid() {
        var client = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.GBP));
        initClientData(client);
        campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
        MassResult<Long> result = addCpmGroup();
        assertThat(result, isSuccessful(false));
    }

    private MassResult<Long> addCpmGroup() {
        CpmAudioAdGroup adGroup = clientAdGroup(campaignId);
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

    private static CpmAudioAdGroup clientAdGroup(Long campaignId) {
        return new CpmAudioAdGroup()
                .withType(AdGroupType.CPM_AUDIO)
                .withCampaignId(campaignId)
                .withName("test cpm group " + randomNumeric(5))
                .withGeo(asList(RUSSIA_REGION_ID));
    }
}
