package ru.yandex.direct.core.entity.banner.service;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.Language;

import static ru.yandex.direct.utils.CommonUtils.nvl;

public class BannersAddOperationTestData {

    static CpmIndoorBanner createExpectedBanner(CpmIndoorBanner bannerToAdd,
                                                BannerStatusModerate statusModerate,
                                                BannerStatusPostModerate statusPostModerate,
                                                BannerCreativeStatusModerate bannerCreativeStatusModerate) {
        return fillCommonFieldsWithExpectedValues(new CpmIndoorBanner(), bannerToAdd)
                .withCreativeId(bannerToAdd.getCreativeId())

                .withStatusModerate(statusModerate)
                .withStatusPostModerate(statusPostModerate)
                .withCreativeStatusModerate(bannerCreativeStatusModerate);
    }

    private static CpmIndoorBanner fillCommonFieldsWithExpectedValues(CpmIndoorBanner expectedBanner,
                                                                      CpmIndoorBanner bannerToAdd) {
        expectedBanner
                .withAdGroupId(bannerToAdd.getAdGroupId())

                .withHref(bannerToAdd.getHref())

                .withDomain("www.yandex.ru")

                .withBsBannerId(0L)
                .withStatusBsSynced(StatusBsSynced.NO)

                .withStatusModerate(BannerStatusModerate.READY)
                .withStatusPostModerate(BannerStatusPostModerate.NO)

                .withLanguage(nvl(bannerToAdd.getLanguage(), Language.UNKNOWN))

                .withStatusActive(false)
                .withStatusShow(true)
                .withStatusArchived(false)

                .withGeoFlag(false)

                .withFlags(bannerToAdd.getFlags());
        return expectedBanner;
    }
}
