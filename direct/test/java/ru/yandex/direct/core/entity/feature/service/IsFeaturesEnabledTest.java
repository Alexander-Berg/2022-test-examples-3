package ru.yandex.direct.core.entity.feature.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.container.ClientRealFeature;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFeatures;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IsFeaturesEnabledTest {
    @Autowired
    private FeatureSteps featureSteps;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private FeatureManagingService featureManagingService;

    @Autowired
    private ClientFeaturesRepository clientFeaturesRepository;


    @Test
    public void checkRoleAccess() {
        var defaultUserInfo = clientSteps.createDefaultClient();
        var feature = featureSteps.addDefaultFeature(defaultUserInfo.getUid(),
                featureSteps.getDefaultSettings().withRoles(Set.of("CLIENT")));
        var enabledFeatures =
                featureService.getEnabledForClientId(new FeatureRequest().withClientId(defaultUserInfo.getClientId()));
        assertThat(enabledFeatures, hasItem(feature.getFeatureTextId()));
    }


    @Test
    public void checkPercentAccess() {
        var defaultUserInfo = userSteps.createDefaultUser();
        var feature = featureSteps.addDefaultFeature(defaultUserInfo.getUid(),
                featureSteps.getDefaultSettings().withPercent(100));
        var enabledFeatures =
                featureService.getEnabledForClientId(new FeatureRequest().withClientId(defaultUserInfo.getClientInfo().getClientId()));
        assertThat(enabledFeatures, hasItem(feature.getFeatureTextId()));
    }


    @Test
    public void checkPercentNotAccess() {
        var defaultUserInfo = userSteps.createDefaultUser();
        var feature = featureSteps.addDefaultFeature(defaultUserInfo.getUid(),
                featureSteps.getDefaultSettings().withPercent(0));
        var enabledFeatures =
                featureService.getEnabledForClientId(new FeatureRequest().withClientId(defaultUserInfo.getClientInfo().getClientId()));
        assertThat(enabledFeatures, not(hasItem(feature.getFeatureTextId())));
    }


    @Test
    public void checkKnownFeatures_AddThenEnable() {
        var clientId = clientSteps.createDefaultClient().getClientId();

        String featureName = TestFeatures.newFeatureName("some_feature_to_test_");
        featureSteps.addFeature(featureName);

        Map<String, Feature> featuresByTextId = listToMap(featureManagingService.getCachedFeatures(),
                Feature::getFeatureTextId);
        Long featureId = featuresByTextId.get(featureName).getId();

        var allFeatures = Sets.newHashSet(featureService.getAllExistingFeatureTextIds());
        assertThat("Фича не добавлена", allFeatures, hasItem(featureName));

        setFeatureToClient(clientId, featureId, FeatureState.DISABLED);

        var feature = getFeatureById(featureId, clientId);
        assertThat("Фича должна быть выключена", feature.getFeatureState(), is(FeatureState.DISABLED));

        setFeatureToClient(clientId, featureId, FeatureState.ENABLED);

        feature = getFeatureById(featureId, clientId);
        assertThat("Фича должна быть включена", feature.getFeatureState(), is(FeatureState.ENABLED));
    }


    private void setFeatureToClient(ClientId clientId, Long featureId, FeatureState featureState) {
        var clientFeature = new ClientFeature()
                .withClientId(clientId)
                .withId(featureId)
                .withState(featureState);

        clientFeaturesRepository.addClientsFeatures(List.of(clientFeature));
        featureService.clearCaches();
    }

    private ClientRealFeature getFeatureById(Long id, ClientId clientId) {
        var result = featureService
                .getAllKnownFeaturesForClient(new FeatureRequest().withClientId(clientId))
                .stream()
                .filter(clientRealFeature -> clientRealFeature.getFeature().getId().equals(id))
                .findFirst()
                .orElse(null);
        if (result == null) {
            Assert.fail("Не найдено фичи для клинта по заданному имени");
        }
        return result;
    }
}
