package ru.yandex.market.api.internal.report.parsers.json.offer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.OfferPrice;
import ru.yandex.market.api.domain.v2.BundleSettings;
import ru.yandex.market.api.domain.v2.DeliveryConditionsV2;
import ru.yandex.market.api.domain.v2.DeliveryOptionV2;
import ru.yandex.market.api.domain.v2.DeliveryServiceV2;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.domain.v2.ImageDensity;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.PickupDeliveryOptionV2;
import ru.yandex.market.api.domain.v2.RegionField;
import ru.yandex.market.api.domain.v2.RegionV2;
import ru.yandex.market.api.domain.v2.ShopInfoFieldV2;
import ru.yandex.market.api.domain.v2.ShopLogo;
import ru.yandex.market.api.domain.v2.ThumbnailWithDensities;
import ru.yandex.market.api.domain.v2.specifications.InternalSpecification;
import ru.yandex.market.api.domain.v2.specifications.Specifications;
import ru.yandex.market.api.domain.v2.specifications.UsedParam;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.geo.domain.RegionType;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.PP;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.json.OfferV2JsonParser;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.matchers.FiltersMatcher;
import ru.yandex.market.api.matchers.RegionV2Matcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollectionOf;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.ApiMatchers.map;
import static ru.yandex.market.api.matchers.OfferMatcher.manufactCountries;
import static ru.yandex.market.api.matchers.OfferMatcher.offer;
import static ru.yandex.market.api.matchers.OfferMatcher.offerId;
import static ru.yandex.market.api.matchers.OfferMatcher.sku;
import static ru.yandex.market.api.matchers.OfferMatcher.skuType;
import static ru.yandex.market.api.matchers.OfferMatcher.wareMd5;

/**
 * Created by apershukov on 06.12.16.
 */
@WithContext
@WithMocks
public class OfferV2JsonParserTest extends BaseTest {

    private static final int REGION_ID = 54;
    private static final String SPEC_TYPE = "spec";

    private DeliveryService deliveryService = new DeliveryService();

    @Mock
    private CurrencyService currencyService;
    @Mock
    private GeoRegionService geoRegionService;
    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Inject
    private MarketUrls marketUrls;

