package ru.yandex.direct.web.entity.banner.converter;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerCallout;
import ru.yandex.direct.web.entity.banner.model.WebBannerCreative;
import ru.yandex.direct.web.entity.banner.model.WebBannerImageAd;
import ru.yandex.direct.web.entity.banner.model.WebBannerPrice;
import ru.yandex.direct.web.entity.banner.model.WebBannerSitelink;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;
import ru.yandex.direct.web.entity.banner.model.WebBannerVcard;
import ru.yandex.direct.web.entity.banner.model.WebBannerVideoRes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.domain.DomainUtils.refineDomain;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.PRICE_COMPARE_STRATEGY;
import static ru.yandex.direct.web.entity.banner.converter.BannerConverter.toCoreBannerPrice;
import static ru.yandex.direct.web.entity.banner.converter.BannerConverter.webBannerToCoreComplexBanner;
import static ru.yandex.direct.web.entity.banner.converter.BannerConverter.webBannersToCoreComplexBanners;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;

@SuppressWarnings("ConstantConditions")
public class BannerConverterTest {

    private static final Long ID1 = 123L;
    private static final Long ID2 = 345L;
    private static final String URL_PROTO1 = "http://";
    private static final String DOMAIN1 = "ya.ru";
    private static final String HREF1 = URL_PROTO1 + DOMAIN1;
    private static final String URL_PROTO2 = "https://";
    private static final String DOMAIN2 = "yandex.ru";
    private static final String HREF2 = URL_PROTO2 + DOMAIN2;
    private static final String IMAGE_HASH = "1e4f";

