package ru.yandex.direct.core.entity.bidmodifiers.add.demographics;

import java.util.List;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Позитивные сценарии добавления демографических корректировок ставок")
public class AddBidModifiersDemographicsTest {
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
    @Description("Добавляем одну корректировку и проверяем, что она после этого получается методом get")
    public void addOneDemographicModifierTest() {
        List<BidModifierDemographicsAdjustment> demographicsAdjustments = createDefaultClientDemographicsAdjustments();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(demographicsAdjustments)));
        List<BidModifier> gotModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        Long adjustmentId = ((BidModifierDemographics) gotModifiers.get(0)).getDemographicsAdjustments().get(0).getId();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(result.getResult().get(0).getResult()).is(matchedBy(contains(
                    equalTo(getExternalId(adjustmentId, BidModifierType.DEMOGRAPHY_MULTIPLIER)))));
            softly.assertThat(gotModifiers.get(0)).is(matchedBy(
                    demographicModifierWithProperties(
                            campaign.getCampaignId(),
                            demographicsAdjustments.get(0).getPercent(),
                            demographicsAdjustments.get(0).getGender(),
                            demographicsAdjustments.get(0).getAge(),
                            true
                    )));
        });
    }

    private Matcher<BidModifier> demographicModifierWithProperties(
            long campaignId, int percent, GenderType gender, AgeType age, boolean enabled) {
        return allOf(
                hasProperty("campaignId", equalTo(campaignId)),
                hasProperty("adGroupId", nullValue()),
                hasProperty("enabled", equalTo(enabled)),
                hasProperty("demographicsAdjustments", contains(
                        allOf(
                                hasProperty("percent", equalTo(percent)),
                                hasProperty("gender", equalTo(gender)),
                                hasProperty("age", equalTo(age))
                        )
                ))
        );
    }

    @Test
    @Description("Добавляем две корректировки и проверяем, как они разложились в БД")
    public void addTwoDemographicsModifiersDbStateTest() {
        BidModifierDemographicsAdjustment adjustment1 = new BidModifierDemographicsAdjustment()
                .withAge(AgeType._25_34).withGender(GenderType.FEMALE).withPercent(110);
        BidModifierDemographicsAdjustment adjustment2 = new BidModifierDemographicsAdjustment()
                .withAge(AgeType._18_24).withGender(GenderType.FEMALE).withPercent(120);
        List<BidModifierDemographicsAdjustment> adjustments = Lists.newArrayList(adjustment1, adjustment2);
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(adjustments)));

        List<BidModifier> bidModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN),
                        campaign.getUid());
        List<BidModifierDemographicsAdjustment> adjustmentsSaved =
                ((BidModifierDemographics) bidModifiers.get(0)).getDemographicsAdjustments();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(adjustmentsSaved).is(matchedBy(allOf(hasSize(2),
                    containsInAnyOrder(
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(0)))),
                                    hasProperty("percent", equalTo(adjustment1.getPercent())),
                                    hasProperty("age", equalTo(adjustment1.getAge())),
                                    hasProperty("gender", equalTo(adjustment1.getGender()))
                            ),
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(1)))),
                                    hasProperty("percent", equalTo(adjustment2.getPercent())),
                                    hasProperty("age", equalTo(adjustment2.getAge())),
                                    hasProperty("gender", equalTo(adjustment2.getGender()))
                            )
                    ))));
        });
    }

    @Test
    @Description("Добавляем корректировку 45+ и проверяем, как она разложилась в БД")
    public void addDemographicsModifier_45_DbStateTest() {
        BidModifierDemographicsAdjustment adjustment = new BidModifierDemographicsAdjustment()
                .withAge(AgeType._45_).withGender(GenderType.FEMALE).withPercent(110);
        List<BidModifierDemographicsAdjustment> adjustments = singletonList(adjustment);
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withDemographicsAdjustments(adjustments)));

        List<BidModifier> bidModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN),
                        campaign.getUid());
        List<BidModifierDemographicsAdjustment> adjustmentsSaved =
                ((BidModifierDemographics) bidModifiers.get(0)).getDemographicsAdjustments();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(adjustmentsSaved).is(matchedBy(allOf(hasSize(1),
                    contains(
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(0)))),
                                    hasProperty("percent", equalTo(adjustment.getPercent())),
                                    hasProperty("age", equalTo(adjustment.getAge())),
                                    hasProperty("gender", equalTo(adjustment.getGender()))
                            )
                    ))));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}
