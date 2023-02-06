package ru.yandex.direct.core.entity.bidmodifiers.update.absegments;

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
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.container.CampaignIdAndAdGroupIdPair;
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
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultABSegmentsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyABSegmentModifier;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRetCondition;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Сценарии валидации корректировок ставок АБ-сегментов")
public class UpdateABSegmentsBidModifiersTest {
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
    private List<BidModifier> modifiers;


    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
        clientInfo = campaign.getClientInfo();
        metrikaGoals = new ArrayList<>();
        addABSegmentBidModifiers();
        modifiers = getABSegmentBidModifiers();

    }

    @Test
    @Description("Изменяем процент на корректировке. Проверяем результат сохранения.")
    public void replaceModifiers_UpdatePercent() {
        Integer expectedPersent = 120;

        ((BidModifierABSegment) modifiers.get(0)).getAbSegmentAdjustments().get(0).setPercent(expectedPersent);

        bidModifierService.replaceModifiers(clientInfo.getClientId(), clientInfo.getUid(), modifiers,
                singleton(new CampaignIdAndAdGroupIdPair().withCampaignId(campaign.getCampaignId())));

        Integer actualPercent =
                ((BidModifierABSegment) getABSegmentBidModifiers().get(0)).getAbSegmentAdjustments().get(0)
                        .getPercent();
        assertSoftly(softly -> {
            softly.assertThat(actualPercent).isEqualTo(expectedPersent);
        });
    }

    @Test
    @Description("Добавляем вторую корректировку. Проверяем, что теперь их две.")
    public void replaceModifiers_AddTheSecondAdjustment() {
        BidModifierABSegment modifier = ((BidModifierABSegment) modifiers.get(0));
        List<BidModifierABSegmentAdjustment> newABSegmentAdjustments = createAdjustments();

        List<BidModifierABSegmentAdjustment> expectedABSegmentAdjustments = modifier.getAbSegmentAdjustments();
        //добавлям еще одну корректировку
        expectedABSegmentAdjustments.addAll(newABSegmentAdjustments);
        modifier.setAbSegmentAdjustments(expectedABSegmentAdjustments);

        bidModifierService.replaceModifiers(clientInfo.getClientId(), clientInfo.getUid(), modifiers,
                singleton(new CampaignIdAndAdGroupIdPair().withCampaignId(campaign.getCampaignId())));

        List<BidModifierABSegmentAdjustment> actualABSegmentAdjustments =
                ((BidModifierABSegment) getABSegmentBidModifiers().get(0)).getAbSegmentAdjustments();
        assertSoftly(softly -> {
            softly.assertThat(actualABSegmentAdjustments).hasSize(2);
        });
    }

    @Test
    @Description("Заменяем корректировку.")
    public void replaceModifiers_ChangeAdjustment() {
        BidModifierABSegment modifier = ((BidModifierABSegment) modifiers.get(0));
        List<BidModifierABSegmentAdjustment> newABSegmentAdjustments = createAdjustments();

        List<BidModifierABSegmentAdjustment> expectedABSegmentAdjustments = modifier.getAbSegmentAdjustments();
        Long oldRetCondId = expectedABSegmentAdjustments.get(0).getAbSegmentRetargetingConditionId();
        Long newBeforeSaveRetCondId = newABSegmentAdjustments.get(0).getAbSegmentRetargetingConditionId();

        //заменяем корректировку
        modifier.setAbSegmentAdjustments(newABSegmentAdjustments);

        bidModifierService.replaceModifiers(clientInfo.getClientId(), clientInfo.getUid(), modifiers,
                singleton(new CampaignIdAndAdGroupIdPair().withCampaignId(campaign.getCampaignId())));

        List<BidModifierABSegmentAdjustment> actualABSegmentAdjustments =
                ((BidModifierABSegment) getABSegmentBidModifiers().get(0)).getAbSegmentAdjustments();

        Long newAfterSaveRetCondId = actualABSegmentAdjustments.get(0).getAbSegmentRetargetingConditionId();

        assertSoftly(softly -> {
            softly.assertThat(actualABSegmentAdjustments).hasSize(1);
            softly.assertThat(newAfterSaveRetCondId).isEqualTo(newBeforeSaveRetCondId);
            softly.assertThat(newAfterSaveRetCondId).isNotEqualTo(oldRetCondId);
        });
    }

    private List<BidModifierABSegmentAdjustment> createAdjustments() {
        RetargetingCondition retCond = defaultABSegmentRetCondition(clientInfo.getClientId());
        RetConditionInfo retConditionInfo = retConditionSteps.createABSegmentRetCondition(retCond, clientInfo);

        metrikaGoals.addAll(retConditionInfo.getRetCondition().collectGoals());

        retargetingGoalsSteps.createMetrikaGoalsInPpcDict(metrikaGoals);
        metrikaClientStub.addGoals(clientInfo.getUid(), new HashSet<>(metrikaGoals));
        return createDefaultABSegmentsAdjustments(retConditionInfo);
    }

    private void addABSegmentBidModifiers() {
        List<BidModifierABSegmentAdjustment> abSegmentAdjustments = createAdjustments();

        bidModifierService.add(
                Lists.newArrayList(
                        createEmptyABSegmentModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withAbSegmentAdjustments(abSegmentAdjustments)
                                .withEnabled(true)
                ),
                campaign.getClientId(), campaign.getUid());
    }

    private List<BidModifier> getABSegmentBidModifiers() {
        return bidModifierService.getByCampaignIds(
                clientInfo.getClientId(),
                singleton(campaign.getCampaignId()),
                singleton(BidModifierType.AB_SEGMENT_MULTIPLIER),
                singleton(BidModifierLevel.CAMPAIGN),
                clientInfo.getUid());
    }
}

