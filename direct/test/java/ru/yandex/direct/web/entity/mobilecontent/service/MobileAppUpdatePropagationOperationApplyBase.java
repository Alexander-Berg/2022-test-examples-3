package ru.yandex.direct.web.entity.mobilecontent.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.container.AdsSelectionCriteria;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.web.core.entity.mobilecontent.model.DisplayedAttribute;
import ru.yandex.direct.web.entity.mobilecontent.model.PropagationMode;
import ru.yandex.direct.web.entity.mobilecontent.model.PropagationRequest;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileAppBanner;

class MobileAppUpdatePropagationOperationApplyBase {
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";
    static final String TRACKING_URL_ON_APP = "http://app.adjust.com/1111";
    static final String IMPRESSION_URL_ON_APP = "http://view.adjust.com/impression/1111";
    private static final String TRACKING_URL_ON_BANNER2 = "http://app.adjust.com/1111?aaa=111";
    private static final String IMPRESSION_URL_ON_BANNER2 = "http://view.adjust.com/impression/1111?aaa=111";
    static final String TRACKING_URL_ON_BANNER3 = "http://app.adjust.com/2222";
    static final String IMPRESSION_URL_ON_BANNER3 = "http://view.adjust.com/impression/2222";
    static final String TRACKING_URL_ON_REQ = "http://app.adjust.com/newnewnew";
    static final String IMPRESSION_URL_ON_REQ = "http://view.adjust.com/impression/newnewnew";

    @SuppressWarnings("UnstableApiUsage")
    static final Map<NewReflectedAttribute, Boolean> REFLECTED_ATTRIBUTE1 =
            StreamEx.of(EnumSet.allOf(NewReflectedAttribute.class))
                    .mapToEntry(v -> false)
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    private static final Map<NewReflectedAttribute, Boolean> REFLECTED_ATTRIBUTE2 = ImmutableMap.of(
            NewReflectedAttribute.RATING, true,
            NewReflectedAttribute.PRICE, true,
            NewReflectedAttribute.ICON, false,
            NewReflectedAttribute.RATING_VOTES, false);

    @SuppressWarnings("UnstableApiUsage")
    static final Map<NewReflectedAttribute, Boolean> REFLECTED_ATTRIBUTE3 =
            StreamEx.of(EnumSet.allOf(NewReflectedAttribute.class))
                    .mapToEntry(v -> true)
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    static final Map<NewReflectedAttribute, Boolean> REFLECTED_ATTRIBUTE4 = REFLECTED_ATTRIBUTE2;

    private static final Set<DisplayedAttribute> ATTRIBUTES_IN_REQ = ImmutableSet.of(
            DisplayedAttribute.RATING, DisplayedAttribute.ICON);

    static final Map<NewReflectedAttribute, Boolean> NEW_ATTRIBUTES = ImmutableMap.of(
            NewReflectedAttribute.RATING, true,
            NewReflectedAttribute.PRICE, false,
            NewReflectedAttribute.ICON, true,
            NewReflectedAttribute.RATING_VOTES, false
    );

    @Autowired
    private TrustedRedirectsService trustedRedirectsService;
    @Autowired
    private Steps steps;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private MobileAppUpdatePropagationOperationFactory mobileAppUpdatePropagationOperationFactory;

    MobileAppBanner actualBanner1;
    MobileAppBanner actualBanner2;
    MobileAppBanner actualBanner3;
    MobileAppBanner actualBanner4;

