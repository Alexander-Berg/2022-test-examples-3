package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithNetworkSettings;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathHelper;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.NO_CONTEXT_LIMIT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithNetworkSettingsValidatorTest {
    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData() {
        return new Object[][]{
                {RbacRole.SUPER, NO_CONTEXT_LIMIT, true, null, null},
                {RbacRole.SUPER, MAX_CONTEXT_LIMIT + 1, true,
                        new Path(List.of(PathHelper.field(CampaignWithNetworkSettings.CONTEXT_LIMIT.name()))),
                        inInterval(MIN_CONTEXT_LIMIT, MAX_CONTEXT_LIMIT)},
                {RbacRole.CLIENT, NO_CONTEXT_LIMIT, true, new Path(List.of(PathHelper.field("contextLimit"))),
                        inInterval(MIN_CONTEXT_LIMIT, MAX_CONTEXT_LIMIT)},
                {RbacRole.CLIENT, MIN_CONTEXT_LIMIT, true, null, null},
                {RbacRole.CLIENT, MIN_CONTEXT_LIMIT, null,
                        new Path(List.of(PathHelper.field(CampaignWithNetworkSettings.ENABLE_CPC_HOLD.name()))),
                        notNull()},
                {RbacRole.MANAGER, NO_CONTEXT_LIMIT, true, new Path(List.of(PathHelper.field("contextLimit"))),
                        inInterval(MIN_CONTEXT_LIMIT, MAX_CONTEXT_LIMIT)},
                {RbacRole.AGENCY, NO_CONTEXT_LIMIT, true, new Path(List.of(PathHelper.field("contextLimit"))),
                        inInterval(MIN_CONTEXT_LIMIT, MAX_CONTEXT_LIMIT)},
        };
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("role: {0}, value {1}, cpc_hold: {2}")
    public void checkCampaignWithNetworkSettingsValidator(RbacRole operatorRole,
                                                          Integer contextLimit,
                                                          @Nullable Boolean enableCpcHold,
                                                          @Nullable Path expectedPath,
                                                          @Nullable Defect expectedDefect) {

        CampaignWithNetworkSettingsValidator validator = new CampaignWithNetworkSettingsValidator(operatorRole);
        CampaignWithNetworkSettings campaign = new TextCampaign()
                .withContextLimit(contextLimit)
                .withEnableCpcHold(enableCpcHold);

        checkValidator(validator, campaign, expectedPath, expectedDefect);
    }

    private void checkValidator(CampaignWithNetworkSettingsValidator validator,
                                CampaignWithNetworkSettings campaign, @Nullable Path expectedPath,
                                @Nullable Defect expectedDefect) {
        ValidationResult<CampaignWithNetworkSettings, Defect> result = validator.apply(campaign);

        if (expectedDefect == null) {
            assertThat(result).
                    is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            assertThat(result).
                    is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }
}
