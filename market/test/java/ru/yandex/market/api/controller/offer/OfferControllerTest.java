package ru.yandex.market.api.controller.offer;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.controller.OfferController;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.domain.v1.VendorInfo;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.integration.BaseTestContext;
import ru.yandex.market.api.model.Photo;
import ru.yandex.market.api.offer.Delivery;
import ru.yandex.market.api.offer.DeliveryMethod;
import ru.yandex.market.api.offer.LocalDelivery;
import ru.yandex.market.api.offer.Offer;
import ru.yandex.market.api.outlet.Outlet;
import ru.yandex.market.api.outlet.Telephone;
import ru.yandex.market.api.outlet.WorkingTime;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.shop.ShopInfoV1;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by anton0xf on 24.01.17.
 */
public class OfferControllerTest extends BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(OfferControllerTest.class);

    private static final String WARE_MD5 = "newWareMd5_1";
    private static final String FEESHOW = "feeshow1";
    private static final long MODEL_ID = 13485515L;
    private static final int CATEGORY_ID = 91491;
    private static final long VENDOR_ID = 153061L;
    private static final long SHOP_ID = 108546L;
    private static final String PRICE = "41595";
    private static final String DELIVERY_PRICE = "300";
    private static final String REGION_NAME = "Москва";

    @Inject
    private OfferController offerController;

    @Inject
    private OfferIdEncodingService offerIdEncodingService;

    @Inject
    private ReportTestClient reportClient;

    private void setVendorWithoutCpc() {
        ContextHolder.update(ctx -> {
            Client vendor = new Client();
            vendor.setTariff(TestTariffs.VENDOR);
            vendor.setShowShopUrl(false);
            ctx.setClient(vendor);
        });
    }

    @Test
    public void checkDelivery() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(SHOP_ID), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        // вызов системы
        Offer offer = doCall(offerIdEncodingService.decode(WARE_MD5),
            "/ru/yandex/market/api/controller/offer/offerv1-test1-report-offerinfo.json");
        // проверка утверждений
        Delivery delivery = offer.getDelivery();
        assertNotNull(delivery);
        assertTrue(delivery.isDelivery());
        assertTrue(delivery.isPickup());
        assertEquals("Delivery description", delivery.getDescription());

        assertNotNull(delivery.getUserRegion());
        assertEquals(BaseTestContext.REGION_ID, delivery.getUserRegion().getId());

        assertNotNull(delivery.getShopRegion());
        assertEquals(BaseTestContext.REGION_ID, delivery.getShopRegion().getId());
        assertEquals(REGION_NAME, delivery.getShopRegion().getName());

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

    @Test
    public void checkOffer() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(SHOP_ID), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        // вызов системы
        Offer offer = doCall(offerIdEncodingService.decode(WARE_MD5),
            "/ru/yandex/market/api/controller/offer/offerv1-test1-report-offerinfo.json");
        // проверка утверждений
        assertNotNull(offer);
        assertEquals(null, offer.getRecommended());
        assertEquals(1, offer.getWarranty());

        assertEquals(WARE_MD5, offer.getWareMd5());
        assertEquals(FEESHOW, offer.getFeeShow());
        assertNotNull(offer.getId());
        assertEquals(WARE_MD5, offer.getId().getWareMd5());
        assertEquals(FEESHOW, offer.getId().getFeeShow());

        assertEquals(Long.valueOf(MODEL_ID), offer.getModelId());
        assertEquals(CATEGORY_ID, offer.getCategoryId());
        assertEquals("Test offer name", offer.getName());
        assertEquals("Test offer description", offer.getDescription());
        assertEquals(Integer.valueOf(1), offer.getAdult());
        assertEquals(Integer.valueOf(1), offer.getCpa());

        assertNotNull(offer.getPrice());
        assertEquals(Currency.RUR.name(), offer.getPrice().getCurrencyCode());
        assertEquals(Currency.RUR.getShortName(), offer.getPrice().getCurrencyName());
        assertEquals(PRICE, offer.getPrice().getValue());

        assertNotNull(offer.getPhone());
        assertEquals("+7 125 212-92-12", offer.getPhone().getNumber());
        assertEquals("+71252129212", offer.getPhone().getSanitizedNumber());
    }

    @Test
    public void checkOutlet() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(SHOP_ID), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        // вызов системы
        Offer offer = doCall(offerIdEncodingService.decode(WARE_MD5),
            "/ru/yandex/market/api/controller/offer/offerv1-test1-report-offerinfo.json");
        // проверка утвержденийц
        Outlet outlet = offer.getOutlet();
        assertNotNull(outlet);
        assertEquals("294831", outlet.getPointId());
        assertEquals("Outlet point name", outlet.getPointName());
        assertEquals("DEPOT", outlet.getPointType());
        assertEquals(REGION_NAME, outlet.getLocalityName());
        assertEquals("Street", outlet.getThoroughfareName());
        assertEquals("15/16", outlet.getPremiseNumber());
        assertEquals("17", outlet.getBlock());
        assertEquals("123", outlet.getOfficeNumber());
        assertEquals("2", outlet.getBuilding());
        assertEquals(Long.valueOf(108546L), outlet.getShopId());
        assertNotNull(outlet.getGeo());
        assertEquals("37.523743", outlet.getGeo().getLongitude());
        assertEquals("55.895782", outlet.getGeo().getLatitude());
        assertEquals(Integer.valueOf(BaseTestContext.REGION_ID), outlet.getGeo().getGeoId());

        assertEquals(1, outlet.getSchedule().size());
        WorkingTime workingTime = outlet.getSchedule().get(0);
        assertEquals("1", workingTime.getWorkingDaysFrom());
        assertEquals("5", workingTime.getWorkingDaysTill());
        assertEquals("12:00", workingTime.getWorkingHoursFrom());
        assertEquals("19:00", workingTime.getWorkingHoursTill());

        Telephone phone = outlet.getPhone();
        assertNotNull(phone);
        assertEquals("7", phone.getCountry());
        assertEquals("495", phone.getCity());
        assertEquals("212-9268", phone.getNumber());

    }

    @Test
    public void checkPhotos() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(SHOP_ID), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");
        // вызов системы
        Offer offer = doCall(offerIdEncodingService.decode(WARE_MD5),
            "/ru/yandex/market/api/controller/offer/offerv1-test1-report-offerinfo.json");
        // проверка утверждений
        assertEquals(1, offer.getPreviewPhotos().size());
        assertEquals(new Photo("//0.cs-ellpic01gt.yandex.ru/thumbnail_190x250.jpg", 190, 190),
                     offer.getPreviewPhotos().get(0));
        Photo bigPhoto = new Photo("//0.cs-ellpic01gt.yandex.ru/original_1x1.jpg", 1000, 1000);
        assertEquals(bigPhoto, offer.getBigPhoto());
        assertEquals(1, offer.getPhotos().size());
        assertEquals(bigPhoto, offer.getPhotos().get(0));
    }

    @Test
    public void checkShop() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(SHOP_ID), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        // вызов системы
        Offer offer = doCall(offerIdEncodingService.decode(WARE_MD5),
            "/ru/yandex/market/api/controller/offer/offerv1-test1-report-offerinfo.json");
        // проверка утверждений
        ShopInfoV1 shop = offer.getShopInfo();
        assertNotNull(shop);
        assertEquals(SHOP_ID, shop.getId());
        assertEquals(Integer.valueOf(5), shop.getRating());
        assertEquals(Integer.valueOf(1960), shop.getGradeTotal());
    }

    @Test
    public void checkVendor() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(SHOP_ID), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        // вызов системы
        Offer offer = doCall(offerIdEncodingService.decode(WARE_MD5),
            "/ru/yandex/market/api/controller/offer/offerv1-test1-report-offerinfo.json");
        // проверка утверждений
        assertEquals(VENDOR_ID, offer.getVendorId());
        VendorInfo vendor = offer.getVendor();
        assertNotNull(vendor);
        assertEquals(VENDOR_ID, vendor.getId());
        assertEquals("Vendor name", vendor.getName());
        assertEquals("http://www.vendor.com/ru/home", vendor.getSite());
        assertEquals("https://mdata.yandex.net/i?path=b123_img.png", vendor.getPicture());
    }

    @Test
    public void testOfferDirectCpc() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(342553L), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        Offer offer = doCallDirect(new OfferId("1", null), "cpc-offer.json");
        assertEquals("https://shop.ru", offer.getUrl());
    }

    @Test
    public void testOfferDirectCpcForVendor() {
        context.setVersion(Version.V1_0_0);
        setVendorWithoutCpc();

        reportClient.getShopsRatings(Collections.singleton(342553L), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        Offer offer = doCallDirect(new OfferId("1", null), "cpc-offer.json");
        assertEquals("https://shop.ru", offer.getUrl());
    }

    @Test
    public void testOfferDirectCpaOnly() {
        context.setVersion(Version.V1_0_0);

        reportClient.getShopsRatings(Collections.singleton(342553L), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        Offer offer = doCallDirect(new OfferId("1", null), "cpa-only-offer.json");
        assertEquals("https://shop.ru", offer.getUrl());
    }

    @Test
    public void testOfferDirectCpaOnlyForVendor() {
        context.setVersion(Version.V1_0_0);
        setVendorWithoutCpc();

        reportClient.getShopsRatings(Collections.singleton(342553L), "/ru/yandex/market/api/controller/offer/offerv1-test-report-shop-ratings.json");

        Offer offer = doCallDirect(new OfferId("1", null), "empty-offer.json", "cpa-only-offer.json");
        assertEquals("https://market-click2.yandex.ru/fast/checkout", offer.getUrl());
    }

    @Test
    public void testOfferDirectCpaForVendor() {
        context.setVersion(Version.V1_0_0);
        setVendorWithoutCpc();

        Offer offer = doCallDirect(new OfferId("1", null), "cpa-only-offer.json","cpa-only-offer.json");
        assertEquals("https://shop.ru", offer.getUrl());
    }

    @Test
    public void testOfferDirectNotFoundForVendor() {
        context.setVersion(Version.V1_0_0);
        setVendorWithoutCpc();

        exception.expect(NotFoundException.class);
        expectMessage("Offer 1 not found");

        doCallDirect(new OfferId("1", null), "empty-offer.json", "empty-offer.json");

    }

    private Offer doCall(OfferId offerId, String filename) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("format", "json");

        reportClient.getOfferInfo(offerId, filename);

        ModelAndView offerModel = offerController.getOffer(
            offerId,
            true,
            "",
            Collections.emptySet(),
            request,
            genericParams
        ).waitResult();

        LOG.debug("offer: {}", offerModel);
        assertNotNull(offerModel);
        Map modelMap = (Map) offerModel.getModel().get("json");
        assertNotNull(modelMap);
        assertEquals(Collections.singleton("offer"), modelMap.keySet());

        return (Offer) modelMap.get("offer");
    }

    private Offer doCallDirect(OfferId offerId, String filename) {
        return doCallDirect(offerId, filename, null);
    }

    private Offer doCallDirect(OfferId offerId, String filename, String filenameForOfferWithoutCpc) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("format", "json");

        if (null != filenameForOfferWithoutCpc) {
            reportClient.getOfferInfoWithCpc(offerId, filename);
            reportClient.getOfferInfoWithoutFilterByCpc(offerId, filenameForOfferWithoutCpc);
        } else {
            reportClient.getOfferInfo(offerId, filename);
        }

        ModelAndView offerModel = offerController.getOfferDirect(
            offerId,
            "",
            true,
            request,
            genericParams,
            Collections.emptySet()
        ).waitResult();

        LOG.debug("offer: {}", offerModel);
        assertNotNull(offerModel);
        Map modelMap = (Map) offerModel.getModel().get("json");
        assertNotNull(modelMap);
        assertEquals(Collections.singleton("offer"), modelMap.keySet());

        return (Offer) modelMap.get("offer");
    }

}
