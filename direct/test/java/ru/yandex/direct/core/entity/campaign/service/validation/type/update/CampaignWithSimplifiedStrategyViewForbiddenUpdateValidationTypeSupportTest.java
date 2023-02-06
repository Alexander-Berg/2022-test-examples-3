package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithSimplifiedStrategyViewForbidden;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.validation.defects.RightsDefects;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
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
public class CampaignWithSimplifiedStrategyViewForbiddenUpdateValidationTypeSupportTest {
    private final CampaignWithSimplifiedStrategyViewForbiddenUpdateValidationTypeSupport validationTypeSupport =
            new CampaignWithSimplifiedStrategyViewForbiddenUpdateValidationTypeSupport();

    private ModelChanges<CampaignWithSimplifiedStrategyViewForbidden> campaignModelChanges;
    private CampaignValidationContainer container;
    private ValidationResult<List<ModelChanges<CampaignWithSimplifiedStrategyViewForbidden>>, Defect> vr;

    @Before
    public void before() {
        ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
        container = CampaignValidationContainer.create(0, RandomUtils.nextLong(), clientId);
        campaignModelChanges = new ModelChanges<>(RandomUtils.nextLong(),
                CampaignWithSimplifiedStrategyViewForbidden.class);
        vr = new ValidationResult<>(List.of(campaignModelChanges));
    }

    @Test
    public void preValidate_NoModelChanges() {
        var result = validationTypeSupport.preValidate(container, vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_ValueNull_MustHaveNoModelChanges() {
        campaignModelChanges.process(null,
                CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED);
        var result = validationTypeSupport.preValidate(container, vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED)),
                RightsDefects.forbiddenToChange()
        )));
    }

    @Test
    public void preValidate_ValueFalse_MustHaveNoModelChanges() {
        campaignModelChanges.process(false,
                CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED);
        var result = validationTypeSupport.preValidate(container, vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED)),
                RightsDefects.forbiddenToChange()
        )));
    }

    @Test
    public void preValidate_ValueTrue_MustHaveNoModelChanges() {
        campaignModelChanges.process(true,
                CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED);
        var result = validationTypeSupport.preValidate(container, vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithSimplifiedStrategyViewForbidden.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED)),
                RightsDefects.forbiddenToChange()
        )));
    }
}
