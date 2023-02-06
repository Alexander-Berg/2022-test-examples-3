package ru.yandex.travel.hotels.common.partners.dolphin.api;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.opentracing.mock.MockTracer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.commons.retry.Retry;
import ru.yandex.travel.hotels.common.partners.dolphin.DefaultDolphinClient;
import ru.yandex.travel.hotels.common.partners.dolphin.DolphinClientProperties;
import ru.yandex.travel.hotels.common.partners.dolphin.model.CalculateOrderRequest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.CreateOrderRequest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.Guest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.OrderState;
import ru.yandex.travel.hotels.common.partners.dolphin.model.OrdersInfoRequest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.PriceKey;
import ru.yandex.travel.hotels.common.partners.dolphin.model.QueryPeriod;
import ru.yandex.travel.hotels.common.token.Occupancy;

import static org.assertj.core.api.Assertions.assertThat;


@Ignore
public class RealTests {

    private DefaultDolphinClient client;

    @Before
    public void prepareClient() {
        AsyncHttpClient ahcClient = Dsl.asyncHttpClient(Dsl.config()
                .setThreadPoolName("ahcPool")
                .build());
        DolphinClientProperties properties = new DolphinClientProperties();
        properties.setBaseUrl("https://www.delfin-tour.ru/jsonyandex/Subagents");
        properties.setEnableRetries(true);
        properties.setHttpReadTimeout(Duration.ofSeconds(15));
        properties.setHttpRequestTimeout(Duration.ofSeconds(20));
        properties.setLogin("hidden");
        properties.setPassword("hidden");
        properties.setEnableRetries(false);
        AsyncHttpClientWrapper wrapper = new AsyncHttpClientWrapper(ahcClient, LoggerFactory.getLogger("test"),
                "dolphin", new MockTracer(), DefaultDolphinClient.getMethods().getNames());

        client = new DefaultDolphinClient(wrapper, properties, new Retry(new MockTracer()));
    }

    @Test
    public void testSearchCheckins() throws ExecutionException, InterruptedException {
        var f = client.searchCheckins(
                LocalDate.of(2020, 4, 26),
                LocalDate.of(2020, 4, 27), Occupancy.fromString("2"), Collections.singletonList("13831"),
                null);
        f.get();
        assertThat(f).isCompleted();
        assertThat(f.get().getOfferLists()).isNotNull();
        assertThat(f.get().getOfferLists()).isNotEmpty();
    }


    @Test
    public void testCalculateOrder() throws ExecutionException, InterruptedException {
        var request = CalculateOrderRequest.builder()
                .priceKey(PriceKey.builder()
                        .hotelId(13831)
                        .tourId(16618)
                        .pansionId(63)
                        .roomId(61)
                        .roomCategoryId(2884)
                        .date(LocalDate.of(2020, 4, 26))
                        .nights(1)
                        .beds(List.of(1, 1))
                        .build())
                .adults(2)
                .children(Collections.emptyList())
                .build();

        var f = client.calculateOrder(request);
        f.get();
        assertThat(f).isCompleted();
        assertThat(f.get()).isNotNull();
        assertThat(f.get().getCost()).isNotNull();
    }

    @Test
    public void testListOrders() throws ExecutionException, InterruptedException {
        var request = OrdersInfoRequest.builder()
                .createPeriod(new QueryPeriod(LocalDate.now(), LocalDate.now()))
                .build();
        var r = client.getOrders(request).get();
        assertThat(r).isNotEmpty();
    }

    @Test
    public void testListNonAnnulatedOrders() throws ExecutionException, InterruptedException {
        var request = OrdersInfoRequest.builder()
                .createPeriod(new QueryPeriod(LocalDate.now(), LocalDate.now()))
                .states(List.of(OrderState.OK, OrderState.IN_WORK, OrderState.WAIT_LIST))
                .build();
        var r = client.getOrders(request).get();
        assertThat(r).isEmpty();
    }

    @Test
    public void testListAnnulatedOrders() throws ExecutionException, InterruptedException {
        var request = OrdersInfoRequest.builder()
                .createPeriod(new QueryPeriod(LocalDate.now(), LocalDate.now()))
                .states(List.of(OrderState.ANNULATED))
                .build();
        var r = client.getOrders(request).get();
        assertThat(r).isNotEmpty();
    }


    @Test
    public void testGetOrder() throws ExecutionException, InterruptedException {
        var res = client.getOrder("KRK00401BM").get();
        assertThat(res).isNotNull();
    }

    @Test
    public void testCreateOrder() throws ExecutionException, InterruptedException {
        var createOrderRequest = CreateOrderRequest.builder()
                .adults(2)
                .children(Collections.emptyList())
                .priceKey(PriceKey.builder()
                        .hotelId(13831)
                        .tourId(16618)
                        .pansionId(63)
                        .roomId(61)
                        .roomCategoryId(2884)
                        .date(LocalDate.of(2020, 4, 26))
                        .nights(1)
                        .beds(List.of(1, 1))
                        .build())
                .guest(Guest.createCyrillic("Test", "Guest"))
                .guest(Guest.createCyrillic("Test", "Guest2"))
                .email("foo@bar.ru")
                .phone("2345632453264")
                .build();
        var order = client.createOrder(createOrderRequest).get();
        assertThat(order).isNotNull();
        assertThat(order.getState()).isEqualTo(OrderState.OK);
    }

    @Test
    public void testCreateAndThenGet() throws ExecutionException, InterruptedException {
        var createOrderRequest = CreateOrderRequest.builder()
                .adults(2)
                .children(Collections.emptyList())
                .priceKey(PriceKey.builder()
                        .hotelId(13831)
                        .tourId(16618)
                        .pansionId(63)
                        .roomId(61)
                        .roomCategoryId(2884)
                        .date(LocalDate.of(2020, 4, 26))
                        .nights(1)
                        .beds(List.of(1, 1))
                        .build())
                .guest(Guest.createCyrillic("Test", "Guest"))
                .guest(Guest.createCyrillic("Test", "Guest2"))
                .email("foo@bar.ru")
                .phone("2345632453264")
                .build();
        var createdOrder = client.createOrder(createOrderRequest).get();
        var fetchedOrder = client.getOrder(createdOrder.getCode()).get();
        assertThat(fetchedOrder).isNotNull();
    }

    @Test
    public void testHotelContent() throws ExecutionException, InterruptedException {
        var f = client.getHotelContent("13831");
        f.get();
        assertThat(f).isCompleted();
        assertThat(f.get()).isNotNull();
        assertThat(f.get().getName()).isEqualTo("\"Инструкция\" отель");
    }
}
