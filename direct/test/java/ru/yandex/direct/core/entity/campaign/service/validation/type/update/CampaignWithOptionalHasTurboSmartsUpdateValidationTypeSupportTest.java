package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;

import javax.annotation.Nullable;
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

import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalHasTurboSmarts;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

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
public class CampaignWithOptionalHasTurboSmartsUpdateValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;

    private CampaignWithOptionalHasTurboSmartsUpdateValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        validationTypeSupport = new CampaignWithOptionalHasTurboSmartsUpdateValidationTypeSupport(featureService);
    }

    @Parameterized.Parameters(name = "featureEnabled = {0}, valueReceived = {1}, expecting defect = {2}")
    public static Object[][] params() {
        return new Object[][]{
                {false, null, null},
                {false, false, null},
                {false, true, null},
                {true, false, null},
                {true, true, null},
                {true, null, CommonDefects.notNull()},
        };
    }

    @Test
    @Parameters(method = "params")
    public void validate_SmartCampaign(boolean featureEnabled,
                                       Boolean valueReceived,
                                       @Nullable Defect expectedDefect) {
        SmartCampaign campaign = TestCampaigns.defaultSmartCampaign()
                .withClientId(clientId.asLong())
                .withHasTurboSmarts(valueReceived);
        ValidationResult<List<CampaignWithOptionalHasTurboSmarts>, Defect> vr =
                new ValidationResult<>(List.of(campaign));

        when(featureService.isEnabledForClientId(clientId, FeatureName.TURBO_SMARTS))
                .thenReturn(featureEnabled);

        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        if (expectedDefect == null) {
            assertThat(result, hasNoDefectsDefinitions());
        } else {
            assertThat(result, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithOptionalHasTurboSmarts.HAS_TURBO_SMARTS)),
                    expectedDefect
            )));
        }
    }
}
