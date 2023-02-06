package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.util.List;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.Language;

public class TestNewBanners {

    public static <B extends BannerWithSystemFields> B fillSystemFieldsForActiveBanner(B banner) {
        banner
                .withLanguage(Language.EN)
                .withLastChange(LocalDateTime.now())
                .withStatusShow(true)
                .withStatusArchived(false)
                .withStatusModerate(BannerStatusModerate.YES)
                .withStatusPostModerate(BannerStatusPostModerate.YES)
                .withBsBannerId(12345L)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusActive(true);
        return banner;
    }

    public static List<BannerAdditionalHref> clientBannerAdditionalHrefs() {
        return List.of(
                new BannerAdditionalHref().withHref("http://google.com"),
                new BannerAdditionalHref().withHref("http://yahoo.com")
        );
    }
}
