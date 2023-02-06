package ru.yandex.direct.core.entity.feature.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.abt.container.AllowedFeatures;
import ru.yandex.direct.core.entity.abt.container.TestInfo;
import ru.yandex.direct.core.entity.abt.container.UaasInfoRequest;
import ru.yandex.direct.core.entity.abt.container.UaasInfoResponse;
import ru.yandex.direct.core.entity.abt.service.UaasInfoService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.container.FeatureRequestFactory;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureServiceWithUaasTest {
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

    @Autowired
    private Steps steps;

    private ClientId clientId;
    private FeatureService featureService;
    private UaasInfoService uaasInfoService;

    private static final String IP = "12.12.12.12";
    private static final String YANDEXUID = "1233453453";
    private static final String HOST = "host.ru";
    private static final String USER_AGENT = "User-agent";

    private UaasInfoRequest uaasInfoRequest;
    private FeatureRequest featureRequest;

    @Before
    public void setUp() {
        uaasInfoService = mock(UaasInfoService.class);
        featureService = new FeatureService(featureCache, clientFeaturesRepository, clientService, shardHelper,
                agencyClientRelationService, uaasInfoService, EnvironmentType.DEVELOPMENT, featureRequestFactory);
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient(RbacRole.MANAGER));
        clientId = clientInfo.getClientId();
        uaasInfoRequest = new UaasInfoRequest()
                .withClientId(clientId.asLong())
                .withIp("12.12.12.12")
                .withIp(IP)
                .withHost(HOST)
                .withYandexUid(YANDEXUID)
                .withUserAgent(USER_AGENT)
                .withInterfaceLang("RU")
                .withEnabledFeatures(Set.of());
        featureRequest = new FeatureRequest()
                .withClientId(clientId)
                .withUserAgent(USER_AGENT)
                .withHost(HOST)
                .withIp(IP)
                .withYandexuid(YANDEXUID)
                .withInterfaceLang("RU");
    }

    @Test
    public void featureManuallyDisablesUaasEnabled() {
        var disabledFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientId)
                .withId(disabledFeature.getId())
                .withState(FeatureState.DISABLED);
        steps.featureSteps().addClientFeature(firstClientFeature);

        var testInfo = new TestInfo()
                .withFeatures(List.of(disabledFeature.getFeatureTextId()))
                .withTestIds(List.of("1324"));

        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of(testInfo))
                        .withBoxesCrypted("xsfafss")
                        .withBoxes("1324,0,12")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).doesNotContainKey(disabledFeature.getId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("1324,0,12");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("xsfafss");
    }

    @Test
    public void featureManuallyEnabledUaasEnabled() {
        var enabledFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientId)
                .withId(enabledFeature.getId())
                .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(firstClientFeature);

        var testInfo = new TestInfo()
                .withFeatures(List.of(enabledFeature.getFeatureTextId()))
                .withTestIds(List.of("1324"));
        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of(testInfo))
                        .withBoxesCrypted("xsfafss")
                        .withBoxes("1324,0,12")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).containsKey(enabledFeature.getId());
        assertThat(featuresWithExpBoxes.getIdToFeatureMap().get(enabledFeature.getId()).getFeature().getFeatureTextId()).isEqualTo(enabledFeature.getFeatureTextId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("1324,0,12");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("xsfafss");
    }

    @Test
    public void featureNotExposedManuallyEnabledUaasEnabled() {
        var enabledFeature = steps.featureSteps().addDefaultFeature();

        var testInfo = new TestInfo()
                .withFeatures(List.of(enabledFeature.getFeatureTextId()))
                .withTestIds(List.of("1324"));
        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of(testInfo))
                        .withBoxesCrypted("xsfafss")
                        .withBoxes("1324,0,12")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).containsKey(enabledFeature.getId());
        assertThat(featuresWithExpBoxes.getIdToFeatureMap().get(enabledFeature.getId()).getFeature().getFeatureTextId()).isEqualTo(enabledFeature.getFeatureTextId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("1324,0,12");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("xsfafss");
    }

    @Test
    public void featureManuallyEnabledUaasDisabled() {
        var enabledFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientId)
                .withId(enabledFeature.getId())
                .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(firstClientFeature);
        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of())
                        .withBoxesCrypted("")
                        .withBoxes("")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).containsKey(enabledFeature.getId());
        assertThat(featuresWithExpBoxes.getIdToFeatureMap().get(enabledFeature.getId()).getFeature().getFeatureTextId()).isEqualTo(enabledFeature.getFeatureTextId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("");
    }

    @Test
    public void featureEnabledByRoleUaasDisabled() {
        var feature = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withRoles(Set.of(
                "MANAGER")));
        steps.featureSteps().addFeatures(List.of(feature));
        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of())
                        .withBoxesCrypted("")
                        .withBoxes("")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).containsKey(feature.getId());
        assertThat(featuresWithExpBoxes.getIdToFeatureMap().get(feature.getId()).getFeature().getFeatureTextId()).isEqualTo(feature.getFeatureTextId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("");
    }

    @Test
    public void featureEnabledByPercentUaasEnabled() {
        var feature = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        steps.featureSteps().addFeatures(List.of(feature));
        var testInfo = new TestInfo()
                .withFeatures(List.of(feature.getFeatureTextId()))
                .withTestIds(List.of("1324"));
        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of(testInfo))
                        .withBoxesCrypted("xsfafss")
                        .withBoxes("1324,0,12")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).containsKey(feature.getId());
        assertThat(featuresWithExpBoxes.getIdToFeatureMap().get(feature.getId()).getFeature().getFeatureTextId()).isEqualTo(feature.getFeatureTextId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("1324,0,12");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("xsfafss");
    }

    @Test
    public void featureEnabledByPercentUaasDisabled() {
        var feature = steps.featureSteps().getDefaultFeature().withSettings(new FeatureSettings().withPercent(100));
        steps.featureSteps().addFeatures(List.of(feature));
        when(uaasInfoService.getInfo(anyList(), any(AllowedFeatures.class)))
                .thenReturn(List.of(new UaasInfoResponse()
                        .withClientId(clientId)
                        .withTests(List.of())
                        .withBoxesCrypted("")
                        .withBoxes("")));

        var featuresWithExpBoxes = featureService.getFeaturesWithExpBoxes(featureRequest);
        assertThat(featuresWithExpBoxes.getIdToFeatureMap()).containsKey(feature.getId());
        assertThat(featuresWithExpBoxes.getIdToFeatureMap().get(feature.getId()).getFeature().getFeatureTextId()).isEqualTo(feature.getFeatureTextId());
        assertThat(featuresWithExpBoxes.getExpBoxes()).isEqualTo("");
        assertThat(featuresWithExpBoxes.getExpBoxesCrypted()).isEqualTo("");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void featuresOnlyFromDb() {
        var feature = steps.featureSteps().getDefaultFeature();
        steps.featureSteps().addFeatures(List.of(feature));
        featureService.isEnabledForClientIdsOnlyFromDb(Set.of(featureRequest.getClientId()),
                feature.getFeatureTextId());
        verify(uaasInfoService, never()).getInfo(any(List.class));
    }
}
