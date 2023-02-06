package ru.yandex.market.api.internal.report.parsers.json;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import ru.yandex.market.api.common.DeliveryService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.v2.RegionField;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlNormalizer;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.model.Photo;
import ru.yandex.market.api.offer.Delivery;
import ru.yandex.market.api.offer.DeliveryMethod;
import ru.yandex.market.api.offer.LocalDelivery;
import ru.yandex.market.api.offer.Offer;
import ru.yandex.market.api.outlet.Outlet;
import ru.yandex.market.api.outlet.Telephone;
import ru.yandex.market.api.outlet.WorkingTime;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.shop.ShopInfoV1;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Created by anton0xf on 25.01.17.
 */
@WithContext
@WithMocks
public class OfferV1JsonParserTest extends BaseTest {

    private static final String WARE_MD5 = "LP3e5x_dp4TX7pRS0n9x9g";
    private static final String FEESHOW = "feeshow1";
    private static final long MODEL_ID = 10495456L;
    private static final int CATEGORY_ID = 91491;
    private static final String PRICE = "26900";
    private static final long VENDOR_ID = 153043L;
    private static final long SHOP_ID = 342553L;
    private static final String DELIVERY_PRICE = "300";
    private static final int REGION_ID = 54;

    private ReportParserFactory reportParserFactory;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private GeoRegionService geoRegionService;

    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Inject
    private MarketUrls marketUrls;

    @Mock
    private BlueRule blueRule;

    @Inject
    private UrlNormalizer urlNormalizer;

    @Mock
    @SuppressWarnings("unused")
    private DeliveryService deliveryService;

    @Mock
    private ClientHelper clientHelper;

    @Before
    public void setUp() {
        when(currencyService.getCurrencyName(Currency.RUR.name()))
            .thenReturn(Currency.RUR.getShortName());
        when(geoRegionService.getInfoOrNull(anyInt()))
            .then(invocation -> {
                GeoRegion res = new GeoRegion();
                res.setId((int) (invocation.getArguments()[0]));
                return res;
            });
        when(geoRegionService.getSafeRegion(anyInt(), anyCollectionOf(RegionField.class), any(GeoRegion.class), eq(RegionVersion.V1)))
            .then(invocation -> {
                GeoRegion res = new GeoRegion();
                res.setId((int) (invocation.getArguments()[0]));
                return res;
            });

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

        reportParserFactory = new ReportParserFactory(
            currencyService,
            deliveryService,
            geoRegionService,
            null,
            null,
            marketUrls,
                urlParamsFactoryImpl,
            clientHelper,
            null,
            null
        );
    }

    @Test
    public void parse() {
        Offer offer = getParser().parse(ResourceHelpers.getResource("offer.json"));
        checkOffer(offer);
        checkPhotos(offer);
        checkShop(offer.getShopInfo());
        checkDelivery(offer.getDelivery());
        checkOutlet(offer.getOutlet());

        Assert.assertNotNull(offer.getUrl());
        Assert.assertNotNull(offer.getRawUrl());
    }

    private OfferV1JsonParser getParser() {
        ReportRequestContext reportContext = new ReportRequestContext();
        Collection<Field> fields = new ArrayList<>();
        reportContext.setFields(fields);
        reportContext.setUserRegionId(GeoUtils.DEFAULT_GEO_ID);
        return reportParserFactory.getOfferV1JsonParser(reportContext);
    }

    private void checkOffer(Offer offer) {
        Assert.assertNotNull(offer);
        assertEquals(Integer.valueOf(1), offer.getRecommended());
        assertEquals(1, offer.getWarranty());

        assertEquals(WARE_MD5, offer.getWareMd5());
        assertEquals(FEESHOW, offer.getFeeShow());
        assertNotNull(offer.getId());
        assertEquals(WARE_MD5, offer.getId().getWareMd5());
        assertEquals(FEESHOW, offer.getId().getFeeShow());

        assertEquals(Long.valueOf(MODEL_ID), offer.getModelId());
        assertEquals(CATEGORY_ID, offer.getCategoryId());
        assertEquals("Смартфон Apple iPhone 5S 16Гб", offer.getName());
        assertEquals("GSM, LTE, смартфон, iOS 7, ...", offer.getDescription());
        assertEquals(Integer.valueOf(1), offer.getCpa());

        assertNotNull(offer.getPrice());
        assertEquals("RUR", offer.getPrice().getCurrencyCode());
        assertEquals("руб.", offer.getPrice().getCurrencyName());
        assertEquals(PRICE, offer.getPrice().getValue());

        assertNotNull(offer.getPhone());
        assertEquals("+7 (343) 386-17-34", offer.getPhone().getNumber());
        assertEquals("+73433861734", offer.getPhone().getSanitizedNumber());

        assertEquals(VENDOR_ID, offer.getVendorId());
        assertEquals(VENDOR_ID, offer.getVendor().getId());
    }

