package ru.yandex.direct.core.entity.feature.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.abt.service.UaasInfoService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.container.FeatureRequestFactory;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureServiceForUidTest {
    @Autowired
    private FeatureCache featureCache;
    @Autowired
    private ClientFeaturesRepository clientFeaturesRepository;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private AgencyClientRelationService agencyClientRelationService;
    @Autowired
    private FeatureRequestFactory featureRequestFactory;

    private FeatureService featureService;

    @Autowired
    private Steps steps;


    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        var uaasInfoService = mock(UaasInfoService.class);
        when(uaasInfoService.getInfo(any(List.class))).thenReturn(List.of());
        featureService = spy(new FeatureService(featureCache, clientFeaturesRepository, clientService, shardHelper,
                agencyClientRelationService, uaasInfoService, EnvironmentType.DEVELOPMENT, featureRequestFactory));

    }


    /**
     * Тест проверяет, что если для переданного uid'а есть clientId, то он будет считать их для clientId
     */
    @Test
    public void testUidWithClientId() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var feature1 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        var feature2 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        var feature3 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(0));
        steps.featureSteps().addFeatures(List.of(feature1));
        steps.featureSteps().addFeatures(List.of(feature2));
        steps.featureSteps().addFeatures(List.of(feature3));

        // фича выключена для clientId, она должны быть выключена в рузельтате и не разыгрываться
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientInfo.getClientId())
                .withId(feature1.getId())
                .withState(FeatureState.DISABLED);
        steps.featureSteps().addClientFeature(firstClientFeature);
        var uidToFeaturesMap = featureService.getEnabledForUids(List.of(clientInfo.getUid()));
        assertThat(uidToFeaturesMap).containsOnlyKeys(clientInfo.getUid());
        assertThat(uidToFeaturesMap.get(clientInfo.getUid())).contains(feature2.getFeatureTextId());
        assertThat(uidToFeaturesMap.get(clientInfo.getUid())).doesNotContain(feature1.getFeatureTextId(),
                feature3.getFeatureTextId());
    }

    @Test
    public void testUidWithoutClientId() {
        var feature1 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        var feature2 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(0));
        steps.featureSteps().addFeatures(List.of(feature1));
        steps.featureSteps().addFeatures(List.of(feature2));

        var uidWithoutClient = steps.userSteps().generateNewUserUid();
        var uidToFeaturesMap = featureService.getEnabledForUids(List.of(uidWithoutClient));

        assertThat(uidToFeaturesMap).containsOnlyKeys(uidWithoutClient);
        assertThat(uidToFeaturesMap.get(uidWithoutClient)).contains(feature1.getFeatureTextId());
        assertThat(uidToFeaturesMap.get(uidWithoutClient)).doesNotContain(feature2.getFeatureTextId());
    }

    /**
     * Тест проверяет, что если для одного uid'а есть clientId, а для другого нет, то результат объединится верным
     * образом
     */
    @Test
    public void testSeveralUids() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var feature1 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        var feature2 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        var feature3 = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(0));
        steps.featureSteps().addFeatures(List.of(feature1));
        steps.featureSteps().addFeatures(List.of(feature2));
        steps.featureSteps().addFeatures(List.of(feature3));

        // фича выключена для clientId, она должны быть выключена в рузельтате и не разыгрываться
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientInfo.getClientId())
                .withId(feature1.getId())
                .withState(FeatureState.DISABLED);
        steps.featureSteps().addClientFeature(firstClientFeature);
        var uidWithoutClient = steps.userSteps().generateNewUserUid();
        var uidToFeaturesMap = featureService.getEnabledForUids(List.of(clientInfo.getUid(), uidWithoutClient));
        assertThat(uidToFeaturesMap).containsOnlyKeys(clientInfo.getUid(), uidWithoutClient);
        assertThat(uidToFeaturesMap.get(clientInfo.getUid())).contains(feature2.getFeatureTextId());
        assertThat(uidToFeaturesMap.get(clientInfo.getUid())).doesNotContain(feature1.getFeatureTextId(),
                feature3.getFeatureTextId());

        assertThat(uidToFeaturesMap.get(uidWithoutClient)).contains(feature1.getFeatureTextId(),
                feature2.getFeatureTextId());
        assertThat(uidToFeaturesMap.get(uidWithoutClient)).doesNotContain(feature3.getFeatureTextId());
    }
}
