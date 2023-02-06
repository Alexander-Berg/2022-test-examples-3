package ru.yandex.direct.web.entity.feature.service;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.model.WebSuccessResponse;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.feature.model.SetFeatureStateItem;
import ru.yandex.direct.web.validation.model.ValidationResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DirectWebTest
public class FeatureWebServiceTest {
    @Autowired
    private FeatureWebService service;
    @Autowired
    private DirectWebAuthenticationSource auth;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientSteps clientSteps;
    @Autowired
    private FeatureSteps featureSteps;

    private User user;
    private Feature feature;

    @Before
    public void before() {
        ClientInfo defaultClientAndUser = clientSteps.createDefaultClient();
        user = userRepository
                .fetchByUids(defaultClientAndUser.getShard(), ImmutableSet.of(defaultClientAndUser.getUid()))
                .iterator().next();
        feature = featureSteps.addDefaultFeature(
                user.getUid(),
                new FeatureSettings()
                        .withCanEnable(ImmutableSet.of(RbacRole.AGENCY.name()))
                        .withCanDisable(ImmutableSet.of(RbacRole.CLIENT.name()))
        );

        DirectWebAuthenticationSourceMock.castToMock(auth)
                .withOperator(user)
                .withSubjectUser(user);
    }


    @Test
    public void clientCanDisableFeature() {
        WebResponse resp =
                service.setState(singletonList(new SetFeatureStateItem(feature.getFeatureTextId(), false)));
        assertThat(resp)
                .isInstanceOf(WebSuccessResponse.class);
    }

    @Test
    public void clientCantEnableFeature() {
        WebResponse resp =
                service.setState(singletonList(new SetFeatureStateItem(feature.getFeatureTextId(), true)));
        assertThat(resp)
                .isInstanceOf(ValidationResponse.class);
    }
}
