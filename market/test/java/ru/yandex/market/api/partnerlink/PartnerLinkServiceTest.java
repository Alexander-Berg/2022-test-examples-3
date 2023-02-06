package ru.yandex.market.api.partnerlink;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.api.category.Category;
import ru.yandex.market.api.category.CategoryService;
import ru.yandex.market.api.common.client.MarketTypeResolver;
import ru.yandex.market.api.common.url.MarketPageType;
import ru.yandex.market.api.domain.Vendor;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.BaseVendorV2;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.ClckHttpClient;
import ru.yandex.market.api.internal.cataloger.CatalogerService;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.report.InternalSkuResult;
import ru.yandex.market.api.internal.templator.TemplatorClient;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.model.ModelServiceImpl;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.ApiClientServiceMem;
import ru.yandex.market.api.sku.SkuService;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.api.vendor.VendorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partnerlink.PartnerLinkService.SEARCH_RESULT;

@SuppressWarnings("deprecation")
public class PartnerLinkServiceTest extends BaseTest {
    public static final String HTTP_EXAMPLE_RU = "http://example.ru";

    PartnerLinkService partnerLinkService;
    TemplatorClient templatorClient;
    @Inject
    private ApiClientServiceMem apiClientServiceMem;
    private Document document;

    @Before
    public void setUp() throws Exception {
        ModelV2 model = new ModelV2();
        model.setName("example");
        ModelV2 model2 = new ModelV2();
        model2.setName("example2");

        Sku sku = new Sku();
        sku.setId("100210864686");
        sku.setName("example");
        InternalSkuResult internalSkuResult = new InternalSkuResult(sku, Collections.emptyList(),
                Collections.emptyList());
        Sku sku2 = new Sku();
        sku2.setId("11111111");
        sku2.setName("example2");
        InternalSkuResult internalSkuResult2 = new InternalSkuResult(sku2, Collections.emptyList(),
                Collections.emptyList());
        Image image = new Image();
        image.setUrl(HTTP_EXAMPLE_RU);
        model.setPhoto(image);

        Category category = new Category();
        category.setName("electronic");

        Promise<Vendor> vendorPromise = Futures.newImmediatePromise();
        BaseVendorV2 baseVendorV2 = new BaseVendorV2();
        baseVendorV2.setId(10714190);
        baseVendorV2.setPicture(HTTP_EXAMPLE_RU);
        baseVendorV2.setName("apple");
        vendorPromise.trySuccess(baseVendorV2);

        OfferV2 offer = new OfferV2();
        offer.setName("компьютер");
        ShopInfoV2 shop = new ShopInfoV2();
        shop.setName("магазин компьютеров");
        Image imageOffer = new Image();
        imageOffer.setUrl("http://some-photo.ru");
        offer.setShop(shop);
        offer.setPhoto(imageOffer);

        ModelService modelService = mock(ModelServiceImpl.class);
        ClckHttpClient clckHttpClient = mock(ClckHttpClient.class);
        CategoryService categoryService = mock(CategoryService.class);
        CatalogerService catalogerService = mock(CatalogerService.class);
        VendorService vendorService = mock(VendorService.class);
        MarketTypeResolver marketTypeResolver = mock(MarketTypeResolver.class);
        SkuService skuService = mock(SkuService.class);
        templatorClient = mock(TemplatorClient.class);
        OfferService offerService = mock(OfferService.class);

        when(modelService.getModel(14260832, Collections.emptyList(), GenericParams.DEFAULT)).thenReturn(Pipelines.startWithValue(model));
        when(modelService.getModel(533458036, Collections.emptyList(), GenericParams.DEFAULT)).thenReturn(Pipelines.startWithValue(model2));
        when(vendorService.getVendor(10714190, Collections.emptyList())).thenReturn(vendorPromise);
        Map<String, OfferV2> offerV2Map = new HashMap<>();
        offerV2Map.put("5DNVXu83mLyU3PxBrYJCxg", offer);
        doReturn(Pipelines.startWithValue(offerV2Map)).when(offerService).getOffersV2(anyListOf(String.class), any(), any());

        when(categoryService.isCategoryExists(54440)).thenReturn(true);
        when(categoryService.isCategoryExists(56034)).thenReturn(true);
        when(categoryService.isCategoryExists(80155)).thenReturn(true);
        when(categoryService.isCategoryExists(80542)).thenReturn(true);
        when(categoryService.isCategoryExists(23303)).thenReturn(false);
        when(categoryService.getCategory(54440, Collections.emptyList())).thenReturn(category);
        when(categoryService.getCategory(56034, Collections.emptyList())).thenReturn(category);
        when(categoryService.getCategory(80155, Collections.emptyList())).thenReturn(category);
        when(categoryService.getCategory(80542, Collections.emptyList())).thenReturn(category);
        when(modelService.getModel(14260832, Collections.singletonList(ModelInfoField.PHOTO), GenericParams.DEFAULT)).thenReturn(Pipelines.startWithValue(model));
        when(skuService.getSku("100210864686", null, false, Collections.emptyList(), GenericParams.DEFAULT))
                .thenReturn(Pipelines.startWithValue(internalSkuResult));
        when(skuService.getSku("11111111", null, false, Collections.emptyList(), GenericParams.DEFAULT))
                .thenReturn(Pipelines.startWithValue(internalSkuResult2));

        when(clckHttpClient.clckSimple(any())).thenReturn(Futures.newSucceededFuture("http://short_url.ru"));

        partnerLinkService = spy(new PartnerLinkService(
                new HashSet<>(Stream.of("text", "promo-type", "shoppromoid").collect(Collectors.toSet())),
                20000,
                clckHttpClient, modelService, categoryService,
                vendorService, templatorClient, catalogerService, marketTypeResolver, skuService, apiClientServiceMem,
                offerService));
        document = Jsoup.parse("<html lang=\"ru\">\n" +
                "<head>\n" +
                "    <title itemprop=\"headline\">Some page title</title>\n" +
                "        <meta property=\"og:title\" content=\"Заголовок из og:title\"/>\n" +
                "        <meta property=\"og:image\"\n" +
                "              content=\"http://url-from-og-image\"/></head><body/></html>\n"
        );
        doReturn(Pipelines.startWithValue(document)).when(partnerLinkService).getDoc(any());
    }

