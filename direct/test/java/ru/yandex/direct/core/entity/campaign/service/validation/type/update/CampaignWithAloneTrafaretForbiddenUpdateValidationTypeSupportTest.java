package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAloneTrafaretForbidden;
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
public class CampaignWithAloneTrafaretForbiddenUpdateValidationTypeSupportTest {

    private CampaignWithAloneTrafaretForbiddenUpdateValidationTypeSupport validationTypeSupport =
            new CampaignWithAloneTrafaretForbiddenUpdateValidationTypeSupport();

    private ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
    private CampaignWithAloneTrafaretForbidden campaign;
    private ValidationResult<List<CampaignWithAloneTrafaretForbidden>, Defect> vr;
    private CampaignValidationContainer container = CampaignValidationContainer
            .create(0,RandomUtils.nextLong(), clientId);

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.CPM_PRICE},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        campaign = (CampaignWithAloneTrafaretForbidden) TestCampaigns.newCampaignByCampaignType(campaignType)
                .withClientId(clientId.asLong());
        vr = new ValidationResult<>(List.of(campaign));
    }

    @Test
    public void validate_ValueNull_NoDefects() {
        campaign.withIsAloneTrafaretAllowed(null);
        var result = validationTypeSupport.validate(container, vr);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_ValueFalse_DefectMustBeNull() {
        campaign.withIsAloneTrafaretAllowed(false);
        var result = validationTypeSupport.validate(container, vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAloneTrafaretForbidden.IS_ALONE_TRAFARET_ALLOWED)),
                CommonDefects.isNull()
        )));
    }

    @Test
    public void validate_ValueTrue_DefectMustBeNull() {
        campaign.withIsAloneTrafaretAllowed(true);
        var result = validationTypeSupport.validate(container, vr);
        assertThat(result, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAloneTrafaretForbidden.IS_ALONE_TRAFARET_ALLOWED)),
                CommonDefects.isNull()
        )));
    }
}
