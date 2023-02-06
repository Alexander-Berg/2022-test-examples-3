package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.banner;

import java.util.List;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexImageHashBanner;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.ComplexAdGroupUpdateOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.fullImageBannerWithImage;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class ComplexUpdateBannerTestBase extends ComplexAdGroupUpdateOperationTestBase {

    // TextBanner

    protected TextBannerInfo createRandomTitleTextBanner(AdGroupInfo adGroupInfo) {
        OldTextBanner randomTitleBanner = activeTextBanner()
                .withTitle(randomAlphabetic(10));
        return steps.bannerSteps().createBanner(randomTitleBanner, adGroupInfo);
    }

    protected TextBanner randomTitleTextBanner(TextBannerInfo bannerInfo) {
        return clientTextBanner()
                .withId(bannerInfo != null ? bannerInfo.getBannerId() : null)
                .withTitle(randomAlphabetic(10))
                .withHref("https://yandex.ru");
    }

    protected TextBanner emptyTitleTextBanner(TextBannerInfo bannerInfo) {
        return clientTextBanner()
                .withId(bannerInfo != null ? bannerInfo.getBannerId() : null)
                .withTitle("");
    }

    protected ComplexTextBanner randomTitleTextComplexBanner(TextBannerInfo bannerInfo) {
        return new ComplexTextBanner()
                .withBanner(randomTitleTextBanner(bannerInfo));
    }

    protected ComplexTextBanner randomTitleTextComplexBanner() {
        return randomTitleTextComplexBanner(null);
    }

    protected ComplexTextBanner emptyTitleTextComplexBanner(TextBannerInfo bannerInfo) {
        return new ComplexTextBanner().withBanner(emptyTitleTextBanner(bannerInfo));
    }

    protected ComplexTextBanner emptyTitleTextComplexBanner() {
        return emptyTitleTextComplexBanner(null);
    }

    // ImageHashBanner

    protected ImageBanner randomHrefImageHashBanner(ImageHashBannerInfo bannerInfo, String imageHash) {
        return fullImageBannerWithImage(imageHash)
                .withId(bannerInfo != null ? bannerInfo.getBannerId() : null)
                .withHref("https://yandex.ru/" + randomAlphanumeric(10))
                .withDomain(null);
    }

    protected ImageBanner randomHrefImageHashBanner(String imageHash) {
        return randomHrefImageHashBanner(null, imageHash);
    }

    protected ComplexImageHashBanner randomHrefComplexImageHashBanner(ImageHashBannerInfo bannerInfo,
                                                                      String imageHash) {
        return new ComplexImageHashBanner().withBanner(randomHrefImageHashBanner(bannerInfo, imageHash));
    }

    protected ComplexImageHashBanner randomHrefComplexImageHashBanner(String imageHash) {
        return new ComplexImageHashBanner().withBanner(randomHrefImageHashBanner(imageHash));
    }

    // sitelinks

    protected SitelinkSet randomHrefSitelinkSet() {
        return new SitelinkSet()
                .withSitelinks(asList(
                        new Sitelink()
                                .withTitle("desc1")
                                .withHref("https://yandex.ru/" + randomAlphanumeric(10)),
                        new Sitelink()
                                .withTitle("desc2")
                                .withHref("https://yandex.ru/" + randomAlphanumeric(10)),
                        new Sitelink()
                                .withTitle("desc3")
                                .withHref("https://yandex.ru/" + randomAlphanumeric(10))
                ));
    }

    // common

    protected TextBanner findBannerByTitleWithAssumption(List<Banner> banners, ComplexTextBanner bannerToFind) {
        String titleToFind = bannerToFind.getBanner().getTitle();
        List<TextBanner> found = StreamEx.of(banners)
                .select(TextBanner.class)
                .filter(b -> b.getTitle().equals(titleToFind))
                .toList();
        assumeThat("не найден баннер с title = " + titleToFind,
                found, not(emptyIterable()));
        assumeThat("найдено более одного баннера с title = " + titleToFind,
                found, hasSize(1));
        return found.get(0);
    }
}
