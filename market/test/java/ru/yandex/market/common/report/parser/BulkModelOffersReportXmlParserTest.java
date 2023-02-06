package ru.yandex.market.common.report.parser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.common.report.model.Model;
import ru.yandex.market.common.report.model.Offer;
import ru.yandex.market.common.report.model.Offers;
import ru.yandex.market.common.report.model.Prices;
import ru.yandex.market.common.report.parser.xml.BulkModelOffersReportXmlParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author vbudnev
 */
public class BulkModelOffersReportXmlParserTest {
    private static final String ZERO_RATING_FILE = "/files/bulk_model_offers_with_zero_rating.xml";
    private static final String NO_SHOP_PRIORITY_REGION_FILE = "/files/bulk_model_offers_without_shop_priority_region.xml";
    private static final String NO_DELIVERY_INFO_FILE = "/files/bulk_model_offers_without_delivery_info.xml";
    private static final String FILE = "/files/bulk_model_offers.xml";


    private BulkModelOffersReportXmlParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new BulkModelOffersReportXmlParser();
    }


    @Test
    public void test_should_convertRatingToNegative_when_ratingIsZero() throws Exception {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(ZERO_RATING_FILE));
        List<Model> models = parser.getResult();
        assertEquals(1, models.size());

        Model model = models.get(0);
        assertEquals(7012977L, model.getId());
        assertNull(model.getName());
        assertNull(model.getPrices());

        Offers offers = model.getOffers();
        assertIntegerEquals(41,offers.getOnline());
        assertEquals(1, offers.getOffers().size());

        Offer offer = offers.getOffers().get(0);
        assertEquals("Мобильные телефоны Samsung I9100 Galaxy S II 16Gb (черный)", offer.getName());
        assertEquals("market-verstka.yandex.ru", offer.getShopName());
        assertEquals(BigDecimal.valueOf(12_790L), offer.getPrice());
        assertIntegerEquals(-1, offer.getShopRating());
        assertIntegerEquals(1, offer.getInStock());
        assertLongEquals(213L, offer.getRegionId());
        assertEquals(BigDecimal.valueOf(555L), offer.getShippingCost());
        assertIntegerEquals(1, offer.getPos());
        assertNull(offer.getDiscount());
        assertNull(offer.getPreDiscountPrice());
    }



    @Test
    public void test_should_setDeliveryRegion_when_shopPriorityRegionIsNotAvailable() throws IOException {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(NO_SHOP_PRIORITY_REGION_FILE));
        List<Model> models = parser.getResult();
        assertEquals(1, models.size());

        Model model = models.get(0);
        assertEquals(7012977L, model.getId());
        assertNull(model.getName());
        assertNull(model.getPrices());

        Offers offers = model.getOffers();
        assertIntegerEquals(41, offers.getOnline());
        assertEquals(1, offers.getOffers().size());

        Offer offer = offers.getOffers().get(0);
        assertEquals("Мобильные телефоны Samsung I9100 Galaxy S II 16Gb (черный)", offer.getName());
        assertEquals("market-verstka.yandex.ru", offer.getShopName());
        assertEquals(BigDecimal.valueOf(12_790L), offer.getPrice());
        assertIntegerEquals(5, offer.getShopRating());
        assertIntegerEquals(1, offer.getInStock());
        assertLongEquals(777L, offer.getRegionId());
        assertEquals(BigDecimal.valueOf(555L), offer.getShippingCost());
        assertIntegerEquals(1, offer.getPos());
        assertNull(offer.getDiscount());
        assertNull(offer.getPreDiscountPrice());
    }


    @Test
    public void test_should_setShippingCostToNegative_when_deliveryInfoNotAvailable() throws IOException {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(NO_DELIVERY_INFO_FILE));
        List<Model> models = parser.getResult();
        assertEquals(1, models.size());

        Model model = models.get(0);
        assertEquals(7012977L, model.getId());
        assertNull(model.getName());
        assertNull(model.getPrices());

        Offers offers = model.getOffers();
        assertIntegerEquals(41, offers.getOnline());
        assertEquals(1, offers.getOffers().size());

        Offer offer = offers.getOffers().get(0);
        assertEquals("Мобильные телефоны Samsung I9100 Galaxy S II 16Gb (черный)", offer.getName());
        assertEquals("market-verstka.yandex.ru", offer.getShopName());
        assertEquals(BigDecimal.valueOf(12_790L), offer.getPrice());
        assertIntegerEquals(5, offer.getShopRating());
        assertIntegerEquals(1, offer.getInStock());
        assertLongEquals(213L, offer.getRegionId());
        assertEquals(BigDecimal.valueOf(-1), offer.getShippingCost());
        assertIntegerEquals(1, offer.getPos());
        assertNull(offer.getDiscount());
        assertNull(offer.getPreDiscountPrice());
    }

    @Test
    public void test_should_parseAndKeepOrder_when_xmlIsOk() throws IOException {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(FILE));
        List<Model> models = parser.getResult();
        assertEquals(2, models.size());

        Model model = models.get(0);
        assertEquals(7717686L, model.getId());
        assertNull(model.getName());
        assertNull(model.getPrices());

        Offers offers = model.getOffers();
        assertIntegerEquals(0, offers.getOnline());
        assertEquals(0, offers.getOffers().size());


        model = models.get(1);
        assertEquals(7012977L, model.getId());
        assertEquals("Samsung I9100 Galaxy S II 16Gb", model.getName());

        Prices prices = model.getPrices();
        assertEquals(BigDecimal.valueOf(12_790L), prices.getMin());
        assertEquals(BigDecimal.valueOf(12_790L), prices.getAvg());
        assertEquals(BigDecimal.valueOf(12_790L), prices.getMax());

        offers = model.getOffers();
        assertIntegerEquals(41, offers.getOnline());
        assertEquals(2, offers.getOffers().size());

        Offer offer = offers.getOffers().get(0);
        assertEquals("Мобильные телефоны Samsung I9100 Galaxy S II 16Gb (черный)", offer.getName());
        assertEquals("market-verstka.yandex.ru", offer.getShopName());
        assertEquals(BigDecimal.valueOf(12_790L), offer.getPrice());
        assertIntegerEquals(4, offer.getShopRating());
        assertIntegerEquals(1, offer.getInStock());
        assertLongEquals(213L, offer.getRegionId());
        assertEquals(BigDecimal.valueOf(555L), offer.getShippingCost());
        assertIntegerEquals(1, offer.getPos());
        assertIntegerEquals(17, offer.getDiscount());
        assertEquals(BigDecimal.valueOf(12_345L), offer.getPreDiscountPrice());

        offer = offers.getOffers().get(1);
        assertEquals("Мобильные телефоны Samsung I9100 Galaxy S II 16Gb (черный)", offer.getName());
        assertEquals("watch.moskow_spb.yandex.ru", offer.getShopName());
        assertEquals(BigDecimal.valueOf(12_790L), offer.getPrice());
        assertIntegerEquals(1, offer.getShopRating());
        assertIntegerEquals(0, offer.getInStock());
        assertLongEquals(213L, offer.getRegionId());
        assertEquals(BigDecimal.ZERO, offer.getShippingCost());
        assertIntegerEquals(2, offer.getPos());
        assertIntegerEquals(17, offer.getDiscount());
        assertEquals(BigDecimal.valueOf(12_345L), offer.getPreDiscountPrice());
    }

    private void assertIntegerEquals(Integer expected, Integer actual) {
        assertEquals(expected, actual);
    }

    private void assertLongEquals(Long expected, Long actual) {
        assertEquals(expected, actual);
    }
}
