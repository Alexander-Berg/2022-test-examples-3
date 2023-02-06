package ru.yandex.market.api.common.url;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.URIMatcher;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.common.url.params.CAPISearchUrlParams;
import ru.yandex.market.api.common.url.params.CatalogUrlParams;
import ru.yandex.market.api.common.url.params.ModelOpinionUrlParams;
import ru.yandex.market.api.common.url.params.ModelUrlParams;
import ru.yandex.market.api.common.url.params.ShopOpinionUrlParams;
import ru.yandex.market.api.common.url.params.SkuUrlParams;
import ru.yandex.market.api.common.url.params.UrlParams;
import ru.yandex.market.api.common.url.params.UrlParamsFactory;
import ru.yandex.market.api.common.url.params.VendorUrlParams;
import ru.yandex.market.api.domain.Model;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.domain.v2.criterion.Criteria;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.PartnerInfo;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.offer.Offer;
import ru.yandex.market.api.server.context.ContextHelper;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.Urls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.api.util.functional.Functionals.getOrNull;

/**
 * Created by apershukov on 06.12.16.
 */
@WithContext
@ActiveProfiles(MarketUrlsTest.PROFILE)
public class MarketUrlsTest extends ContainerTestBase {
    static final String PROFILE = "MarketUrlsTest";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Bean
        @Primary
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }

    }

    private int defaultRegionId;

    @Inject
    private MarketUrls marketUrls;

    @Inject
    private ClientHelper clientHelper;

    @Inject
    private UrlParamsFactory urlParamsFactory;

    private MockClientHelper mockClientHelper;

    @Before
    public void setUp() throws Exception {
        defaultRegionId = ContextHelper.getRegionId();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void offerV2WithoutCpc() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        OfferV2 offer = new OfferV2();
        offer.setWareMd5("LP3e5x_dp4TX7pRS0n9x9g");
        offer.setCategoryId(10);

        assertEquals("http://market.yandex.ru/offer/LP3e5x_dp4TX7pRS0n9x9g?hid=10", marketUrls.offer(offer).url());
    }

    @Test
    public void offerV2WithCpc() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        OfferV2 offer = new OfferV2();
        offer.setWareMd5("LP3e5x_dp4TX7pRS0n9x9g");
        offer.setCategoryId(10);
        offer.setCpc("DwjATgCHzD5F-Fm-j7ytQQpvZqn5eVnz4iLmpcZxxRbs5zloirDf2hUM79esXC5m");

        assertEquals("http://market.yandex.ru/offer/LP3e5x_dp4TX7pRS0n9x9g?hid=10" +
                "&cpc=DwjATgCHzD5F-Fm-j7ytQQpvZqn5eVnz4iLmpcZxxRbs5zloirDf2hUM79esXC5m",
            marketUrls.offer(offer).url());
    }

    @Test
    public void offerV1WithoutCpc() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        Offer offer = new Offer();
        offer.setWareMd5("LP3e5x_dp4TX7pRS0n9x9g");
        offer.setCategoryId(10);

        assertEquals("http://market.yandex.ru/offer/LP3e5x_dp4TX7pRS0n9x9g?hid=10", marketUrls.offer(offer).url());
    }

    @Test
    public void offerV1WithCpc() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        Offer offer = new Offer();
        offer.setWareMd5("LP3e5x_dp4TX7pRS0n9x9g");
        offer.setCategoryId(10);
        offer.setCpc("DwjATgCHzD5F-Fm-j7ytQQpvZqn5eVnz4iLmpcZxxRbs5zloirDf2hUM79esXC5m");

        assertEquals("http://market.yandex.ru/offer/LP3e5x_dp4TX7pRS0n9x9g?hid=10" +
                "&cpc=DwjATgCHzD5F-Fm-j7ytQQpvZqn5eVnz4iLmpcZxxRbs5zloirDf2hUM79esXC5m",
            marketUrls.offer(offer).url());
    }

    @Test
    public void searchWithPriceFrom() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        Collection<Criterion> criteria = Collections.singleton(new Criterion(
            Filters.PRICE_FILTER_CODE, "1000~", Criterion.CriterionType.API_FILTER));

        assertEquals("http://market.yandex.ru/search?pricefrom=1000&nid=10&hid=20",
                urlParamsFactory.create(CAPISearchUrlParams.class)
                        .withNid(10)
                        .withHids(IntLists.singleton(20))
                        .withFilterParams(criteria)
                        .url());
    }

    @Test
    public void searchWithPriceTo() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        Collection<Criterion> criteria = Collections.singleton(new Criterion(
            Filters.PRICE_FILTER_CODE, "~2000", Criterion.CriterionType.API_FILTER));

        assertEquals("http://market.yandex.ru/search?priceto=2000&nid=10&hid=20",
                urlParamsFactory.create(CAPISearchUrlParams.class)
                        .withNid(10)
                        .withHids(IntLists.singleton(20))
                        .withFilterParams(criteria)
                        .url());
    }

    @Test
    public void shouldApplyPartnerParameters() {
        setRegionId(defaultRegionId);
        setLinkTo(FrontendType.DESKTOP);

        Offer offer = new Offer();
        offer.setWareMd5("LP3e5x_dp4TX7pRS0n9x9g");
        offer.setCategoryId(10);

        ContextHolder.get().setPpList(IntLists.singleton(100));
        ContextHolder.get().setPartnerInfo(PartnerInfo.create("test-clid", "774",123L, 456L, null));

        assertEquals("http://market.yandex.ru/offer/LP3e5x_dp4TX7pRS0n9x9g?pp=100" +
            "&clid=test-clid&vid=774&mclid=123&distr_type=456&hid=10", marketUrls.offer(offer).url());
    }

    @Test
    public void ridsRussiaInOfferLinkWhenContextIsNull() {
        ContextHolder.set(null);
        OfferV2 offerV2 = new OfferV2();
        offerV2.setWareMd5("ware1");

        assertEquals("https://market.yandex.ru/offer/ware1?lr=" + GeoUtils.DEFAULT_GEO_ID, marketUrls.offer(offerV2).url());
    }

    @Test
    public void ridsFromContextInOfferLink() {
        setRegionId(213);
        setLinkTo(FrontendType.DESKTOP);

        OfferV2 offerV2 = new OfferV2();
        offerV2.setWareMd5("ware1");

        assertEquals("http://market.yandex.ru/offer/ware1?lr=213", marketUrls.offer(offerV2).url());
    }

    @Test
    public void modelLinkToDesktop() {
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withSlug("")
                .withHid(34)
                .withFrontendDeviceType(FrontendDeviceType.DESKTOP)
                .url();

        assertEquals("http://market.yandex.ru/product/12?hid=34", url);
    }

    @Test
    public void modelLinkWithSlugToDesktop() {
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withSlug("slug")
                .withHid(34)
                .withFrontendDeviceType(FrontendDeviceType.DESKTOP)
                .url();

        assertEquals("http://market.yandex.ru/product--slug/12?hid=34", url);
    }

    @Test
    public void modelLinkToTouch() {
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withSlug("")
                .withHid(34)
                .withFrontendDeviceType(FrontendDeviceType.TOUCH)
                .url();

        assertEquals("http://m.market.yandex.ru/product/12?hid=34", url);
    }

    @Test
    public void modelLinkWithSlugToTouch() {
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withSlug("slug")
                .withHid(34)
                .withFrontendDeviceType(FrontendDeviceType.TOUCH)
                .url();

        assertEquals("http://m.market.yandex.ru/product--slug/12?hid=34", url);
    }

    @Test
    public void modelLinkToDefault() {
        setLinkTo(null);
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withSlug("")
                .withHid(34)
                .url();

        assertEquals("http://market.yandex.ru/product/12?hid=34", url);
    }

    @Test
    public void modelLinkWithSlugToDefault() {
        setLinkTo(null);
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withSlug("slug")
                .withHid(34)
                .withFrontendDeviceType(FrontendDeviceType.DESKTOP)
                .url();

        assertEquals("http://market.yandex.ru/product--slug/12?hid=34", url);
    }

    @Test
    public void modelContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withHid(34)
                .withSlug("")
                .url();

        assertEquals("http://market.yandex.ru/product/12?hid=34", url);
    }

    @Test
    public void modelContextLinkToDesktopWithSlug() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withHid(34)
                .withSlug("slug")
                .url();

        assertEquals("http://market.yandex.ru/product--slug/12?hid=34", url);
    }

    @Test
    public void modelContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withHid(34)
                .withSlug("")
                .url();

        assertEquals("http://m.market.yandex.ru/product/12?hid=34", url);
    }

    @Test
    public void modelContextLinkWithSlugToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ModelUrlParams.class)
                .withModelId(12)
                .withHid(34)
                .withSlug("slug")
                .url();

        assertEquals("http://m.market.yandex.ru/product--slug/12?hid=34", url);
    }

    @Test
    public void redirectLinkToDesktop() {
        String url = marketUrls.redirect("search", ContextHolder.get(), FrontendDeviceType.DESKTOP);
        assertEquals("http://market.yandex.ru/search?cvredirect=1&text=search", url);
    }

    @Test
    public void redirectLinkToTouch() {
        String url = marketUrls.redirect("search", ContextHolder.get(), FrontendDeviceType.TOUCH);
        assertEquals("http://m.market.yandex.ru/search?cvredirect=1&text=search", url);
    }

    @Test
    public void redirectLinkWithPartner() {
        ContextHolder.update(
            ctx -> {
                ctx.setPpList(IntLists.singleton(45));
                ctx.setPartnerInfo(PartnerInfo.create("12", "47", 89L, 1L, null));
            }
        );
        String url = marketUrls.redirect("search", ContextHolder.get(), FrontendDeviceType.DESKTOP);
        assertEquals(
            "http://market.yandex.ru/search?cvredirect=1&text=search&pp=45&clid=12&vid=47&mclid=89&distr_type=1",
            url
        );
    }

    @Test
    public void vendorContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(VendorUrlParams.class)
                .withVendorId(12)
                .withSlug("")
                .url();
        assertEquals("http://market.yandex.ru/brands/12", url);
    }

    @Test
    public void vendorContextLinkToDesktopWithSlug() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(VendorUrlParams.class)
                .withVendorId(12)
                .withSlug("brand-slug")
                .url();

        assertEquals("http://market.yandex.ru/brands--brand-slug/12", url);
    }

    @Test
    public void vendorContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(VendorUrlParams.class).withVendorId(12).url();

        assertEquals("http://m.market.yandex.ru/brands/12", url);
    }

    @Test
    public void vendorContextLinkToTouchWithSlug() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(VendorUrlParams.class)
                .withVendorId(12)
                .withSlug("brand-slug")
                .url();

        assertEquals("http://m.market.yandex.ru/brands--brand-slug/12", url);
    }

    @Test
    public void catalogContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withNid(12)
                .withHid(34)
                .withFilterParams(Collections.singleton(
                        new Criterion("123", "345", Criterion.CriterionType.GLFILTER)
                ))
                .withText("query")
                .withReportState("12")
                .uri();

        Assert.assertThat(
                uri,
                URIMatcher.uri(
                        URIMatcher.scheme("http"),
                        URIMatcher.host("market.yandex.ru"),
                        URIMatcher.hasSingleQueryParam("hid", "34"),
                        URIMatcher.hasSingleQueryParam("text", "query"),
                        URIMatcher.hasSingleQueryParam("glfilter", "123:345"),
                        URIMatcher.hasSingleQueryParam("rs", "12"),
                        URIMatcher.hasSingleQueryParam("onstock", "1")
                )
        );
    }

    @Test
    public void catalogContextLinkWithSlugToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withNid(12)
                .withHid(34)
                .withSlug("slug")
                .withFilterParams(Collections.singleton(
                        new Criterion("123", "345", Criterion.CriterionType.GLFILTER)
                ))
                .withText("query")
                .withReportState("12")
                .uri();

        Assert.assertThat(
                uri,
                URIMatcher.uri(
                        URIMatcher.scheme("http"),
                        URIMatcher.path("/catalog--slug/12/list"),
                        URIMatcher.hasSingleQueryParam("hid", "34"),
                        URIMatcher.hasSingleQueryParam("text", "query"),
                        URIMatcher.hasSingleQueryParam("glfilter", "123:345"),
                        URIMatcher.hasSingleQueryParam("rs", "12"),
                        URIMatcher.hasSingleQueryParam("onstock", "1"),
                        URIMatcher.hasNoQueryParams("onstock", "0")
                )
        );
    }

    @Test
    public void catalogContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withNid(12)
                .withHid(34)
                .withFilterParams(Collections.singleton(
                        new Criterion("123", "345", Criterion.CriterionType.GLFILTER)
                ))
                .withText("query")
                .withReportState("12")
                .uri();

        Assert.assertThat(
                uri,
                URIMatcher.uri(
                        URIMatcher.scheme("http"),
                        URIMatcher.host("m.market.yandex.ru"),
                        URIMatcher.path("/catalog/12/list"),
                        URIMatcher.hasSingleQueryParam("hid", "34"),
                        URIMatcher.hasSingleQueryParam("text", "query"),
                        URIMatcher.hasSingleQueryParam("glfilter", "123:345"),
                        URIMatcher.hasSingleQueryParam("rs", "12"),
                        URIMatcher.hasSingleQueryParam("onstock", "1"),
                        URIMatcher.hasNoQueryParams("onstock", "0")
                )
        );
    }

    @Test
    public void catalogContextLinkWithSlugToTouch() {
        setLinkTo(FrontendType.TOUCH);
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withNid(12)
                .withHid(34)
                .withSlug("slug")
                .withFilterParams(Collections.singleton(
                        new Criterion("123", "345", Criterion.CriterionType.GLFILTER)
                ))
                .withText("query")
                .withReportState("12")
                .uri();

        assertThat(
                uri,
                URIMatcher.uri(
                        URIMatcher.scheme("http"),
                        URIMatcher.host("m.market.yandex.ru"),
                        URIMatcher.path("/catalog--slug/12/list"),
                        URIMatcher.hasSingleQueryParam("hid", "34"),
                        URIMatcher.hasSingleQueryParam("text", "query"),
                        URIMatcher.hasSingleQueryParam("glfilter", "123:345"),
                        URIMatcher.hasSingleQueryParam("rs", "12"),
                        URIMatcher.hasSingleQueryParam("onstock", "1"),
                        URIMatcher.hasNoQueryParams("onstock", "0")
                )
        );
    }

    @Test
    public void articleContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = marketUrls.promoArticle("promo");
        assertEquals("http://market.yandex.ru/articles/promo", url);
    }


    @Test
    public void articleContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = marketUrls.promoArticle("promo");
        assertEquals("http://m.market.yandex.ru/articles/promo", url);
    }

    @Test
    public void promoCollectionContextLinkToDektop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = marketUrls.promoCollection("promo");
        assertEquals("http://market.yandex.ru/collections/promo", url);
    }

    @Test
    public void promoCollectionContextLinkToTouch() {
        setLinkTo(FrontendType.DESKTOP);
        String url = marketUrls.promoCollection("promo");
        assertEquals("http://market.yandex.ru/collections/promo", url);
    }

    @Test
    public void shopOpinionContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ShopOpinionUrlParams.class)
                .withShopId(12)
                .url();
        assertEquals("http://market.yandex.ru/shop/12/reviews", url);
    }

    @Test
    public void shopOpinionContextLinkWithSlugToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ShopOpinionUrlParams.class)
                .withShopId(12)
                .withSlug("slug")
                .url();
        assertEquals("http://market.yandex.ru/shop--slug/12/reviews", url);
    }

    @Test
    public void shopOpinionContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ShopOpinionUrlParams.class)
                .withShopId(12)
                .url();
        assertEquals("http://m.market.yandex.ru/shop/12", url);
    }

    @Test
    public void shopOpinionContextLinkWithSlugToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ShopOpinionUrlParams.class)
                .withShopId(12)
                .withSlug("slug")
                .url();
        assertEquals("http://m.market.yandex.ru/shop--slug/12", url);
    }

    @Test
    public void modelOpinionContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withOpinionId(3)
                .url();
        assertTrue(url.startsWith("http://market.yandex.ru/product/1/reviews?"
            + "hid=2&firstReviewId=3"));
    }

    @Test
    public void modelOpinionContextLinkWithSlugToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withOpinionId(3)
                .withSlug("slug")
                .url();
        assertTrue(url.startsWith("http://market.yandex.ru/product--slug/1/reviews?"
            + "hid=2&firstReviewId=3"));
    }

    @Test
    public void modelOpinionContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withOpinionId(3)
                .url();
        assertTrue(url.startsWith("http://m.market.yandex.ru/product/1/reviews?"
            + "hid=2&firstReviewId=3"));
    }

    @Test
    public void modelOpinionContextLinkWithSlugToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withSlug("slug")
                .withHid(2)
                .withOpinionId(3)
                .url();
        assertTrue(url.startsWith("http://m.market.yandex.ru/product--slug/1/reviews?"
            + "hid=2&firstReviewId=3"));
    }

    @Test
    public void modelOpinionsContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withSlug("")
                .url();
        assertTrue(url.startsWith("http://market.yandex.ru/product/1/reviews?hid=2"));
    }

    @Test
    public void modelOpinionsContextLinkWithSlugToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withSlug("slug")
                .url();
        assertTrue(url.startsWith("http://market.yandex.ru/product--slug/1/reviews?hid=2"));
    }

    @Test
    public void modelOpinionsContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withSlug("")
                .url();
        assertTrue(url.startsWith("http://m.market.yandex.ru/product/1/reviews?hid=2"));
    }

    @Test
    public void modelOpinionsContextLinkWithSlugToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = urlParamsFactory.create(ModelOpinionUrlParams.class)
                .withModelId(1)
                .withHid(2)
                .withSlug("slug")
                .url();
        assertTrue(url.startsWith("http://m.market.yandex.ru/product--slug/1/reviews?hid=2"));
    }

    @Test
    public void similarSearchContextLinkToDesktop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = marketUrls.lookasSearch("1");
        assertEquals("http://m.market.yandex.ru/picsearch?cbir_id=1", url);
    }

    @Test
    public void similarSearchContextLinkToTouch() {
        setLinkTo(FrontendType.TOUCH);
        String url = marketUrls.lookasSearch("1");
        assertEquals("http://m.market.yandex.ru/picsearch?cbir_id=1", url);
    }

    @Test
    public void modelOffersWithFiltersByShop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = marketUrls.modelOffers(
            13,
            0,
            "",
            Collections.singleton(Criteria.shop(666))
        );

        assertEquals("http://market.yandex.ru/product/13/offers?fesh=666", url);
    }

    @Test
    public void modelOffersWithSlugWithFiltersByShop() {
        setLinkTo(FrontendType.DESKTOP);
        String url = marketUrls.modelOffers(
            13,
            0,
            "slug",
            Collections.singleton(Criteria.shop(666))
        );

        assertEquals("http://market.yandex.ru/product--slug/13/offers?fesh=666", url);
    }

    @Test
    public void desktopUrl() {
        String url = marketUrls.desktop("/catalog/123/");
        assertEquals("http://market.yandex.ru/catalog/123/", url);
    }

    @Test
    public void beruDesktopUrlOldFrontend() {
        setBlue(false);
        String url = marketUrls.desktop("/catalog/123/");
        assertEquals("http://beru.ru/catalog/123/", url);
    }

    @Test
    public void beruDesktopUrl() {
        setBlue(true);
        String url = marketUrls.desktop("/catalog/123/");
        assertEquals("http://pokupki.market.yandex.ru/catalog/123/", url);
    }

    @Test
    public void touchUrl() {
        String url = marketUrls.touch("/catalog/123");
        assertEquals("http://m.market.yandex.ru/catalog/123", url);
    }

    @Test
    public void beruTouchUrlOldFrontend() {
        setBlue(false);
        String url = marketUrls.touch("/catalog/123/");
        assertEquals("http://m.beru.ru/catalog/123/", url);
    }

    @Test
    public void beruTouchUrl() {
        setBlue(true);
        String url = marketUrls.touch("/catalog/123/");
        assertEquals("http://m.pokupki.market.yandex.ru/catalog/123/", url);
    }

    @Test
    public void blueTouchUrl() {
        String url = marketUrls.blueTouch("/catalog/123");
        assertEquals("http://m.pokupki.market.yandex.ru/catalog/123", url);
    }

    @Test
    public void skuUrl() {
        setBlue(true);

        Sku sku = new Sku();
        sku.setId("123");

        String url = marketUrls.sku(sku).url();

        assertEquals("http://pokupki.market.yandex.ru/product/123", url);
    }

    @Test
    public void skuUrlWithSlug() {
        setBlue(true);

        Sku sku = new Sku();
        sku.setId("123");
        sku.setName("Кот в мешке");

        String url = marketUrls.sku(sku).url();

        assertEquals("http://pokupki.market.yandex.ru/product/kot-v-meshke/123", url);
    }

    @Test
    public void beruModelUrl() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        model.setId(1L);
        model.setCategoryId(2);

        OfferV2 offer = new OfferV2();
        offer.setSku("123");
        model.setOffer(offer);
        // TODO
        String url = marketUrls.model(model)
                .url();

        assertEquals("http://pokupki.market.yandex.ru/product/123", url);
    }

    @Test
    public void beruModelUrlWithoutSkuInfo() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        String url = marketUrls.model(model)
                .url();

        assertEquals("http://pokupki.market.yandex.ru", url);
    }

    @Test
    public void modelUrlWithWprid() {
        ModelV2 model = new ModelV2();
        String url = ((ModelUrlParams) marketUrls.model(model))
                .withWprid("12334.4524.24")
                .url();

        assertEquals("http://market.yandex.ru/product/0?wprid=12334.4524.24", url);
    }

    @Test
    public void beruModelUrlWithSlug() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        model.setId(1L);
        model.setCategoryId(2);

        OfferV2 offer = new OfferV2();
        offer.setSku("123");
        offer.setName("Кот в мешке");
        offer.setWareMd5("asdfasdf");
        offer.getTransientF().setWareMd5("asdfasdf");
        model.setOffer(offer);
        String url = ((SkuUrlParams) marketUrls.model(model))
                .withWprid("123.133")
                .url();
        assertEquals("http://pokupki.market.yandex.ru/product/kot-v-meshke/123?wprid=123.133&offerid=asdfasdf", url);
    }

    @Test
    public void skuUrlFromModelWithNoSkuInfo() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        model.setId(1L);

        assertNull(marketUrls.sku(model));
    }

    @Test
    public void skuUrlFromModelWithSkuAndNoOffer() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        model.setId(1L);
        model.setSku("123");

        String url = marketUrls.sku(model).url();

        assertEquals("http://pokupki.market.yandex.ru/product/123", url);
    }

    @Test
    public void beruModelOpinionsUrl() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        model.setId(1L);
        model.setCategoryId(2);

        OfferV2 offer = new OfferV2();
        offer.setSku("123");
        model.setOffer(offer);

        String url = marketUrls.modelOpinions(model).url();

        assertEquals("http://pokupki.market.yandex.ru/product/123/reviews?track=partner", url);
    }

    @Test
    public void beruModelOpinionsUrlWithoutSkuInfo() {
        setBlue(true);

        ModelV2 model = new ModelV2();

        String url = marketUrls.modelOpinions(model).url();

        assertEquals("http://pokupki.market.yandex.ru", url);
    }

    @Test
    public void beruModelOpinionsUrlWithSlug() {
        setBlue(true);

        ModelV2 model = new ModelV2();
        model.setId(1L);
        model.setCategoryId(2);

        OfferV2 offer = new OfferV2();
        offer.setSku("123");
        offer.setName("Кот в мешке");
        model.setOffer(offer);

        String url = marketUrls.modelOpinions(model).url();

        assertEquals("http://pokupki.market.yandex.ru/product/kot-v-meshke/123/reviews?track=partner", url);
    }

    @Test
    public void beruOfferUrl() {
        setBlue(true);

        OfferV2 offer = new OfferV2();
        offer.setSku("123");
        offer.setWareMd5("abcd");
        offer.setCategoryId(2);

        String url = marketUrls.offer(offer).url();

        assertEquals("http://pokupki.market.yandex.ru/product/123?hid=2&offerid=abcd", url);
    }

    @Test
    public void testCountry_Russia() {
        setRegionId(54);

        int result = marketUrls.getCountry(ContextHolder.get());
        Assert.assertEquals("Должны получить Россию т.к. текущий регион пользователя Екатеринбург",
            GeoUtils.Country.RUSSIA, result);
    }

    @Test
    public void testCountry_Ukraine() {
        setRegionId(143);

        int result = marketUrls.getCountry(ContextHolder.get());
        Assert.assertEquals("Должны получить Украину т.к. текущий регион пользователя Киев",
            GeoUtils.Country.UKRAINE, result);
    }

    @Test
    public void blueOfferUrlForWhiteMarket() {
        OfferV2 offer = new OfferV2();
        offer.setSku("123");
        offer.setWareMd5("abcd");
        offer.setCategoryId(2);
        offer.setOwnMarketPlace(true);

        String url = marketUrls.offer(offer).url();

        assertEquals("http://pokupki.market.yandex.ru/product/123?hid=2&offerid=abcd", url);
    }

    @Test
    public void geoLink() {
        ModelV2 model = new ModelV2();
        model.setId(123);
        model.setName("abc");

        ContextHolder.update(
            ctx -> ctx.setPpList(IntLists.singleton(12))
        );

        String url = marketUrls.geo(model);
        assertEquals("http://market.yandex.ru/product--abc/123/geo?pp=12", url);
    }

    @Test
    public void blueGeoLink() {
        ModelV2 model = new ModelV2();
        model.setId(123);
        model.setName("abc");

        ContextHolder.update(
                ctx -> ctx.setPpList(IntLists.singleton(12))
        );
        setBlue(true);

        String url = marketUrls.geo(model);
        assertNull(url);
    }

    @Test
    public void specLink() {
        ModelV2 model = new ModelV2();
        model.setId(123);
        model.setName("abc");

        ContextHolder.update(
            ctx -> ctx.setPpList(IntLists.singleton(12))
        );

        String url = getOrNull(marketUrls.modelSpecifications(model).uri(), URI::toASCIIString);
        assertEquals("http://market.yandex.ru/product--abc/123/spec?pp=12", url);
    }

    @Test
    public void blueSpecLink() {
        ModelV2 model = new ModelV2();

        OfferV2 offer = new OfferV2();
        offer.setName("abc");
        offer.setSku(String.valueOf(123));

        model.setOffer(offer);

        ContextHolder.update(
                ctx -> ctx.setPpList(IntLists.singleton(12))
        );
        setBlue(true);

        String url = getOrNull(marketUrls.modelSpecifications(model).uri(), URI::toASCIIString);
        assertEquals("http://pokupki.market.yandex.ru/product/abc/123/spec?pp=12", url);
    }

    @Test
    public void modelOpinionAddLink() {
        ModelV2 model = new ModelV2();
        model.setId(123);
        model.setName("abc");

        ContextHolder.update(
            ctx -> ctx.setPpList(IntLists.singleton(12))
        );

        String url = marketUrls.modelOpinionsAdd(((Model) model).getId(), ((Model) model).getSlug());
        assertEquals("http://market.yandex.ru/product--abc/123/reviews/add?pp=12", url);
    }

    @Test
    public void blueModelOpinionAddLink() {
        ModelV2 model = new ModelV2();
        model.setId(123);
        model.setName("abc");

        ContextHolder.update(
                ctx -> ctx.setPpList(IntLists.singleton(12))
        );
        setBlue(true);

        String url = marketUrls.modelOpinionsAdd(((Model) model).getId(), ((Model) model).getSlug());
        assertEquals("http://pokupki.market.yandex.ru/product/abc/123/reviews/add?pp=12", url);
    }

    @Test
    public void unknown() {
        String url = marketUrls.unknown("http://market.yandex.ru/product/1", null);
        assertEquals("http://market.yandex.ru/product/1", url);
    }

    @Test
    public void beruUnknown() {
        String url = marketUrls.unknown("http://pokupki.market.yandex.ru/product/1", null);
        assertEquals("http://pokupki.market.yandex.ru/product/1", url);
    }

    @Test
    public void beruUnknownOldFrontend() {
        String url = marketUrls.unknown("http://beru.ru/product/1", null);
        assertEquals("http://beru.ru/product/1", url);
    }

    @Test
    public void noMarketUnknown() {
        String url = marketUrls.unknown("http://anything.yandex.ru/product/1", null);
        assertNull(url);
    }

    @Test
    public void unknownUrlWithoutBlacklist() {
        String url = marketUrls.unknown(
                "http://market.yandex.ru/product/123?utm_content=ss&abc=def&utm_medium=dd&xyz=123",
                null
        );

        URI uri = Urls.toUri(url);
        Assert.assertThat(
                uri,
                URIMatcher.uri(
                        URIMatcher.host("market.yandex.ru"),
                        URIMatcher.hasQueryParams("abc", "def"),
                        URIMatcher.hasQueryParams("xyz", "123"),
                        URIMatcher.hasQueryParams("utm_content", "ss"),
                        URIMatcher.hasQueryParams("utm_medium", "dd")
                )
        );
    }

    @Test
    public void unknownUrlWithBlacklist() {
        String url = marketUrls.unknown(
                "http://market.yandex.ru/product/123?utm_content=ss&abc=def&utm_medium=dd&xyz=123",
                Urls.UTM_PARAMS
        );

        URI uri = Urls.toUri(url);
        Assert.assertThat(
                uri,
                URIMatcher.uri(
                        URIMatcher.host("market.yandex.ru"),
                        URIMatcher.hasQueryParams("abc", "def"),
                        URIMatcher.hasQueryParams("xyz", "123"),
                        URIMatcher.hasNoQueryParams("utm_content"),
                        URIMatcher.hasNoQueryParams("utm_medium")
                )
        );
    }

    @Test
    public void beruSupplierLink() {
        OfferV2 offer = new OfferV2();
        offer.setWareMd5("LP3e5x_dp4TX7pRS0n9x9g");
        setBlue(true);

        String url = marketUrls.supplier(Collections.singletonList(offer));
        assertEquals("http://pokupki.market.yandex.ru/suppliers/info-by-offers?offerId=LP3e5x_dp4TX7pRS0n9x9g", url);

    }

    @Test
    public void beruSupplierLinkWithMultipleOffers() {
        setBlue(true);

        OfferV2 offer1 = new OfferV2(), offer2 = new OfferV2();
        offer1.setWareMd5("123test321");
        offer2.setWareMd5("123asdf321");

        List<OfferV2> offers = new ArrayList<>();
        offers.add(offer1);
        offers.add(offer2);

        String url = marketUrls.supplier(offers);
        assertEquals("http://pokupki.market.yandex.ru/suppliers/info-by-offers?offerId=123test321&offerId=123asdf321", url);
    }

    @Test
    public void beruSupplierLinkWithEmptyWareMd5() {
        setBlue(true);

        List<OfferV2> offers = new ArrayList<>();
        offers.add(new OfferV2());

        String url = marketUrls.supplier(offers);
        assertNull(url);
    }

    @Test
    public void beruSupplierLinkWithEmptyOffersList() {
        setBlue(true);

        String url = marketUrls.supplier(Collections.emptyList());
        assertNull(url);
    }

    //https://st.yandex-team.ru/MARKETAPI-6280
    @Test
    public void fixOnStockDuplicatesMarketApi6280WithOnStockFalse() {
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withFilterParams(Collections.singletonList(new Criterion("-3", "0", Criterion.CriterionType.API_FILTER)))
                .uri();

        Assert.assertThat(uri, URIMatcher.hasSingleQueryParam("onstock", "0"));
        Assert.assertThat(uri, URIMatcher.hasNoQueryParams("onstock", "1"));
    }

    //https://st.yandex-team.ru/MARKETAPI-6280
    @Test
    public void fixOnStockDuplicatesMarketApi6280WithOnStockNotSetAndFiltersEmpty() {
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withFilterParams(Collections.singletonList(new Criterion("-8", "abc", Criterion.CriterionType.API_FILTER)))
                .uri();

        Assert.assertThat(uri, URIMatcher.hasSingleQueryParam("text", "abc"));
        Assert.assertThat(uri, URIMatcher.hasSingleQueryParam("onstock", "1"));
        Assert.assertThat(uri, URIMatcher.hasNoQueryParams("onstock", "0"));
    }

    //https://st.yandex-team.ru/MARKETAPI-6280
    @Test
    public void fixOnStockDuplicatesMarketApi6280WithOnStockNotSetFalseAndFiltersEmpty() {
        URI uri = urlParamsFactory.create(CatalogUrlParams.class)
                .withFilterParams(Collections.singletonList(Criteria.onStock(false)))
                .uri();

        Assert.assertThat(uri, URIMatcher.hasSingleQueryParam("onstock", "0"));
        Assert.assertThat(uri, URIMatcher.hasNoQueryParams("onstock", "1"));
    }


    private void setRegionId(int regionId) {
        ContextHolder.get().getRegionInfo().setRawRegionId(regionId);
    }

    private void setBlue(boolean useNewBlueFrontend) {
        Client client = new Client();
        client.setType(Client.Type.MOBILE);

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setUseNewBlueFrontend(useNewBlueFrontend);
            ctx.setClientVersionInfo(
                    new KnownMobileClientVersionInfo(
                            Platform.IOS,
                            DeviceType.TABLET,
                            new SemanticVersion(1, 0, 0)
                    )
            );
        });
    }

    private void setLinkTo(FrontendType frontendType) {
        ContextHolder.update(ctx -> ctx.setFrontendType(frontendType));
    }

}
