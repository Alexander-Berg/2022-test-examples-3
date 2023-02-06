package ru.yandex.direct.web.testing.data;

import ru.yandex.direct.web.entity.banner.model.WebBannerSitelink;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

public class TestBannerSitelinks {

    private TestBannerSitelinks() {
    }

    public static WebBannerSitelink randomTitleWebSitelink() {
        return new WebBannerSitelink()
                .withTitle(randomAlphabetic(7))
                .withDescription(randomAlphabetic(7))
                .withUrlProtocol("https://")
                // URL дефолтного сайтлинка должен матчиться с URL дефолтного баннера
                .withHref("www.yandex.ru/company");
    }
}
