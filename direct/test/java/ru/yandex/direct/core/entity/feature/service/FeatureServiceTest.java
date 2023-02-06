package ru.yandex.direct.core.entity.feature.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import one.util.streamex.EntryStream;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.util.Preconditions.checkState;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureServiceTest {

    private static final String NOT_EXISTING_FEATURE_NAME = "not_existing_feature";
    private static final String EXISTING_FEATURE_NAME_FOR_PERCENT_100 = "existing_feature_name_for_percent_100";
    private static final String EXISTING_FEATURE_NAME_FOR_PERCENT_0 = "existing_feature_name_for_percent_0";

    @Autowired
    private FeatureService featureService;

    @Autowired
    private FeatureManagingService featureManagingService;

    @Autowired
    private Steps steps;

    private final Map<String, ClientFeature> features = new HashMap<>();
    private final List<String> existingFeatureTextIds = new ArrayList<>();

    private ClientId clientId;
    private Feature enableFeature;
    private Feature disableFeature;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo());
        clientId = clientInfo.getClientId();
        enableFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientId)
                .withId(enableFeature.getId())
                .withState(FeatureState.ENABLED);
        features.put(enableFeature.getFeatureTextId(), firstClientFeature);
        disableFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature secondClientFeature = new ClientFeature()
                .withClientId(clientId)
                .withId(disableFeature.getId())
                .withState(FeatureState.DISABLED);
        features.put(disableFeature.getFeatureTextId(), secondClientFeature);
        steps.featureSteps().addClientFeatures(features.values());
        existingFeatureTextIds.addAll(features.keySet());
    }

    @Test
    public void getFeatureStates_success() {
        Map<String, FeatureState> featureStates = featureService.getFeatureStates(clientId, existingFeatureTextIds);

        Map<String, FeatureState> expectedFeatureStates = EntryStream.of(features)
                .mapValues(ClientFeature::getState)
                .toMap();
        assertThat(featureStates, beanDiffer(expectedFeatureStates));
    }

    @Test
    public void getFeatureStatesWithNonExistingFeature_success() {
        existingFeatureTextIds.add(NOT_EXISTING_FEATURE_NAME);
        Map<String, FeatureState> featureStates = featureService.getFeatureStates(clientId, existingFeatureTextIds);

        Map<String, FeatureState> expectedFeatureStates = EntryStream.of(features)
                .mapValues(ClientFeature::getState)
                .append(NOT_EXISTING_FEATURE_NAME, FeatureState.UNKNOWN)
                .toMap();
        assertThat(featureStates, beanDiffer(expectedFeatureStates));
    }

    @Test
    public void getFeatureStatesWithNotClientFeature_success() {
        Feature notClientFeature = steps.featureSteps().addDefaultFeature();
        String notClientFeatureName = notClientFeature.getFeatureTextId();
        existingFeatureTextIds.add(notClientFeatureName);
        Map<String, FeatureState> featureStates = featureService.getFeatureStates(clientId, existingFeatureTextIds);

        Map<String, FeatureState> expectedFeatureStates = EntryStream.of(features)
                .mapValues(ClientFeature::getState)
                .append(notClientFeatureName, FeatureState.DISABLED)
                .toMap();
        assertThat(featureStates, beanDiffer(expectedFeatureStates));
    }

    @Test
    public void getPublicForClientId_enabled_success() {
        checkState(featureService.isEnabledForClientId(new FeatureRequest().withClientId(clientId),
                enableFeature.getFeatureTextId()),
                "Фича включена для клиента");
        checkState(!featureService.isEnabledForClientId(new FeatureRequest().withClientId(clientId),
                disableFeature.getFeatureTextId()),
                "Фича выключена для клиента");
        featureManagingService.updateFeaturePublicity(enableFeature.getFeatureTextId(), true);

        Set<String> publicForClientId = featureService.getPublicForClientId(
                new FeatureRequest().withClientId(clientId));
        assertThat(publicForClientId, hasSize(1));
        assertThat(publicForClientId, contains(enableFeature.getFeatureTextId()));
    }

    @Test
    public void getFeatureStates_enabledOnPercent_success() {
        steps.featureSteps().addFeature(EXISTING_FEATURE_NAME_FOR_PERCENT_100, new FeatureSettings().withPercent(100));

        checkState(featureService.isEnabledForClientId(new FeatureRequest().withClientId(clientId),
                EXISTING_FEATURE_NAME_FOR_PERCENT_100),
                "Фича выключена для клиента");

        Map<String, FeatureState> featureStates = featureService.getFeatureStates(
                clientId, List.of(EXISTING_FEATURE_NAME_FOR_PERCENT_100));
        assertThat(featureStates, hasEntry(EXISTING_FEATURE_NAME_FOR_PERCENT_100, FeatureState.ENABLED));
    }

    @Test
    public void getFeatureStates_disabledOnPercent_success() {
        steps.featureSteps().addFeature(EXISTING_FEATURE_NAME_FOR_PERCENT_0, new FeatureSettings().withPercent(0));

        checkState(!featureService.isEnabledForClientId(new FeatureRequest().withClientId(clientId),
                EXISTING_FEATURE_NAME_FOR_PERCENT_0),
                "Фича включена для клиента");

        Map<String, FeatureState> featureStates = featureService.getFeatureStates(
                clientId, List.of(EXISTING_FEATURE_NAME_FOR_PERCENT_0));
        assertThat(featureStates, hasEntry(EXISTING_FEATURE_NAME_FOR_PERCENT_0, FeatureState.DISABLED));
    }

    @Test
    public void isFeatureEnabledByPercent_goodDistribution() {
        var percent = 30;
        var testsCount = 1000;

        var feature = new Feature()
                .withId(1L)
                .withSettings(new FeatureSettings().withPercent(percent));

        var successes = LongStream.range(1, testsCount)
                .filter(x -> FeatureService.isFeatureEnabledByPercent(feature, x) == FeatureState.ENABLED)
                .count();

        Assertions.assertThat(100.0 * successes / testsCount)
                .isCloseTo(percent, Percentage.withPercentage(5));
    }
}
