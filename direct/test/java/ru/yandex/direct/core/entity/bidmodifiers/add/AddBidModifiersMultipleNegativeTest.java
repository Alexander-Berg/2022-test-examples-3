package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.invalidPercentShouldBePositive;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddBidModifiersMultipleNegativeTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private ClientSteps clientSteps;

    private ClientInfo client;
    private CampaignInfo campaign1;
    private CampaignInfo campaign2;
    private RetConditionInfo retCondition1;
    private RetConditionInfo retCondition2;

    @Before
    public void before() {
        client = clientSteps.createDefaultClient();
        campaign1 = campaignSteps.createActiveTextCampaign(client);
        campaign2 = campaignSteps.createActiveTextCampaign(client);
        retCondition1 = retConditionSteps.createDefaultRetCondition(client);
        retCondition2 = retConditionSteps.createDefaultRetCondition(client);
    }

    // TODO : перенести этот тест в делегат
//    @Test
//    @Description("Попытка добавления различных типов корректировок в одном объекте BidModifier")
//    public void severalAdjustmentTypesInOneBidModifierTest() {
//        MassResult<List<Long>> result = bidModifierService.add(
//                Lists.newArrayList(
//                        new BidModifierItem()
//                                .withCampaignId(campaign1.getCampaignId())
//                                .withMobileAdjustment(createDefaultMobileAdjustment())
//                                .withRegionalAdjustments(createDefaultGeoAdjustments())
//                ), client.getClientId(), client.getUid());
//        assertThat(result.getValidationResult(), hasDefectWithDefinition(
//                validationError(path(index(0)),
//                        new Defect<>(BidModifiersDefectIds.ModelDefects.POSSIBLE_ONLY_ONE_FIELD,
//                                BidModifierAddOperation.ALL_ADJUSTMENT_FIELDS))));
//    }

    @Test
    @Description("При добавлении набора демографических корректировок ставок возникающая ошибка - общая для всего набора")
    public void multipleDemographicsAdjustmentSingleErrorTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign1.getCampaignId())
                                .withDemographicsAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._0_17)
                                                        .withGender(GenderType.MALE)
                                                        .withPercent(110),
                                                new BidModifierDemographicsAdjustment()
                                                        .withAge(AgeType._25_34)
                                                        .withGender(GenderType.FEMALE)
                                                        .withPercent(-1)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("demographicsAdjustments"), index(1), field("percent")),
                        invalidPercentShouldBePositive())));
    }

    @Test
    @Description("При добавлении набора корректировок ставок ретаргетинга возникающая ошибка - общая для всего набора")
    public void multipleRetargetingAdjustmentSingleErrorTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(campaign1.getCampaignId())
                                .withRetargetingAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierRetargetingAdjustment()
                                                        .withRetargetingConditionId(retCondition1.getRetConditionId())
                                                        .withPercent(110),
                                                new BidModifierRetargetingAdjustment()
                                                        .withRetargetingConditionId(retCondition2.getRetConditionId())
                                                        .withPercent(-1)
                                        )
                                )));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("retargetingAdjustments"), index(1), field("percent")),
                        invalidPercentShouldBePositive())));
    }

    @Test
    @Description("При добавлении нескольких корректировок ставок одного типа, одна из которх некорректна, " +
            "результат операции выводится для каждого объекта BidModifier раздельно")
    public void multipleModifiersOneInvalidSameTypesTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign1.getCampaignId())
                                .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments()),
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign2.getCampaignId())
                                .withDemographicsAdjustments(Collections.singletonList(
                                        new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._25_34)
                                                .withGender(GenderType.FEMALE)
                                                .withPercent(1301)))));

        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(result.getResult().get(1).getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field("demographicsAdjustments"), index(0), field("percent")),
                            new Defect<>(
                                    NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX,
                                    new NumberDefectParams().withMax(1300))))));
        });
    }

    @Test
    @Description("При добавлении нескольких некорректных корректировок ставок одного типа " +
            "результат операции выводится для каждого объекта BidModifier раздельно")
    public void multipleModifiersAllInvalidSameTypesTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign1.getCampaignId())
                                .withDemographicsAdjustments(Collections.singletonList(
                                        new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._25_34)
                                                .withGender(GenderType.FEMALE)
                                                .withPercent(-1))),
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign2.getCampaignId())
                                .withDemographicsAdjustments(Collections.singletonList(
                                        new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._25_34)
                                                .withGender(GenderType.FEMALE)
                                                .withPercent(1301)))));

        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field("demographicsAdjustments"), index(0), field("percent")),
                            invalidPercentShouldBePositive()))));
            softly.assertThat(result.getResult().get(1).getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field("demographicsAdjustments"), index(0), field("percent")),
                            new Defect<>(
                                    NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX,
                                    new NumberDefectParams().withMax(1300))))));
        });
    }

    @Test
    @Description("При добавлении нескольких корректировок ставок разных типов, обе из которых корректны " +
            "результат операции выводится для каждого объекта BidModifier раздельно")
    public void multipleModifiersOneInvalidDifferentTypesTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(campaign1.getCampaignId())
                                .withMobileAdjustment(new BidModifierMobileAdjustment().withPercent(10)),
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign2.getCampaignId())
                                .withDemographicsAdjustments(Collections.singletonList(
                                        new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._25_34)
                                                .withGender(GenderType.FEMALE)
                                                .withPercent(110)))));

        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(1).getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        });
    }

    @Test
    @Description("При добавлении нескольких некорректных корректировок ставок разных типов, одна из которых некорректна " +
            "результат операции выводится для каждого объекта BidModifier раздельно")
    public void multipleModifiersAllInvalidDifferentTypesTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(campaign1.getCampaignId())
                                .withMobileAdjustment(new BidModifierMobileAdjustment().withPercent(10)),
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign2.getCampaignId())
                                .withDemographicsAdjustments(Collections.singletonList(
                                        new BidModifierDemographicsAdjustment()
                                                .withAge(AgeType._25_34)
                                                .withGender(GenderType.FEMALE)
                                                .withPercent(-1)))));

        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(1).getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field("demographicsAdjustments"), index(0), field("percent")),
                            invalidPercentShouldBePositive()))));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, client.getClientId(), client.getUid());
    }
}
