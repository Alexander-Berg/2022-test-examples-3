package ru.yandex.direct.core.entity.feature.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.feature.service.FeatureHelper.feature;
import static ru.yandex.direct.core.entity.feature.service.FeatureHelper.operatorFeature;

@CoreTest
@RunWith(SpringRunner.class)
public class FeatureHelperTest {
    @Autowired
    private Steps steps;

    FeatureName someFeature = FeatureName.GRID;

    @Before
    public void createClientAndEnableFeature() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().setCurrentClientAndOperator(clientInfo);
        steps.featureSteps().enableClientFeature(someFeature);
    }

    @Test
    public void canCheckClientFeatureInStaticContext() {
        var feature = feature(someFeature);
        assertThat(feature).returns(true, FeatureHelper.FeatureSupplier::enabled);
    }

    @Test
    public void canDisableClientFeature() {
        var feature = feature(someFeature);
        changeClientIdInContext();
        assertThat(feature).returns(true, FeatureHelper.FeatureSupplier::disabled);
    }

    @Test
    public void canCheckUidFeatureInStaticContext() {
        var feature = operatorFeature(someFeature);
        assertThat(feature).returns(true, FeatureHelper.FeatureSupplier::enabled);
    }

    @Test
    public void canDisableUidFeature() {
        var feature = operatorFeature(someFeature);
        changeUidInContext();
        assertThat(feature).returns(true, FeatureHelper.FeatureSupplier::disabled);
    }

    @Test(expected = EmptyAuthenticationException.class)
    public void expectingExceptionOnEmptyClient() {
        steps.featureSteps().setCurrentClient(null);
        feature(someFeature).enabled();
    }

    @Test(expected = EmptyAuthenticationException.class)
    public void expectingExceptionOnEmptyOperator() {
        steps.featureSteps().setCurrentOperator(null);
        operatorFeature(someFeature).enabled();
    }

    private void changeClientIdInContext() {
        steps.featureSteps().setCurrentClient(ClientId.fromLong(-1));
    }

    private void changeUidInContext() {
        steps.featureSteps().setCurrentOperator(-1L);
    }
}