    private void checkPhotos(Offer offer) {
        assertEquals(2, offer.getPreviewPhotos().size());
        Photo thumbnail1 = new Photo("//0.cs-ellpic01gt.yandex.ru/picture1_190x250.jpg", 190, 229);
        assertEquals(thumbnail1, offer.getPreviewPhotos().get(0));
        assertEquals(new Photo("//0.cs-ellpic01gt.yandex.ru/picture3_90x120.jpg", 90, 108),
            offer.getPreviewPhotos().get(1));
        assertEquals(thumbnail1, offer.getBigPhoto());
        assertEquals(3, offer.getPhotos().size());
        assertEquals(thumbnail1, offer.getPhotos().get(0));
        assertEquals(new Photo("//0.cs-ellpic01gt.yandex.ru/picture2.jpg", 90, 108),
            offer.getPhotos().get(1));
        assertEquals(new Photo("//0.cs-ellpic01gt.yandex.ru/picture3.jpg", 90, 108),
            offer.getPhotos().get(2));
    }

    private void checkShop(ShopInfoV1 shop) {
        assertNotNull(shop);
        assertEquals(SHOP_ID, shop.getId());
        assertEquals(Integer.valueOf(4), shop.getRating());
        assertEquals(Integer.valueOf(6), shop.getGradeTotal());
        assertEquals("Территория Apple", shop.getName());
    }

    private void checkDelivery(Delivery delivery) {
        assertNotNull(delivery);
        assertTrue(delivery.isDelivery());
        assertTrue(delivery.isPickup());
        assertEquals("Delivery description", delivery.getDescription());

        assertNotNull(delivery.getUserRegion());
        assertEquals(GeoUtils.DEFAULT_GEO_ID, delivery.getUserRegion().getId());

        assertNotNull(delivery.getShopRegion());
        assertEquals(REGION_ID, delivery.getShopRegion().getId());

        assertNotNull(delivery.getMethods());
        assertEquals(ImmutableList.of(7, 9, 10, 102),
            delivery.getMethods().stream().map(DeliveryMethod::getServiceId)
                .collect(Collectors.toList()));

        assertNotNull(delivery.getPrice());
        assertEquals(Currency.RUR.name(), delivery.getPrice().getCurrencyCode());
        assertEquals(DELIVERY_PRICE, delivery.getPrice().getValue());

        assertNotNull(delivery.getLocalDeliveryList());
        assertEquals(2, delivery.getLocalDeliveryList().size());
        LocalDelivery localDelivery = delivery.getLocalDeliveryList().get(0);
        assertNotNull(localDelivery.getPrice());
        assertEquals(Currency.RUR.name(), localDelivery.getPrice().getCurrencyCode());
        assertEquals(DELIVERY_PRICE, localDelivery.getPrice().getValue());
        assertTrue(localDelivery.getDefaultLocalDelivery());
        assertEquals("16", localDelivery.getOrderBefore());
        assertNull(localDelivery.getDayFrom());
        assertNull(localDelivery.getDayTo());

        LocalDelivery localDelivery2 = delivery.getLocalDeliveryList().get(1);
        assertNotNull(localDelivery2.getPrice());
        assertEquals(Currency.RUR.name(), localDelivery2.getPrice().getCurrencyCode());
        assertEquals("0", localDelivery2.getPrice().getValue());
        assertFalse(localDelivery2.getDefaultLocalDelivery());
        assertNull(localDelivery2.getOrderBefore());
        assertEquals(Integer.valueOf(2), localDelivery2.getDayFrom());
        assertEquals(Integer.valueOf(7), localDelivery2.getDayTo());
    }

    private void checkOutlet(Outlet outlet) {
        assertNotNull(outlet);
        assertEquals("440721", outlet.getPointId());
        assertEquals("Магазин", outlet.getPointName());
        assertEquals("MIXED", outlet.getPointType());
        assertEquals("Екатеринбург", outlet.getLocalityName());
        assertEquals("Первомайская", outlet.getThoroughfareName());
        assertEquals("77", outlet.getPremiseNumber());
        assertEquals("2", outlet.getBuilding());
        assertEquals("1", outlet.getBlock());
        assertEquals("6", outlet.getOfficeNumber());
        assertEquals(Long.valueOf(342553L), outlet.getShopId());
        assertNotNull(outlet.getGeo());
        assertEquals("60.64092485", outlet.getGeo().getLongitude());
        assertEquals("56.84540286", outlet.getGeo().getLatitude());
        assertEquals(Integer.valueOf(REGION_ID), outlet.getGeo().getGeoId());

        assertEquals(1, outlet.getSchedule().size());
        WorkingTime workingTime = outlet.getSchedule().get(0);
        assertEquals("1", workingTime.getWorkingDaysFrom());
        assertEquals("5", workingTime.getWorkingDaysTill());
        assertEquals("10:00", workingTime.getWorkingHoursFrom());
        assertEquals("19:00", workingTime.getWorkingHoursTill());

        Telephone phone = outlet.getPhone();
        assertNotNull(phone);
        assertEquals("7", phone.getCountry());
        assertEquals("343", phone.getCity());
        assertEquals("386-1734", phone.getNumber());
    }

}
