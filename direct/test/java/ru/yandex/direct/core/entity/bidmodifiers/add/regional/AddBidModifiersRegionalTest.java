package ru.yandex.direct.core.entity.bidmodifiers.add.regional;

import java.util.List;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Позитивные сценарии добавления корректировок ставок на регион")
public class AddBidModifiersRegionalTest {
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
    public void addOneGeoModifierTest() {
        List<BidModifierRegionalAdjustment> geoAdjustment = createDefaultClientGeoAdjustments();
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRegionalAdjustments(geoAdjustment)));
        List<BidModifier> gotModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.GEO_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        List<BidModifierRegionalAdjustment> gotAdjustments =
                ((BidModifierGeo) gotModifiers.get(0)).getRegionalAdjustments();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(result.getResult().get(0).getResult()).is(matchedBy(containsInAnyOrder(
                    gotAdjustments.stream().filter(it -> !it.getHidden()).map(adjustment ->
                            equalTo(getExternalId(adjustment.getId(), BidModifierType.GEO_MULTIPLIER)))
                            .collect(toList()))));
            softly.assertThat(gotAdjustments.get(0)).is(matchedBy(
                    geoAdjustmentWithProperties(
                            geoAdjustment.get(0).getPercent(),
                            geoAdjustment.get(0).getRegionId())));
        });
    }

    private Matcher<BidModifierRegionalAdjustment> geoAdjustmentWithProperties(int percent, long regionId) {
        return allOf(
                hasProperty("percent", equalTo(percent)),
                hasProperty("regionId", equalTo(regionId))
        );
    }

    @Test
    @Description("Добавляем две корректировки, проверяем то, что добавилось")
    public void addTwoGeoModifiersTest() {
        List<BidModifierRegionalAdjustment> geoAdjustments = Lists.newArrayList(
                new BidModifierRegionalAdjustment()
                        .withRegionId(Region.RUSSIA_REGION_ID).withHidden(false).withPercent(110),
                new BidModifierRegionalAdjustment()
                        .withRegionId(Region.MOSCOW_REGION_ID).withHidden(false).withPercent(120)
        );
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRegionalAdjustments(geoAdjustments)));
        List<BidModifier> gotModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.GEO_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        List<BidModifierRegionalAdjustment> gotAdjustments =
                ((BidModifierGeo) gotModifiers.get(0)).getRegionalAdjustments();
        //
        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getResult()).is(matchedBy(containsInAnyOrder(
                    gotAdjustments.stream().filter(it -> !it.getHidden()).map(adjustment ->
                            equalTo(getExternalId(adjustment.getId(), BidModifierType.GEO_MULTIPLIER)))
                            .collect(toList()))));
            softly.assertThat(filterList(gotAdjustments, it -> !it.getHidden())).is(matchedBy(containsInAnyOrder(
                    geoAdjustmentWithProperties(110, Region.RUSSIA_REGION_ID),
                    geoAdjustmentWithProperties(120, Region.MOSCOW_REGION_ID))));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}
