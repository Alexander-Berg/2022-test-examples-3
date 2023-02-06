package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.bidmodifier;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.ComplexAdGroupUpdateOperationTestBase;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientBidModifierDesktop;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientBidModifierSmartTV;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientBidModifierVideo;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;

public class ComplexUpdateBidModifierTestBase extends ComplexAdGroupUpdateOperationTestBase {

    private static final ImmutableSet<BidModifierType> ALL_TYPES = ImmutableSet.copyOf(BidModifierType.values());

    @Autowired
    private BidModifierRepository bidModifierRepository;

    protected int getClientBidModifiersCount() {
        return bidModifierRepository.getByCampaignIds(shard, singleton(campaignId), ALL_TYPES,
                ImmutableSet.of(BidModifierLevel.ADGROUP, BidModifierLevel.CAMPAIGN)).size();
    }

    protected List<BidModifier> findBidModifiersInAdGroup(AdGroupInfo adGroupInfo) {
        return findBidModifiersInAdGroups(Collections.singletonList(adGroupInfo));
    }

    protected List<BidModifier> findBidModifiersInAdGroups(Collection<AdGroupInfo> adGroupInfos) {
        Map<Long, Long> campaignIdsByAdGroupIds = StreamEx.of(adGroupInfos)
                .mapToEntry(AdGroupInfo::getAdGroupId, AdGroupInfo::getCampaignId)
                .toMap();
        return bidModifierRepository.getByAdGroupIds(shard, campaignIdsByAdGroupIds, ALL_TYPES,
                ImmutableSet.of(BidModifierLevel.ADGROUP));
    }

    protected AdGroupBidModifierInfo createBidModifierMobile(AdGroupInfo adGroupInfo) {
        BidModifierMobile bidModifier = randomBidModifierMobile();
        fillMobileBidModifierDefaultFields(bidModifier);
        return steps.bidModifierSteps().createAdGroupBidModifier(bidModifier, adGroupInfo);
    }

    protected AdGroupBidModifierInfo createBidModifierDemographics(AdGroupInfo adGroupInfo) {
        BidModifierDemographics bidModifier = randomBidModifierDemographics();
        fillDemographicsBidModifierDefaultFields(bidModifier);
        return steps.bidModifierSteps().createAdGroupBidModifier(bidModifier, adGroupInfo);
    }

    private void fillDemographicsBidModifierDefaultFields(BidModifierDemographics bidModifierDemographics) {
        LocalDateTime now = LocalDateTime.now();
        bidModifierDemographics.withLastChange(now);
        bidModifierDemographics.getDemographicsAdjustments().forEach(adj -> adj.setLastChange(now));
    }

    private void fillMobileBidModifierDefaultFields(BidModifierMobile bidModifierMobile) {
        LocalDateTime now = LocalDateTime.now();
        bidModifierMobile.withLastChange(now);
        bidModifierMobile.getMobileAdjustment().setLastChange(now);
    }

    protected BidModifierMobile randomBidModifierMobile() {
        return createDefaultBidModifierMobile(null)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withMobileAdjustment(createDefaultMobileAdjustment()
                        .withPercent(nextInt(50, 1300)));
    }

    protected BidModifierDesktop randomBidModifierDesktop() {
        return createDefaultClientBidModifierDesktop(null)
                .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                        .withPercent(nextInt(50, 1300)));
    }

    protected BidModifierSmartTV randomBidModifierSmartTV() {
        return createDefaultClientBidModifierSmartTV(null)
                .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                        .withPercent(nextInt(50, 1300)));
    }

    protected BidModifierVideo randomBidModifierVideo() {
        return createDefaultClientBidModifierVideo(null)
                .withType(BidModifierType.VIDEO_MULTIPLIER)
                .withVideoAdjustment(new BidModifierVideoAdjustment()
                        .withPercent(nextInt(50, 500)));
    }

    protected BidModifierDemographics randomBidModifierDemographics() {
        BidModifierDemographicsAdjustment adjustment = createDefaultDemographicsAdjustment()
                .withAge(AgeType._18_24)
                .withGender(GenderType.MALE)
                .withPercent(nextInt(1, 1300));
        return createDefaultBidModifierDemographics(null)
                .withType(BidModifierType.DEMOGRAPHY_MULTIPLIER)
                .withDemographicsAdjustments(singletonList(adjustment));
    }

    protected ComplexBidModifier randomComplexBidModifierMobile() {
        return new ComplexBidModifier()
                .withMobileModifier(randomBidModifierMobile());
    }

    protected ComplexBidModifier randomComplexBidModifierDesktop() {
        return new ComplexBidModifier()
                .withDesktopModifier(randomBidModifierDesktop());
    }

    protected ComplexBidModifier randomComplexBidModifierVideo() {
        return new ComplexBidModifier()
                .withVideoModifier(randomBidModifierVideo());
    }

    protected ComplexBidModifier randomComplexBidModifierDemographics() {
        return new ComplexBidModifier()
                .withDemographyModifier(randomBidModifierDemographics());
    }

    protected ComplexBidModifier randomComplexBidModifierMobileAndDemographics() {
        return new ComplexBidModifier()
                .withMobileModifier(randomBidModifierMobile())
                .withDemographyModifier(randomBidModifierDemographics());
    }
}
