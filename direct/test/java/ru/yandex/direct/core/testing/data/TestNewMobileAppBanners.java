package ru.yandex.direct.core.testing.data;

import java.util.EnumSet;

import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class TestNewMobileAppBanners {

    private TestNewMobileAppBanners() {
    }

    public static MobileAppBanner fullMobileAppBanner(Long campaignId, Long adgroupId) {
        var banner = clientMobileAppBanner()
                .withAdGroupId(adgroupId)
                .withCampaignId(campaignId);
        fillMobileAppBannerSystemFields(banner);
        return banner;

    }

    private static void fillMobileAppBannerSystemFields(MobileAppBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner.withGeoFlag(true);
    }

    public static MobileAppBanner clientMobileAppBanner() {
        MobileAppBanner banner = new MobileAppBanner();
        fillMobileAppBannerClientFields(banner);
        return banner;
    }

    public static MobileAppBanner fullMobileBanner() {
        return fullMobileBanner(null, null);
    }

    public static MobileAppBanner fullMobileBanner(Long campaignId, Long adGroupId) {
        var banner = clientMobileAppBanner()
                .withCampaignId(campaignId)
                .withAdGroupId(adGroupId);
        fillMobileBannerSystemFields(banner);
        return banner;
    }


    private static void fillMobileAppBannerClientFields(MobileAppBanner banner) {

        var reflectedAttributes = EnumSet.allOf(NewReflectedAttribute.class).stream()
                .collect(toMap(identity(), attr -> true));

        banner.withTitle("MobileAppBanner Title")
                .withBody("MobileAppBanner Body")
                .withHref("https://trusted1.com")
                .withPrimaryAction(NewMobileContentPrimaryAction.BUY)
                .withReflectedAttributes(reflectedAttributes)
                .withImpressionUrl("https://trusted.impression.com/impression")
        ;
    }

    private static void fillMobileBannerSystemFields(MobileAppBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withDomain("trusted1.com")
                .withGeoFlag(false)
                .withImageStatusModerate(ifNotNull(banner.getImageHash(),
                        b -> StatusBannerImageModerate.YES))
                .withStatusShow(true);
    }
}
