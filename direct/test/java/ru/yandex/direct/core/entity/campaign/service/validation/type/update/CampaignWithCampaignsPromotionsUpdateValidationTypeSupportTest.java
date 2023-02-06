package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithCampaignsPromotions;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPromotion;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.DateDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithCampaignsPromotionsUpdateValidationTypeSupportTest {

    private static final long CID = 1L;

    @InjectMocks
    private CampaignWithCampaignsPromotionsUpdateValidationTypeSupport typeSupport;

    @Test
    public void testValidateSuccessfully() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(2)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(CID)
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        ValidationResult<List<CampaignWithCampaignsPromotions>, Defect> result = typeSupport.validate(
                container,
                new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testValidationError() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(start)
                .withFinish(start.minusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(CID)
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        ValidationResult<List<CampaignWithCampaignsPromotions>, Defect> result = typeSupport.validate(
                container,
                new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasDefectDefinitionWith(validationError(path(
                index(0), field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.FINISH)),
                greaterThanOrEqualTo(start))));
    }

}
