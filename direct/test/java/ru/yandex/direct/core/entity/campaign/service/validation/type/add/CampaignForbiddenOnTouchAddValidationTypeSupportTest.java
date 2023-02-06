package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignForbiddenOnTouch;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class CampaignForbiddenOnTouchAddValidationTypeSupportTest {

    private static ClientId clientId;
    private static Long uid;

    private CampaignForbiddenOnTouchAddValidationTypeSupport validationTypeSupport;

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
    public void setUp() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        validationTypeSupport = new CampaignForbiddenOnTouchAddValidationTypeSupport();
    }

    @Test
    public void validate_Successfully() {
        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(createCampaign())));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_expectedMustBeNull() {
        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(
                List.of(createCampaign().withIsTouch(true), createCampaign().withIsTouch(false))
        ));
        var firstRes = path(index(0), field(CampaignForbiddenOnTouch.IS_TOUCH));
        var secondRes = path(index(1), field(CampaignForbiddenOnTouch.IS_TOUCH));
        assertThat(vr, allOf(
                hasDefectWithDefinition(validationError(firstRes, DefectIds.MUST_BE_NULL)),
                hasDefectWithDefinition(validationError(secondRes, DefectIds.MUST_BE_NULL))
        ));
    }

    private CampaignForbiddenOnTouch createCampaign() {
        return (CampaignForbiddenOnTouch) TestCampaigns.newCampaignByCampaignType(campaignType);
    }
}
