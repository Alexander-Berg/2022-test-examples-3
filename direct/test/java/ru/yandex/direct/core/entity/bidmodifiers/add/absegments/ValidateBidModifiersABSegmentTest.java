package ru.yandex.direct.core.entity.bidmodifiers.add.absegments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.RetargetingGoalsSteps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.abSegmentNotFound;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.abSegmentSectionNotFound;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientABSegmentsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientABSegmentModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRetCondition;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Сценарии валидации корректировок ставок АБ-сегментов")
public class ValidateBidModifiersABSegmentTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private RetargetingGoalsSteps retargetingGoalsSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    private CampaignInfo campaign;
    private List<Goal> metrikaGoals;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
        clientInfo = campaign.getClientInfo();
        metrikaGoals = new ArrayList<>();
    }

    private List<BidModifierABSegmentAdjustment> createAdjustments() {
        RetargetingCondition retCond = defaultABSegmentRetCondition(clientInfo.getClientId());
        RetConditionInfo retConditionInfo = retConditionSteps.createABSegmentRetCondition(retCond, clientInfo);

        metrikaGoals.addAll(retConditionInfo.getRetCondition().collectGoals());

        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(metrikaGoals);
        metrikaClientStub.addGoals(clientInfo.getUid(), new HashSet<>(metrikaGoals));
        return createDefaultClientABSegmentsAdjustments(retConditionInfo);
    }

    @Test
    @Description("Пытаемся сохранить корректировки для неправильной секции")
    public void validateABSegmentsAdjustments_WrongSectionId_AbSegmentSectionNotFoundError() {
        List<BidModifierABSegmentAdjustment> abSegmentAdjustments = createAdjustments();

        abSegmentAdjustments.get(0).setSectionId(12345L);

        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientABSegmentModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withAbSegmentAdjustments(abSegmentAdjustments)));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("abSegmentAdjustments"), index(0)),
                        abSegmentSectionNotFound(abSegmentAdjustments.get(0).getSegmentId()))));

    }

    @Test
    @Description("Пытаемся сохранить корректировки для несуществуюшим сегментом")
    public void validateABSegmentsAdjustments_WrongSegmentId_AbSegmentNotFoundError() {
        List<BidModifierABSegmentAdjustment> abSegmentAdjustments = createAdjustments();

        abSegmentAdjustments.get(0).setSegmentId(12345L);

        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientABSegmentModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withAbSegmentAdjustments(abSegmentAdjustments)));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("abSegmentAdjustments"), index(0)),
                        abSegmentNotFound(12345L))));
    }

    @Test
    @Description("Пытаемся сохранить корректировки для неправильной секции и несуществующим сегментом. Ругаемся " +
            "только на сегмент")
    public void validateABSegmentsAdjustments_WrongBothSectionIdAndSegmentId_AbSegmentNotFoundError() {
        List<BidModifierABSegmentAdjustment> abSegmentAdjustments = createAdjustments();

        abSegmentAdjustments.get(0).setSegmentId(67890L);
        abSegmentAdjustments.get(0).setSectionId(12345L);

        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientABSegmentModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withAbSegmentAdjustments(abSegmentAdjustments)));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("abSegmentAdjustments"), index(0)),
                        abSegmentNotFound(67890L))));

    }

    @Test
    @Description("Пытаемся сохранить корректировки для сегмента с невалидными ID")
    public void validateABSegmentsAdjustmentSchema_WrongSegmentId_AbSegmentNotFoundError() {
        List<BidModifierABSegmentAdjustment> abSegmentAdjustments = createAdjustments();

        abSegmentAdjustments.get(0).setSegmentId(-10L);
        abSegmentAdjustments.get(0).setSectionId(-20L);

        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientABSegmentModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withAbSegmentAdjustments(abSegmentAdjustments)));

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("abSegmentAdjustments"), index(0), field("segmentId")),
                        validId())));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("abSegmentAdjustments"), index(0), field("sectionId")),
                        validId())));
    }

    @Test
    @Description("Добавляем в одном запросе три типа корректировок ставок - АБ-сегменты, демографическую и мобильную")
    public void addMultipleBidModifiers_SuccessResult() {
        List<BidModifierABSegmentAdjustment> abSegmentAdjustments = createAdjustments();


        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientABSegmentModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withAbSegmentAdjustments(abSegmentAdjustments),
                        createEmptyClientMobileModifier().withCampaignId(campaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment()),
                        createEmptyClientDemographicsModifier().withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments())
                ));

        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(3);
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}
