package ru.yandex.direct.core.entity.feature.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToClientIdState;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.Preconditions.checkState;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureManagingServiceTest {

    @Autowired
    private FeatureManagingService featureManagingService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private Steps steps;

    @Autowired
    private FeatureSteps featureSteps;

    @Test
    public void addFeaturesWithDefaultSettings_success() {
        Feature feature = featureSteps.getDefaultFeature();
        Result<List<Feature>> result =
                featureManagingService.addFeaturesWithDefaultSettings(Collections.singletonList(feature));
        checkState(result.isSuccessful(), "Фича успешно добавлена");

        Feature expectedFeature = new Feature()
                .withFeatureTextId(feature.getFeatureTextId())
                .withFeaturePublicName(feature.getFeaturePublicName())
                .withSettings(FeatureManagingService.getDefaultFeatureSettings());

        Feature actualFeature = getFeatureByTextId(feature.getFeatureTextId());

        assertThat(actualFeature, beanDiffer(expectedFeature).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addFeaturesWithIsAgencySettings_success() {
        Feature feature = featureSteps.getDefaultFeature();

        FeatureSettings featureSettings = FeatureManagingService.getDefaultFeatureSettings().withIsAgencyFeature(true);

        Result<List<Feature>> result =
                featureManagingService.addFeaturesWithSettings(
                        Collections.singletonList(feature),
                        featureSettings);
        checkState(result.isSuccessful(), "Фича успешно добавлена");

        Feature expectedFeature = new Feature()
                .withFeatureTextId(feature.getFeatureTextId())
                .withFeaturePublicName(feature.getFeaturePublicName())
                .withSettings(featureSettings);

        Feature actualFeature = getFeatureByTextId(feature.getFeatureTextId());

        assertThat(actualFeature, beanDiffer(expectedFeature).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void addFeaturesWithDefaultSettings_duplicateKey_exception() {
        Feature feature = featureSteps.getDefaultFeature();
        Result<List<Feature>> result1 =
                featureManagingService.addFeaturesWithDefaultSettings(Collections.singletonList(feature));
        checkState(result1.isSuccessful(), "Фича успешно добавлена");

        Result<List<Feature>> result2 =
                featureManagingService.addFeaturesWithDefaultSettings(Collections.singletonList(feature));

        Assertions.assertThat(result2.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(), CommonDefects.inconsistentStateAlreadyExists()))));
    }

    @Test
    public void addFeaturesWithDefaultSettings_nullFeatureParams_exception() {
        Feature feature = new Feature();
        Result<List<Feature>> result =
                featureManagingService.addFeaturesWithDefaultSettings(Collections.singletonList(feature));

        MatcherAssert.assertThat(result.getValidationResult(), CoreMatchers.allOf(
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feature.FEATURE_TEXT_ID)),
                        CommonDefects.notNull())),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feature.FEATURE_PUBLIC_NAME)),
                        CommonDefects.notNull()))
        ));
    }

    @Test
    public void addFeaturesWithDefaultSettings_emptyFeatureParams_exception() {
        Feature feature = new Feature()
                .withFeaturePublicName("")
                .withFeatureTextId("");
        Result<List<Feature>> result =
                featureManagingService.addFeaturesWithDefaultSettings(Collections.singletonList(feature));

        MatcherAssert.assertThat(result.getValidationResult(), CoreMatchers.allOf(
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feature.FEATURE_TEXT_ID)),
                        StringDefects.notEmptyString())),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(Feature.FEATURE_PUBLIC_NAME)),
                        StringDefects.notEmptyString()))
        ));
    }

    @Test
    public void deleteFeature_success() {
        Feature feature = featureSteps.getDefaultFeature();
        Result<List<Feature>> result = featureManagingService
                .addFeaturesWithDefaultSettings(Collections.singletonList(feature));
        checkState(result.isSuccessful(), "Фича успешно добавлена");

        featureManagingService.deleteFeature(feature.getId());

        Assertions.assertThat(getFeatureByTextId(feature.getFeatureTextId())).isNull();
    }

    @Test
    public void deleteFeature_checkClientFeatureTable_success() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        Feature feature = featureSteps.getDefaultFeature();
        Result<List<Feature>> result = featureManagingService
                .addFeaturesWithDefaultSettings(Collections.singletonList(feature));
        checkState(result.isSuccessful(), "Фича успешно добавлена");

        Long featureId = feature.getId();
        ClientFeature clientFeature = new ClientFeature()
                .withClientId(client.getClientId())
                .withId(featureId)
                .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(clientFeature);
        List<ClientFeature> clientFeaturesByFeatureId =
                steps.featureSteps().getClientsWithFeatures(singletonList(featureId)).get(featureId);
        checkState(clientFeaturesByFeatureId != null, "Фича успешно добавлена клиенту");

        featureManagingService.deleteFeature(featureId);

        Map<Long, List<ClientFeature>> clientsWithFeatures =
                steps.featureSteps().getClientsWithFeatures(singletonList(featureId));
        Assertions.assertThat(clientsWithFeatures.containsKey(featureId)).isFalse();
    }

    @Test
    public void deleteFeature_nonexistentId() {
        Long nonexistentId = -1L;
        Result<Long> result = featureManagingService.deleteFeature(nonexistentId);

        Assertions.assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(), CommonDefects.objectNotFound()))));
    }

    @Test
    public void getFeatureClientSummary_success() {
        ClientInfo client1 = steps.clientSteps().createDefaultClient();
        ClientInfo client2 = steps.clientSteps().createDefaultClient();
        Feature feature = featureSteps.getDefaultFeature();
        featureManagingService.addFeaturesWithDefaultSettings(singletonList(feature));
        List<FeatureTextIdToClientIdState> stateList = new ArrayList<>();
        stateList.add(new FeatureTextIdToClientIdState().withState(FeatureState.ENABLED)
                .withClientId(client1.getClientId()).withTextId(feature.getFeatureTextId()));
        stateList.add(new FeatureTextIdToClientIdState().withState(FeatureState.DISABLED)
                .withClientId(client2.getClientId()).withTextId(feature.getFeatureTextId()));
        featureManagingService.switchFeaturesStateForClientIds(stateList);
        Map<Long, Map<Boolean, Integer>> summary = featureManagingService.getFeaturesStateSummary();
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(summary).containsKey(feature.getId());
        sa.assertThat(summary.getOrDefault(feature.getId(), emptyMap())).containsEntry(true, 1);
        sa.assertThat(summary.getOrDefault(feature.getId(), emptyMap())).containsEntry(false, 1);
        sa.assertAll();
    }

    private Feature getFeatureByTextId(String textId) {
        Map<String, Feature> existingFeatures =
                listToMap(featureManagingService.getCachedFeatures(), Feature::getFeatureTextId);
        return existingFeatures.getOrDefault(textId, null);
    }
}
