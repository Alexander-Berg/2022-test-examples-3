package ru.yandex.direct.core.entity.feature.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.feature.container.ChiefRepresentativeWithClientFeature;
import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToClientIdState;
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
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.Result;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.feature.model.FeatureConverter.clientFeatureFromFeatureTextIdToClientIdState;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetFeaturesClientsTest {
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

    @Autowired
    private RbacService rbacService;

    @Autowired
    private UserService userService;

    @Autowired
    private FeatureService featureService;

    private FeatureManagingService featureManagingService;

    private Feature defaultFeature;
    private List<FeatureTextIdToClientIdState> featureTextIdToClientIdStateList;
    private FeatureState status;

    @Before
    public void before() {
        status = FeatureState.ENABLED;

        UserInfo defaultUserFirst = userSteps.createDefaultUser();
        UserInfo defaultUserSecond = userSteps.createDefaultUser();
        defaultFeature = featureSteps.addDefaultFeature(defaultUserFirst.getUid());
        featureTextIdToClientIdStateList = List.of(
                new FeatureTextIdToClientIdState()
                        .withState(status)
                        .withTextId(defaultFeature.getFeatureTextId())
                        .withClientId(defaultUserFirst.getClientInfo().getClientId()),
                new FeatureTextIdToClientIdState()
                        .withState(status)
                        .withTextId(defaultFeature.getFeatureTextId())
                        .withClientId(defaultUserSecond.getClientInfo().getClientId()));
        var shardHelper = mock(ShardHelper.class);
        when(shardHelper
                .getLoginsByUids(List.of(defaultUserFirst.getClientInfo().getUid(), defaultUserSecond.getUid())))
                .thenReturn(StreamEx.of(defaultUserFirst.getClientInfo(), defaultUserSecond.getClientInfo())
                        .map(ClientInfo::getLogin).map(List::of).toList());
        userService = mock(UserService.class);
        featureManagingService = new FeatureManagingService(switchFeatureToLoginValidationService,
                featureTextIdValidationService, switchFeatureToClientIdValidationService, featureRepository,
                featureCache, clientFeaturesRepository, featurePercentUpdateValidationService,
                featureRoleUpdateValidationService, featureAddValidationService, shardHelper, rbacService, userService,
                featureService);
        featureManagingService.switchFeaturesStateForClientIds(featureTextIdToClientIdStateList);
    }

    @Test
    public void getFeaturesClients() {
        Result<Map<Long, List<ChiefRepresentativeWithClientFeature>>> featuresClients = featureManagingService
                .getFeaturesClients(List.of(defaultFeature.getFeatureTextId()), status);
        Map<String, Feature> featureByTextId = StreamEx.of(defaultFeature).mapToEntry(Feature::getFeatureTextId)
                .invert()
                .toMap();
        CompareStrategy strategy = DefaultCompareStrategies.allFields();

        var clientFeaturesList = EntryStream.of(featuresClients.getResult())
                .flatMapValues(Collection::stream)
                .mapValues(ChiefRepresentativeWithClientFeature::getClientFeature)
                .values()
                .toList();
        MatcherAssert.assertThat("Полученные цели соответствуют ожиданиям",
                clientFeaturesList,
                containsInAnyOrder(mapList(featureTextIdToClientIdStateList, featureTextIdToClientIdState ->
                        beanDiffer(clientFeatureFromFeatureTextIdToClientIdState(featureTextIdToClientIdState,
                                featureByTextId))
                                .useCompareStrategy(strategy)))
        );
    }
}
