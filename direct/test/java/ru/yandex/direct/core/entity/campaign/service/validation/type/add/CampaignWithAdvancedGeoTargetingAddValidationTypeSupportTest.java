package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

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
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainerImpl;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
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
public class CampaignWithAdvancedGeoTargetingAddValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithAdvancedGeoTargetingAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());


    @Parameterized.Parameters(name = "featureEnabled = {0}, receivedCurrentRegion  = {1}," +
            " receivedRegularRegion  = {2}, expecting defect currentRegion = {3}, expecting defect currentRegion = {4}")
    public static Object[][] params() {
        return new Object[][]{
                {false, false, null, null, null},
                {false, true, null, CommonDefects.isNull(), null},
                {false, true, false, CommonDefects.isNull(), null},
                {false, true, true, CommonDefects.isNull(), CommonDefects.isNull()},
                {false, false, false, null, null},
                {true, true, false, null, null},
                {true, true, true, null, null},
        };
    }

    @Before
    public void before() {
        var client = steps.clientSteps().createDefaultClient();
        clientId = client.getClientId();
        steps.featureSteps().setCurrentClient(client.getClientId());

    }

    @Test
    @Parameters(method = "params")
    public void validateTextCampaign(boolean featureEnabled,
                                     Boolean receivedCurrentRegion,
                                     Boolean receivedRegularRegion,
                                     @Nullable Defect<?> expectedDefectCurrentRegion,
                                     @Nullable Defect<?> expectedDefectRegularRegion) {

        steps.featureSteps().addClientFeature(clientId, FeatureName.ADVANCED_GEOTARGETING, featureEnabled);
        var campaign = new TextCampaign()
                .withClientId(clientId.asLong())
                .withHasExtendedGeoTargeting(featureEnabled)
                .withUseCurrentRegion(receivedCurrentRegion)
                .withUseRegularRegion(receivedRegularRegion);

        var vr = new ValidationResult<List<CampaignWithAdvancedGeoTargeting>, Defect>(List.of(campaign));

        var container = new CampaignValidationContainerImpl(0, 0L, clientId, null, new CampaignOptions(), null,
                emptyMap());
        var result = validationTypeSupport.validate(container, vr);

        if (expectedDefectCurrentRegion == null && expectedDefectRegularRegion == null) {
            assertThat(result, hasNoDefectsDefinitions());
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


