package ru.yandex.direct.core.entity.metrika.service;

import java.util.Arrays;
import java.util.Collection;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@CoreTest
@RunWith(Parameterized.class)
public class MobileGoalsPermissionServiceTest {
    private static final Long[] alwaysAllowedGoalIds = new Long[]{4L};
    private static final Long[] rmpStatTrackerInstallEnabledGoalIds = new Long[]{4L, 5L, 6L, 7L};
    private static final Long[] rmpFeaturesEnabledGoalIds = new Long[]{4L,
            38402972L, 38403008L, 38403053L, 38403071L, 38403080L, 38403095L, 38403104L,
            38403131L, 38403173L, 38403191L, 38403197L, 38403206L, 38403215L, 38403230L,
            38403338L, 38403494L, 38403530L, 38403545L, 38403581L};
    private static final Long[] internalRoleOrManagerGoalIds = new Long[]{3L, 4L, 5L, 6L, 7L,
            38402972L, 38403008L, 38403053L, 38403071L, 38403080L, 38403095L, 38403104L,
            38403131L, 38403173L, 38403191L, 38403197L, 38403206L, 38403215L, 38403230L,
            38403338L, 38403494L, 38403530L, 38403545L, 38403581L};

    @Mock
    private TranslationService translationService;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private MobileGoalsPermissionService mobileGoalsPermissionService;

    @Parameterized.Parameter
    public RbacRole operatorRole;

    @Parameterized.Parameter(1)
    public boolean hasRmpStatTrackerInstallEnabledFeature;

    @Parameterized.Parameter(2)
    public boolean hasRmpStatCpaEnabledFeature;

    @Parameterized.Parameter(3)
    public boolean hasInAppEventsInRmpEnabledFeature;

    @Parameterized.Parameter(4)
    public Long[] expectedMobileGoalIds;

    @Parameterized.Parameters(name = "role {0} - rmp features enabled: {1}, {2}, {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {RbacRole.CLIENT, false, false, false, alwaysAllowedGoalIds},
                {RbacRole.CLIENT, true, false, false, rmpStatTrackerInstallEnabledGoalIds},
                {RbacRole.CLIENT, false, true, false, rmpFeaturesEnabledGoalIds},
                {RbacRole.CLIENT, false, false, true, rmpFeaturesEnabledGoalIds},
                {RbacRole.SUPPORT, false, false, false, rmpFeaturesEnabledGoalIds},
                {RbacRole.MANAGER, false, false, false, internalRoleOrManagerGoalIds},
                {RbacRole.SUPER, true, true, true, internalRoleOrManagerGoalIds},
        });
    }

    private ClientId clientId;

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());

        MockitoAnnotations.initMocks(this);

        doReturn(hasRmpStatTrackerInstallEnabledFeature)
                .when(featureService)
                .isEnabledForClientId(clientId, FeatureName.RMP_STAT_TRACKER_INSTALL_ENABLED);
        doReturn(hasRmpStatTrackerInstallEnabledFeature)
                .when(featureService)
                .isEnabledForClientId(clientId, FeatureName.RMP_STAT_ASSOCIATED_INSTALL_ENABLED);
        doReturn(hasRmpStatCpaEnabledFeature)
                .when(featureService)
                .isEnabledForClientId(clientId, FeatureName.RMP_STAT_CPA_ENABLED);
        doReturn(hasInAppEventsInRmpEnabledFeature)
                .when(featureService)
                .isEnabledForClientId(clientId, FeatureName.IN_APP_EVENTS_IN_RMP_ENABLED);
    }

    @Test
    public void checkAllowedMobileGoalIds() {
        var resultMobileGoalIds = EntryStream.of(mobileGoalsPermissionService.getMobileGoalsWithPermissions())
                .filterValues(permissions -> permissions.test(operatorRole, clientId))
                .keys()
                .map(Goal::getId)
                .toArray();
        assertThat(resultMobileGoalIds).containsExactlyInAnyOrder(expectedMobileGoalIds);
    }
}
