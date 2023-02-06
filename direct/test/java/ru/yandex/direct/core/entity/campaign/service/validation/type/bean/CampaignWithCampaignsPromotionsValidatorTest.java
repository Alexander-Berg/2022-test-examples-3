package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithCampaignsPromotions;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPromotion;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_ACTIVE_CAMPAIGNS_PROMOTIONS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_CAMPAIGNS_PROMOTIONS_PERIOD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_CAMPAIGNS_PROMOTION_PERCENT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_CAMPAIGNS_PROMOTION_PERCENT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignsPromotionsPeriodsAreIntersected;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.maxActiveCampaignsPromotions;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.DateDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.defect.DateDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class CampaignWithCampaignsPromotionsValidatorTest {

    private static final Long CID = 123L;

    @Test
    public void moreThanMaxActivePromotionsCount_HasError() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        for (int i = 0; i <= MAX_ACTIVE_CAMPAIGNS_PROMOTIONS; i++) {
            campaignsPromotions.add(new CampaignsPromotion()
                    .withCid(CID)
                    .withPromotionId(0L)
                    .withPercent(100L)
                    .withStart(LocalDate.now().plusDays(i * 2 + 1))
                    .withFinish(LocalDate.now().plusDays(i * 2 + 2)));
        }
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);


        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.CAMPAIGNS_PROMOTIONS)),
                maxActiveCampaignsPromotions(MAX_ACTIVE_CAMPAIGNS_PROMOTIONS)))));
    }

    @Test
    public void maxActivePromotionsCount_NoError() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        for (int i = 0; i < MAX_ACTIVE_CAMPAIGNS_PROMOTIONS; i++) {
            campaignsPromotions.add(new CampaignsPromotion()
                    .withCid(CID)
                    .withPromotionId(0L)
                    .withPercent(100L)
                    .withStart(LocalDate.now().plusDays(i * 2 + 1))
                    .withFinish(LocalDate.now().plusDays(i * 2 + 2)));
        }
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);


        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void campaignsPromotionsIsNull() {
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(null);


        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void firstPeriodIntersectSecondInLeft() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(2)));
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(2))
                .withFinish(LocalDate.now().plusDays(3)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.CAMPAIGNS_PROMOTIONS)),
                campaignsPromotionsPeriodsAreIntersected()))));
    }

    @Test
    public void firstPeriodIntersectSecondInRight() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(2))
                .withFinish(LocalDate.now().plusDays(3)));
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(2)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.CAMPAIGNS_PROMOTIONS)),
                campaignsPromotionsPeriodsAreIntersected()))));
    }

    @Test
    public void firstPeriodInsideSecond() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(2))
                .withFinish(LocalDate.now().plusDays(3)));
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(4)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.CAMPAIGNS_PROMOTIONS)),
                campaignsPromotionsPeriodsAreIntersected()))));
    }

    @Test
    public void secondPeriodInsideFirst() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(4)));
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(2))
                .withFinish(LocalDate.now().plusDays(3)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.CAMPAIGNS_PROMOTIONS)),
                campaignsPromotionsPeriodsAreIntersected()))));
    }

    @Test
    public void nullInCampaignsPromotionsList() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(4)));
        campaignsPromotions.add(null);
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(1)),
                notNull()))));
    }

    @Test
    public void notUniquePromotionId() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(2L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(4)));
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(2L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(2))
                .withFinish(LocalDate.now().plusDays(3)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0)),
                duplicatedElement()))));
        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(1)),
                duplicatedElement()))));
    }

    @Test
    public void promotionIdIsNull() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(null)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(4)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.PROMOTION_ID)),
                notNull()))));
    }

    @Test
    public void cidIsNull() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(null)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(4)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.CID)),
                notNull()))));
    }

    @Test
    public void startIsNull() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(null)
                .withFinish(LocalDate.now().plusDays(4)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.START)),
                notNull()))));
    }

    @Test
    public void finishIsNull() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(null));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.FINISH)),
                notNull()))));
    }

    @Test
    public void finishBeforeStart() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(start)
                .withFinish(start.minusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.FINISH)),
                greaterThanOrEqualTo(start)))));
    }

    @Test
    public void finishEqualStart() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(start)
                .withFinish(start));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void promoactionPeriodMoreThanMax() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(start)
                .withFinish(start.plusDays(MAX_CAMPAIGNS_PROMOTIONS_PERIOD).plusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.FINISH)),
                lessThanOrEqualTo(start.plusDays(MAX_CAMPAIGNS_PROMOTIONS_PERIOD))))));
    }

    @Test
    public void maxPromoactionPeriod() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(100L)
                .withStart(start)
                .withFinish(start.plusDays(MAX_CAMPAIGNS_PROMOTIONS_PERIOD)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void percentIsNull() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(null)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.PERCENT)),
                notNull()))));
    }

    @Test
    public void minPercent() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(MIN_CAMPAIGNS_PROMOTION_PERCENT)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void maxPercent() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(MAX_CAMPAIGNS_PROMOTION_PERCENT)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void percentLessThanMin() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(MIN_CAMPAIGNS_PROMOTION_PERCENT - 1)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.PERCENT)),
                inInterval(MIN_CAMPAIGNS_PROMOTION_PERCENT, MAX_CAMPAIGNS_PROMOTION_PERCENT)))));
    }

    @Test
    public void percentMoreThanMax() {
        List<CampaignsPromotion> campaignsPromotions = new ArrayList<>();
        campaignsPromotions.add(new CampaignsPromotion()
                .withCid(CID)
                .withPromotionId(0L)
                .withPercent(MAX_CAMPAIGNS_PROMOTION_PERCENT + 1)
                .withStart(LocalDate.now().plusDays(1))
                .withFinish(LocalDate.now().plusDays(1)));
        CampaignWithCampaignsPromotions campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignsPromotions(campaignsPromotions);

        CampaignValidationContainer container = mock(CampaignValidationContainer.class);
        doReturn(false).when(container).isCopy();
        CampaignWithCampaignsPromotionsValidator validator = new CampaignWithCampaignsPromotionsValidator();

        ValidationResult<CampaignWithCampaignsPromotions, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(
                field(TextCampaign.CAMPAIGNS_PROMOTIONS), index(0), field(CampaignsPromotion.PERCENT)),
                inInterval(MIN_CAMPAIGNS_PROMOTION_PERCENT, MAX_CAMPAIGNS_PROMOTION_PERCENT)))));
    }
}
