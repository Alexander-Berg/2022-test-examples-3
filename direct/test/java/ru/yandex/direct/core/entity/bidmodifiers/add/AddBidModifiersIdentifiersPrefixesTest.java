package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.DEMOGRAPHY_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.GEO_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.MOBILE_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.RETARGETING_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.TYPE_PREFIXES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка правильности формата идентификаторов, предназначенных для ответа потребителям")
public class AddBidModifiersIdentifiersPrefixesTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    private CampaignInfo campaign;
    private long retCondId;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveTextCampaign();
        retCondId = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo()).getRetConditionId();
    }

    @Test
    @Description("Идентификаторы корректировок ставок должны начинаться с правильных префиксов")
    public void externalIdentifierTest() {
        MassResult<List<Long>> result = bidModifierService.add(
                Lists.newArrayList(
                        createEmptyClientMobileModifier().withCampaignId(campaign.getCampaignId())
                                .withEnabled(true)
                                .withMobileAdjustment(createDefaultClientMobileAdjustment()),
                        createEmptyClientGeoModifier().withCampaignId(campaign.getCampaignId())
                                .withEnabled(true)
                                .withRegionalAdjustments(createDefaultClientGeoAdjustments()),
                        createEmptyClientRetargetingModifier().withCampaignId(campaign.getCampaignId())
                                .withEnabled(true)
                                .withRetargetingAdjustments(createDefaultClientRetargetingAdjustments(retCondId)),
                        createEmptyClientDemographicsModifier().withCampaignId(campaign.getCampaignId())
                                .withEnabled(true)
                                .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments())
                ), campaign.getClientId(), campaign.getUid());
        assertSoftly(softly -> {
            softly.assertThat(Long.toString(result.getResult().get(0).getResult().get(0)))
                    .startsWith(TYPE_PREFIXES.get(MOBILE_MULTIPLIER));
            softly.assertThat(Long.toString(result.getResult().get(1).getResult().get(0)))
                    .startsWith(TYPE_PREFIXES.get(GEO_MULTIPLIER));
            softly.assertThat(Long.toString(result.getResult().get(2).getResult().get(0)))
                    .startsWith(TYPE_PREFIXES.get(RETARGETING_MULTIPLIER));
            softly.assertThat(Long.toString(result.getResult().get(3).getResult().get(0)))
                    .startsWith(TYPE_PREFIXES.get(DEMOGRAPHY_MULTIPLIER));
        });
    }
}
