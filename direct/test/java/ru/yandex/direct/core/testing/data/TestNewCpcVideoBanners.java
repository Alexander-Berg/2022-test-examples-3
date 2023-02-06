package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;

public class TestNewCpcVideoBanners {

    private final BannersUrlHelper bannersUrlHelper;

    public TestNewCpcVideoBanners(BannersUrlHelper bannersUrlHelper) {
        this.bannersUrlHelper = bannersUrlHelper;
    }

    public static CpcVideoBanner clientCpcVideoBanner(Long creativeId) {
        CpcVideoBanner banner = new CpcVideoBanner();
        fillCpcVideoBannerClientFields(banner, creativeId);
        return banner;
    }

    public CpcVideoBanner fullCpcVideoBanner(Long creativeId) {
        CpcVideoBanner banner = new CpcVideoBanner();
        fillCpcVideoBannerClientFields(banner, creativeId);
        fillCpcVideoBannerSystemFields(banner);
        return banner;
    }

    private static void fillCpcVideoBannerClientFields(
            CpcVideoBanner banner, Long creativeId) {
        banner.withHref("https://www.yandex.ru")
                .withIsMobileVideo(false)
                .withCreativeId(creativeId);
    }

    private void fillCpcVideoBannerSystemFields(CpcVideoBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(banner.getHref()))
                .withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withLanguage(Language.UNKNOWN)
                .withGeoFlag(false);
    }
}