    @Mock
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Before
    public void setUp() {
        when(geoRegionService.getSafeRegion(eq(REGION_ID), eq(singleton(RegionField.DECLENSIONS)), any(), eq(RegionVersion.V2)))
            .thenReturn(new RegionV2(REGION_ID, "Екатеринбург"));

        Mockito
            .when(
                geoRegionService.getRegion(
                    Mockito.anyInt(),
                    Mockito.anyCollection(),
                    Mockito.any(RegionVersion.class)
                )
            )
            .thenReturn(
                new GeoRegion(1, null, null, null, null)
            );

        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void testParseOfferWithCpc() {
        OfferV2 offer = parse();

        assertEquals("LP3e5x_dp4TX7pRS0n9x9g", offer.getWareMd5());
        assertEquals("DwjATgCHzD5F-Fm-j7ytQQpvZqn5eVnz4iLmpcZxxRbs5zloirDf2hUM79esXC5m", offer.getCpc());
        assertEquals(false, offer.getPaymentOptions().getCanPayByCard());
    }

    @Test
    public void testParseWithMultiplePictures() {
        OfferV2 offer = parse();

        assertEquals("http://0.cs-ellpic01gt.yandex.ru/picture1_190x250.jpg", offer.getPhoto().getUrl());

        List<? extends Image> photos = offer.getPhotos();
        assertEquals(3, photos.size());
        assertEquals("http://0.cs-ellpic01gt.yandex.ru/picture1_190x250.jpg", photos.get(0).getUrl());
        assertEquals("http://0.cs-ellpic01gt.yandex.ru/picture2.jpg", photos.get(1).getUrl());
        assertEquals("http://0.cs-ellpic01gt.yandex.ru/picture3.jpg", photos.get(2).getUrl());

        List<? extends Image> previewPhotos = offer.getPreviewPhotos();
        assertEquals(2, previewPhotos.size());
        assertEquals("http://0.cs-ellpic01gt.yandex.ru/picture1_190x250.jpg", previewPhotos.get(0).getUrl());
        assertEquals("http://0.cs-ellpic01gt.yandex.ru/picture3_90x120.jpg", previewPhotos.get(1).getUrl());
    }

    @Test
    public void shouldParseGlobal() {
        OfferV2 offer = parse();
        assertTrue(((DeliveryV2) offer.getDelivery()).getGlobal());
    }

    @Test
    public void testParseBundleSettingsForMobile() {
        ContextHolder.get().setClient(new Client() {{
            setType(Type.MOBILE);
        }});
        ContextHolder.get().setPpList(IntLists.singleton(PP.DEFAULT));

        OfferV2 offer = parse();

        BundleSettings settings = offer.getBundleSettings();

        assertNotNull(settings);
        assertEquals(1, settings.getQuantityLimit().getMinimum());
        assertEquals(2, settings.getQuantityLimit().getStep());
    }

    @Test
    public void testSkipBundleSettingsForReqular() {
        OfferV2 offer = parse();
        assertNull(offer.getBundleSettings());
    }

    @Test
    public void testParseDelivery() {

        ContextHolder.get().setVersion(Version.V2_0_7);
        OfferV2 offer = parse();

        DeliveryV2 delivery = (DeliveryV2) offer.getDelivery();
        assertNotNull(delivery);

        checkDeliveryOptions(delivery.getOptions());

        verify(geoRegionService, times(2))
            .getSafeRegion(eq(REGION_ID), eq(singleton(RegionField.DECLENSIONS)), any(), eq(RegionVersion.V2));
        assertNotNull(delivery.getUserRegion());
        assertNotNull(delivery.getShopRegion());
    }

    private void checkDeliveryOptions(List<DeliveryOptionV2> options) {
        assertNotNull(options);
        assertEquals(2, options.size());

        DeliveryOptionV2 option0 = options.get(0);
        assertTrue(option0.isDefaultOption());

        assertNotNull(option0.getService());
        DeliveryServiceV2 service0 = option0.getService();
        assertEquals(1, service0.getId());

        assertNotNull(option0.getConditions());
        DeliveryConditionsV2 condition0 = option0.getConditions();
        assertNull(condition0.getDeliveryIncluded());
        assertEquals(Integer.valueOf(16), condition0.getOrderBefore());

        OfferPrice price0 = condition0.getPrice();
        assertEquals("300", price0.getValue());

        DeliveryOptionV2 option1 = options.get(1);
        assertFalse(option1.isDefaultOption());
        assertNull(option1.getService());

        DeliveryConditionsV2 condition1 = option1.getConditions();
        assertTrue(condition1.getDeliveryIncluded());
        assertNull(condition1.getOrderBefore());
        assertEquals(Integer.valueOf(2), condition1.getDaysFrom());
        assertEquals(Integer.valueOf(7), condition1.getDaysTo());

        OfferPrice price2 = condition1.getPrice();
        assertEquals("0", price2.getValue());
    }

    @Test
    public void testParseShopOfferId() {
        OfferV2 offer = parse();
        assertNotNull(offer.getShopOfferId());
        assertEquals(431760, offer.getShopOfferId().getFeedId());
        assertEquals("24", offer.getShopOfferId().getOfferId());
    }

    @Test
    public void shouldParsePromocodeInfo() {
        OfferV2 offer = parse();
        assertThat(offer.getPromocode(), is(true));
    }

    @Test
    public void shouldParseWarnings() {
        OfferV2 offer = parse();
        assertThat(offer.getWarning(), is("Возрастное ограничение"));
        assertThat(offer.getWarnings().get(0).getCode(), is("adult"));
    }

    @Test
    public void shouldParseOutletCount() {
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        assertEquals(Integer.valueOf(1), offer.getOutletCount());
    }

    @Test
    public void shouldParsePickupCount() {
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        assertEquals(Integer.valueOf(3), offer.getPickupCount());
    }

    @Test
    public void shouldParseLocalStoreCount() {
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        assertEquals(Integer.valueOf(2), offer.getLocalStoreCount());
    }

    @Test
    public void shouldParseOutletUrl() {
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        assertEquals("http://market-click2.yandex.ru/redir/338FT8NBgRsF_Zj3BDwyRcEutL0x6JDd4MVL6TK2E0H7PXmd2mUtT7mxPFsPRZUJBKTvOygQENY9CcIQiKx3ivMDys6ltBgwlccQ17oEYuy7ns7EZOE4qmKu3Gg6r_rlSJf0hTcoh_82G0tTCrpbKaOSwgifY314lZ1VsVt2smZEucM27pS__q1SsAROaj46UFddQKvUQZd6q1wG1eNTIO-KZhf0MPp8wRFyd-RsSjbeUZ5Jg2_gkJEglGIQtiDKd-Y0-7snf7JAIeBuiwDduiJ1hszx5PHOiWo4gZapinP1GuFK4mez5YqwNEyH28zxMoqUGooH325jnk_FVQTRV6f15bB06xvnEt2BrTEPbstU5_VjSiZ3BhmkyOZalWlVYt_K1oUmMmgAoK1wUKFQIUeO2NEqj6SJDPVcuhXdyomwT6dHyD6hkKqPOc-lpV_GYE73-nLUKXNW7S2kz2CvduFGntdV2KWkQkriixxKDuqAPp-wJNv48sjiQ9O19j5ofwopYPEhwDAI-4rs7_cGtmaJG13854mX2_W6Uwvy91PhWIiRPlvQFCDDk9L063axCLflukKPQp7S2bZEi7VECB7FTRGoPUZqXi8vMImcorNgtqZhh7hFowBpDfvR_qCTg85YTvDA30KtqPE-Um41RI33d-CfzlCKbgYyQTISNaJSCHU4aWAWPaZ2jw_35TYLa8Va9TozsTBeEu0EjDsYU4LcrHAcqqYhMEfgvBUDA-dKpw7TD7yCgoGQjJWqKS-lAJ3FcA2KwqplETaOlAR8Ojl5SbeP4K_m?data=41WTYndNxdlaG-5xTfn6okY8p8Pwg5EL5PtKPUoATUEvlEoXLlDZ2bvDfds-sUcJ5VvvbvvUrhojXUySvgxksYZlt_cYH9e6FFZhI2kR1t47g8_yQVWKYW9T5YUxEqMWQWj2arcDYLhI3dWDi87uQ2PjW6CuY0NpLxuQpP9v2QY,&b64e=1&sign=d355fa3b69b0b834912945eef2dfd08c&keyno=1", offer.getOutletUrl());
    }

    @Test
    public void shouldParseDeliveryOption0Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        DeliveryOptionV2 deliveryOption = getDeliveryOption(offer, 0);
        assertEquals("Не указаны дни доставки. Не можем сформировать сроки.", "на&nbsp;заказ", deliveryOption.getBrief());
    }

