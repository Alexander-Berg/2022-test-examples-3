package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignForbiddenOnTouch;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignForbiddenOnTouchUpdateValidationTypeSupportTest {

    private static CampaignValidationContainer container;

    private CampaignForbiddenOnTouchUpdateValidationTypeSupport validationTypeSupport;

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
        var clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        var uid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, uid, clientId);
        validationTypeSupport = new CampaignForbiddenOnTouchUpdateValidationTypeSupport();
    }

    @Test
    public void preValidate_expectedForbiddenToChange() {
        var campaign = (CampaignForbiddenOnTouch) TestCampaigns.newCampaignByCampaignType(campaignType);
        var vr = validationTypeSupport.preValidate(container, new ValidationResult<>(List.of(
                ModelChanges.build(campaign, CampaignForbiddenOnTouch.IS_TOUCH, true),
                ModelChanges.build(campaign, CampaignForbiddenOnTouch.IS_TOUCH, false)
        )));
        var firstRes = path(index(0), field(CampaignForbiddenOnTouch.IS_TOUCH));
        var secondRes = path(index(1), field(CampaignForbiddenOnTouch.IS_TOUCH));
        assertThat(vr, allOf(
                hasDefectWithDefinition(validationError(firstRes, DefectIds.FORBIDDEN_TO_CHANGE)),
                hasDefectWithDefinition(validationError(secondRes, DefectIds.FORBIDDEN_TO_CHANGE))
        ));
    }

}
