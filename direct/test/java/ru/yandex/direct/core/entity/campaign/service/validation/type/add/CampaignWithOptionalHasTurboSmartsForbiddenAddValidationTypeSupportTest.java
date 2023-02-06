package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalHasTurboSmartsForbidden;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
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
@RunWith(Parameterized.class)
public class CampaignWithOptionalHasTurboSmartsForbiddenAddValidationTypeSupportTest {

    private CampaignWithOptionalHasTurboSmartsForbiddenAddValidationTypeSupport validationTypeSupport =
            new CampaignWithOptionalHasTurboSmartsForbiddenAddValidationTypeSupport();

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
    private CampaignWithOptionalHasTurboSmartsForbidden campaign;
    private ValidationResult<List<CampaignWithOptionalHasTurboSmartsForbidden>, Defect> vr;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.CPM_BANNER},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        campaign = ((CampaignWithOptionalHasTurboSmartsForbidden) TestCampaigns.newCampaignByCampaignType(campaignType))
                .withClientId(clientId.asLong());
        vr = new ValidationResult<>(List.of(campaign));
    }

    @Test
    public void validate_ValueNull() {
        campaign.withHasTurboSmarts(null);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_ValueFalse_MustBeNull() {
        campaign.withHasTurboSmarts(false);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithOptionalHasTurboSmartsForbidden.HAS_TURBO_SMARTS)),
                CommonDefects.isNull()
        )));
    }

    @Test
    public void validate_ValueTrue_MustBeNull() {
        campaign.withHasTurboSmarts(true);
        var result = validationTypeSupport.validate(CampaignValidationContainer.create(0, 0L, clientId), vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithOptionalHasTurboSmartsForbidden.HAS_TURBO_SMARTS)),
                CommonDefects.isNull()
        )));
    }
}
