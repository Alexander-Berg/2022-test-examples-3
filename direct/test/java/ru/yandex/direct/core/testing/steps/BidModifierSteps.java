package ru.yandex.direct.core.testing.steps;

import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignBidModifierInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createAnotherBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createBidModifierDemographicsWithTwoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createBidModifierDemographicsWithUnsupportedAgeType;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBannerType;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierAbSegment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDesktopOnly;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierGeo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierPerformanceTgo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierRetargeting;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierRetargetingFilter;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierSmartTV;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierVideo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierWeather;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultIncomeGradeModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultIosBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultTrafaretPositionModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultTrafficModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createInventoryAdjustment;

public class BidModifierSteps {

    @Autowired
    private BidModifierRepository bidModifierRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private CampaignSteps campaignSteps;


    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierWeather(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierWeather(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierTrafaretPosition(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultTrafaretPositionModifier(), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierIncomeGrade(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultIncomeGradeModifier(), adGroupInfo);
    }

    /**
     * Неподдерживаемая корректировка, но есть в данных - нужна, чтобы проверять в тестах, что мы ее пропускаем
     */
    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierTraffic(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultTrafficModifier(), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierDemographics(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierDemographics(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createAdGroupBidModifierDemographicsWithMulipleAdjustments(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createBidModifierDemographicsWithTwoAdjustments(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createAdGroupBidModifierDemographicsWithUnsupportedAgeType(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createBidModifierDemographicsWithUnsupportedAgeType(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierRetargeting(
            AdGroupInfo adGroupInfo, List<Long> retCondIds) {
        return createAdGroupBidModifier(createDefaultBidModifierRetargeting(
                null, null, retCondIds), adGroupInfo);
    }

    public CampaignBidModifierInfo createDefaultCampaignBidModifierDemographics(CampaignInfo campaignInfo) {
        return createCampaignBidModifier(createDefaultBidModifierDemographics(null), campaignInfo);
    }

    public CampaignBidModifierInfo createAnotherCampaignBidModifierDemographics(CampaignInfo campaignInfo) {
        return createCampaignBidModifier(createAnotherBidModifierDemographics(null), campaignInfo);
    }

    public CampaignBidModifierInfo createDefaultCampaignBidModifierRetargeting(
            CampaignInfo adGroupInfo, List<Long> retCondIds) {
        return createCampaignBidModifier(createDefaultBidModifierRetargeting(
                null, null, retCondIds), adGroupInfo);
    }

    public AdGroupBidModifierInfo createAdGroupBidModifierRetargetingFilterWithRetCondIds(AdGroupInfo adGroupInfo,
                                                                                          List<Long> retCondId) {
        return createAdGroupBidModifier(
                createDefaultBidModifierRetargetingFilter(
                        adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), retCondId),
                adGroupInfo
        );
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierMobile(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierMobile(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupIosBidModifierMobile(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultIosBidModifierMobile(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierDesktop(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierDesktop(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierPerformanceTgo(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierPerformanceTgo(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierSmartTV(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierSmartTV(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierVideo(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierVideo(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierDesktopOnly(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierDesktopOnly(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultAdGroupBidModifierGeo(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierGeo(null), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultABSegmentBidModifier(AdGroupInfo adGroupInfo,
                                                                    RetConditionInfo retConditionInfo) {
        return createAdGroupBidModifier(createDefaultBidModifierAbSegment(null, adGroupInfo.getAdGroupId(),
                retConditionInfo), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultBannerTypeBidModifier(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createDefaultBannerType(null, adGroupInfo.getAdGroupId()), adGroupInfo);
    }

    public AdGroupBidModifierInfo createDefaultInventoryBidModifier(AdGroupInfo adGroupInfo) {
        return createAdGroupBidModifier(createEmptyInventoryModifier().withInventoryAdjustments(
                List.of(createInventoryAdjustment(InventoryType.INAPP),
                        createInventoryAdjustment(InventoryType.INBANNER))
        ), adGroupInfo);
    }

    public CampaignBidModifierInfo createDefaultCampBidModifierGeo(CampaignInfo campaignInfo) {
        return createCampaignBidModifier(createDefaultBidModifierGeo(null), campaignInfo);
    }

    public AdGroupBidModifierInfo createAdGroupBidModifier(BidModifier bidModifier, AdGroupInfo adGroupInfo) {
        AdGroupBidModifierInfo bidModifierInfo = new AdGroupBidModifierInfo()
                .withAdGroupInfo(adGroupInfo)
                .withBidModifier(bidModifier);
        return createAdGroupBidModifier(bidModifierInfo);
    }

    private AdGroupBidModifierInfo createAdGroupBidModifier(AdGroupBidModifierInfo bidModifierInfo) {
        if (bidModifierInfo.getBidModifier() == null) {
            bidModifierInfo.withBidModifier(createDefaultBidModifierMobile(null));
        }
        if (bidModifierInfo.getBidModifierId() == null) {
            adGroupSteps.createAdGroup(bidModifierInfo.getAdGroupInfo());
            bidModifierInfo.getBidModifier()
                    .withCampaignId(bidModifierInfo.getCampaignId())
                    .withAdGroupId(bidModifierInfo.getAdGroupId())
                    .withLastChange(now());
            DSLContext dslContext = dslContextProvider.ppc(bidModifierInfo.getShard());
            bidModifierRepository.addModifiers(dslContext, singletonList(bidModifierInfo.getBidModifier()),
                    emptyMap(), bidModifierInfo.getClientId(), bidModifierInfo.getUid());
        }
        return bidModifierInfo;
    }

    public CampaignBidModifierInfo createCampaignBidModifier(BidModifier bidModifier, CampaignInfo campaignInfo) {
        CampaignBidModifierInfo bidModifierInfo = new CampaignBidModifierInfo()
                .withCampaignInfo(campaignInfo)
                .withBidModifiers(singletonList(bidModifier));
        return createCampaignBidModifier(bidModifierInfo);
    }

    private CampaignBidModifierInfo createCampaignBidModifier(CampaignBidModifierInfo bidModifierInfo) {
        if (bidModifierInfo.getBidModifiers() == null || bidModifierInfo.getBidModifiers().isEmpty()) {
            bidModifierInfo.withBidModifiers(singletonList(createDefaultBidModifierMobile(null)));
        }
        CampaignInfo campaignInfo = campaignSteps.createCampaign(bidModifierInfo.getCampaignInfo());
        bidModifierInfo.getBidModifiers().forEach(bm -> {
            if (bm.getId() == null || bm.getId() == 0L) {
                bm.withCampaignId(campaignInfo.getCampaignId());
            }
        });
        DSLContext dslContext = dslContextProvider.ppc(bidModifierInfo.getShard());
        bidModifierRepository.addModifiers(dslContext, bidModifierInfo.getBidModifiers(),
                emptyMap(), bidModifierInfo.getClientId(), bidModifierInfo.getUid());
        return bidModifierInfo;
    }

    public CampaignBidModifierInfo getCampaignBidModifiers(int shard, long campaignId, Set<BidModifierType> types) {
        List<BidModifier> bidModifiers = bidModifierRepository
                .getByCampaignIds(shard, singletonList(campaignId), types, singleton(BidModifierLevel.CAMPAIGN));
        CampaignBidModifierInfo campaignBidModifierInfo = new CampaignBidModifierInfo();
        campaignBidModifierInfo.withBidModifiers(bidModifiers);
        return campaignBidModifierInfo;
    }

    public AdGroupBidModifierInfo getAdGroupBidModifiers(int shard, long campaignId, long adGroupId,
                                                         Set<BidModifierType> types) {
        List<BidModifier> bidModifiers = bidModifierRepository
                .getByAdGroupIds(shard, singletonMap(adGroupId, campaignId), types,
                        singleton(BidModifierLevel.ADGROUP));
        AdGroupBidModifierInfo adGroupBidModifierInfo = new AdGroupBidModifierInfo();
        adGroupBidModifierInfo.withBidModifiers(bidModifiers);
        return adGroupBidModifierInfo;
    }
}