    @Test
    public void shouldParseDeliveryOption1Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        DeliveryOptionV2 deliveryOption = getDeliveryOption(offer, 1);
        assertEquals("Указаны даты доставки", "2-7 дней", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption0Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 0);
        assertEquals("завтра при заказе до 12:00 • 3 пункта магазина", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption1Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 1);
        assertEquals("сегодня при заказе до 10:00 • 1 пункт магазина", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption2Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 2);
        assertEquals("до&nbsp;3 дней • 1 пункт магазина", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption3Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 3);
        assertEquals("4&nbsp;дня • 1 пункт магазина", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption4Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 4);
        assertEquals("Срок уточняйте при заказе • 1 пункт магазина", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption5Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 5);
        assertEquals("Срок уточняйте при заказе • 1 пункт, SHOP LOGISTIC", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption6Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 6);
        assertEquals("Срок уточняйте при заказе • 1 пункт", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption7Brief() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 7);
        assertEquals("до&nbsp;2 дней • 1 пункт", deliveryOption.getBrief());
    }

    @Test
    public void shouldParsePickupDeliveryOption0OutletCount() {
        ContextHolder.get().setVersion(Version.V2_1_0);
        // вызов системы
        OfferV2 offer = parse();
        // проверка утверждений
        PickupDeliveryOptionV2 deliveryOption = getPickupDeliveryOption(offer, 0);
        assertEquals(3, deliveryOption.getOutletCount());
    }

    @Test
    public void shouldParseSku() {
        OfferV2 offer = parse("offer-sku.json");

        assertThat(
            offer,
            offer(
                sku("FirstSku"),
                skuType("market"),
                offerId(
                    wareMd5("Sku1Price5-IiLVm1Goleg")
                )
            )
        );

    }

    @Test
    public void shouldParsePreorder() {
        OfferV2 offer = parse("offer-v2-preorder.json");
        assertEquals(Boolean.TRUE, offer.getPreorder());
    }

    @Test
    public void shouldNotParsePreorder() {
        OfferV2 offer = parse();
        assertNull(offer.getPreorder());
    }

    @Test
    public void shouldParseModelAwareTitle() {
        OfferV2 offer = parse("offer-with-model-aware-title.json");
        assertEquals("Шейкер IRONTRUE 916-600 0.7 л the flash", offer.getModelAwareTitle());
    }

    @Test
    public void shouldParseWithUrlsByPpForSovetink() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);

        OfferV2 offer = parse("offer_urls_by_pp.json");

        Map<String, String> urlsFirst = offer.getUrls();

        assertNotNull(urlsFirst);
        assertEquals(2, urlsFirst.size());
        assertThat(urlsFirst, hasEntry("10", "http://market-click2.yandex.ru/redir/12345"));
        assertThat(urlsFirst, hasEntry("12", "http://market-click2.yandex.ru/redir/67890"));
    }

    @Test
    public void shouldNotParseWithUrlsByPpEmptyEncrypted() {
        ContextHolder.get().setClient(new Client() {{
            setId("2532");
        }});

        OfferV2 offer = parse("offer_urls_by_pp_empty_encrypted.json");

        Map<String, String> urlsFirst = offer.getUrls();
        assertNull(urlsFirst);
    }

    @Test
    public void shouldNotParseWuthEmptyUrlsByPp() {
        ContextHolder.get().setClient(new Client() {{
            setId("2532");
        }});

        OfferV2 offer = parse("offer_empty_urls_by_pp.json");

        Map<String, String> urlsFirst = offer.getUrls();
        assertNull(urlsFirst);
    }

    @Test
    public void shouldNotParseWithUrlsByPpForNotSovetnik() {
        OfferV2 offer = parse("offer_urls_by_pp.json");

        Map<String, String> urlsFirst = offer.getUrls();
        assertNull(urlsFirst);
    }


    @Test
    public void shouldParseSpasiboForSovetnik() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);
        OfferV2 offer = parse();
        JsonNode spasibo = offer.getSpasibo();
        assertNotNull(spasibo);
    }


    @Test
    public void shouldNotParseSpasiboForNotSovetnik() {
        OfferV2 offer = parse();
        JsonNode spasibo = offer.getSpasibo();
        assertNull(spasibo);
    }


    @Test
    public void shouldParseManufactCountries() {
        Mockito.when(
            geoRegionService.getSafeRegion(
                eq(134),
                anyCollectionOf(RegionField.class),
                any(GeoRegion.class),
                any(RegionVersion.class)
            )
        ).thenReturn(
            new RegionV2() {{
                setId(134);
                setName("Китай");
                setType(RegionType.COUNTRY);
            }}
        );

        OfferV2 offer = parse("offer-v2-manufacturer-countries.json");

        assertThat(
            offer,
            manufactCountries(
                Matchers.contains(
                    RegionV2Matcher.regionV2(
                        RegionV2Matcher.id(134),
                        RegionV2Matcher.name("Китай"),
                        RegionV2Matcher.type(RegionType.COUNTRY)
                    )
                )
            )
        );
    }

    @Test
    public void shouldParsePremiumMark() {
        OfferV2 offer = parse("offer-premium.json");

        assertEquals(Boolean.TRUE, offer.isPremium());
    }

    @Test
    public void shouldNotParseTraceByDefault() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, false);
        mockClientHelper.is(ClientHelper.Type.SOVETNIK_FOR_SITE, false);

        OfferV2 offer = parse("offer-with-trace.json");
        assertNull(offer.getTrace());
    }

    @Test
    public void shouldParseTraceForSovetnik() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);

        OfferV2 offer = parse("offer-with-trace.json");
        assertNotNull(offer.getTrace());
    }

    @Test
    public void shouldParseSnippetFilters() {
        OfferV2 offer = parse("offer-with-snippet-filters.json");
        Assert.assertThat(offer.getSnippetFilters(), Matchers.containsInAnyOrder(
            FiltersMatcher.filter(
                FiltersMatcher.id("-11"),
                FiltersMatcher.name("Производитель")
            ),
            FiltersMatcher.filter(
                FiltersMatcher.id("14343856"),
                FiltersMatcher.name("Тип")
            )
        ));
    }

    @Test
    public void shouldParseTurboUrl() {
        OfferV2 offer = parse("offer-with-turbo.json");
        Assert.assertThat(offer.getTurboUrl(), Matchers.is("http://market-click2.yandex.ru/redir/abcd"));
    }

    @Test
    public void shouldParsePickupGeoUrlForSovetnik() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);
        OfferV2 offer = parse("offer-with-urls.json");
        Assert.assertThat(offer.getPickupGeoUrl(), Matchers.is("http://market-click2.yandex.ru/redir/pickupGeo"));
    }

    @Test
    public void shouldParseShopLogoWithDensities() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);
        OfferV2 offer = parse("offer-with-shop-logo.json", Arrays.asList(ShopInfoFieldV2.PICTURE, ShopInfoFieldV2.PICTURE_ALL_DENSITIES));
        Assert.assertThat(offer.getShopPicture(), is(
                both(instanceOf(ShopLogo.class))
                        .and(map(x -> ((ShopLogo) x).getThumbnails(),
                                "has thumbnails",
                                hasItem(map(ThumbnailWithDensities::getDensities,
                                        "thumbnail densities",
                                        hasItem(map(ImageDensity::getUrl,
                                                "density url",
                                                is("http://avatars.mds.yandex.net/get-market-shop-logo/1539910/2a0000016a44aa3e1d7da56fc50014c7b893/orig")))
                                ))
                        ))
        ));
    }

    @Test
    public void cutPrice() {
        OfferV2 offer = parse("offer-cutprice.json");
        Assert.assertTrue(offer.getCutPrice());
        JsonNode condition = offer.getCondition();
        Assert.assertEquals("like-new", condition.get("type").asText());
        Assert.assertEquals("Новый, вскрыта упаковка, снята пленка с экрана, комплект не распакованный.", condition.get("reason").asText());
    }

    @Test
    public void shouldParseOrderMinCost() {
        OfferV2 offer = parse("offer-with-order-min-cost.json", Collections.singletonList(OfferFieldV2.ORDER_MIN_COST));
        Assert.assertEquals(799L, offer.getOrderMinCost().longValue());
    }

    @Test
    public void parseSupplier() {
        OfferV2 offer = parse("offer-v2-supplier.json");
        Assert.assertNotNull(offer.getSupplier());
        Assert.assertEquals(972892L, offer.getSupplierId().longValue());
        Assert.assertEquals(972892L, offer.getSupplier().getId());
        Assert.assertEquals("smart100.su", offer.getSupplier().getName());
        Assert.assertNotNull(offer.getSupplier().getOperationalRating());
        Assert.assertTrue(Math.abs(offer.getSupplier().getOperationalRating().getTotal() - 99.91) < 0.000001);
    }

    @Test
    public void parseSpecs() {
        OfferV2 offer = parse();
        Specifications expectedSpecs = new Specifications(
                Arrays.asList(
                        new InternalSpecification(SPEC_TYPE, "medicine", null),
                        new InternalSpecification(SPEC_TYPE, "vidal",
                                Collections.singletonList(new UsedParam("J05AX13")))
                )
        );
        Specifications specs = offer.getSpecs();

        Assert.assertNotNull(specs);
        Assert.assertEquals(expectedSpecs, specs);
    }

    @Test
    public void checkInternalSpecWithUsedParamIdDefaultValue() {
        clientMobile();

        InternalSpecification internalSpecWithParam =
                new InternalSpecification(SPEC_TYPE, "vidal", Collections.singletonList(new UsedParam("J05AX13")));
        // Заполнение UsedParam.id значением по умолчанию
        Long usedParamId = internalSpecWithParam.getUsedParams().get(0).getId();
        assertEquals(0L, (long) usedParamId);
    }

    private static void clientMobile() {
        Client client = new Client();
        client.setType(Client.Type.MOBILE);
        ContextHolder.update(ctx -> ctx.setClient(client));
    }

    private OfferV2 parse(String filename, Collection<Field> fields) {
        ReportRequestContext context = new ReportRequestContext();
        context.setFields(fields);
        context.setUserRegionId(REGION_ID);

        OfferV2JsonParser parser = new OfferV2JsonParser(
                context,
                currencyService,
                deliveryService,
                geoRegionService,
                new FilterFactory(),
                marketUrls,
                clientHelper);
        return parser.parse(ResourceHelpers.getResource(filename));
    }

    private OfferV2 parse(String filename) {
        Collection<Field> fields = ParametersV2
                .MULTI_MODEL_FIELDS
                .getItems()
                .stream()
                .flatMap(x -> x.getValues().stream())
                .collect(Collectors.toSet());
        fields.addAll(
                ParametersV2.OFFER_BY_MODEL_FIELDS.getItems()
                        .stream()
                        .flatMap(items -> items.getValues().stream())
                        .collect(Collectors.toList())
        );

        return parse(filename, fields);
    }

    private OfferV2 parse() {
        return parse("offer-v2.json");
    }

    private DeliveryOptionV2 getDeliveryOption(OfferV2 offer, int index) {
        DeliveryV2 delivery = (DeliveryV2) offer.getDelivery();
        assertNotNull(delivery);
        List<DeliveryOptionV2> options = delivery.getOptions();
        assertNotNull(options);
        assertTrue(index < options.size());
        return options.get(index);
    }

    private PickupDeliveryOptionV2 getPickupDeliveryOption(OfferV2 offer, int index) {
        DeliveryV2 delivery = (DeliveryV2) offer.getDelivery();
        assertNotNull(delivery);
        List<PickupDeliveryOptionV2> options = delivery.getPickupOptions();
        assertNotNull(options);
        assertTrue(index < options.size());
        return options.get(index);
    }


}
