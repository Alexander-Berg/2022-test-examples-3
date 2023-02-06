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

import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalS2sTracking;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainerImpl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
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
public class CampaignWithOptionalS2sTrackingUpdateValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;

    private CampaignWithOptionalS2sTrackingUpdateValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        validationTypeSupport = new CampaignWithOptionalS2sTrackingUpdateValidationTypeSupport(featureService);
    }

    @Parameterized.Parameters(name = "featureEnabled = {0}, valueReceived = {1}, expecting defect = {2}")
    public static Object[][] params() {
        return new Object[][]{
                {false, null, null},
                {false, false, null},
                {false, true, CommonDefects.invalidValue()},
                {true, false, null},
                {true, true, null},
                {true, null, null},
        };
    }

    @Test
    @Parameters(method = "params")
    public void validateTextCampaign(boolean featureEnabled,
                                     Boolean valueReceived,
                                     @Nullable Defect expectedDefect) {
        var campaign = new TextCampaign()
                .withClientId(clientId.asLong())
                .withIsS2sTrackingEnabled(valueReceived);

        var vr = new ValidationResult<List<CampaignWithOptionalS2sTracking>, Defect>(List.of(campaign));
        when(featureService.isEnabledForClientId(clientId, FeatureName.IS_S2S_TRACKING_ENABLED))
                .thenReturn(featureEnabled);

        var container = new CampaignValidationContainerImpl(0, 0L, clientId, null, new CampaignOptions(), null,
                emptyMap());
        var result = validationTypeSupport.validate(container, vr);

        if (expectedDefect == null) {
            assertThat(result, hasNoDefectsDefinitions());
        } else {
            assertThat(result, hasDefectWithDefinition(validationError(
                    path(index(0), field(CampaignWithOptionalS2sTracking.IS_S2S_TRACKING_ENABLED)),
                    expectedDefect
            )));
        }
    }

}