    @Test
    public void webBannerToCoreComplexBanner_TextBannerPrimitiveFields_Converted() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1);

        ComplexBanner expected = new ComplexBanner().withBanner(new TextBanner()
                .withId(ID1)
                .withIsMobile(false)
                .withTitle(webBanner.getTitle())
                .withTitleExtension(webBanner.getTitleExtension())
                .withBody(webBanner.getBody())
                .withHref(webBanner.getUrlProtocol() + webBanner.getHref())
                .withDisplayHref(webBanner.getDisplayHref())
                .withDomain(webBanner.getDomain())
                .withTurboLandingHrefParams(webBanner.getTurbolandingHrefParams()));

        ComplexBanner actual = webBannerToCoreComplexBanner(webBanner);
        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerIdIsNull_ResultIdIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1).withId(null);
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getId(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerIdIsZero_ResultIdIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1).withId(0L);
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getId(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerBannerTypeIsNull_ResultIsNotMobile() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withBannerType(null);
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getIsMobile(), is(false));
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerBannerTypeIsEmpty_ResultIsNotMobile() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withBannerType("");
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getIsMobile(), is(false));
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerBannerTypeIsDesktop_ResultIsNotMobile() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withBannerType("desktop");
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getIsMobile(), is(false));
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerBannerTypeIsMobile_ResultIsMobile() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withBannerType("mobile");
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getIsMobile(), is(true));
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerTitleExtIsBlank_ResultTitleExtIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withTitleExtension(" ");
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getTitleExtension(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerDisplayHrefIsNull_ResultDisplayHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withDisplayHref(null);
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getDisplayHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerDisplayHrefIsBlank_ResultDisplayHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withDisplayHref(" ");
        var textBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(textBanner.getDisplayHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerHrefIsBlank_ResultHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withUrlProtocol("https://")
                .withHref(" ");
        assertThat(((TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner()).getHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerHrefIsNull_ResultHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withUrlProtocol("https://")
                .withHref(null);
        assertThat(((TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner()).getHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerUrlProtoIsBlank_ResultHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withUrlProtocol(" ")
                .withHref("yandex.ru");
        assertThat(((TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner()).getHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerUrlProtoIsNull_ResultHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withUrlProtocol(null)
                .withHref("yandex.ru");
        assertThat(((TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner()).getHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerHrefAndUrlProtocolAreBlank_ResultHrefIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withUrlProtocol(" ")
                .withHref(" ");
        assertThat(((TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner()).getHref(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerDomainIsBlank_ResultDomainIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withDomain(" ");
        assertThat(((TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner()).getDomain(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerTurboParamsIsNull_ResultTurboParamsIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withTurbolandingHrefParams(null);
        var resultBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(resultBanner.getTurboLandingHrefParams(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerTurboParamsIsBlank_ResultTurboParamsIsNull() {
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withTurbolandingHrefParams(" ");
        var resultBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(resultBanner.getTurboLandingHrefParams(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_WebBannerTurboParamsIsSet_ResultTurboParamsIsSet() {
        String params = "p1=v1";
        WebBanner webBanner = randomTitleWebTextBanner(ID1)
                .withTurbolandingHrefParams(params);
        var resultBanner = (TextBanner) webBannerToCoreComplexBanner(webBanner).getBanner();
        assertThat(resultBanner.getTurboLandingHrefParams(), equalTo(params));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithVcard_VcardConverted() {
        WebBannerVcard webVcard = new WebBannerVcard().withApart("abc");
        WebBanner webBanner = emptyWebTextBanner().withVcard(webVcard);
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(complexBanner.getVcard(), notNullValue());
        assertThat(complexBanner.getVcard().getApart(), equalTo("abc"));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithEmptySitelinks_SitelinksConverted() {
        ComplexBanner complexBanner =
                webBannerToCoreComplexBanner(emptyWebTextBanner().withSitelinks(emptyList()));
        assertThat(complexBanner.getSitelinkSet(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithOneSitelink_SitelinkConverted() {
        WebBannerSitelink webSitelink1 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTO1)
                .withHref(DOMAIN1);
        WebBanner webBanner = emptyWebTextBanner().withSitelinks(singletonList(webSitelink1));
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(complexBanner.getSitelinkSet(), notNullValue());
        assertThat(complexBanner.getSitelinkSet().getSitelinks(), hasSize(1));
        assertThat(complexBanner.getSitelinkSet().getSitelinks().get(0).getHref(), equalTo(HREF1));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithTwoSitelinks_SitelinksConverted() {
        WebBannerSitelink webSitelink1 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTO1)
                .withHref(DOMAIN1);
        WebBannerSitelink webSitelink2 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTO2)
                .withHref(DOMAIN2);
        WebBanner webBanner = emptyWebTextBanner().withSitelinks(asList(webSitelink1, webSitelink2));
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(complexBanner.getSitelinkSet(), notNullValue());
        assertThat(complexBanner.getSitelinkSet().getSitelinks(), hasSize(2));
        assertThat(complexBanner.getSitelinkSet().getSitelinks().get(0).getHref(), equalTo(HREF1));
        assertThat(complexBanner.getSitelinkSet().getSitelinks().get(1).getHref(), equalTo(HREF2));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithSkippedSitelink_SitelinksConverted() {
        WebBannerSitelink webSitelink1 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTO1)
                .withHref(DOMAIN1);
        WebBannerSitelink webSitelink2 = new WebBannerSitelink()
                .withUrlProtocol(URL_PROTO2)
                .withHref(DOMAIN2);
        WebBanner webBanner = emptyWebTextBanner().withSitelinks(asList(webSitelink1, null, webSitelink2));
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(complexBanner.getSitelinkSet(), notNullValue());
        assertThat(complexBanner.getSitelinkSet().getSitelinks(), hasSize(3));
        assertThat(complexBanner.getSitelinkSet().getSitelinks().get(0).getHref(), equalTo(HREF1));
        assertThat(complexBanner.getSitelinkSet().getSitelinks().get(1), nullValue());
        assertThat(complexBanner.getSitelinkSet().getSitelinks().get(2).getHref(), equalTo(HREF2));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithEmptyCallouts_CalloutsConverted() {
        WebBanner webBanner = emptyWebTextBanner().withCallouts(emptyList());
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        var textBanner = (TextBanner) complexBanner.getBanner();
        assertThat(textBanner.getCalloutIds(), emptyIterable());
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithOneCallout_CalloutsConverted() {
        WebBanner webBanner = emptyWebTextBanner()
                .withCallouts(singletonList(new WebBannerCallout().withAdditionsItemId(ID1)));
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        var textBanner = (TextBanner) complexBanner.getBanner();
        assertThat(textBanner.getCalloutIds(), hasSize(1));
        assertThat(textBanner.getCalloutIds().get(0), equalTo(ID1));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithTwoCallouts_CalloutsConverted() {
        WebBanner webBanner = emptyWebTextBanner()
                .withCallouts(asList(
                        new WebBannerCallout().withAdditionsItemId(ID1),
                        new WebBannerCallout().withAdditionsItemId(ID2)));
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        var textBanner = (TextBanner) complexBanner.getBanner();
        assertThat(textBanner.getCalloutIds(), hasSize(2));
        assertThat(textBanner.getCalloutIds().get(0), equalTo(ID1));
        assertThat(textBanner.getCalloutIds().get(1), equalTo(ID2));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithVideo_VideoConverted() {
        WebBanner webBanner = emptyWebTextBanner()
                .withVideoResources(new WebBannerVideoRes().withId(ID1));
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(((TextBanner) complexBanner.getBanner()).getCreativeId(), equalTo(ID1));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithTurbolanding_TurbolandingConverted() {
        WebBannerTurbolanding webTurbolanding = new WebBannerTurbolanding()
                .withId(ID1);
        WebBanner webBanner = emptyWebTextBanner().withTurbolanding(webTurbolanding);

        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(((TextBanner) complexBanner.getBanner()).getTurboLandingId(), equalTo(ID1));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithImage_ImageConverted() {
        WebBanner webBanner = emptyWebTextBanner()
                .withId(ID1)
                .withImageHash(IMAGE_HASH);

        ComplexBanner complexBanner = webBannerToCoreComplexBanner(webBanner);
        assertThat(((TextBanner) complexBanner.getBanner()).getImageHash(), equalTo(IMAGE_HASH));
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithNulls_DoesntFailAndReturnsModel() {
        ComplexBanner complexBanner = webBannerToCoreComplexBanner(emptyWebTextBanner());
        assertThat(complexBanner, notNullValue());
        assertThat(complexBanner.getBanner(), notNullValue());
        assertThat(complexBanner.getVcard(), nullValue());
        assertThat(complexBanner.getSitelinkSet(), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanner_TextBannerWithBlankCreative_ConvertedToTextBanner() {
        ComplexBanner complexBanner =
                webBannerToCoreComplexBanner(emptyWebTextBanner()
                        .withCreative(new WebBannerCreative().withCreativeId("")));
        assertThat(complexBanner.getBanner() instanceof TextBanner, equalTo(true));
    }

    @Test
    public void webBannerToCoreComplexBanner_ImageBannerWithNoCreative_ConvertedToImageHashBanner() {
        WebBanner given = emptyWebImageBanner()
                .withId(ID1)
                .withUrlProtocol(URL_PROTO1)
                .withHref(DOMAIN1)
                .withImageAd(new WebBannerImageAd().withHash(IMAGE_HASH));

        var actual = (ImageBanner) webBannerToCoreComplexBanner(given).getBanner();

        var expected = new ImageBanner()
                .withId(ID1)
                .withIsMobileImage(false)
                .withHref(HREF1)
                .withImageHash(IMAGE_HASH);

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreComplexBanner_ImageBannerWithCreativeWithoutId_ConvertedToImageHashBanner() {
        WebBanner given = emptyWebImageBanner()
                .withId(ID1)
                .withUrlProtocol(URL_PROTO1)
                .withHref(DOMAIN1)
                .withImageAd(new WebBannerImageAd().withHash(IMAGE_HASH))
                .withCreative(new WebBannerCreative());

        var actual = (ImageBanner) webBannerToCoreComplexBanner(given).getBanner();

        var expected = new ImageBanner()
                .withId(ID1)
                .withIsMobileImage(false)
                .withHref(HREF1)
                .withImageHash(IMAGE_HASH);

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreComplexBanner_ImageBannerWithCreativeWithId_ImageCreativeBannerConverted() {
        String creativeIdStr = ID1.toString();

        WebBanner given = emptyWebImageBanner()
                .withCreative(new WebBannerCreative().withCreativeId(creativeIdStr));

        var actual = (ImageBanner) webBannerToCoreComplexBanner(given).getBanner();

        var expected = new ImageBanner()
                .withIsMobileImage(false)
                .withCreativeId(ID1);
        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void webBannerToCoreComplexBanner_CpcVideoBannerWithCreativeWithId_CpcVideoBannerConverted() {
        String creativeIdStr = ID1.toString();

        WebBanner given = emptyWebCpcVideoBanner()
                .withCreative(new WebBannerCreative().withCreativeId(creativeIdStr));

        var actual = (CpcVideoBanner) webBannerToCoreComplexBanner(given).getBanner();

        var expected = new CpcVideoBanner()
                .withCreativeId(ID1)
                .withIsMobileVideo(false);
        assertThat(actual, beanDiffer(expected));
    }


    @Test
    public void webBannerToCoreComplexBanner_Null_ReturnsNull() {
        assertThat(webBannerToCoreComplexBanner(null), nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void webBannerToCoreComplexBanner_NoAdType_ThrowsException() {
        webBannerToCoreComplexBanner(new WebBanner());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void webBannerToCoreComplexBanner_UnsupportedAdType_ThrowsException() {
        webBannerToCoreComplexBanner(new WebBanner().withAdType(BannersBannerType.cpm_banner.getLiteral()));
    }

    @Test
    public void webBannersToCoreComplexBanners_Null_ReturnsNull() {
        assertThat(webBannersToCoreComplexBanners(null), nullValue());
    }

    @Test
    public void webBannerToCoreComplexBanners_EmptyList_ReturnsEmptyList() {
        assertThat(webBannersToCoreComplexBanners(emptyList()), emptyIterable());
    }

    @Test
    public void webBannerToCoreComplexBanners_NullElement_ReturnsNullElement() {
        assertThat(webBannersToCoreComplexBanners(singletonList(null)), contains(nullValue()));
    }

    @Test
    public void refineDomain_DomainStartsWithHttp_RemovesHttp() {
        assertThat(refineDomain("http://yandex.ru"), equalTo("yandex.ru"));
    }

    @Test
    public void refineDomain_DomainStartsWithHttps_RemovesHttps() {
        assertThat(refineDomain("https://yandex.ru"), equalTo("yandex.ru"));
    }

    @Test
    public void refineDomain_DomainIsNull_RemovesProto() {
        assertThat(refineDomain(null), nullValue());
    }

    @Test
    public void toCoreBannerPriceTest_bannerPriceConverted() {
        var expected = new BannerPrice()
                .withPrice(new BigDecimal("6467.00"))
                .withPriceOld(new BigDecimal("676977777777777792.00"))
                .withPrefix(null)
                .withCurrency(BannerPricesCurrency.EUR);

        WebBannerPrice webBannerPrice = new WebBannerPrice()
                .withPrice("6 467,00")
                .withPriceOld("676 977 777 777 777 792,00")
                .withPrefix("")
                .withCurrency("EUR");

        assertThat(toCoreBannerPrice(webBannerPrice), beanDiffer(expected).useCompareStrategy(PRICE_COMPARE_STRATEGY));
    }

    private WebBanner emptyWebTextBanner() {
        return new WebBanner().withAdType(BannersBannerType.text.getLiteral());
    }

    private WebBanner emptyWebImageBanner() {
        return new WebBanner().withAdType(BannersBannerType.image_ad.getLiteral());
    }

    private WebBanner emptyWebCpcVideoBanner() {
        return new WebBanner().withAdType(BannersBannerType.cpc_video.getLiteral());
    }
}
