package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithSimplifiedStrategyViewForbidden;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
public class CampaignWithSimplifiedStrategyViewForbiddenAddValidationTypeSupportTest {
    private final CampaignWithSimplifiedStrategyViewForbiddenAddValidationTypeSupport validationTypeSupport =
            new CampaignWithSimplifiedStrategyViewForbiddenAddValidationTypeSupport();

    private ClientId clientId;
    private CampaignWithSimplifiedStrategyViewForbidden campaign;
    private ValidationResult<List<CampaignWithSimplifiedStrategyViewForbidden>, Defect> vr;

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomUtils.nextLong());
        campaign = new CpmBannerCampaign().withClientId(clientId.asLong());
        vr = new ValidationResult<>(List.of(campaign));
    }

    @Test
    public void validate_ValueNull() {
        campaign.withIsSimplifiedStrategyViewEnabled(null);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_ValueFalse_MustBeNull() {
        campaign.withIsSimplifiedStrategyViewEnabled(false);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED)),
                CommonDefects.isNull()
        )));
    }

    @Test
    public void validate_ValueTrue_MustBeNull() {
        campaign.withIsSimplifiedStrategyViewEnabled(true);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED)),
                CommonDefects.isNull()
        )));
    }
}
