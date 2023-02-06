package ru.yandex.direct.core.entity.feature.service.validation;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.feature.container.FeatureTextIdToClientIdState;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.rbac.RbacRole.AGENCY;
import static ru.yandex.direct.rbac.RbacRole.CLIENT;
import static ru.yandex.direct.rbac.RbacRole.SUPER;

@RunWith(Parameterized.class)
public class SwitchFeatureByClientIdValidationServiceTest {
    public static final String FEATURE_NAME = "xxx";
    private final SwitchFeatureByClientIdValidationService service;

    public SwitchFeatureByClientIdValidationServiceTest() {
        this.service = new SwitchFeatureByClientIdValidationService(mock(ShardHelper.class));
    }

    @Parameterized.Parameter(0)
    public boolean result;
    @Parameterized.Parameter(1)
    public RbacRole role;
    @Parameterized.Parameter(2)
    public FeatureState state;

    @Parameterized.Parameter(3)
    public List<RbacRole> canEnable;
    @Parameterized.Parameter(4)
    public List<RbacRole> canDisable;


    @Parameterized.Parameters
    public static Object[][] params() {
        return new Object[][]{
                {true, CLIENT, FeatureState.ENABLED, asList(CLIENT), asList()},
                {true, CLIENT, FeatureState.ENABLED, asList(CLIENT), null},
                {true, CLIENT, FeatureState.DISABLED, asList(), asList(CLIENT)},
                {true, CLIENT, FeatureState.ENABLED, asList(AGENCY, CLIENT), asList()},
                {true, AGENCY, FeatureState.ENABLED, asList(AGENCY, CLIENT), asList()},

                {false, AGENCY, FeatureState.ENABLED, asList(SUPER), asList(AGENCY)},
                {false, AGENCY, FeatureState.DISABLED, asList(SUPER), null},
        };
    }

    @Test
    public void testValidation() {
        Feature feature = new Feature()
                .withFeatureTextId(FEATURE_NAME)
                .withSettings(
                        new FeatureSettings()
                                .withCanEnable(canEnable == null
                                        ? null
                                        : canEnable.stream().map(RbacRole::name).collect(toSet()))
                                .withCanDisable(canDisable == null
                                        ? null
                                        : canDisable.stream().map(RbacRole::name).collect(toSet()))
                );

        ValidationResult<List<FeatureTextIdToClientIdState>, Defect> vr =
                service.validateRolePermissions(
                        ImmutableMap.of(FEATURE_NAME, feature),
                        role,
                        asList(
                                new FeatureTextIdToClientIdState()
                                        .withClientId(ClientId.fromLong(1L))
                                        .withTextId(FEATURE_NAME)
                                        .withState(state)
                        )
                );

        assertThat(!vr.hasAnyErrors()).isEqualTo(result);
    }

}
