package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignSupportedOnTouch;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.validation.defects.RightsDefects;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CampaignSupportedOnTouchUpdateValidationTypeSupportTest {
    @Mock
    private FeatureService featureService;
    @Mock
    private RbacService rbacService;

    private CampaignSupportedOnTouchUpdateValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
    private Long userId = RandomUtils.nextLong();
    private CampaignValidationContainer container = CampaignValidationContainer.create(0, userId, clientId);
    private CampaignSupportedOnTouch campaign;
    private ValidationResult<List<CampaignSupportedOnTouch>, Defect> vr;

    @Parameterized.Parameters(name = "Successful validation, featureEnabled = {0}, valueReceived = {1}")
    private static Object[][] successParameters() {
        return new Object[][]{
                {false, null},
                {false, false},
                {true, false},
                {true, true},
        };
    }

    @Parameterized.Parameters(name = "Failed validation, featureEnabled = {0}, valueReceived = {1}, expectedDefect = " +
            "{2}")
    private static Object[][] failParameters() {
        return new Object[][]{
                {false, true, CommonDefects.invalidValue()},
                {true, null, CommonDefects.notNull()},
        };
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        validationTypeSupport = new CampaignSupportedOnTouchUpdateValidationTypeSupport(featureService, rbacService);
        campaign = new TextCampaign().withClientId(clientId.asLong()).withId(42L);
        vr = new ValidationResult<>(List.of(campaign));
    }

    @Test
    @Parameters(method = "successParameters")
    public void validate_testSuccess(boolean featureEnabled, Boolean valueReceived) {
        mockFeatureService(featureEnabled);
        campaign.withIsTouch(valueReceived);
        var result = validationTypeSupport.validate(container, vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    @Parameters(method = "failParameters")
    public void validate_testFail(boolean featureEnabled, Boolean valueReceived, Defect expectedDefect) {
        mockFeatureService(featureEnabled);
        campaign.withIsTouch(valueReceived);
        var result = validationTypeSupport.validate(container, vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignSupportedOnTouch.IS_TOUCH)), expectedDefect
        )));
    }

    @Test
    public void validateBeforeApply_success() {
        when(rbacService.getUidRole(userId)).thenReturn(RbacRole.SUPER);
        var modelChanges = ModelChanges.build(campaign, CampaignSupportedOnTouch.IS_TOUCH, true);
        var vr = ListValidationBuilder.of(singletonList(modelChanges), Defect.class).getResult();

        var result = validationTypeSupport.validateBeforeApply(container, vr, Map.of(campaign.getId(), campaign));

        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_fail() {
        when(rbacService.getUidRole(userId)).thenReturn(RbacRole.CLIENT);
        var modelChanges = ModelChanges.build(campaign, CampaignSupportedOnTouch.IS_TOUCH, true);
        var vr = ListValidationBuilder.of(singletonList(modelChanges), Defect.class).getResult();

        var result = validationTypeSupport.validateBeforeApply(container, vr, Map.of(campaign.getId(), campaign));

        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignSupportedOnTouch.IS_TOUCH)), RightsDefects.noRights()
        )));
    }

    private void mockFeatureService(boolean isFeatureEnabled) {
        when(featureService.isEnabledForClientId(clientId, FeatureName.TOUCH_DIRECT_ENABLED))
                .thenReturn(isFeatureEnabled);
    }
}
