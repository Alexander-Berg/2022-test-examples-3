package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

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

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalAloneTrafaret;
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
public class CampaignWithOptionalAloneTrafaretAddValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;
    private CampaignWithOptionalAloneTrafaretAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
    private CampaignWithOptionalAloneTrafaret campaign;
    private ValidationResult<List<CampaignWithOptionalAloneTrafaret>, Defect> vr;

    @Parameterized.Parameters(name = "Successful validation, featureEnabled = {0}, valueReceived = {1}")
    private static Object[][] successParameters() {
        return new Object[][] {
                {false, null},
                {false, false},
                {true, false},
                {true, true},
        };
    }

    @Parameterized.Parameters(name = "Failed validation, featureEnabled = {0}, valueReceived = {1}, expectedDefect = {2}")
    private static Object[][] failParameters() {
        return new Object[][] {
                {false, true, CommonDefects.invalidValue()},
                {true, null, CommonDefects.notNull()},
        };
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        validationTypeSupport = new CampaignWithOptionalAloneTrafaretAddValidationTypeSupport(featureService);
    }

    @Test
    @Parameters(method = "successParameters")
    public void validate_testSuccessForTextCampaign(boolean featureEnabled, Boolean valueReceived) {
        createValidationResult(CampaignType.TEXT);
        validate_testSuccess(featureEnabled, valueReceived);
    }

    @Test
    @Parameters(method = "successParameters")
    public void validate_testSuccessForDynamicCampaign(boolean featureEnabled, Boolean valueReceived) {
        createValidationResult(CampaignType.DYNAMIC);
        validate_testSuccess(featureEnabled, valueReceived);
    }

    @Test
    @Parameters(method = "successParameters")
    public void validate_testSuccessForSmartCampaign(boolean featureEnabled, Boolean valueReceived) {
        createValidationResult(CampaignType.PERFORMANCE);
        validate_testSuccess(featureEnabled, valueReceived);
    }

    @Test
    @Parameters(method = "successParameters")
    public void validate_testSuccessForMobileContentCampaign(boolean featureEnabled, Boolean valueReceived) {
        createValidationResult(CampaignType.MOBILE_CONTENT);
        validate_testSuccess(featureEnabled, valueReceived);
    }

    private void validate_testSuccess(boolean featureEnabled, Boolean valueReceived) {
        mockFeatureService(featureEnabled);
        campaign.withIsAloneTrafaretAllowed(valueReceived);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    @Parameters(method = "failParameters")
    public void validate_testFailForTextCampaign(boolean featureEnabled, Boolean valueReceived, Defect expectedDefect) {
        createValidationResult(CampaignType.TEXT);
        validate_testFail(featureEnabled, valueReceived, expectedDefect);
    }

    @Test
    @Parameters(method = "failParameters")
    public void validate_testFailForDynamicCampaign(boolean featureEnabled,
                                                    Boolean valueReceived,
                                                    Defect expectedDefect) {
        createValidationResult(CampaignType.DYNAMIC);
        validate_testFail(featureEnabled, valueReceived, expectedDefect);
    }

    @Test
    @Parameters(method = "failParameters")
    public void validate_testFailForSmartCampaign(boolean featureEnabled,
                                                  Boolean valueReceived,
                                                  Defect expectedDefect) {
        createValidationResult(CampaignType.PERFORMANCE);
        validate_testFail(featureEnabled, valueReceived, expectedDefect);
    }

    @Test
    @Parameters(method = "failParameters")
    public void validate_testFailForMobileContentCampaign(boolean featureEnabled,
                                                          Boolean valueReceived,
                                                          Defect expectedDefect) {
        createValidationResult(CampaignType.MOBILE_CONTENT);
        validate_testFail(featureEnabled, valueReceived, expectedDefect);
    }

    private void validate_testFail(boolean featureEnabled, Boolean valueReceived, Defect expectedDefect) {
        mockFeatureService(featureEnabled);
        campaign.withIsAloneTrafaretAllowed(valueReceived);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithOptionalAloneTrafaret.IS_ALONE_TRAFARET_ALLOWED)),
                expectedDefect
        )));
    }

    private void mockFeatureService(boolean isFeatureEnabled) {
        when(featureService.isEnabledForClientId(clientId, FeatureName.ALONE_TRAFARET_OPTION_ENABLED))
                .thenReturn(isFeatureEnabled);
    }

    private void createValidationResult(CampaignType campaignType) {
        campaign = (CampaignWithOptionalAloneTrafaret) TestCampaigns.newCampaignByCampaignType(campaignType);
        campaign.withClientId(clientId.asLong());
        vr = new ValidationResult<>(List.of(campaign));
    }
}
