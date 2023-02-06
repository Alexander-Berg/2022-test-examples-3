package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToPercent;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureIdToClientId;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.entity.feature.service.DirectAuthContextService;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

public class FeatureSteps {
    public static final int PERCENT = 40;
    private static final String FEATURE_NAME = "feature_name";
    private static final String FEATURE_TEXT_ID = "feature_text_id";
    private static final Set<String> ADDED_FEATURES = new HashSet<>();
    private static final AtomicLong FEATURE_COUNTER = new AtomicLong(10000L);
    private final FeatureManagingService featureManagingService;
    private final ClientFeaturesRepository clientFeaturesRepository;
    private final DirectAuthContextService directAuthContextService;

    public FeatureSteps(FeatureManagingService featureManagingService,
                        ClientFeaturesRepository clientFeaturesRepository,
                        DirectAuthContextService directAuthContextService) {
        this.featureManagingService = featureManagingService;
        this.clientFeaturesRepository = clientFeaturesRepository;
        this.directAuthContextService = directAuthContextService;
    }

    public void addFeatures(List<Feature> features) {
        featureManagingService.addFeatures(features);
    }

    public Feature addDefaultFeature(Long id) {
        Feature feature = getDefaultFeature(id);
        featureManagingService.addFeatures(List.of(feature));
        return feature;
    }

    public Feature addDefaultFeature() {
        Feature feature = getDefaultFeature();
        featureManagingService.addFeatures(List.of(feature));
        return feature;
    }

    public Feature addDefaultFeature(Long id, FeatureSettings featureSettings) {
        Feature feature = getDefaultFeature(id).withSettings(featureSettings);
        featureManagingService.addFeatures(List.of(feature));
        return feature;
    }

    public Feature getDefaultFeature(Long id) {
        return new Feature().withFeaturePublicName(FEATURE_NAME + id)
                .withFeatureTextId(FEATURE_TEXT_ID + id)
                .withId(id)
                .withSettings(getDefaultSettings());
    }

    public Feature getDefaultFeature() {
        long id = FEATURE_COUNTER.incrementAndGet();
        return new Feature().withFeaturePublicName(FEATURE_NAME + id)
                .withFeatureTextId(FEATURE_TEXT_ID + id)
                .withId(id)
                .withSettings(getDefaultSettings());
    }

    public List<Feature> getFeatures() {
        return featureManagingService.getCachedFeatures();
    }

    public Feature getFeature(Long featureId) {
        List<Feature> features = featureManagingService.getCachedFeatures(List.of(featureId));
        if (features.isEmpty()) {
            throw new IllegalArgumentException("no feature " + featureId);
        }

        return features.get(0);
    }

    public Feature getFeature(String featureTextId) {
        return featureManagingService.getCachedFeatures().stream()
                .filter(x -> x.getFeatureTextId().equals(featureTextId))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("no feature " + featureTextId));
    }


    public Map<Long, List<ClientFeature>> getClientsWithFeatures(List<Long> featureIds) {
        Map<Long, List<ClientFeature>> clientsWithFeatures =
                clientFeaturesRepository.getClientsWithFeatures(featureIds, FeatureState.ENABLED);
        clientsWithFeatures.putAll(clientFeaturesRepository.getClientsWithFeatures(featureIds, FeatureState.DISABLED));
        clientsWithFeatures.putAll(clientFeaturesRepository.getClientsWithFeatures(featureIds, FeatureState.UNKNOWN));
        return clientsWithFeatures;
    }

    public FeatureState getClientFeatureStatus(FeatureIdToClientId featureIdToClientId) {
        List<ClientFeature> switchFeatureToClientList =
                clientFeaturesRepository.getClientsFeaturesStatus(Collections.singleton(featureIdToClientId));
        if (switchFeatureToClientList.isEmpty()) {
            return FeatureState.UNKNOWN;
        }
        return switchFeatureToClientList.get(0).getState();
    }

    public void addClientFeatures(Collection<ClientFeature> featureIdsToClientId) {
        clientFeaturesRepository.addClientsFeatures(featureIdsToClientId);
        featureManagingService.clearCaches();
    }

    public void addClientFeature(ClientFeature featureIdToClientId) {
        addClientFeatures(Collections.singleton(featureIdToClientId));
    }

    public FeatureSettings getDefaultSettings() {
        return new FeatureSettings()
                .withPercent(0)
                .withRoles(emptySet())
                .withIsPublic(false)
                .withIsAccessibleAfterDisabling(false);
    }

    public void addFeature(FeatureName featureName) {
        addFeature(featureName.getName());
    }

    public void setCurrentOperator(Long uid) {
        directAuthContextService.usingOperatorUid(uid);
    }

    public void setCurrentClient(ClientId clientId) {
        directAuthContextService.usingClientId(clientId);
    }

    public void setCurrentClientAndOperator(ClientInfo clientInfo) {
        setCurrentOperator(clientInfo.getUid());
        setCurrentClient(clientInfo.getClientId());
    }


    public void enableClientFeature(FeatureName featureName) {
        addClientFeature(directAuthContextService.getClientId(), featureName, true);
    }

    public void enableClientFeature(ClientId clientId, FeatureName featureName) {
        addClientFeature(clientId, featureName, true);
    }

    public void addClientFeature(ClientId clientId, FeatureName featureName, boolean enabled) {
        addFeature(featureName);

        //noinspection OptionalGetWithoutIsPresent
        Long featureId = getFeatures().stream()
                .filter(f -> f.getFeatureTextId().equals(featureName.getName()))
                .map(Feature::getId)
                .findFirst()
                .get();
        ClientFeature featureIdToClientId =
                new ClientFeature()
                        .withClientId(clientId)
                        .withId(featureId)
                        .withState(enabled ? FeatureState.ENABLED : FeatureState.DISABLED);
        addClientFeature(featureIdToClientId);
    }

    public void addFeature(String featureStringId) {
        addFeature(featureStringId, getDefaultSettings());
    }

    public synchronized void addFeature(String featureStringId, FeatureSettings settings) {
        if (ADDED_FEATURES.contains(featureStringId)) {
            return;
        }

        long id = FEATURE_COUNTER.incrementAndGet();

        addFeatures(List.of(new Feature()
                .withFeatureTextId(featureStringId)
                .withId(id)
                .withFeaturePublicName("public name: " + featureStringId)
                .withSettings(settings)));

        ADDED_FEATURES.add(featureStringId);
    }

    public void deleteFeature(Long featureId) {
        featureManagingService.deleteFeature(featureId);
    }

    public void updatePercentForFeature(String featureStringId, Integer percent) {
        FeatureTextIdToPercent featureTextIdToPercent =
                new FeatureTextIdToPercent().withTextId(featureStringId).withPercent(percent);
        featureManagingService.updateFeaturePercent(singletonList(featureTextIdToPercent));
    }
}
