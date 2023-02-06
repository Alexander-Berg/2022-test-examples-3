package ru.yandex.market.checkout.checkouter.controller;


import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.bucket.OfferBucket;
import ru.yandex.market.checkout.checkouter.offer.AbstractOfferCategorizeTestBase;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.common.report.model.FoundOffer;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class OfferControllerTest extends AbstractOfferCategorizeTestBase {

    private static final Long REGION_ID = 215L;

    @Test
    public void categorizeOffers() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", 145, 1L),
                createOffer("222", 146, 1L));
        mockReport(REGION_ID, givenOffers);

        List<OfferBucket> buckets = client.categorizeOffers(Color.BLUE, REGION_ID, toWareMd5(givenOffers));

        checkCategorizeBy(buckets, 145, 146);


        verifyReportColorCalls(reportMock, ru.yandex.market.common.report.model.Color.BLUE, 1);
    }

    @Test
    public void categorizeOffersByFulfilmentWarehouseWhenFulfilment() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", 145, 1001L, 1L),
                createOffer("222", 146, 1002L, 1L));
        givenOffers.forEach(o -> o.setFulfillment(true));
        mockReport(REGION_ID, givenOffers);

        List<OfferBucket> buckets = client
                .categorizeOffers(Color.BLUE, REGION_ID, toWareMd5(givenOffers));

        checkCategorizeBy(buckets, 1001, 1002);
        reportMock.verify(1, anyRequestedFor(urlPathEqualTo("/yandsearch")));
    }

    @Test
    public void categorizeOffersByWarehouseWhenNotFulfilment() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", 145, 1001L, 1L),
                createOffer("222", 146, 1002L, 1L));
        givenOffers.forEach(o -> o.setFulfillment(false));
        mockReport(REGION_ID, givenOffers);

        List<OfferBucket> buckets = client
                .categorizeOffers(Color.BLUE, REGION_ID, toWareMd5(givenOffers));

        checkCategorizeBy(buckets, 145, 146);
    }

    @Test
    public void categorizeOffersWithWhiteReportExp() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", 145, 1L),
                createOffer("222", 146, 1L));
        mockReport(reportMockWhite, REGION_ID, givenOffers);

        List<OfferBucket> buckets = client.categorizeOffers(
                Color.BLUE,
                REGION_ID,
                toWareMd5(givenOffers),
                Experiments.BERU_USE_WHITE_REPORT + "=1");

        checkCategorizeBy(buckets, 145, 146);

        verifyReportColorCalls(reportMockWhite, ru.yandex.market.common.report.model.Color.BLUE, 1);

    }

    @Test
    public void categorizeOffersWithoutWarehouseId() {
        List<FoundOffer> givenOffers = Lists.newArrayList(
                createOffer("111", null, 1L),
                createOffer("222", null, 2L));
        mockReport(reportMockWhite, REGION_ID, givenOffers);

        List<OfferBucket> buckets = client.categorizeOffers(
                Color.BLUE,
                REGION_ID,
                toWareMd5(givenOffers),
                Experiments.BERU_USE_WHITE_REPORT + "=1");

        assertThat(buckets, hasSize(2));
        assertThat(buckets.stream().map(OfferBucket::getShopId).collect(Collectors.toList()),
                containsInAnyOrder(1L, 2L));

    }

    private void checkCategorizeBy(List<OfferBucket> buckets, Integer... warehouses) {
        assertThat(buckets, CoreMatchers.not(Matchers.empty()));
        Assertions.assertEquals(2, buckets.size());
        assertThat(buckets.stream().map(OfferBucket::getWarehouseId).collect(Collectors.toList()),
                containsInAnyOrder(warehouses));
    }
}
