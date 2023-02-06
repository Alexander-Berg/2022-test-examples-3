package ru.yandex.direct.core.testing.data;

import java.util.Collections;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;

import static ru.yandex.direct.core.testing.data.TestNewBanners.fillSystemFieldsForActiveBanner;

public class TestNewCpmGeoPinBanners {
    private TestNewCpmGeoPinBanners() {
    }

    public static CpmGeoPinBanner clientCpmGeoPinBanner(Long creativeId, Long permalinkId) {
        CpmGeoPinBanner banner = new CpmGeoPinBanner();
        fillCpmGeoPinBannerClientFields(banner, creativeId, permalinkId);
        return banner;
    }

    public static CpmGeoPinBanner fullCpmGeoPinBanner(Long creativeId, Long permalinkId) {
        CpmGeoPinBanner banner = new CpmGeoPinBanner();
        fillSystemFieldsForActiveBanner(banner);
        fillSystemFieldsForActiveCpmBanner(banner);
        fillCpmGeoPinBannerClientFields(banner, creativeId, permalinkId);
        banner.withPreferVCardOverPermalink(false);
        return banner;
    }

    private static void fillCpmGeoPinBannerClientFields(CpmGeoPinBanner banner, Long creativeId, Long permalinkId) {
        banner
                .withCreativeId(creativeId)
                .withPixels(Collections.emptyList())
                .withPermalinkId(permalinkId);
    }

    private static void fillSystemFieldsForActiveCpmBanner(CpmGeoPinBanner banner) {
        banner.withCreativeStatusModerate(BannerCreativeStatusModerate.YES)
                .withGeoFlag(false);
    }
}
