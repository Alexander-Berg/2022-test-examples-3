package ru.yandex.market.api.controller.v2;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.domain.v2.distribution.OrderStatsResult;
import ru.yandex.market.api.domain.v2.distribution.StatsClicksResult;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.util.httpclient.clients.PartnerStatTestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DistributionStatsControllerV3Test extends BaseTest {

    private static final String BERU_DISTRIBUTION_ORDERS_PATH = "/v3/affiliate/beru/orders";
    private static final String MARKET_DISTRIBUTION_STATS_CLICKS_PATH = "/v3/affiliate/market/stats/clicks";
    private static final String MARKET_DISTRIBUTION_ORDER_PATH = "/v3/affiliate/order";

    private static final String PARTNER_CLID_1 = "8888888";
    private static final String CLID_1_SECRET = "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv";

    private static final String PARTNER_CLID_2 = "7777777";
    private static final String CLID_2_SECRET = "wqeytqwetwquyte26722137621368h";
    private static final String ORDER_ID = "555777";


    @Inject
    private PartnerStatTestClient partnerStatTestClient;

    @Test
    public void testGetDistributionOrdersTotal() {
        final HttpHeaders headers = new HttpHeaders();
        final String responseFileName = "partner_stat_distribution_orders_total_" + PARTNER_CLID_1 + ".json";
        mockPartnerStatResponse(headers, CLID_1_SECRET, PARTNER_CLID_1,
                () -> partnerStatTestClient.getDistributionOrders(PARTNER_CLID_1, "true", responseFileName),
                BERU_DISTRIBUTION_ORDERS_PATH);

        ResponseEntity<OrderStatsResult> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_DISTRIBUTION_ORDERS_PATH + "?clid=" + PARTNER_CLID_1 + "&total=1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OrderStatsResult.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        OrderStatsResult result = responseEntity.getBody();

        assertNotNull(result.getOrders());
        assertEquals(2, result.getOrders().size());

        assertEquals("60", result.getOrders().get(0).getCart());
        assertEquals(Long.valueOf(PARTNER_CLID_1), result.getOrders().get(0).getClid());
        assertEquals(Long.valueOf(22222), result.getOrders().get(0).getOrderId());
        assertEquals("2019-11-02T10:30:00", result.getOrders().get(0).getDateCreated());
        assertEquals("2019-12-05T10:30:00", result.getOrders().get(0).getDateUpdated());
        assertEquals("6", result.getOrders().get(0).getPayment());
        assertEquals("APPROVED", result.getOrders().get(0).getStatus());
        assertEquals("ON_HOLD", result.getOrders().get(1).getStatus());
        assertEquals("new", result.getOrders().get(0).getTariff());
        assertEquals("aaabb", result.getOrders().get(0).getVid());
        assertEquals("BOOK-AF", result.getOrders().get(0).getPromocode());
        assertEquals(
                Arrays.asList("ABUSE", "UNDER_MINIMUM_PAYOUT_LIMIT", "OVER_LIMIT"),
                result.getOrders().get(0).getAdditionalInfo()
        );
        assertEquals(
                Collections.emptyList(),
                result.getOrders().get(1).getAdditionalInfo()
        );

        assertNull(result.getOrders().get(0).getItems());
    }

    @Test
    public void testGetDistributionOrdersTotalOtherClid() {
        final HttpHeaders headers = new HttpHeaders();
        final String responseFileName = "partner_stat_distribution_orders_total_" + PARTNER_CLID_2 + ".json";
        mockPartnerStatResponse(headers, CLID_1_SECRET, PARTNER_CLID_1,
                () -> partnerStatTestClient.getDistributionOrders(PARTNER_CLID_2, "true", responseFileName),
                BERU_DISTRIBUTION_ORDERS_PATH);

        ResponseEntity<OrderStatsResult> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_DISTRIBUTION_ORDERS_PATH + "?clid=" + PARTNER_CLID_2 + "&total=1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OrderStatsResult.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        OrderStatsResult result = responseEntity.getBody();

        assertNotNull(result.getOrders());
        assertEquals(2, result.getOrders().size());

        assertEquals("60", result.getOrders().get(0).getCart());
        assertEquals(Long.valueOf(PARTNER_CLID_2), result.getOrders().get(0).getClid());
        assertEquals(Long.valueOf(22222), result.getOrders().get(0).getOrderId());
        assertEquals("2019-11-02T10:30:00", result.getOrders().get(0).getDateCreated());
        assertEquals("2019-12-05T10:30:00", result.getOrders().get(0).getDateUpdated());
        assertEquals("6", result.getOrders().get(0).getPayment());
        assertEquals("APPROVED", result.getOrders().get(0).getStatus());
        assertEquals("ON_HOLD", result.getOrders().get(1).getStatus());
        assertEquals("new", result.getOrders().get(0).getTariff());
        assertEquals("aaabb", result.getOrders().get(0).getVid());
        assertEquals("BOOK-AF", result.getOrders().get(0).getPromocode());
        assertEquals(
                Arrays.asList("ABUSE", "UNDER_MINIMUM_PAYOUT_LIMIT", "OVER_LIMIT"),
                result.getOrders().get(0).getAdditionalInfo()
        );
        assertEquals(
                Collections.emptyList(),
                result.getOrders().get(1).getAdditionalInfo()
        );

        assertNull(result.getOrders().get(0).getItems());
    }

    @Test
    public void testGetDistributionOrdersDetailed() {
        final HttpHeaders headers = new HttpHeaders();
        mockPartnerStatResponse(headers, CLID_1_SECRET, PARTNER_CLID_1,
                () -> partnerStatTestClient.getDistributionOrders(
                        PARTNER_CLID_1, "false", "partner_stat_distribution_orders_detailed_response.json"),
                BERU_DISTRIBUTION_ORDERS_PATH);

        ResponseEntity<OrderStatsResult> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + BERU_DISTRIBUTION_ORDERS_PATH + "?clid=" + PARTNER_CLID_1 + "&total=0",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OrderStatsResult.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        OrderStatsResult result = responseEntity.getBody();

        assertNotNull(result.getOrders());
        assertEquals(2, result.getOrders().size());

        assertEquals("60", result.getOrders().get(0).getCart());
        assertEquals(8888888L, (long) result.getOrders().get(0).getClid());
        assertEquals(22222L, (long) result.getOrders().get(0).getOrderId());
        assertEquals("2019-11-02T10:30:00", result.getOrders().get(0).getDateCreated());
        assertEquals("2019-12-05T10:30:00", result.getOrders().get(0).getDateUpdated());
        assertEquals("6", result.getOrders().get(0).getPayment());
        assertEquals("APPROVED", result.getOrders().get(0).getStatus());
        assertEquals("new", result.getOrders().get(0).getTariff());
        assertEquals("aaabb", result.getOrders().get(0).getVid());
        assertEquals("BOOK-AF", result.getOrders().get(0).getPromocode());

        assertNotNull(result.getOrders().get(0).getItems());
        assertEquals(3, result.getOrders().get(0).getItems().size());

        assertEquals("10", result.getOrders().get(0).getItems().get(0).getCart());
        assertEquals(0, (int) result.getOrders().get(0).getItems().get(0).getItemId());
        assertEquals("1", result.getOrders().get(0).getItems().get(0).getPayment());
        assertEquals("0.1", result.getOrders().get(0).getItems().get(0).getTariffRate());
        assertEquals("CEHAC",result.getOrders().get(0).getItems().get(0).getTariffName());
        assertNull(result.getOrders().get(0).getItems().get(2).getTariffName());
        assertEquals(3, (int) result.getOrders().get(0).getItems().get(0).getItemCount());

        assertEquals(
                Arrays.asList("ABUSE", "UNDER_MINIMUM_PAYOUT_LIMIT"),
                result.getOrders().get(0).getAdditionalInfo()
        );
        assertEquals(
                Collections.emptyList(),
                result.getOrders().get(1).getAdditionalInfo()
        );

    }

    @Test
    public void testGetDistributionOrder() {
        final HttpHeaders headers = new HttpHeaders();
        mockPartnerStatResponse(
                headers, CLID_1_SECRET, PARTNER_CLID_1,
                () -> partnerStatTestClient.getDistributionOrderById(
                        PARTNER_CLID_1, ORDER_ID,
                        "true",
                        "partner_stat_distribution_order_response.json"),
                MARKET_DISTRIBUTION_ORDER_PATH);


        ResponseEntity<OrderStatsResult> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + MARKET_DISTRIBUTION_ORDER_PATH
                        + "?clid=" + PARTNER_CLID_1 + "&orderId=" + ORDER_ID + "&total=1",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OrderStatsResult.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        OrderStatsResult result = responseEntity.getBody();

        assertNotNull(result.getOrders());
        assertEquals(1, result.getOrders().size());
        assertEquals(Long.valueOf(PARTNER_CLID_1), result.getOrders().get(0).getClid());
        assertEquals(Long.valueOf(ORDER_ID), result.getOrders().get(0).getOrderId());
        assertEquals(Long.valueOf(2L), result.getOrders().get(0).getDeliveryRegionData().getRegionId());
        assertEquals("Санкт-Петербург", result.getOrders().get(0).getDeliveryRegionData().getRegionRuName());
    }

    @Test
    public void testGetDistributionStatsClicks() {
        final HttpHeaders headers = new HttpHeaders();
        String dateStart = "2020-01-01";
        String dateEnd = "2020-01-03";
        String expectedResponseFileName = "partner_stat_distribution_stats_clicks_" + PARTNER_CLID_1 + ".json";

        mockPartnerStatResponse(
                headers, CLID_1_SECRET, PARTNER_CLID_1,
                () -> partnerStatTestClient.getDistributionStatsClicks(
                        PARTNER_CLID_1, dateStart, dateEnd, expectedResponseFileName),
                MARKET_DISTRIBUTION_STATS_CLICKS_PATH);

        ResponseEntity<StatsClicksResult> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + MARKET_DISTRIBUTION_STATS_CLICKS_PATH + "?clid=" + PARTNER_CLID_1
                        + "&dateStart=" + dateStart + "&dateEnd=" + dateEnd,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StatsClicksResult.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        StatsClicksResult result = responseEntity.getBody();

        assertNotNull(result.getClicks());
        assertEquals(2, result.getClicks().size());

        assertEquals("2020-01-01", result.getClicks().get(0).getDate());
        assertEquals(Long.parseLong(PARTNER_CLID_1), (long) result.getClicks().get(0).getClid());
        assertEquals("5ecd04c0cc62ae0b2785a81a", result.getClicks().get(0).getVid());
        assertEquals(100000L, (long) result.getClicks().get(0).getClicks());
        assertEquals(new BigDecimal("999.99"), result.getClicks().get(0).getPayment());
        assertNull(result.getClicks().get(0).getOrders());
        assertNull(result.getClicks().get(0).getGmv());
        assertEquals(new BigDecimal("0.55"), result.getClicks().get(0).getTariffRate());
    }

    @Test
    public void testGetDistributionStatsClicksForOtherClid() {
        final HttpHeaders headers = new HttpHeaders();
        String dateStart = "2020-01-01";
        String dateEnd = "2020-01-03";
        String expectedResponseFileName = "partner_stat_distribution_stats_clicks_" + PARTNER_CLID_2 + ".json";

        mockPartnerStatResponse(
                headers, CLID_1_SECRET, PARTNER_CLID_1,
                () -> partnerStatTestClient.getDistributionStatsClicks(
                        PARTNER_CLID_2, dateStart, dateEnd, expectedResponseFileName),
                MARKET_DISTRIBUTION_STATS_CLICKS_PATH);

        ResponseEntity<StatsClicksResult> responseEntity = REST_TEMPLATE.exchange(
                baseUrl + MARKET_DISTRIBUTION_STATS_CLICKS_PATH + "?clid=" + PARTNER_CLID_2
                        + "&dateStart=" + dateStart + "&dateEnd=" + dateEnd,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StatsClicksResult.class
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        StatsClicksResult result = responseEntity.getBody();

        assertNotNull(result.getClicks());
        assertEquals(2, result.getClicks().size());

        assertEquals("2020-01-01", result.getClicks().get(0).getDate());
        assertEquals(Long.parseLong(PARTNER_CLID_2), (long) result.getClicks().get(0).getClid());
        assertEquals("5ecd04c0cc62ae0b2785a81a", result.getClicks().get(0).getVid());
        assertEquals(100000L, (long) result.getClicks().get(0).getClicks());
        assertEquals(new BigDecimal("999.99"), result.getClicks().get(0).getPayment());
        assertNull(result.getClicks().get(0).getOrders());
        assertNull(result.getClicks().get(0).getGmv());
        assertEquals(new BigDecimal("0.55"), result.getClicks().get(0).getTariffRate());
    }


    @Test(expected = HttpClientErrorException.class)
    public void errorByLocalDateValidation() {
        final HttpHeaders headers = new HttpHeaders();
        String dateStartWithError = "2020-33-01";
        String dateEnd = "2020-01-03";
        headers.add("Authorization", "GGSj03H0ztvdd9yrs7o84gWjYy7Sjv");

        REST_TEMPLATE.exchange(
                baseUrl + MARKET_DISTRIBUTION_STATS_CLICKS_PATH + "?clid=" + PARTNER_CLID_1
                        + "&dateStart=" + dateStartWithError + "&dateEnd=" + dateEnd,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                StatsClicksResult.class
        );
    }

    private void mockPartnerStatResponse(
            HttpHeaders headers, String secret, String clid, Runnable clientCall, String path) {
        final Client client = new Client();
        client.setTariff(TestTariffs.CUSTOM);
        client.setType(Client.Type.EXTERNAL);
        client.setClid(clid);
        client.setThisLoginClids(Stream.of(clid).collect(Collectors.toSet()));

        ContextHolder.update(ctx -> {
            HttpServletRequest request = MockRequestBuilder.start()
                    .methodName(path)
                    .build();
            ctx.setRequest(request);
            ctx.setClient(client);
        });
        clientCall.run();
        headers.add("Authorization", secret);
    }
}
