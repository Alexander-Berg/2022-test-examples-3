package ru.yandex.direct.core.entity.bidmodifiers.add.performance;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultPerformanceTgoAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientPerformanceTgoModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Позитивные сценарии добавления корректировок ставок Смарт ТГО")
public class AddBidModifiersPerformanceTgoTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaign;

    @Before
    public void before() {
        campaign = campaignSteps.createActivePerformanceCampaign();
    }

    @Test
    @Description("Добавляем одну корректировку и проверяем, что она после этого получается методом get")
    public void addOnePerformanceTgoModifierTest() {
        BidModifierPerformanceTgoAdjustment performanceTgoAdjustment = createDefaultPerformanceTgoAdjustment();
        MassResult<List<Long>> result = addModifier(performanceTgoAdjustment);
        List<BidModifier> gotModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        ALL_TYPES,
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        Long id = gotModifiers.get(0).getId();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(result.getResult().get(0).getResult()).is(matchedBy(contains(
                    equalTo(getExternalId(id, BidModifierType.PERFORMANCE_TGO_MULTIPLIER)))));
        });
    }

    private MassResult<List<Long>> addModifier(BidModifierPerformanceTgoAdjustment adjustment) {
        return bidModifierService.add(
                singletonList(
                        createEmptyClientPerformanceTgoModifier()
                                .withEnabled(true)
                                .withCampaignId(campaign.getCampaignId())
                                .withPerformanceTgoAdjustment(adjustment)
                ), campaign.getClientId(), campaign.getUid()
        );
    }

    @Test
    @Description("Добавляем корректировку Смарт-ТГО и проверяем записи, созданные в БД")
    public void addPerformanceTgoModifierAndCheckDbState() {
        BidModifierPerformanceTgoAdjustment adjustment = createDefaultPerformanceTgoAdjustment();
        MassResult<List<Long>> result = addModifier(adjustment);
        List<BidModifier> bidModifiersInDb =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        ALL_TYPES,
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        Long addedId = result.getResult().get(0).getResult().get(0);
        //
        assertThat(bidModifiersInDb, allOf(
                hasSize(1),
                contains(
                        allOf(
                                hasProperty("id", equalTo(BidModifierService.getRealId(addedId))),
                                hasProperty("campaignId", equalTo(campaign.getCampaignId())),
                                hasProperty("adGroupId", nullValue()),
                                hasProperty("type", equalTo(BidModifierType.PERFORMANCE_TGO_MULTIPLIER)),
                                hasProperty("performanceTgoAdjustment",
                                        hasProperty("percent", equalTo(adjustment.getPercent()))),
                                hasProperty("enabled", equalTo(true))
                        )
                )));
    }
}
