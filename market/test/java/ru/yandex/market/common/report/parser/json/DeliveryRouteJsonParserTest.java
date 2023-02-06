package ru.yandex.market.common.report.parser.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryOffer;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.DeliveryRouteOption;
import ru.yandex.market.common.report.model.DeliveryRouteResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.OfferProblem;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.common.report.parser.json.ActualDeliveryJsonParserTest.assertTimeIntervals;

/**
 * @author mmetlov
 */
public class DeliveryRouteJsonParserTest extends AbstractDeliveryJsonParserTest {
    @Test
    public void shouldParseDeliveryRoutePlace() throws IOException {
        DeliveryRouteJsonParser parser = new DeliveryRouteJsonParser();
        DeliveryRoute result = parser.parse(DeliveryRouteJsonParserTest.class.getResourceAsStream
                ("/files/delivery_route.json"));

        assertThat(
                result.getResults(),
                hasSize(1)
        );

        DeliveryRouteResult deliveryResult = getOnlyElement(result.getResults());
        assertNotNull(deliveryResult);
        assertThat(deliveryResult.getRoute(), equalTo("{\"route\":111,\"this\":\"is\"}"));
        assertThat(deliveryResult.getRouteId(), equalTo(UUID.fromString("84bf78ff-0127-11ed-934a-6aca27bab52e")));

        DeliveryRouteOption option = deliveryResult.getOption();
        assertDeliveryOptionContent(option);

        assertThat(deliveryResult.getWeight(), comparesEqualTo(new BigDecimal("52.5")));

        Iterator<BigDecimal> expectedDimIt = Arrays.asList(
                new BigDecimal("10"), new BigDecimal("27"), new BigDecimal("118.7")).iterator();
        Iterator<BigDecimal> actualDimIt = deliveryResult.getDimensions().iterator();
        while (expectedDimIt.hasNext()) {
            assertThat(actualDimIt.next(), comparesEqualTo(expectedDimIt.next()));
        }
        assertFalse(actualDimIt.hasNext());

        assertThat(option.getOutletIds(), containsInAnyOrder(123L, 456L));

        assertTrue(option.isMarketCourier());
        assertTrue(option.isExternalLogistics());

        assertDeliveryOffer(deliveryResult.getOffers());
    }

    @Test
    public void shouldParseCommonAndOfferProblems() throws IOException {
        DeliveryRouteJsonParser parser = new DeliveryRouteJsonParser();
        DeliveryRoute deliveryRoute = parser.parse(ActualDeliveryJsonParserTest.class.getResourceAsStream(
                "/files/actual_delivery_offer_problems.json" //одинаковый формат с delivery_route
        ));

        assertThat(deliveryRoute.getOfferProblems(), hasSize(1));

        OfferProblem offerProblem = deliveryRoute.getOfferProblems().get(0);
        assertThat(offerProblem.getWareId(), is("P78u-wFgtz4fkH0iWUOi9A"));
        assertThat(offerProblem.getProblems(), hasItems("NONEXISTENT_OFFER"));

        assertThat(deliveryRoute.getCommonProblems(), hasSize(1));
        assertThat(deliveryRoute.getCommonProblems(), containsInAnyOrder("NO_POST_OFFICE_FOR_POST_CODE"));
    }

    @Test
    public void tariffStatsParserTest() throws IOException {
        testTariffStatsParser(new DeliveryRouteJsonParser());
    }

    @Test
    public void testIsEstimated() throws IOException {
        DeliveryRouteJsonParser parser = new DeliveryRouteJsonParser();
        DeliveryRoute deliveryRoute = parser.parse(DeliveryRouteJsonParserTest.class.getResourceAsStream
                ("/files/delivery_route.json"));

        DeliveryRouteOption localDelivery = deliveryRoute.getResults().get(0).getOption();

        assertTrue(localDelivery.getEstimated());
    }

    private static void assertDeliveryOptionContent(ActualDeliveryOption deliveryOption) {
        assertEquals("market_delivery", deliveryOption.getPartnerType());
        assertEquals(BigDecimal.ZERO, deliveryOption.getCost());
        assertEquals(BigDecimal.valueOf(90), deliveryOption.getPriceWithoutVat());
        assertEquals(7, (int) deliveryOption.getDayFrom());
        assertEquals(7, (int) deliveryOption.getDayTo());
        assertEquals(50, (long) deliveryOption.getDeliveryServiceId());
        assertThat(
                deliveryOption.getPaymentMethods(),
                containsInAnyOrder("YANDEX", "CASH_ON_DELIVERY")
        );
        assertTimeIntervals(
                deliveryOption,
                new DeliveryTimeInterval(LocalTime.of(12, 00), LocalTime.of(18, 30), true)
        );
        assertEquals(25L * 3600 + 30 * 60, deliveryOption.getPackagingTime().getSeconds());
        assertThat(deliveryOption.getTariffId(), equalTo(2234562L));
    }

    private void assertDeliveryOffer(List<DeliveryOffer> deliveryOffers) {
        assertThat(deliveryOffers, hasSize(1));
        assertEquals(52L, (long) deliveryOffers.get(0).getSellerPrice());
        assertEquals("RUB", deliveryOffers.get(0).getCurrency().getAliases()[0]);
        assertEquals(15166435, (long) deliveryOffers.get(0).getMarketSku());
        assertEquals(73L, (long) deliveryOffers.get(0).getFulfillmentWarehouseId());
    }
}
