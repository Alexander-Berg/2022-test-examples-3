package ru.yandex.market.checkout.checkouter.offer;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.bucket.OfferBucket;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ReportSearchParameters;
import ru.yandex.market.common.report.model.FoundOffer;

public class OfferServiceTest extends AbstractOfferCategorizeTestBase {

    private static final Long REGION_ID = 215L;

    @Autowired
    private OfferService offerService;

    @Test
    public void testCategorizeOffersDifferentWarehouses() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", 145, 1L),
                createOffer("222", 146, 1L));
        mockReport(REGION_ID, givenOffers);

        ReportSearchParameters parameters = ReportSearchParameters.builder()
                .withRegionId(REGION_ID)
                .withRgb(Color.BLUE)
                .build();

        List<OfferBucket> buckets = offerService.categorizeOffers(parameters, toWareMd5(givenOffers));

        Assertions.assertEquals(2, buckets.size());
        Assertions.assertEquals(1, buckets.get(0).getWareMd5Ids().size());
        Assertions.assertEquals(1, buckets.get(1).getWareMd5Ids().size());
        Assertions.assertNotEquals(buckets.get(0).getWarehouseId(), buckets.get(1).getWarehouseId());
        Assertions.assertEquals(buckets.get(0).getShopId(), buckets.get(1).getShopId());
    }

    @Test
    public void testCategorizeOffersDifferentShops() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", 145, 1L),
                createOffer("222", 145, 2L));
        mockReport(REGION_ID, givenOffers);

        ReportSearchParameters parameters = ReportSearchParameters.builder()
                .withRegionId(REGION_ID)
                .withRgb(Color.BLUE)
                .build();

        List<OfferBucket> buckets = offerService.categorizeOffers(parameters, toWareMd5(givenOffers));

        Assertions.assertEquals(2, buckets.size());
        Assertions.assertEquals(1, buckets.get(0).getWareMd5Ids().size());
        Assertions.assertEquals(1, buckets.get(1).getWareMd5Ids().size());
        Assertions.assertEquals(buckets.get(0).getWarehouseId(), buckets.get(1).getWarehouseId());
        Assertions.assertNotEquals(buckets.get(0).getShopId(), buckets.get(1).getShopId());
    }

    @Test
    @DisplayName("Цифровые заказ должен попадать в отдельную корзину")
    public void testCategorizeOffersDigital() {
        var offer1 = createDigitalOffer("555", 1L);
        var offer2 = createOffer("555", 145, 1L);
        List<FoundOffer> givenOffers = Lists.newArrayList(offer1, offer2);
        mockReport(REGION_ID, givenOffers);

        ReportSearchParameters parameters = ReportSearchParameters.builder()
                .withRegionId(REGION_ID)
                .withRgb(Color.BLUE)
                .build();

        List<OfferBucket> buckets = offerService.categorizeOffers(parameters, toWareMd5(givenOffers));

        Assertions.assertEquals(2, buckets.size());
        Assertions.assertEquals(true, buckets.get(0).isDigital());
        Assertions.assertEquals(false, buckets.get(1).isDigital());
    }

    @Test
    @DisplayName("Несколько цифровыф заказов должен попадать в отдельную корзину")
    public void testCategorizeOffersDigitalMany() {
        var offer1 = createDigitalOffer("555", 1L);
        var offer2 = createDigitalOffer("555", 1L);
        var offer3 = createOffer("555", 145, 1L);
        var offer4 = createOffer("555", 145, 1L);
        var offer5 = createOffer("555", 145, 1L);
        List<FoundOffer> givenOffers = Lists.newArrayList(offer1, offer2, offer3, offer4, offer5);
        mockReport(REGION_ID, givenOffers);

        ReportSearchParameters parameters = ReportSearchParameters.builder()
                .withRegionId(REGION_ID)
                .withRgb(Color.BLUE)
                .build();

        List<OfferBucket> buckets = offerService.categorizeOffers(parameters, toWareMd5(givenOffers));

        Assertions.assertEquals(2, buckets.size());
        Assertions.assertEquals(true, buckets.get(0).isDigital());
        Assertions.assertEquals(false, buckets.get(1).isDigital());
        Assertions.assertEquals(2, buckets.get(0).getWareMd5Ids().size());
        Assertions.assertEquals(3, buckets.get(1).getWareMd5Ids().size());
    }
}
