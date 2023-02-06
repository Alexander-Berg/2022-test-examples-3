package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddBidModifiersAdjustmentMultiTypesTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    private ClientId clientId;
    private long clientUid;
    private long campaignId;
    private long retCondId;

    @Before
    public void before() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign();
        clientId = campaignInfo.getClientId();
        clientUid = campaignInfo.getUid();
        campaignId = campaignInfo.getCampaignId();

        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());
        retCondId = retCondition.getRetConditionId();
    }

    @Test
    @Description("Добавляем в одном запросе четыре типа корректировок ставок - демографическую, ретаргетинговую, региональную и мобильную")
    public void addSeveralBidModifiersTest() {
        MassResult<List<Long>> result = addBidModifiers(
                newArrayList(
                        createEmptyClientMobileModifier().withCampaignId(campaignId)
                                .withMobileAdjustment(createDefaultClientMobileAdjustment()),
                        createEmptyClientGeoModifier().withCampaignId(campaignId)
                                .withRegionalAdjustments(createDefaultClientGeoAdjustments()),
                        createEmptyClientRetargetingModifier().withCampaignId(campaignId)
                                .withRetargetingAdjustments(createDefaultClientRetargetingAdjustments(retCondId)),
                        createEmptyClientDemographicsModifier().withCampaignId(campaignId)
                                .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments())));
        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(4);
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        });
    }

    @Test
    public void addDemographicThenDemographicAndMobileTest() {
        MassResult<List<Long>> firstReqResult = addBidModifiers(
                newArrayList(
                        createEmptyClientDemographicsModifier()
                                .withCampaignId(campaignId)
                                .withDemographicsAdjustments(createDefaultClientDemographicsAdjustments())));

        assertSoftly(softly -> {
            softly.assertThat(firstReqResult.getResult()).hasSize(1);
            softly.assertThat(firstReqResult.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        });
        MassResult<List<Long>> secondReqResult = addBidModifiers(
                newArrayList(
                        createEmptyClientDemographicsModifier().withCampaignId(campaignId)
                                .withDemographicsAdjustments(
                                        singletonList(createDefaultClientDemographicsAdjustment().withAge(AgeType._55_))),
                        createEmptyClientMobileModifier().withCampaignId(campaignId)
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())));
        assertSoftly(softly -> {
            softly.assertThat(secondReqResult.getResult()).hasSize(2);
            softly.assertThat(secondReqResult.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }
}
