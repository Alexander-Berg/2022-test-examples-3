package ru.yandex.direct.core.entity.feature.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.container.ClientRealFeature;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureConverter;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientFeatureStorageTest {
    @Autowired
    private FeatureSteps featureSteps;
    @Autowired
    private UserSteps userSteps;

    private Feature defaultFeature;
    private ClientFeaturesStorage clientFeaturesStorage;
    private ClientId clientId;
    private FeatureState state;

    @Before
    public void before() {
        UserInfo defaultUser = userSteps.createDefaultUser();
        defaultFeature = featureSteps.addDefaultFeature(defaultUser.getUid());
        clientFeaturesStorage = new ClientFeaturesStorage();
        clientId = defaultUser.getClientInfo().getClientId();
    }

    @Test
    public void getKnown_updateToDisabled_getIt() {
        state = FeatureState.DISABLED;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));

        ClientRealFeature expectedClientRealFeature =
                new ClientRealFeature().withFeatureState(state).withFeature(defaultFeature).withClientId(clientId);
        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getKnownFeatures();
        assertThat(actualClientRealFeatureList.get(0), equalTo(expectedClientRealFeature));
    }

    @Test
    public void getKnown_updateToUnknown_getEmptyStorage() {
        state = FeatureState.UNKNOWN;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));

        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getKnownFeatures();
        assertThat(actualClientRealFeatureList, empty());
    }

    @Test
    public void getUnknown_notUpdate_getIt() {
        state = FeatureState.UNKNOWN;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        ClientRealFeature expectedClientRealFeature =
                new ClientRealFeature().withFeatureState(state).withFeature(defaultFeature).withClientId(clientId);
        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getUnknownFeatures();
        assertThat(actualClientRealFeatureList.get(0), equalTo(expectedClientRealFeature));
    }

    @Test
    public void getUnknown_updateToUnknown_getIt() {
        state = FeatureState.UNKNOWN;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));

        ClientRealFeature expectedClientRealFeature =
                new ClientRealFeature().withFeatureState(state).withFeature(defaultFeature).withClientId(clientId);
        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getUnknownFeatures();
        assertThat(actualClientRealFeatureList.get(0), equalTo(expectedClientRealFeature));
    }

    @Test
    public void getUnknown_updateToDisabled_getEmptyStorage() {
        state = FeatureState.DISABLED;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));

        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getUnknownFeatures();
        assertThat(actualClientRealFeatureList, empty());
    }

    @Test
    public void getEnabled_updateToEnabled_getIt() {
        state = FeatureState.ENABLED;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));
        ClientRealFeature expectedClientRealFeature =
                new ClientRealFeature().withFeatureState(state).withFeature(defaultFeature).withClientId(clientId);
        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getKnownFeatures();
        assertThat(actualClientRealFeatureList.get(0), equalTo(expectedClientRealFeature));
    }

    @Test
    public void getEnabled_updateToDisabled_getEmptyStorage() {
        state = FeatureState.DISABLED;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));

        List<ClientRealFeature> actualClientRealFeatureList =
                clientFeaturesStorage.getEnabledFeatures();
        assertThat(actualClientRealFeatureList, empty());
    }

    @Test
    public void updateByClientFeatures() {
        state = FeatureState.ENABLED;
        var clientFeaturesStorage = new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                List.of(defaultFeature));
        clientFeaturesStorage.update(List.of(new ClientFeature().withState(state).withClientId(clientId).withId(defaultFeature.getId())));

        ClientRealFeature expectedClientRealFeature =
                new ClientRealFeature().withFeatureState(state).withFeature(defaultFeature).withClientId(clientId);

        ClientFeaturesStorage actualStorage =
                new ClientFeaturesStorage(List.of(new FeatureRequest().withClientId(clientId)),
                        List.of(defaultFeature));
        actualStorage
                .update(mapList(clientFeaturesStorage.getClientRealFeatureList(), FeatureConverter::toClientFeature));
        assertThat(actualStorage.getClientRealFeatureList().get(0), equalTo(expectedClientRealFeature));
    }
}
