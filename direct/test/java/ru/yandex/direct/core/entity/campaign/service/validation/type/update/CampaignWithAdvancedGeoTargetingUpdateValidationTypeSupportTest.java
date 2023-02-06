package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithAdvancedGeoTargeting;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithAdvancedGeoTargetingUpdateValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private FeatureService featureService;

    private CampaignWithOptionalAdvancedGeoTargetingUpdateValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());

    private CampaignValidationContainer container;

    @Parameterized.Parameters(name = "featureEnabled = {0}, receivedCurrentRegion  = {1}," +
            " receivedRegularRegion  = {2}, expecting defect currentRegion = {3}, expecting defect currentRegion = {4}")
    public static Object[][] paramsPreValidate() {
        var forbiddenToChange = new Defect<>(DefectIds.FORBIDDEN_TO_CHANGE);
        return new Object[][]{
                {false, false, null, forbiddenToChange, null},
                {false, true, null, forbiddenToChange, null},
                {false, true, false, forbiddenToChange, forbiddenToChange},
                {false, true, true, forbiddenToChange, forbiddenToChange},
                {false, null, null, null, null},
                {true, true, false, null, null},
                {true, true, true, null, null},
                {true, null, null, null, null},
        };
    }

    @Before
    public void before() {
        var client = steps.clientSteps().createDefaultClient();
        clientId = client.getClientId();
        steps.featureSteps().setCurrentClient(client.getClientId());
        validationTypeSupport = new CampaignWithOptionalAdvancedGeoTargetingUpdateValidationTypeSupport(featureService);
        container = CampaignValidationContainer.create(0, clientId.asLong(), clientId);

    }

    @Test
    @Parameters(method = "paramsPreValidate")
    public void preValidateTextCampaign(boolean featureEnabled,
                                        @Nullable Boolean receivedCurrentRegion,
                                        @Nullable Boolean receivedRegularRegion,
                                        @Nullable Defect expectedDefectCurrentRegion,
                                        @Nullable Defect expectedDefectRegularRegion) {
        steps.featureSteps().addClientFeature(clientId, FeatureName.ADVANCED_GEOTARGETING, featureEnabled);
        var campaignId = RandomNumberUtils.nextPositiveLong();
        var modelChanges = new ModelChanges<>(campaignId, CampaignWithAdvancedGeoTargeting.class)
                .processNotNull(receivedCurrentRegion, TextCampaign.USE_CURRENT_REGION)
                .processNotNull(receivedRegularRegion, TextCampaign.USE_REGULAR_REGION);

        var result = validationTypeSupport.preValidate(container, new ValidationResult<>(List.of(modelChanges)));

        if (expectedDefectCurrentRegion == null && expectedDefectRegularRegion == null) {
            if ((receivedCurrentRegion != null && receivedCurrentRegion)
                    || (receivedRegularRegion != null && receivedRegularRegion) || !featureEnabled
        || (receivedCurrentRegion == null && receivedRegularRegion == null)
            ) {
                assertThat(result, hasNoDefectsDefinitions());
            } else {
                assertThat(result, hasDefectWithDefinition(validationError(
                        path(index(0)), CommonDefects.invalidValue()
                )));
            }
        } else {
            if (expectedDefectCurrentRegion != null) {
                assertThat(result, hasDefectWithDefinition(validationError(
                        path(index(0), field(CampaignWithAdvancedGeoTargeting.USE_CURRENT_REGION)),
                        expectedDefectCurrentRegion
                )));
            }
            if (expectedDefectRegularRegion != null) {
                assertThat(result, hasDefectWithDefinition(validationError(
                        path(index(0), field(CampaignWithAdvancedGeoTargeting.USE_REGULAR_REGION)),
                        expectedDefectRegularRegion
                )));
            }
        }
    }
}


