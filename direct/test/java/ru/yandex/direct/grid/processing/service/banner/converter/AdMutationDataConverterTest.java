package ru.yandex.direct.grid.processing.service.banner.converter;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdPriceCurrency;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAd;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultCpcVideoAdWithStaticIds;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultCreativeAdWithStaticIds;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultImageHashAdWithStaticIds;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultTextAdWithStaticIds;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getSimpleTextAdWithStaticIds;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getTextAdWithPermalink;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getTextAdWithPrice;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class AdMutationDataConverterTest {

    @Test
    public void convertFullTextAd() {
        GdUpdateAd gdUpdateAd = getDefaultTextAdWithStaticIds();

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));

        var expectedBanner = new TextBanner()
                .withId(gdUpdateAd.getId())
                .withHref(gdUpdateAd.getHref())
                .withTitle(gdUpdateAd.getTitle())
                .withTitleExtension(gdUpdateAd.getTitleExtension())
                .withBody(gdUpdateAd.getBody())
                .withVcardId(gdUpdateAd.getVcardId())
                .withCalloutIds(gdUpdateAd.getCalloutIds())
                .withImageHash(gdUpdateAd.getTextBannerImageHash())
                .withCreativeId(gdUpdateAd.getCreativeId())
                .withDisplayHref(gdUpdateAd.getDisplayHref())
                .withTurboLandingId(gdUpdateAd.getTurbolandingId())
                .withTurboLandingHrefParams(gdUpdateAd.getTurbolandingHrefParams())
                .withTurboGalleryHref(gdUpdateAd.getTurboGalleryParams().getTurboGalleryHref());

        checkConvertedBanner(banners, expectedBanner);
    }

    @Test
    public void convertImageHashAd() {
        GdUpdateAd gdUpdateAd = getDefaultImageHashAdWithStaticIds();

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));

        var expectedBanner = new ImageBanner()
                .withId(gdUpdateAd.getId())
                .withIsMobileImage(false)
                .withImageHash(gdUpdateAd.getImageCreativeHash())
                .withHref(gdUpdateAd.getHref())

                .withTurboLandingId(gdUpdateAd.getTurbolandingId())
                .withTurboLandingHrefParams(gdUpdateAd.getTurbolandingHrefParams());

        checkConvertedBanner(banners, expectedBanner);

    }

    @Test
    public void convertImageCreativeAd() {
        GdUpdateAd gdUpdateAd = getDefaultCreativeAdWithStaticIds();

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));


        var expectedBanner = new ImageBanner()
                .withId(gdUpdateAd.getId())
                .withIsMobileImage(false)
                .withCreativeId(gdUpdateAd.getCreativeId())
                .withHref(gdUpdateAd.getHref())
                .withTurboLandingId(gdUpdateAd.getTurbolandingId())
                .withTurboLandingHrefParams(gdUpdateAd.getTurbolandingHrefParams());

        checkConvertedBanner(banners, expectedBanner);

    }

    @Test
    public void convertCpcVideoAd() {
        GdUpdateAd gdUpdateAd = getDefaultCpcVideoAdWithStaticIds();

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));

        var expectedBanner = new CpcVideoBanner()
                .withId(gdUpdateAd.getId())
                .withCreativeId(gdUpdateAd.getCreativeId())
                .withHref(gdUpdateAd.getHref())
                .withTurboLandingId(gdUpdateAd.getTurbolandingId())
                .withTurboLandingHrefParams(gdUpdateAd.getTurbolandingHrefParams());

        checkConvertedBanner(banners, expectedBanner);

    }

    @Test
    public void convertAdWithOnlyNonNullFields() {
        GdUpdateAd gdUpdateAd = getSimpleTextAdWithStaticIds();

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));

        var expectedBanner = new TextBanner()
                .withId(gdUpdateAd.getId())
                .withTitle(gdUpdateAd.getTitle())
                .withBody(gdUpdateAd.getBody());

        checkConvertedBanner(banners, expectedBanner);


    }

    @Test
    public void convertAdWithAdPrice() {
        GdAdPrice price = new GdAdPrice()
                .withPrice(BigDecimal.ONE.setScale(2, HALF_UP).toString())
                .withCurrency(GdAdPriceCurrency.RUB);
        GdUpdateAd gdUpdateAd = getTextAdWithPrice(price);

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));

        var expectedBanner = new TextBanner()
                .withId(gdUpdateAd.getId())
                .withTitle(gdUpdateAd.getTitle())
                .withBody(gdUpdateAd.getBody())

                .withBannerPrice(new BannerPrice()
                        .withPrice(BigDecimal.ONE.setScale(2, HALF_UP))
                        .withCurrency(BannerPricesCurrency.RUB));
        checkConvertedBanner(banners, expectedBanner);
    }

    @Test
    public void convertAdWithPermalink() {
        Long permalinkId = 345L;
        GdUpdateAd gdUpdateAd = getTextAdWithPermalink(permalinkId);

        var banners = AdMutationDataConverter.toCoreBanners(singletonList(gdUpdateAd));

        var expectedBanner = new TextBanner()
                .withId(gdUpdateAd.getId())
                .withTitle(gdUpdateAd.getTitle())
                .withBody(gdUpdateAd.getBody())
                .withPermalinkId(permalinkId);
        checkConvertedBanner(banners, expectedBanner);
    }

    private void checkConvertedBanner(List<BannerWithSystemFields> banners, BannerWithSystemFields expectedBanner) {
        assertThat(banners).is(matchedBy(
                beanDiffer(singletonList(expectedBanner))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }
}