    void init(PropagationMode propagationMode) {
        trustedRedirectsService.invalidateCache();
        steps.trustedRedirectSteps().addValidCounters();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        MobileAppInfo mobileAppInfo1 =
                steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL, TRACKING_URL_ON_APP, IMPRESSION_URL_ON_APP);
        CampaignInfo campaignInfo1 = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo, mobileAppInfo1);
        AdGroupInfo groupInfo1 = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo1);

        // ещё одна кампания, привязанная к первому приложению, но без баннеров
        steps.campaignSteps().createActiveMobileAppCampaign(clientInfo, mobileAppInfo1);

        var bannerInfo1 = steps.mobileAppBannerSteps().createMobileAppBanner(
                new NewMobileAppBannerInfo()
                        .withAdGroupInfo(groupInfo1)
                        .withBanner(fullMobileAppBanner(groupInfo1.getCampaignId(), groupInfo1.getAdGroupId())
                                .withHref(null)
                                .withImpressionUrl(null)
                                .withReflectedAttributes(REFLECTED_ATTRIBUTE1)));

        var bannerInfo2 = steps.mobileAppBannerSteps().createMobileAppBanner(
                new NewMobileAppBannerInfo()
                        .withAdGroupInfo(groupInfo1)
                        .withBanner(fullMobileAppBanner(groupInfo1.getCampaignId(), groupInfo1.getAdGroupId())
                                .withHref(TRACKING_URL_ON_BANNER2)
                                .withImpressionUrl(IMPRESSION_URL_ON_BANNER2)
                                .withReflectedAttributes(REFLECTED_ATTRIBUTE2)));

        var bannerInfo3 = steps.mobileAppBannerSteps().createMobileAppBanner(
                new NewMobileAppBannerInfo()
                        .withAdGroupInfo(groupInfo1)
                        .withBanner(fullMobileAppBanner(groupInfo1.getCampaignId(), groupInfo1.getAdGroupId())
                                .withHref(TRACKING_URL_ON_BANNER3)
                                .withImpressionUrl(IMPRESSION_URL_ON_BANNER3)
                                .withReflectedAttributes(REFLECTED_ATTRIBUTE3)));

        MobileAppInfo mobileAppInfo2 =
                steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL, TRACKING_URL_ON_APP, IMPRESSION_URL_ON_APP);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo, mobileAppInfo2);
        AdGroupInfo groupInfo2 = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo2);
        var bannerInfo4 = steps.mobileAppBannerSteps().createMobileAppBanner(
                new NewMobileAppBannerInfo()
                        .withAdGroupInfo(groupInfo2)
                        .withBanner(fullMobileAppBanner(groupInfo1.getCampaignId(), groupInfo1.getAdGroupId())
                                .withHref(TRACKING_URL_ON_APP)
                                .withImpressionUrl(IMPRESSION_URL_ON_APP)
                                .withReflectedAttributes(REFLECTED_ATTRIBUTE4)));

        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo1.getMobileAppId())
                .withDisplayedAttributes(ATTRIBUTES_IN_REQ)
                .withPropagationMode(propagationMode)
                .withTrackingUrl(TRACKING_URL_ON_REQ)
                .withTrackingImpressionUrl(IMPRESSION_URL_ON_REQ)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request);
        assertThat("Операция должна создаться", propagationOperation.hasOperation(), equalTo(true));
        MassResult<?> massResult = propagationOperation.getOperation().prepareAndApply();
        assertThat("Операция должна завершиться успешно", massResult.isSuccessful(), equalTo(true));

        var banners = bannerService.getBannersBySelectionCriteria(
                clientInfo.getUid(), clientInfo.getClientId(),
                new AdsSelectionCriteria()
                        .withAdIds(bannerInfo1.getBannerId(), bannerInfo2.getBannerId(),
                                bannerInfo3.getBannerId(), bannerInfo4.getBannerId()),
                LimitOffset.maxLimited());
        assertThat("Получены все баннеры", banners, hasSize(4));
        Map<Long, MobileAppBanner> bannerMap = StreamEx.of(banners)
                .mapToEntry(Banner::getId)
                .invert()
                .selectValues(MobileAppBanner.class)
                .toMap();
        actualBanner1 = checkNotNull(bannerMap.get(bannerInfo1.getBannerId()));
        actualBanner2 = checkNotNull(bannerMap.get(bannerInfo2.getBannerId()));
        actualBanner3 = checkNotNull(bannerMap.get(bannerInfo3.getBannerId()));
        actualBanner4 = checkNotNull(bannerMap.get(bannerInfo4.getBannerId()));
    }
}
