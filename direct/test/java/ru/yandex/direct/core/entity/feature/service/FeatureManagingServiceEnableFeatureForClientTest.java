package ru.yandex.direct.core.entity.feature.service;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.FeatureAssignmentException;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFeatures;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureManagingServiceEnableFeatureForClientTest {
    private static final String NON_EXISTENT_FEATURE_NAME = TestFeatures.newFeatureName("bogus_feature_");
    private static final ClientId NON_EXISTENT_CLIENT = ClientId.fromLong(0xDEADBEEFL);

    @Autowired
    private FeatureManagingService service;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private Steps steps;

    private ClientId clientId;
    private String existingFeatureName;

    @Before
    public void setUp() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        existingFeatureName = TestFeatures.newFeatureName("existing_feature_");
        steps.featureSteps().addFeature(existingFeatureName);
    }

    @Test
    public void existingFeature() {
        TestUtils.assumeThat(featureService.isEnabledForClientId(new FeatureRequest().withClientId(clientId), existingFeatureName), equalTo(false));

        service.enableFeatureForClient(clientId, existingFeatureName);
        assertThat(featureService.isEnabledForClientId(new FeatureRequest().withClientId(clientId), existingFeatureName), equalTo(true));
    }

    @Test(expected = FeatureAssignmentException.class)
    public void nonExistentFeature() {
        service.enableFeatureForClient(clientId, NON_EXISTENT_FEATURE_NAME);
    }

    @Test(expected = FeatureAssignmentException.class)
    public void nonExistentClient() {
        service.enableFeatureForClient(NON_EXISTENT_CLIENT, existingFeatureName);
    }
}