    @Test
    public void testPageNameModel() {
        Future<String> pageName = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/product--robot-pylesos-xiaomi-mi" +
                "-robot-vacuum-cleaner" +
                "/14260832", document);
        assertThat(Futures.waitAndGet(pageName), is("example"));
    }

    @Test
    public void testPageNameModelEmpty() {
        Future<String> pageName = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/product--robot-pylesos-xiaomi-mi" +
                "-robot-vacuum-cleaner" +
                "/ads", document);
        assertThat(Futures.waitAndGet(pageName), is("Заголовок из og:title"));
    }

    @Test
    public void testPokupkiPageNameModel() {
        Future<String> pageName = partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/product/smartfon-apple-iphone-x-64gb-seryi-kosmos-mqac2ru-a/100210864686", document);
        assertThat(Futures.waitAndGet(pageName), is("example"));
    }

    @Test
    public void testPokupkiPageNameModelEmpty() {
        Future<String> pageName = partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/product/smartfon-apple-iphone-x-64gb-seryi-kosmos-mqac2ru-a/asd", document);
        assertThat(Futures.waitAndGet(pageName), is("Заголовок из og:title"));
    }

    @Test
    public void testPageNameMain() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex.ru/", document));
        assertThat(pageName, is(PartnerLinkService.MARKET));
    }

    @Test
    public void testPokupkiPageNameMain() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://pokupki.market.yandex.ru", document));
        assertThat(pageName, is(PartnerLinkService.MARKET));
    }

    @Test
    public void testPageNameCategory() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex" +
                ".ru/catalog--elektronika/54440", document));
        assertThat(pageName, is("electronic"));
    }

    @Test
    public void testPageNameCategoryEmpty() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex" +
                ".ru/catalog--elektronika/asd", document));
        assertThat(pageName, is("Заголовок из og:title"));
    }

    @Test
    public void testCreateLinkWithUnsafeCharacters() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/search?text=аккумуляторы%20makita&cvredirect=0&track=redirbarup&local-offers-first=0", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getPageName(),
                is("аккумуляторы makita — Результаты поиска"));
    }

    @Test
    public void testGetNavigationCategory() {
        String name = Futures.getNowSafe(partnerLinkService.getCategoryName(54440, document));
        assertThat(name, is("electronic"));
    }

    @Test
    public void testGetCategory() {
        String name = Futures.getNowSafe(partnerLinkService.getCategoryName(23303, document));
        assertThat(name, is(nullValue()));
    }

    @Test
    public void testPokupkiPageNameCategory() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/catalog/elektronika/80155?hid=198119", document));
        assertThat(pageName, is("electronic"));
    }

    @Test
    public void testPokupkiPageNameCategoryEmpty() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/catalog/elektronika/assdhid=198119", document));
        assertThat(pageName, is("Заголовок из og:title"));
    }

    @Test
    public void testPokupkiPageNameCategoryList() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/catalog/smartfony-i-mobilnye-telefony/80542/list?hid=91491", document));
        assertThat(pageName, is("electronic"));
    }

    @Test
    public void testPokupkiPageNameCategoryListEmpty() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/catalog/smartfony-i-mobilnye-telefony/sad/list?hid=91491", document));
        assertThat(pageName, is("Заголовок из og:title"));
    }

    @Test
    public void testPageNameCategoryList() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex" +
                ".ru/catalog--umnye-chasy-i-braslety" +
                "/56034/list?hid=10498025", document));
        assertThat(pageName, is("electronic"));
    }

    @Test
    public void testPageNameCategoryListEmpty() {
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex" +
                ".ru/catalog--umnye-chasy-i-braslety" +
                "/sdf/list?hid=10498025", document));
        assertThat(pageName, is("Заголовок из og:title"));
    }

    @Test
    public void testPageNameBrand() {
        Future<String> pageName = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/brands--acuvue/10714190", document);
        assertThat(Futures.waitAndGet(pageName), is("apple"));
    }

    @Test
    public void testPageNameBrandEmpty() {
        Future<String> pageName = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/brands--acuvue/asdasd", document);
        assertThat(Futures.waitAndGet(pageName), is("Заголовок из og:title"));
    }

    @Test
    public void testPageNameSearchResult() {
        Future<String> pageName = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/search?text=аккумуляторы%20makita&cvredirect=0&track=redirbarup&local-offers-first=0",
                document);
        assertThat(Futures.waitAndGet(pageName), is("аккумуляторы makita" + SEARCH_RESULT));
    }

    @Test
    public void testPageNameSearchResultEmpty() {
        Future<String> pageName = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/search?local-offers-first=0", document);
        assertThat(Futures.waitAndGet(pageName), is("Заголовок из og:title"));
    }

    @Test
    public void testPokupkiPageNameSearchResult() {
        Future<String> pageName = partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/search?cvredirect=2&text=аккумуляторы%20makita", document);
        assertThat(Futures.waitAndGet(pageName), is("аккумуляторы makita" + SEARCH_RESULT));
    }

    @Test
    public void testPokupkiPageNameSearchResultEmpty() {
        Future<String> pageName = partnerLinkService.getPageName("https://pokupki.market.yandex" +
                ".ru/search", document);
        assertThat(Futures.waitAndGet(pageName), is("Заголовок из og:title"));
    }

    @Test
    public void testPageNamePromo() {
        when(templatorClient.getPromoLanding(any(), any()))
                .thenReturn(Pipelines.startWithValue("Эльдорадо на Я. Маркете"));
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex.ru/promo/eldorado", document));
        assertThat(pageName, is("Эльдорадо на Я. Маркете"));
    }

    @Test
    public void testPageNamePromoEmpty() {
        when(templatorClient.getPromoLanding(any(), any()))
                .thenReturn(Pipelines.startWithValue("Эльдорадо на Я. Маркете"));
        String pageName = Futures.getNowSafe(partnerLinkService.getPageName("https://market.yandex.ru/promo/", document));
        assertThat(pageName, is("Заголовок из og:title"));
    }

    @Test
    public void testPageNameJournal() {
        when(templatorClient.getJournal(any(), any())).thenReturn(Pipelines.startWithValue("Капсульная" +
                " кофемашина Nespresso Vertuo Plus D"));
        Future<String> future = partnerLinkService.getPageName("https://market.yandex" +
                ".ru/journal/overview/kapsulnaja-kofemashina-nespresso-vertuo-plus-d", document);
        assertThat(Futures.waitAndGet(future), is("Заголовок из og:title"));
    }

    @Test
    public void testPagePhotoModel() {
        Future<String> photoFuture = partnerLinkService.getProductPhoto("https://market.yandex" +
                ".ru/product--robot-pylesos-xiaomi-mi" +
                "-robot-vacuum-cleaner" +
                "/14260832", document);
        assertThat(Futures.getNowSafe(photoFuture), is(HTTP_EXAMPLE_RU));
    }

    @Test
    public void testPagePhotoModelEmpty() {
        Future<String> photoFuture = partnerLinkService.getProductPhoto("https://market.yandex" +
                ".ru/product--robot-pylesos-xiaomi-mi" +
                "-robot-vacuum-cleaner" +
                "/asd", document);
        assertThat(Futures.getNowSafe(photoFuture), is("http://url-from-og-image"));
    }

    @Test
    public void testPagePhotoCategory() {
        Future<String> photoFuture = partnerLinkService.getProductPhoto("https://market.yandex" +
                ".ru/brands--acuvue/10714190", document);
        assertThat(Futures.getNowSafe(photoFuture), is(HTTP_EXAMPLE_RU));
    }

    @Test
    public void testPagePhotoCategoryEmpty() {
        Future<String> photoFuture = partnerLinkService.getProductPhoto("https://market.yandex" +
                ".ru/brands--acuvue/asd", document);
        assertThat(Futures.getNowSafe(photoFuture), is("http://url-from-og-image"));
    }

    @Test
    public void testCreateLink() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru?clid=239890&pp=900&mclid=1003&distr_type=7"));

    }

    @Test
    public void testCreateLinkWithEmptyClid() {
        Client client = new Client();
        client.setUserLogin("login3");

        ContextHolder.update(ctx -> ctx.setClient(client));
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru", "", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru?clid=8888888&pp=900&mclid=1003&distr_type=7"));
    }

    @Test
    public void testParamFromWhitelist() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/search?text=abc", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru/search?text=abc&clid=239890&pp=900&mclid=1003&distr_type=7"));

    }

    @Test
    public void testMultiValueParam() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/?promo-type=1,2", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru/?promo-type=1,2&clid=239890&pp=900&mclid=1003&distr_type=7"));
    }

    @Test
    public void testIgnoreClidInUrl() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru?clid=123", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru?clid=239890&pp=900&mclid=1003&distr_type=7"));
    }

    @Test
    public void testMarketDeals() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/deals?clid=123", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/deals?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_DEALS));
        assertThat(resp.getPageName(), is("Список актуальных скидок и акций"));
        assertThat(resp.getProductPhoto(), is(StringUtils.EMPTY));
    }

    @Test
    public void testMarketProductReviews() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/reviews", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/reviews?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getPageName(), is("example - отзывы"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_PRODUCT_REVIEWS));
        assertThat(resp.getProductPhoto(), is(HTTP_EXAMPLE_RU));
    }

    @Test
    public void testMarketProductSpec() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/spec", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/spec?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getPageName(), is("example - характеристики"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_PRODUCT_SPEC));
        assertThat(resp.getProductPhoto(), is(HTTP_EXAMPLE_RU));
    }

    @Test
    public void testMarketProductQuestions() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/questions", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/questions?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getPageName(), is("example - вопросы покупателей"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_PRODUCT_QUESTIONS));
        assertThat(resp.getProductPhoto(), is(HTTP_EXAMPLE_RU));
    }

    @Test
    public void testMarketProductArticles() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/articles", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832/articles?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_PRODUCT_ARTICLES));
        assertThat(resp.getPageName(), is("example - обзоры"));
        assertThat(resp.getProductPhoto(), is(HTTP_EXAMPLE_RU));
    }

    @Test
    public void testMarketProductOffer() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/offer/5DNVXu83mLyU3PxBrYJCxg", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/offer/5DNVXu83mLyU3PxBrYJCxg?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_PRODUCT_OFFER));
        assertThat(resp.getPageName(), is("компьютер - магазин компьютеров"));
        assertThat(resp.getProductPhoto(), is("http://some-photo.ru"));
    }

    @Test
    public void testMarketLive() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/live", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/live?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_LIVE));
        assertThat(resp.getPageName(), is("Live-трансляции (стримы)"));
        assertThat(resp.getProductPhoto(), is("http://url-from-og-image"));
    }

    @Test
    public void testMarketWishlist() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/my/wishlist", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/my/wishlist?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_WISHLIST));
        assertThat(resp.getPageName(), is("Избранное"));
        assertThat(resp.getProductPhoto(), is(StringUtils.EMPTY));
    }

    @Test
    public void testMarketSpecial() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/special/express", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/special/express?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_SPECIAL));
        assertThat(resp.getPageName(), is("Заголовок из og:title"));
        assertThat(resp.getProductPhoto(), is("http://url-from-og-image"));
    }

    @Test
    public void testMarketCart() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/cart", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(), is("https://market.yandex.ru/cart?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getSearchType(), is(MarketPageType.MARKET_CART));
        assertThat(resp.getPageName(), is("Корзина"));
        assertThat(resp.getProductPhoto(), is(StringUtils.EMPTY));
    }

    @Test
    public void testUnsupportedMarketPage() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.ru/special/huggies-1810", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru/special/huggies-1810?clid=239890&pp=900&mclid=1003&distr_type=7"));
        assertThat(resp.getProductPhoto(), is("http://url-from-og-image"));
        assertThat(resp.getPageName(), is("Заголовок из og:title"));
    }

    @Test
    public void testHashSign() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink(
                        "https://market.yandex.ru/special/blue-set-landing?shopPromoId=%2312773", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.ru/special/blue-set-landing?shopPromoId=%2312773&clid=239890&pp=900&mclid=1003&distr_type=7"));

    }

    @Test
    public void testCreateLinkKz() {
        Future<PartnerLinkResponse> linkFuture =
                partnerLinkService.createLink("https://market.yandex.kz", "239890", null);
        PartnerLinkResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getUrl(),
                is("https://market.yandex.kz?clid=239890&pp=900&mclid=1003&distr_type=7"));

    }

    @Test
    public void testCreateBatchOfLinksSimple() {
        Future<PartnerLinkBatchResponse> linkFuture =
                partnerLinkService.createBatchOfLinks(
                        ImmutableList.of("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832",
                                "https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/533458036"
                                ), "239890", null, false, false);
        PartnerLinkBatchResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getItems(), iterableWithSize(2));
        assertThat(resp.getItems(), containsInAnyOrder(
                new PartnerLinkItem("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832?clid=239890&pp=900&mclid=1003&distr_type=7", null, null),
                new PartnerLinkItem("https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/533458036?clid=239890&pp=900&mclid=1003&distr_type=7" , null, null)));
    }

    @Test
    public void testCreateBatchOfLinksShortUrls() {
        Future<PartnerLinkBatchResponse> linkFuture =
                partnerLinkService.createBatchOfLinks(
                        ImmutableList.of("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832",
                                "https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/533458036"
                        ), "239890", null, false, true);
        PartnerLinkBatchResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getItems(), iterableWithSize(2));
        assertThat(resp.getItems(), containsInAnyOrder(
                new PartnerLinkItem("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832?clid=239890&pp=900&mclid=1003&distr_type=7", "http://short_url.ru", null),
                new PartnerLinkItem("https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/533458036?clid=239890&pp=900&mclid=1003&distr_type=7" , "http://short_url.ru", null)));
    }

    @Test
    public void testCreateBatchOfLinksComplex() {
        Future<PartnerLinkBatchResponse> linkFuture =
                partnerLinkService.createBatchOfLinks(
                        ImmutableList.of("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832",
                                "https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/533458036"
                        ), "239890", null, true, true);
        PartnerLinkBatchResponse resp = Futures.waitAndGet(linkFuture);
        assertThat(resp.getItems(), iterableWithSize(2));
        assertThat(resp.getItems(), containsInAnyOrder(
                new PartnerLinkItem("https://market.yandex.ru/product--smartfon-apple-iphone-12-pro-128gb/14260832?clid=239890&pp=900&mclid=1003&distr_type=7", "http://short_url.ru", "example"),
                new PartnerLinkItem("https://market.yandex.ru/product--konvektor-nobo-ntl4s-05/533458036?clid=239890&pp=900&mclid=1003&distr_type=7" , "http://short_url.ru", "example2")));
    }
}
