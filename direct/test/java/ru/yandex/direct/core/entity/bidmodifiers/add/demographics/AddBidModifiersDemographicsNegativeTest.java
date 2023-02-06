package ru.yandex.direct.core.entity.bidmodifiers.add.demographics;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.demographicsConditionsIntersection;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Негативные сценарии добавления демографических корректировок ставок")
public class AddBidModifiersDemographicsNegativeTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaign;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
    }

    @Test
    @Description("Отсутствие полей Age и Gender, одно из которых должно обязательно присутствовать")
    public void noAgeAndGenderTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(
                                        singletonList(new BidModifierDemographicsAdjustment().withPercent(110)))));
        //
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("demographicsAdjustments"), index(0)),
                        BidModifiersDefects.requiredAtLeastOneOfAgeOrGender())));
    }

    @Test
    @Description("Отсутствие поля percent")
    public void noPercent() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(
                                        singletonList(new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._18_24)
                                                .withGender(GenderType.MALE)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(BidModifierDemographics.DEMOGRAPHICS_ADJUSTMENTS),
                        index(0), field(BidModifierAdjustment.PERCENT)),
                        notNull())));
    }

    @Test
    @Description("Идентичные демографические корректировки ставок в одном запросе")
    public void identicalAdjustmentsTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._25_34)
                                                        .withGender(GenderType.FEMALE)
                                                        .withPercent(110),
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._25_34)
                                                        .withGender(GenderType.FEMALE)
                                                        .withPercent(110)))));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("demographicsAdjustments")),
                        demographicsConditionsIntersection())));
    }

    @Test
    @Description("Пересекающиеся демографические корректировки ставок на '45+' и '55+'")
    public void intersectingAdjustmentsTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._45_)
                                                        .withPercent(110),
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._55_)
                                                        .withPercent(120)))));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("demographicsAdjustments")),
                        demographicsConditionsIntersection())));
    }

    @Test
    @Description("Добавляем демографическую корректировку ставок, пересекающуюся с ранее добавленной")
    public void adjustmentIntersectionTest() {
        addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(
                                        singletonList(
                                                new BidModifierDemographicsAdjustment()
                                                        .withGender(GenderType.MALE)
                                                        .withPercent(110)))));
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(
                                        singletonList(
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._0_17)
                                                        .withGender(GenderType.MALE)
                                                        .withPercent(110)))));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("demographicsAdjustments")),
                        demographicsConditionsIntersection())));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}
