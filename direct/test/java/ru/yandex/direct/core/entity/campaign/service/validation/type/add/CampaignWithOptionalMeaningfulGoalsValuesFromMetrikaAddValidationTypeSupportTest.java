package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalMeaningfulGoalsValuesFromMetrika;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainerImpl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.meaningfulGoalsValuesFromMetrikaNotSupported;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaAddValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;

    private CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        validationTypeSupport =
                new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaAddValidationTypeSupport(featureService);
    }

    @Test
    public void preValidate_FeatureIsOff_Fail() {
        when(featureService.isEnabledForClientId(clientId,
                FeatureName.AUTO_BUDGET_MEANINGFUL_GOALS_VALUES_FROM_METRIKA))
                .thenReturn(false);

        SmartCampaign campaign = TestCampaigns.defaultSmartCampaign()
                .withClientId(clientId.asLong())
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        ValidationResult<List<CampaignWithOptionalMeaningfulGoalsValuesFromMetrika>, Defect> vr =
                new ValidationResult<>(List.of(campaign));

        var result = validationTypeSupport.preValidate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0),
                        field(CampaignWithOptionalMeaningfulGoalsValuesFromMetrika.IS_MEANINGFUL_GOALS_VALUES_FROM_METRIKA_ENABLED)),
                meaningfulGoalsValuesFromMetrikaNotSupported()
        )));
    }

    @Test
    public void preValidate_FeatureIsOn_Success() {
        when(featureService.isEnabledForClientId(clientId,
                FeatureName.AUTO_BUDGET_MEANINGFUL_GOALS_VALUES_FROM_METRIKA))
                .thenReturn(true);

        SmartCampaign campaign = TestCampaigns.defaultSmartCampaign()
                .withClientId(clientId.asLong())
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        ValidationResult<List<CampaignWithOptionalMeaningfulGoalsValuesFromMetrika>, Defect> vr =
                new ValidationResult<>(List.of(campaign));

        var result = validationTypeSupport.preValidate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_FeatureIsOffAndCopyMode_Success() {
        when(featureService.isEnabledForClientId(clientId,
                FeatureName.AUTO_BUDGET_MEANINGFUL_GOALS_VALUES_FROM_METRIKA))
                .thenReturn(false);

        SmartCampaign campaign = TestCampaigns.defaultSmartCampaign()
                .withClientId(clientId.asLong())
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        ValidationResult<List<CampaignWithOptionalMeaningfulGoalsValuesFromMetrika>, Defect> vr =
                new ValidationResult<>(List.of(campaign));

        CampaignValidationContainerImpl container = new CampaignValidationContainerImpl(0, 0L,
                clientId, null, new CampaignOptions.Builder().withCopy(true).build(), null, emptyMap());

        var result = validationTypeSupport.preValidate(container, vr);
        assertThat(result, hasNoDefectsDefinitions());
    }
}
