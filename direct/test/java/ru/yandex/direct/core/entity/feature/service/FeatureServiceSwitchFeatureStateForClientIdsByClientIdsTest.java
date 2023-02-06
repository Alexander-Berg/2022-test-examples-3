package ru.yandex.direct.core.entity.feature.service;

import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToClientIdState;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.entity.feature.repository.FeatureRepository;
import ru.yandex.direct.core.entity.feature.service.validation.FeatureAddValidationService;
import ru.yandex.direct.core.entity.feature.service.validation.FeaturePercentUpdateValidationService;
import ru.yandex.direct.core.entity.feature.service.validation.FeatureRoleUpdateValidationService;
import ru.yandex.direct.core.entity.feature.service.validation.FeatureTextIdValidationService;
import ru.yandex.direct.core.entity.feature.service.validation.SwitchFeatureByClientIdValidationService;
import ru.yandex.direct.core.entity.feature.service.validation.SwitchFeatureByLoginValidationService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureServiceSwitchFeatureStateForClientIdsByClientIdsTest {
    @Autowired
    private FeatureSteps featureSteps;
    @Autowired
    private UserSteps userSteps;

    @Autowired
    private ClientFeaturesRepository clientFeaturesRepository;
    @Autowired
    private FeatureRepository featureRepository;
    @Autowired
    private FeatureCache featureCache;

    @Autowired
    private SwitchFeatureByLoginValidationService switchFeatureToLoginValidationService;
    @Autowired
    private SwitchFeatureByClientIdValidationService switchFeatureToClientIdValidationService;
    @Autowired
    private FeatureTextIdValidationService featureTextIdValidationService;
    @Autowired
    private FeaturePercentUpdateValidationService featurePercentUpdateValidationService;
    @Autowired
    private FeatureRoleUpdateValidationService featureRoleUpdateValidationService;
    @Autowired
    private FeatureAddValidationService featureAddValidationService;

    private FeatureManagingService featureManagingService;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeatureService featureService;

    private UserInfo defaultUser;
    private Feature defaultFeature;

    @Before
    public void before() {
        defaultUser = userSteps.createDefaultUser();
        defaultFeature = featureSteps.addDefaultFeature(defaultUser.getUid());
        var shardHelper = mock(ShardHelper.class);
        when(shardHelper.getClientIdsByLogins(List.of(defaultUser.getClientInfo().getLogin())))
                .thenReturn(StreamEx.of(defaultUser.getClientInfo())
                        .mapToEntry(ClientInfo::getLogin, ClientInfo::getClientId)
                        .mapValues(ClientId::asLong)
                        .toMap());
        when(shardHelper.getLoginsByUids(List.of(defaultUser.getUid())))
                .thenReturn(StreamEx.of(defaultUser.getClientInfo())
                        .map(ClientInfo::getLogin)
                        .map(List::of)
                        .toList());
        userService = mock(UserService.class);
        featureManagingService = new FeatureManagingService(switchFeatureToLoginValidationService,
                featureTextIdValidationService, switchFeatureToClientIdValidationService, featureRepository,
                featureCache, clientFeaturesRepository, featurePercentUpdateValidationService,
                featureRoleUpdateValidationService, featureAddValidationService, shardHelper, rbacService, userService,
                featureService);
    }

    @Test
    public void switchFeature() {
        FeatureState status = FeatureState.ENABLED;
        var featureTextIdToClientIdState = new FeatureTextIdToClientIdState()
                .withClientId(defaultUser.getClientInfo().getClientId())
                .withTextId(defaultFeature.getFeatureTextId())
                .withState(status);
        featureManagingService.switchFeaturesStateForClientIds(List.of(featureTextIdToClientIdState));
        var featureIdToClientId = new ClientFeature()
                .withClientId(defaultUser.getClientInfo().getClientId())
                .withId(defaultFeature.getId());

        FeatureState actualStatus = featureSteps.getClientFeatureStatus(featureIdToClientId);
        Assertions.assertThat(actualStatus).isEqualTo(status);
    }
}
