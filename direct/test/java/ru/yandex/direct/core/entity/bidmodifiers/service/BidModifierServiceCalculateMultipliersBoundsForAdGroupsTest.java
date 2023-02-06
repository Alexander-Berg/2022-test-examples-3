package ru.yandex.direct.core.entity.bidmodifiers.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.container.MultipliersBounds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BidModifierSteps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierGeo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultGeoAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultGeoAdjustments;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BidModifierServiceCalculateMultipliersBoundsForAdGroupsTest {
    @Autowired
    public BidModifierService bidModifierService;

    @Autowired
    public BidModifierSteps bidModifierSteps;

    @Autowired
    public AdGroupSteps adGroupSteps;

    private AdGroupInfo defaultAdGroup;
    private ClientId clientId;
    private Long operatorUid;

    @Before
    public void before() {
        defaultAdGroup = adGroupSteps.createDefaultAdGroup();
        operatorUid = defaultAdGroup.getUid();
        clientId = defaultAdGroup.getClientId();
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_NoneMultipliers() {
        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()))
                .isNull();
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_OnlyGroupMultiplier() {
        bidModifierSteps.createDefaultAdGroupBidModifierGeo(defaultAdGroup);
        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getLower())
                .isEqualTo(BidModifierService.ONE_HUNDRED);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getUpper())
                .isEqualTo(TestBidModifiers.DEFAULT_PERCENT);
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_GroupAndCampMultiplierOfSameType() {
        int percent = 121;
        bidModifierSteps.createDefaultAdGroupBidModifierGeo(defaultAdGroup);

        List<BidModifierRegionalAdjustment> campaignGeoAdjustments = createDefaultGeoAdjustments();
        campaignGeoAdjustments.get(0).setPercent(percent);
        bidModifierSteps.createCampaignBidModifier(
                createDefaultBidModifierGeo(null).withRegionalAdjustments(campaignGeoAdjustments),
                defaultAdGroup.getCampaignInfo());

        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getLower())
                .isEqualTo(BidModifierService.ONE_HUNDRED);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getUpper())
                .isEqualTo(TestBidModifiers.DEFAULT_PERCENT);
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_TwoIncreasingGeoMultiplier() {
        List<BidModifierRegionalAdjustment> twoGeoAdjustments = new ArrayList<>();
        twoGeoAdjustments.add(createDefaultGeoAdjustment());
        int twoHundred = 200;
        twoGeoAdjustments
                .add(createDefaultGeoAdjustment().withPercent(twoHundred)
                        .withRegionId(UKRAINE_REGION_ID)
                        .withHidden(false));
        bidModifierSteps.createAdGroupBidModifier(
                createDefaultBidModifierGeo(null).withRegionalAdjustments(twoGeoAdjustments), defaultAdGroup);

        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getLower())
                .isEqualTo(BidModifierService.ONE_HUNDRED);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getUpper())
                .isEqualTo(twoHundred);
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_TwoGeoMultiplierOneDecreasing() {
        int percent = 50;
        List<BidModifierRegionalAdjustment> twoGeoAdjustments = new ArrayList<>();
        twoGeoAdjustments.add(createDefaultGeoAdjustment());
        twoGeoAdjustments
                .add(createDefaultGeoAdjustment().withPercent(percent)
                        .withRegionId(UKRAINE_REGION_ID)
                        .withHidden(false));
        bidModifierSteps.createAdGroupBidModifier(
                createDefaultBidModifierGeo(null).withRegionalAdjustments(twoGeoAdjustments), defaultAdGroup);

        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getLower())
                .isEqualTo(percent);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getUpper())
                .isEqualTo(TestBidModifiers.DEFAULT_PERCENT);
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_OneGeoAndOneDemographicForAdGroupId() {
        bidModifierSteps.createDefaultAdGroupBidModifierGeo(defaultAdGroup);
        bidModifierSteps.createDefaultAdGroupBidModifierDemographics(defaultAdGroup);
        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getLower())
                .isEqualTo(BidModifierService.ONE_HUNDRED);

        int expected = 121;
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getUpper())
                .isEqualTo(expected);
    }

    @Test
    public void calculateMultipliersBoundsForAdGroups_EveryIsLoverThanOneHundred() {
        int percent = 50;
        List<BidModifierRegionalAdjustment> geoAdjustments = new ArrayList<>();
        geoAdjustments
                .add(createDefaultGeoAdjustment().withPercent(percent)
                        .withRegionId(187L)
                        .withHidden(false));
        bidModifierSteps.createAdGroupBidModifier(
                createDefaultBidModifierGeo(null).withRegionalAdjustments(geoAdjustments), defaultAdGroup);

        Map<Long, Long> groupIdToCampaignId = new HashMap<>();
        groupIdToCampaignId.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
        Map<Long, MultipliersBounds> longMultipliersBoundsMap =
                bidModifierService.calculateMultipliersBoundsForAdGroups(clientId, operatorUid, groupIdToCampaignId);
        assertThat(longMultipliersBoundsMap.get(defaultAdGroup.getAdGroupId()).getUpper())
                .isEqualTo(percent);
    }
}
