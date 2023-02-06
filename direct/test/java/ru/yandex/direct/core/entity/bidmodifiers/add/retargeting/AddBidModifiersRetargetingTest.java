package ru.yandex.direct.core.entity.bidmodifiers.add.retargeting;

import java.util.List;

import com.google.common.collect.Lists;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilterAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultRetargetingFilterAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingFilterModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Позитивные сценарии добавления корректировок ставок ретаргетинга")
public class AddBidModifiersRetargetingTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    private CampaignInfo campaign;
    private AdGroupInfo adGroup;
    private Long retCondId;
    private Long retCondId2;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
        retCondId = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo()).getRetConditionId();
        retCondId2 = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo()).getRetConditionId();
        adGroup = adGroupSteps.createDefaultAdGroup(campaign);
    }

    @Test
    @Description("Добавляем корректировку и получаем её же")
    public void addOneRetargetingModifierTest() {
        List<AbstractBidModifierRetargetingAdjustment> retargetingAdjustments =
                createDefaultClientRetargetingAdjustments(retCondId);
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRetargetingAdjustments(retargetingAdjustments)));
        List<BidModifier> gotModifiers = getBidModifierOfGivenTypeAndLevelFromDb(BidModifierType.RETARGETING_MULTIPLIER,
                BidModifierLevel.CAMPAIGN);
        List<AbstractBidModifierRetargetingAdjustment> gotAdjustments =
                ((BidModifierRetargeting) gotModifiers.get(0)).getRetargetingAdjustments();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(gotAdjustments.get(0))
                    .is(matchedBy(
                            retargetingAdjustmentWithProperties(
                                    getRealId(result.getResult().get(0).getResult().get(0)),
                                    retargetingAdjustments.get(0).getPercent(), retCondId, true)));
        });
    }

    private Matcher<BidModifierRetargetingAdjustment> retargetingAdjustmentWithProperties(
            long id, int percent, long retCondId, boolean accessible) {
        return allOf(
                hasProperty("id", equalTo(id)),
                hasProperty("percent", equalTo(percent)),
                hasProperty("retargetingConditionId", equalTo(retCondId)),
                hasProperty("accessible", equalTo(accessible))
        );
    }

    @Test
    @Description("Добавляем две корректировки и проверяем, как они разложились в БД")
    public void addTwoRetargetingModifiersDbStateTest() {
        BidModifierRetargetingAdjustment adjustment1 =
                new BidModifierRetargetingAdjustment().withRetargetingConditionId(retCondId).withPercent(110);
        BidModifierRetargetingAdjustment adjustment2 =
                new BidModifierRetargetingAdjustment().withRetargetingConditionId(retCondId2).withPercent(120);
        List<AbstractBidModifierRetargetingAdjustment> retargetingAdjustments =
                Lists.newArrayList(adjustment1, adjustment2);
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRetargetingAdjustments(retargetingAdjustments)));

        List<BidModifier> modifiersInDb = getBidModifierOfGivenTypeAndLevelFromDb(BidModifierType.RETARGETING_MULTIPLIER,
                BidModifierLevel.CAMPAIGN);
        List<AbstractBidModifierRetargetingAdjustment> adjustments =
                ((BidModifierRetargeting) modifiersInDb.get(0)).getRetargetingAdjustments();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(adjustments).is(matchedBy(allOf(hasSize(2),
                    containsInAnyOrder(
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(0)))),
                                    hasProperty("percent", equalTo(adjustment1.getPercent())),
                                    hasProperty("retargetingConditionId",
                                            equalTo(adjustment1.getRetargetingConditionId())),
                                    hasProperty("accessible", equalTo(true))
                            ),
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(1)))),
                                    hasProperty("percent", equalTo(adjustment2.getPercent())),
                                    hasProperty("retargetingConditionId",
                                            equalTo(adjustment2.getRetargetingConditionId())),
                                    hasProperty("accessible", equalTo(true))
                            )
                    ))));
        });
    }

    @Test
    @Description("Добавляем две корректировки разного типа, убеждаемся что их можно строго различить")
    public void addRetargetingModifierAndRetargetingFilterModifier() {
        List<AbstractBidModifierRetargetingAdjustment> adjustment1 = createDefaultRetargetingAdjustments(retCondId);
        List<AbstractBidModifierRetargetingAdjustment> adjustment2 =
                createDefaultRetargetingFilterAdjustments(retCondId2);
        var retargetingModifier = createEmptyClientRetargetingModifier()
                .withAdGroupId(adGroup.getAdGroupId())
                .withRetargetingAdjustments(adjustment1);
        var retargetingFilterModifier = createEmptyRetargetingFilterModifier()
                .withAdGroupId(adGroup.getAdGroupId())
                .withRetargetingAdjustments(adjustment2);

        MassResult<List<Long>> result = addBidModifiers(List.of(retargetingModifier, retargetingFilterModifier));
        SoftAssertions soft = new SoftAssertions();

        var firstModifierInDb = getBidModifierOfGivenTypeAndLevelFromDb(BidModifierType.RETARGETING_MULTIPLIER,
                BidModifierLevel.ADGROUP);
        var firstAdjustmentInDb = ((BidModifierRetargeting) firstModifierInDb.get(0)).getRetargetingAdjustments();
        var secondModifierInDb = getBidModifierOfGivenTypeAndLevelFromDb(BidModifierType.RETARGETING_FILTER,
                BidModifierLevel.ADGROUP);
        var secondAdjustmentInDb = ((BidModifierRetargetingFilter) secondModifierInDb.get(0)).getRetargetingAdjustments();


        soft.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        checkSingleBidModifierByType(soft, BidModifierType.RETARGETING_MULTIPLIER, firstModifierInDb);
        checkSingleBidModifierByType(soft, BidModifierType.RETARGETING_FILTER, secondModifierInDb);

        checkSingleAdjustmentByCLass(soft, BidModifierRetargetingAdjustment.class, firstAdjustmentInDb);
        checkSingleAdjustmentByCLass(soft, BidModifierRetargetingFilterAdjustment.class, secondAdjustmentInDb);

        soft.assertAll();
    }

    private List<BidModifier> getBidModifierOfGivenTypeAndLevelFromDb(BidModifierType type, BidModifierLevel level) {
        return bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                singleton(type),
                singleton(level), campaign.getUid());
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }

    private void checkSingleBidModifierByType(SoftAssertions soft, BidModifierType type, List<BidModifier> modifiers) {
        soft.assertThat(modifiers).is(matchedBy(
                allOf(hasSize(1),
                        contains(hasProperty("type", equalTo(type)))
                )
        ));
    }

    private void checkSingleAdjustmentByCLass(SoftAssertions soft,
                                              Class<?> type,
                                              List<AbstractBidModifierRetargetingAdjustment> adjustments) {
        soft.assertThat(adjustments).is(matchedBy(allOf(
                hasSize(1),
                contains(instanceOf(type))
        )));
    }
}
