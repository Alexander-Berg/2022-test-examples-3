package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.common.report.model.DeliveryMethod;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.OfferPicture;
import ru.yandex.market.common.report.model.ShowUrlsParam;
import ru.yandex.market.common.report.model.filter.Filter;
import ru.yandex.market.common.report.model.filter.FilterUnit;
import ru.yandex.market.common.report.model.filter.FilterValue;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 05/04/2017
 */
public class MarketReportSearchServiceTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer reportMock;
    @Autowired
    private WireMockServer fallbackReportMock;
    @Autowired
    private MarketReportSearchService searchService;
    @Autowired
    private PersonalDataService personalDataService;
    private ActualItem item;

    private final long shopId = 100500L;
    private final long regionId = 213L;
    private final long feedId = 200310551L;
    private ReportSearchParameters parameters;

    @BeforeEach
    public void setUp() throws Exception {
        item = new ActualItem();
        item.setShopId(shopId);
        parameters = ReportSearchParameters.builder(item)
                .withRgb(Color.BLUE)
                .withRegionId(regionId).build();
    }

    // Вынесли no-delivery-discount из сервиса в потребители
    // Тесты будут дописаны здесь MARKETCHECKOUT-20708
    @Test
    @Disabled
    public void searchActualDeliveryWithNoDeliveryDiscountParam() {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.ACTUAL_DELIVERY.getId()))
                .withQueryParam("no-delivery-discount", equalTo("1"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));

        Order order = new Order();
        order.setRgb(Color.BLUE);
        order.setDelivery(new Delivery());

        searchService.searchActualDelivery(new ActualDeliveryRequestBuilder().withOrder(order));

        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
    }

    @Test
    public void searchItems() throws Exception {
        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        List<FoundOffer> foundOffers = searchService.searchItems(parameters, Collections.singleton(feedOfferId));
        assertThat(foundOffers.size(), is(1));

        FoundOffer foundOffer = foundOffers.get(0);
        assertThat(foundOffer.getWareMd5(), is("s336nKDyHxwX8XHqslzuWg"));
        assertThat(foundOffer.getFeedId(), is(feedId));
        assertThat(foundOffer.getShopOfferId(), is("1"));
        assertThat(foundOffer.getFeedOfferId(), is(feedOfferId));
        assertThat(foundOffer.getShopCategoryId(), is("1"));
        assertThat(foundOffer.getHyperCategoryId(), is(91214));
        assertThat(foundOffer.getHyperId(), is(13909442L));
        assertThat(foundOffer.getName(), is("test_vendor M00001"));
        assertThat(foundOffer.getDescription(), is("test_description"));
        assertThat(foundOffer.getPrice(), is(BigDecimal.valueOf(251)));
        assertThat(foundOffer.getPriceCurrency(), is(Currency.RUR));
        assertThat(foundOffer.getShopPrice(), is(BigDecimal.valueOf(111)));
        assertThat(foundOffer.getShopCurrency(), is(Currency.RUR));
        assertThat(foundOffer.getShopToUserCurrencyRate(), is(BigDecimal.valueOf(1.1234)));
        assertThat(foundOffer.getCpa(), is("sandbox"));
        assertThat(foundOffer.getFee(), is(200));
        assertThat(foundOffer.getFeeSum(), is(BigDecimal.valueOf(0.17)));
        assertThat(foundOffer.getFeeShow(), is("8-qH2tqoDtLtI_1pXpfWY8ApB72xu73YlwSQL1PoZ4Td4i6YqIZiqS1M1" +
                "-JdCZvt0T3xVdgTOzwYvoxiIrie1Rf0hSu2XzjMxkSEAzZUzp39smPetllTog,,"));
        assertThat(foundOffer.getOnStock(), is(false));
        assertThat(foundOffer.getClassifierMagicId(), is("1ec5b43b86445f8b62e1b9eb084a9020"));
        assertThat(foundOffer.isGlobal(), is(false));
        assertThat(foundOffer.getShopName(), is("pupper2018.yandex.ru"));
        assertThat(foundOffer.getQuantityLimits().getMinimum(), is(10));
        assertThat(foundOffer.getQuantityLimits().getStep(), is(3));
        assertThat(foundOffer.isPromotedByVendor(), is(false));
        assertThat(foundOffer.isRecommendedByVendor(), is(false));
        assertThat(foundOffer.isPrepayEnabled(), is(false));

        assertThat(foundOffer.getWarningsRaw(), notNullValue());
        checkWarnings(foundOffer.getWarningsRaw());

        assertThat(foundOffer.getPictures(), hasSize(8));
        checkPictures(foundOffer);

        assertThat(foundOffer.getBookingOutlets(), hasSize(2));
        checkBookingOutlets(foundOffer.getBookingOutlets());

        assertThat(foundOffer.getDeliveryMethods(), hasSize(10));
        checkDeliveryMethods(foundOffer.getDeliveryMethods());

        assertThat(foundOffer.getDirectUrl(), is("http://kompas39.ru/catalog/130_raskhodnye_materialy_kantselyarskie_" +
                "i_chistyashchie_materialy/kartridzhi_zapravki/epson/originalnye_epson/kartridzh_epson_t048540a_" +
                "r200_300rx500_600_light_cyan.html")
        );

        assertThat(foundOffer.getFilters(), hasSize(8));
        checkFilters(foundOffer.getFilters());

        assertThat(foundOffer.isSubsidies(), is(true));

        assertThat(foundOffer.getRefMinPrice(), comparesEqualTo(new BigDecimal("200")));
        assertThat(foundOffer.getRefMinPriceCurrency(), is(Currency.RUR));
        assertThat(foundOffer.isGoldenMatrix(), is(true));
        assertThat(foundOffer.getDynamicPriceStrategy(), is(2));
    }

    @Test
    public void searchItemsBusinessClient() throws Exception {
        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);

        item = new ActualItem();
        item.setShopId(shopId);

        parameters = ReportSearchParameters.builder(item)
                .withRgb(Color.BLUE)
                .withRegionId(regionId)
                .withBusinessClient(true)
                .build();

        // мокаем WireMock 2 раза, один мок реагирует на УРЛ с параметром available-for-business, тогда вернется
        // offerInfo_business.json второй - без этого параметра, тогда вернется offerInfo.json,
        // так мы проверим, что параметр available-for-business успешно уходит к Репорту

        URL resource1 = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        URL resource2 = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo_business.json");

        mockReport(resource1, Collections.singletonList(feedOfferId));
        mockReportBusinessClient(resource2, Collections.singletonList(feedOfferId));

        List<FoundOffer> foundOffers = searchService.searchItems(parameters, Collections.singleton(feedOfferId));
        assertThat(foundOffers.size(), is(1));

        FoundOffer foundOffer = foundOffers.get(0);
        assertThat(foundOffer.getWareMd5(), is("s336nKDyHxwX8XHqslzuWg"));
        assertThat(foundOffer.getFeedId(), is(feedId));
        assertThat(foundOffer.getShopOfferId(), is("1"));
        assertThat(foundOffer.getFeedOfferId(), is(feedOfferId));
        assertThat(foundOffer.getShopCategoryId(), is("1"));
        assertThat(foundOffer.getHyperCategoryId(), is(91214));
        assertThat(foundOffer.getHyperId(), is(13909442L));
        assertThat(foundOffer.getName(), is("test_vendor M00001 BUSINESS"));
        assertThat(foundOffer.getDescription(), is("test_description_business"));
        assertThat(foundOffer.getPrice(), is(BigDecimal.valueOf(251)));
        assertThat(foundOffer.getPriceCurrency(), is(Currency.RUR));
        assertThat(foundOffer.getShopPrice(), is(BigDecimal.valueOf(111)));
        assertThat(foundOffer.getShopCurrency(), is(Currency.RUR));
        assertThat(foundOffer.getShopToUserCurrencyRate(), is(BigDecimal.valueOf(1.1234)));
        assertThat(foundOffer.getCpa(), is("sandbox"));
        assertThat(foundOffer.getFee(), is(200));
        assertThat(foundOffer.getFeeSum(), is(BigDecimal.valueOf(0.17)));
        assertThat(foundOffer.getFeeShow(), is("8-qH2tqoDtLtI_1pXpfWY8ApB72xu73YlwSQL1PoZ4Td4i6YqIZiqS1M1" +
                "-JdCZvt0T3xVdgTOzwYvoxiIrie1Rf0hSu2XzjMxkSEAzZUzp39smPetllTog,,"));
        assertThat(foundOffer.getOnStock(), is(false));
        assertThat(foundOffer.getClassifierMagicId(), is("1ec5b43b86445f8b62e1b9eb084a9020"));
        assertThat(foundOffer.isGlobal(), is(false));
        assertThat(foundOffer.getShopName(), is("pupper2018.yandex.ru"));
        assertThat(foundOffer.getQuantityLimits().getMinimum(), is(10));
        assertThat(foundOffer.getQuantityLimits().getStep(), is(3));
        assertThat(foundOffer.isPromotedByVendor(), is(false));
        assertThat(foundOffer.isRecommendedByVendor(), is(false));
        assertThat(foundOffer.isPrepayEnabled(), is(false));

        assertThat(foundOffer.getWarningsRaw(), notNullValue());
        checkWarnings(foundOffer.getWarningsRaw());

        assertThat(foundOffer.getPictures(), hasSize(8));
        checkPictures(foundOffer);

        assertThat(foundOffer.getBookingOutlets(), hasSize(2));
        checkBookingOutlets(foundOffer.getBookingOutlets());

        assertThat(foundOffer.getDeliveryMethods(), hasSize(10));
        checkDeliveryMethods(foundOffer.getDeliveryMethods());

        assertThat(foundOffer.getDirectUrl(), is("http://kompas39.ru/catalog/130_raskhodnye_materialy_kantselyarskie_" +
                "i_chistyashchie_materialy/kartridzhi_zapravki/epson/originalnye_epson/kartridzh_epson_t048540a_" +
                "r200_300rx500_600_light_cyan.html")
        );

        assertThat(foundOffer.getFilters(), hasSize(8));
        checkFilters(foundOffer.getFilters());

        assertThat(foundOffer.isSubsidies(), is(true));

        assertThat(foundOffer.getRefMinPrice(), comparesEqualTo(new BigDecimal("200")));
        assertThat(foundOffer.getRefMinPriceCurrency(), is(Currency.RUR));
        assertThat(foundOffer.isGoldenMatrix(), is(true));
        assertThat(foundOffer.getDynamicPriceStrategy(), is(2));
    }

    @Test
    public void searchActualDeliveryBusinessClient() {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("available-for-business", equalTo("1"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));

        Order order = new Order();
        order.setRgb(Color.BLUE);
        order.setBuyer(new Buyer());
        order.getBuyer().setBusinessBalanceId(123L);

        searchService.searchActualDelivery(new ActualDeliveryRequestBuilder().withOrder(order));

        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
    }

    @Test
    public void searchDeliveryRouteBusinessClient() {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("available-for-business", equalTo("1"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setRegionId(145L);
        delivery.setDeliveryDates(new DeliveryDates(Date.from(Instant.now()), Date.from(Instant.now())));
        delivery.setPrice(new BigDecimal(10));

        searchService.searchDeliveryRoute(
                new DeliveryRouteRequestBuilder()
                        .withPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                        .withColor(Color.BLUE)
                        .withOrderDelivery(delivery, personalDataService)
                        .withBusinessClient(true)
        );

        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
    }

    @Test
    public void searchItemsTurbo() throws Exception {
        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo_turbo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        List<FoundOffer> foundOffers = searchService.searchItems(parameters, Collections.singleton(feedOfferId));
        assertThat(foundOffers.size(), is(1));

        FoundOffer foundOffer = foundOffers.get(0);
        assertNull(foundOffer.getWareMd5());
        assertNull(foundOffer.getFeedId());
        assertNull(foundOffer.getShopOfferId());
        assertNull(foundOffer.getFeedOfferId().getFeedId());
        assertNull(foundOffer.getFeedOfferId().getId());
        assertNull(foundOffer.getShopCategoryId());
        assertThat(foundOffer.getHyperCategoryId(), is(91491));
        assertThat(foundOffer.getHyperId(), is(-1L));
        assertThat(foundOffer.getName(), is("Телефон Explay SL240"));
        assertThat(foundOffer.getDescription(), is("тип: телефон, диагональ экрана: 2.1\"-2.9\", разрешение экрана: " +
                "320×240, 2 SIM-карты, слот для карты памяти, емкость аккумулятора: до 1000 мА⋅ч"));
        assertNull(foundOffer.getPrice());
        assertThat(foundOffer.getPriceCurrency(), is(Currency.RUR));
        assertNull(foundOffer.getShopPrice());
        assertNull(foundOffer.getShopCurrency());
        assertNull(foundOffer.getShopToUserCurrencyRate());
        assertNull(foundOffer.getCpa());
        assertNull(foundOffer.getFee());
        assertNull(foundOffer.getFeeSum());
        assertNull(foundOffer.getFeeShow());
        assertNull(foundOffer.getOnStock());
        assertNull(foundOffer.getClassifierMagicId());
        assertNull(foundOffer.isGlobal());
        assertNull(foundOffer.getShopName());
        assertThat(foundOffer.getQuantityLimits().getMinimum(), is(1));
        assertThat(foundOffer.getQuantityLimits().getStep(), is(1));
        assertThat(foundOffer.isPromotedByVendor(), is(false));
        assertNull(foundOffer.isRecommendedByVendor());
        assertNull(foundOffer.isPrepayEnabled());

        assertNull(foundOffer.getWarningsRaw());

        assertThat(foundOffer.getPictures(), hasSize(9));

        assertThat(foundOffer.getBookingOutlets(), hasSize(0));

        assertThat(foundOffer.getDeliveryMethods(), hasSize(0));

        assertNull(foundOffer.getDirectUrl());


        assertThat(foundOffer.getFilters(), hasSize(11));
        assertThat(foundOffer.isSubsidies(), is(false));
        assertNull(foundOffer.getRefMinPrice());
        assertNull(foundOffer.getRefMinPriceCurrency());
        assertNull(foundOffer.isGoldenMatrix());
        assertThat(foundOffer.getDynamicPriceStrategy(), is(0));
    }


    @Test
    public void searchItemsTurboPlus() throws Exception {
        ReportSearchParameters turboParameters = ReportSearchParameters.builder(item)
                .withRgb(Color.TURBO_PLUS)
                .withRegionId(regionId)
                .withIgnoreHiddenShops(colorConfig.getFor(Color.TURBO_PLUS).isIgnoreHiddenShops())
                .build();

        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo_turbo.json");
        MappingBuilder builder = getMappingBuilder(Collections.singletonList(feedOfferId));
        builder.withQueryParam("dynamic-filter", equalTo("0")); // для турбо параметр должен быть 0
        mockReport(resource, builder);

        List<FoundOffer> foundOffers = searchService.searchItems(turboParameters, Collections.singleton(feedOfferId));
        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
        assertThat(foundOffers.size(), is(1));

        FoundOffer foundOffer = foundOffers.get(0);
        assertNull(foundOffer.getWareMd5());
        assertNull(foundOffer.getFeedId());
        assertNull(foundOffer.getShopOfferId());
        assertNull(foundOffer.getFeedOfferId().getFeedId());
        assertNull(foundOffer.getFeedOfferId().getId());
        assertNull(foundOffer.getShopCategoryId());
        assertThat(foundOffer.getHyperCategoryId(), is(91491));
        assertThat(foundOffer.getHyperId(), is(-1L));
        assertThat(foundOffer.getName(), is("Телефон Explay SL240"));
        assertThat(foundOffer.getDescription(), is("тип: телефон, диагональ экрана: 2.1\"-2.9\", разрешение экрана: " +
                "320×240, 2 SIM-карты, слот для карты памяти, емкость аккумулятора: до 1000 мА⋅ч"));
        assertNull(foundOffer.getPrice());
        assertThat(foundOffer.getPriceCurrency(), is(Currency.RUR));
        assertNull(foundOffer.getShopPrice());
        assertNull(foundOffer.getShopCurrency());
        assertNull(foundOffer.getShopToUserCurrencyRate());
        assertNull(foundOffer.getCpa());
        assertNull(foundOffer.getFee());
        assertNull(foundOffer.getFeeSum());
        assertNull(foundOffer.getFeeShow());
        assertNull(foundOffer.getOnStock());
        assertNull(foundOffer.getClassifierMagicId());
        assertNull(foundOffer.isGlobal());
        assertNull(foundOffer.getShopName());
        assertThat(foundOffer.getQuantityLimits().getMinimum(), is(1));
        assertThat(foundOffer.getQuantityLimits().getStep(), is(1));
        assertThat(foundOffer.isPromotedByVendor(), is(false));
        assertNull(foundOffer.isRecommendedByVendor());
        assertNull(foundOffer.isPrepayEnabled());

        assertNull(foundOffer.getWarningsRaw());

        assertThat(foundOffer.getPictures(), hasSize(9));

        assertThat(foundOffer.getBookingOutlets(), hasSize(0));

        assertThat(foundOffer.getDeliveryMethods(), hasSize(0));

        assertNull(foundOffer.getDirectUrl());


        assertThat(foundOffer.getFilters(), hasSize(11));
        assertThat(foundOffer.isSubsidies(), is(false));
        assertNull(foundOffer.getRefMinPrice());
        assertNull(foundOffer.getRefMinPriceCurrency());
        assertNull(foundOffer.isGoldenMatrix());
        assertThat(foundOffer.getDynamicPriceStrategy(), is(0));
    }

    private void checkWarnings(JSONObject warnings) throws JSONException {
        assertThat(warnings.length(), is(2));
        String warningString = warnings.getJSONArray("common").get(0).toString();
        assertThat(warningString, containsString("Есть противопоказания"));
    }

    private void checkFilters(List<Filter> filters) {
        Map<String, Filter> filterMap = filters.stream()
                .collect(toMap(
                        Filter::getId,
                        v -> v
                ));

        Filter filter = filterMap.get("13868607");
        assertThat(filter.getKind(), is((byte) 2));
        assertThat(filter.getName(), is("Цвет профиля"));
        assertThat(filter.getType(), is("enum"));
        assertThat(filter.getSubType(), is("color"));
        assertThat(filter.getUnits(), nullValue());
        assertThat(filter.getValues(), hasSize(1));
        FilterValue filterValue = filter.getValues().get(0);
        assertThat(filterValue.getId(), is("13868649"));
        assertThat(filterValue.getValue(), is("белый"));
        assertThat(filterValue.getCode(), is("#FFFFFF"));

        filter = filterMap.get("13869309");
        assertThat(filter.getKind(), is((byte) 2));
        assertThat(filter.getName(), is("Цвет переднего стекла"));
        assertThat(filter.getType(), is("enum"));
        assertThat(filter.getSubType(), is("color"));
        assertThat(filter.getUnits(), nullValue());
        assertThat(filter.getValues(), hasSize(1));
        filterValue = filter.getValues().get(0);
        assertThat(filterValue.getId(), is("13869351"));
        assertThat(filterValue.getValue(), is("белый"));
        assertThat(filterValue.getCode(), is("#FFFFFF"));

        filter = filterMap.get("14474342");
        assertThat(filter.getKind(), is((byte) 2));
        assertThat(filter.getName(), is("Размер"));
        assertThat(filter.getType(), is("enum"));
        assertThat(filter.getSubType(), is("size"));
        assertThat(filter.getUnits(), hasSize(3));
        assertThat(filter.getValues(), nullValue());
        FilterUnit filterUnit = filter.getUnits().stream().filter(u -> "14557956".equals(u.getId())).findFirst().get();
        assertThat(filterUnit.getUnitId(), is("возраст, мес."));
        assertThat(filterUnit.getValues(), hasSize(2));
        filterValue = filterUnit.getValues().get(0);
        assertThat(filterValue.getId(), is("14558001"));
        assertThat(filterValue.getValue(), is("3-6"));
        assertThat(filterValue.getUnit(), is("возраст, мес."));
        filterValue = filterUnit.getValues().get(1);
        assertThat(filterValue.getId(), is("14558013"));
        assertThat(filterValue.getValue(), is("6-9"));
        assertThat(filterValue.getUnit(), is("возраст, мес."));
    }

    private void checkBookingOutlets(List<Long> bookingOutlets) {
        assertThat(
                new HashSet<>(bookingOutlets),
                is(new HashSet<>(Arrays.asList(shopId, 200100L)))
        );
    }

    private void checkDeliveryMethods(Collection<DeliveryMethod> deliveryMethods) {
        Map<String, DeliveryMethod> methodMap = deliveryMethods.stream()
                .collect(Collectors.toMap(
                        DeliveryMethod::getServiceId,
                        v -> v
                ));

        Set<String> keySet = methodMap.keySet();
        assertThat(keySet, containsInAnyOrder("1", "3", "7", "8", "9", "11", "12", "30", "100", "16"));

        assertThat(methodMap.get("7").getMarketBranded(), is(false));
        assertThat(methodMap.get("11").getMarketBranded(), is(true));
    }

    private void checkPictures(FoundOffer foundOffer) {
        Map<String, OfferPicture> pictureMap = foundOffer.getPictures().stream()
                .collect(Collectors.toMap(
                        OfferPicture::getUrl,
                        v -> v
                ));
        OfferPicture offerPicture = pictureMap
                .get("//avatars.mds.yandex.net/get-mpic/372220/img_id4117619100564717180.jpeg/60x80");
        assertThat(offerPicture, notNullValue());
        assertThat(offerPicture.getWidth(), is(72));
        assertThat(offerPicture.getHeight(), is(80));
        assertThat(offerPicture.getContainerWidth(), is(60));
        assertThat(offerPicture.getContainerHeight(), is(80));

        OfferPicture offerPicture2 = pictureMap
                .get("//avatars.mds.yandex.net/get-mpic/1111879/img_id6349874103344588912.jpeg/55x70");
        assertThat(offerPicture2, notNullValue());
        assertThat(offerPicture2.getWidth(), is(49));
        assertThat(offerPicture2.getHeight(), is(70));
        assertThat(offerPicture2.getContainerWidth(), is(55));
        assertThat(offerPicture2.getContainerHeight(), is(70));
    }

    @Test
    public void searchItemsBulk() throws Exception {
        Set<FeedOfferId> offers = new HashSet<>();
        offers.add(new FeedOfferId("1", feedId));
        offers.add(new FeedOfferId("2", feedId));
        offers.add(new FeedOfferId("3", feedId));

        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfoBulk.json");
        mockReport(resource, offers);

        List<FoundOffer> foundOffers = searchService.searchItems(parameters, offers);
        assertThat(foundOffers.size(), is(3));

        Set<FeedOfferId> offerIds = foundOffers.stream()
                .map(FoundOffer::getFeedOfferId)
                .collect(toSet());

        assertThat(offerIds, is(offers));
    }

    @Test
    public void searchItemsByOrder() throws Exception {
        Order order = new Order();
        order.setRgb(Color.BLUE);
        Set<FeedOfferId> feeds = new HashSet<FeedOfferId>() {{
            add(new FeedOfferId("1", 1L));
            add(new FeedOfferId("2", 2L));
        }};

        feeds.forEach(feedOfferId -> {
            order.addItem(new OrderItem(feedOfferId, new BigDecimal("10.5"), 1));
        });

        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfoBulk.json");
        mockReportForOrder(reportMock, resource, feeds, false);

        List<FoundOffer> foundOffers = searchService.searchItems(ReportSearchParameters.builder(order).build(), feeds);
        assertThat(foundOffers.size(), is(3));
    }

    @Test
    public void searchItemsByOrderSandbox() throws Exception {
        Order order = new Order();
        order.setRgb(Color.BLUE);
        Set<FeedOfferId> feeds = new HashSet<FeedOfferId>() {{
            add(new FeedOfferId("1", 1L));
            add(new FeedOfferId("2", 2L));
        }};

        feeds.forEach(feedOfferId -> {
            order.addItem(new OrderItem(feedOfferId, new BigDecimal("10.5"), 1));
        });

        order.setContext(Context.SANDBOX);
        order.setFake(true);

        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfoBulk.json");
        mockReportForOrder(reportMock, resource, feeds, true);

        List<FoundOffer> foundOffers = searchService.searchItems(ReportSearchParameters.builder(order).build(), feeds);
        assertThat(foundOffers.size(), is(3));
    }

    @Test
    public void searchUnavailableItemsByOrder() throws Exception {
        Order order = new Order();
        order.setRgb(Color.BLUE);
        Set<FeedOfferId> feeds = new HashSet<FeedOfferId>() {{
            add(new FeedOfferId("30", 30L));
            add(new FeedOfferId("20", 20L));
        }};

        feeds.forEach(feedOfferId -> {
            order.addItem(new OrderItem(feedOfferId, new BigDecimal("10.5"), 1));
        });

        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReportForOrder(reportMock, resource, feeds, false);
        resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfoBulk.json");
        mockReportForOrder(fallbackReportMock, resource, feeds, false);

        List<FoundOffer> foundOffers = searchService.searchItems(ReportSearchParameters.builder(order).build(), feeds);
        assertThat(reportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
        assertThat(fallbackReportMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
        assertThat(foundOffers.size(), is(3));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void searchItemsWithCpaReal(boolean useCpaReal) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_CPA_REAL, useCpaReal);

        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        searchService.searchItems(parameters, Collections.singleton(feedOfferId));
        assertEquals(useCpaReal, checkCpaReal(MarketReportPlace.OFFER_INFO));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void searchActualDeliveryWithCpaReal(boolean useCpaReal) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_CPA_REAL, useCpaReal);

        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));
        Order order = new Order();
        order.setRgb(Color.BLUE);

        searchService.searchActualDelivery(new ActualDeliveryRequestBuilder().withOrder(order));

        assertEquals(useCpaReal, checkCpaReal(MarketReportPlace.ACTUAL_DELIVERY));
    }

    @Test
    public void searchOfferInfoWithSizesForOutlets() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SIZES_FOR_OUTLETS, true);

        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        searchService.searchItems(parameters, Collections.singleton(feedOfferId));

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        List<ServeEvent> events = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.OFFER_INFO.getId()))
                .collect(Collectors.toList());
        assertFalse(events.isEmpty());

        LoggedRequest request = events.get(0).getRequest();

        assertTrue(request.queryParameter("fill-offer-filters-with-jumptable").containsValue("1"));
        assertFalse(request.queryParameter("show-model-card-params").isPresent());
    }

    @Test
    public void searchOfferInfoWithoutSizesForOutlets() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SIZES_FOR_OUTLETS, false);

        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        searchService.searchItems(parameters, Collections.singleton(feedOfferId));

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        List<ServeEvent> events = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.OFFER_INFO.getId()))
                .collect(Collectors.toList());
        assertFalse(events.isEmpty());

        LoggedRequest request = events.get(0).getRequest();

        assertFalse(request.queryParameter("fill-offer-filters-with-jumptable").isPresent());
        assertTrue(request.queryParameter("show-model-card-params").containsValue("1"));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void searchDeliveryRouteWithCpaReal(boolean useCpaReal) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_CPA_REAL, useCpaReal);
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));
        Order order = new Order();
        order.setRgb(Color.BLUE);

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setRegionId(145L);
        delivery.setDeliveryDates(new DeliveryDates(Date.from(Instant.now()), Date.from(Instant.now())));
        delivery.setPrice(new BigDecimal(10));

        searchService.searchDeliveryRoute(
                new DeliveryRouteRequestBuilder()
                        .withPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                        .withColor(Color.BLUE)
                        .withOrderDelivery(delivery, personalDataService)
        );

        assertEquals(useCpaReal, checkCpaReal(MarketReportPlace.DELIVERY_ROUTE));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void searchCreditInfoWithCpaReal(boolean useCpaReal) throws Exception {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));
        searchService.searchCreditInfo(new CreditInfoRequestBuilder()
                .withRgb(Color.BLUE)
                .withTotalPrice(BigDecimal.valueOf(123))
                .build(useCpaReal, false));
        assertEquals(useCpaReal, checkCpaReal(MarketReportPlace.CREDIT_INFO));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void searchOutletsWithCpaReal(boolean useCpaReal) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_CPA_REAL, useCpaReal);
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .willReturn(new ResponseDefinitionBuilder().withBody(IOUtils.toString(
                        ReportConfigurer.class.getResource("/files/report/outlets.xml"),
                        StandardCharsets.UTF_8))));
        searchService.searchShopOutlets(ReportSearchParameters.builder()
                        .withRgb(Color.BLUE)
                        .build(),
                123L, Arrays.asList(100501L), Arrays.asList(100501L), true);
        assertEquals(useCpaReal, checkCpaReal(MarketReportPlace.OUTLETS));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void searchModelInfoWithCpaReal(boolean useCpaReal) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_CPA_REAL, useCpaReal);
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .willReturn(new ResponseDefinitionBuilder().withBody("{}")));
        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        FoundOfferBuilder from = FoundOfferBuilder.createFrom(OrderItemProvider.buildOrderItem(feedOfferId))
                .color(ru.yandex.market.common.report.model.Color.BLUE);
        searchService.fetchOffersByModelIds(parameters, List.of(from.build()));

        assertEquals(useCpaReal, checkCpaReal(MarketReportPlace.MODEL_INFO));
    }

    @Test
    public void shouldSearchOfferInfoWithParamUseTitleAffixesWhenFeatureEnabled() throws Exception {
        //arrange
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_TITLE_AFFIXES_FOR_ITEMS_FROM_OFFER_INFO, true);

        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        //act
        searchService.searchItems(parameters, Collections.singleton(feedOfferId));

        //assert
        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        List<ServeEvent> events = serveEvents.stream()
                .filter(se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.OFFER_INFO.getId()))
                .collect(Collectors.toList());
        LoggedRequest request = events.get(0).getRequest();
        assertTrue(request.queryParameter("use-title-affixes").containsValue("1"));
    }

    @Test
    public void shouldNotSearchOfferInfoWithParamUseTitleAffixesWhenFeatureDisabled() throws Exception {
        //arrange
        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_TITLE_AFFIXES_FOR_ITEMS_FROM_OFFER_INFO, false);
        FeedOfferId feedOfferId = new FeedOfferId("1", feedId);
        URL resource = MarketReportSearchServiceTest.class.getResource("/files/report/offerInfo.json");
        mockReport(resource, Collections.singletonList(feedOfferId));

        //act
        searchService.searchItems(parameters, Collections.singleton(feedOfferId));

        //assert
        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        List<ServeEvent> events = serveEvents.stream()
                .filter(se -> se.getRequest()
                        .queryParameter("place")
                        .containsValue(MarketReportPlace.OFFER_INFO.getId()))
                .collect(Collectors.toList());
        LoggedRequest request = events.get(0).getRequest();
        assertFalse(request.queryParameter("use-title-affixes").isPresent());
    }

    private void mockReportForOrder(
            WireMockServer mockServer,
            URL resource,
            Collection<FeedOfferId> offers,
            boolean isHasGone) throws IOException {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.OFFER_INFO.getId()))
                .withQueryParam("regset", equalTo("1"))
                .withQueryParam("numdoc", equalTo(String.valueOf(offers.size())))
                .withQueryParam("cpa-category-filter", equalTo("0"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("show-urls", equalTo(ShowUrlsParam.DECRYPTED.getId()));

        if (isHasGone) {
            builder.withQueryParam("ignore-has-gone", equalTo("1"));
        }

        for (FeedOfferId offer : offers) {
            builder.withQueryParam("feed_shoffer_id", equalTo(offer.getFeedId() + "-" + offer.getId()));
            builder.withQueryParam(
                    "feed_shoffer_id_base64",
                    equalTo(Base64.encodeBase64String((offer.getFeedId() + "-" + offer.getId()).getBytes()))
            );
        }

        mockServer.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(IOUtils.toByteArray(resource))
                )
        );
    }

    private boolean checkCpaReal(MarketReportPlace reportPlace) {
        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(reportPlace.getId())
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("cpa")
                                .containsValue("real")
                )
                .collect(Collectors.toList());

        return actualDeliveryCalls.size() >= 1;
    }

    private void mockReport(URL resource, MappingBuilder builder) throws IOException {
        reportMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(IOUtils.toByteArray(resource))
                )
        );
    }

    private void mockReport(URL resource, Collection<FeedOfferId> offers) throws IOException {
        MappingBuilder builder = getMappingBuilder(offers);

        reportMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(IOUtils.toByteArray(resource))
                )
        );
    }

    private void mockReportBusinessClient(URL resource, Collection<FeedOfferId> offers) throws IOException {
        MappingBuilder builder = getMappingBuilder(offers)
                .withQueryParam("available-for-business", equalTo("1"));

        reportMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(IOUtils.toByteArray(resource))
                )
        );
    }

    private MappingBuilder getMappingBuilder(Collection<FeedOfferId> offers) {
        MappingBuilder builder = get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo(MarketReportPlace.OFFER_INFO.getId()))
                .withQueryParam("rids", equalTo((String.valueOf(regionId))))
                .withQueryParam("fesh", equalTo(String.valueOf(shopId)))
                .withQueryParam("regset", equalTo("1"))
                .withQueryParam("numdoc", equalTo(String.valueOf(offers.size())))
                .withQueryParam("cpa-category-filter", equalTo("0"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("show-urls", equalTo(ShowUrlsParam.DECRYPTED.getId()));

        for (FeedOfferId offer : offers) {
            builder.withQueryParam("feed_shoffer_id", equalTo(offer.getFeedId() + "-" + offer.getId()));
        }
        return builder;
    }
}
