package ru.yandex.direct.core.entity.feature.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.EntryStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToClientIdState;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IsAgencyFeaturesEnabledTest {
    @Autowired
    private FeatureSteps featureSteps;
    @Autowired
    private UserSteps userSteps;
    @Autowired
    ClientSteps clientSteps;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private AgencyClientRelationService agencyService;
    @Autowired
    private FeatureManagingService featureManagingService;

    private UserInfo defaultUser;
    private UserInfo agencyClient;

    private static final long FEATURE_1 = 100001;
    private static final long FEATURE_2 = 100004;

    @Before
    public void before() {
        defaultUser = createUser(RbacRole.CLIENT);
        agencyClient = createUser(RbacRole.AGENCY);
        bindToAgency(defaultUser, agencyClient);
    }

    @After
    public void after() {
        featureManagingService.deleteFeature(FEATURE_1);
    }

    @Test
    public void checkIsAgencyFeatureIsSet() {
        Feature feature = getFeature(true, FEATURE_1);
        bindFeature(agencyClient, feature);
        var clientIdsFeatures =
                featureService.getEnabledForClientId(new FeatureRequest().withClientId(defaultUser.getClientInfo().getClientId()));
        assertThat(clientIdsFeatures).contains(feature.getFeatureTextId());
    }

    @Test
    public void checkIsAgencyFeatureIsNotSet() {
        Feature feature = getFeature(false, FEATURE_1);
        bindFeature(agencyClient, feature);
        var clientIdsFeatures =
                featureService.getEnabledForClientId(new FeatureRequest().withClientId(defaultUser.getClientInfo().getClientId()));
        assertThat(clientIdsFeatures).doesNotContain(feature.getFeatureTextId());
    }

    @Test
    public void checkIsAgencyFeatureSetSeveralClients() {
        UserInfo defaultUser2 = createUser(RbacRole.CLIENT);
        UserInfo defaultUser3 = createUser(RbacRole.CLIENT);
        bindToAgency(defaultUser2, agencyClient);
        Feature feature = getFeature(true, agencyClient.getUid());
        bindFeature(agencyClient, feature);
        var clientIds =
                Stream.of(defaultUser, defaultUser2, defaultUser3).map(UserInfo::getClientId).collect(Collectors.toSet());
        var clientIdsWithFeatures = EntryStream.of(featureService.getEnabled(clientIds))
                .filterValues(features -> !features.isEmpty())
                .keys().toSet();
        assertThat(clientIdsWithFeatures).containsAll(List.of(defaultUser.getClientId(), defaultUser2.getClientId()));
    }

    @Test
    public void checkSeveralAgencyWithOneClient() {
        Feature feature = getFeature(true, FEATURE_1);
        bindFeature(agencyClient, feature);
        UserInfo agencyClient2 = createUser(RbacRole.AGENCY);
        bindToAgency(defaultUser, agencyClient2);
        Feature feature2 = getFeature(true, FEATURE_2);
        bindFeature(agencyClient2, feature2);

        var clientIdsFeatures =
                featureService.getEnabledForClientId(new FeatureRequest().withClientId(defaultUser.getClientInfo().getClientId()));
        assertThat(clientIdsFeatures)
                        .containsAll(List.of(feature.getFeatureTextId(), feature2.getFeatureTextId()));

        // cleanup
        featureManagingService.deleteFeature(FEATURE_2);
    }

    private void bindToAgency(UserInfo userInfo, UserInfo agencyClient) {
        agencyService.bindClients(agencyClient.getClientInfo().getClientId(),
                singleton(userInfo.getClientInfo().getClientId()));
    }

    private UserInfo createUser(RbacRole role) {
        return clientSteps.createDefaultClientWithRole(role).getChiefUserInfo();
    }

    private Feature getFeature(boolean isAgensy, Long featureId) {
        FeatureSettings featureSettings = featureSteps.getDefaultSettings();
        if (isAgensy) {
            featureSettings.withIsAgencyFeature(true);
        }
        return featureSteps.addDefaultFeature(featureId, featureSettings);
    }

    private void bindFeature(UserInfo userInfo, Feature feature) {
        FeatureTextIdToClientIdState featureTextIdToClientIdState =
                getFeatureTextIdToClientIdState(userInfo, feature);
        featureManagingService
                .switchFeaturesStateForClientIds(singletonList(featureTextIdToClientIdState));
    }

    private FeatureTextIdToClientIdState getFeatureTextIdToClientIdState(UserInfo userInfo, Feature feature) {
        FeatureState status = FeatureState.ENABLED;
        return new FeatureTextIdToClientIdState()
                .withClientId(userInfo.getClientInfo().getClientId())
                .withTextId(feature.getFeatureTextId())
                .withState(status);
    }
}
