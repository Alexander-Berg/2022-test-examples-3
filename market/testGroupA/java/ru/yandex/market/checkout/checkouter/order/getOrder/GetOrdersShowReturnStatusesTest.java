package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mmetlov
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersShowReturnStatusesTest extends AbstractWebTestBase {

    private static final Pair<String, Supplier<MockHttpServletRequestBuilder>> REQ_ORDERS = Pair.of(
            "GET /orders", () -> MockMvcRequestBuilders.get("/orders?rgb=BLUE,WHITE"));

    private static final Pair<String, Supplier<MockHttpServletRequestBuilder>> REQ_ORDERS_BY_UID = Pair.of(
            "GET /orders/by-uid/{uid}", () ->
                    MockMvcRequestBuilders.get("/orders/by-uid/" + BuyerProvider.UID + "?rgb=BLUE,WHITE"));

    private static final Pair<String, Supplier<MockHttpServletRequestBuilder>> REQ_GET_ORDERS = Pair.of(
            "POST /get-orders", () ->
                    MockMvcRequestBuilders.post("/get-orders")
                            .content("{\"rgbs\":[\"BLUE\",\"WHITE\"]}")
                            .contentType(MediaType.APPLICATION_JSON_UTF8));

    private static final Map<Integer, Integer> CHECKPOINTS_BOTH =
            ImmutableMap.<Integer, Integer>builder()
                    .put(1, 1)
                    .put(60, 1)
                    .build();

    private static final Map<Integer, Integer> CHECKPOINTS_ONE =
            ImmutableMap.<Integer, Integer>builder()
                    .put(1, 1)
                    .put(60, 0)
                    .build();

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Autowired
    private NotifyTracksHelper notifyTracksHelper;

    public static Collection<Arguments> testDataShowReturnStatuses() {
        return List.of(
                data(REQ_ORDERS, CHECKPOINTS_BOTH),
                data(REQ_ORDERS_BY_UID, CHECKPOINTS_ONE),
                data(REQ_GET_ORDERS, CHECKPOINTS_BOTH)
        );
    }

    public static Collection<Arguments> testDataHideReturnStatuses() {
        return List.of(
                data(REQ_ORDERS, CHECKPOINTS_ONE),
                data(REQ_ORDERS_BY_UID, CHECKPOINTS_ONE),
                data(REQ_GET_ORDERS, CHECKPOINTS_ONE)
        );
    }

    private static Arguments data(Pair<String, Supplier<MockHttpServletRequestBuilder>> request, Object checkpoints) {
        return Arguments.arguments(request.getKey(), request.getValue(), checkpoints);
    }

    @BeforeAll
    public void init() throws Exception {
        Order order = OrderProvider.getOrderWithTracking();
        order = orderServiceHelper.saveOrder(order);

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(112212, 1),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(112213, 60)
        );
        //push 2 checkpoints: one normal and one return-type
        notifyTracksHelper.notifyTracks(deliveryTrack);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы с showReturnStatuses=true")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataShowReturnStatuses")
    public void testShowReturnStatuses(@SuppressWarnings("unused") String caseName,
                                       Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier,
                                       Map<Integer, Integer> checkpoints) throws Exception {

        execRequestAndCheck(
                builderWithShowReturnStatuses(requestBuilderSupplier, true),
                checkpoints
        );

        execRequestAndCheck(
                requestBuilderSupplier.get().param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()),
                checkpoints
        );
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы с showReturnStatuses=false")
    @ParameterizedTest(name = "{0}")
    @MethodSource("testDataHideReturnStatuses")
    public void testHideReturnStatuses(@SuppressWarnings("unused") String caseName,
                                       Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier,
                                       Map<Integer, Integer> checkpoints) throws Exception {
        execRequestAndCheck(
                builderWithShowReturnStatuses(requestBuilderSupplier, false),
                checkpoints
        );
    }

    private void execRequestAndCheck(
            MockHttpServletRequestBuilder requestBuilder, Map<Integer, Integer> checkpoints
    ) throws Exception {
        ResultActions resultActions = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].delivery.shipments[*].tracks[*].checkpoints[?(@.id>0)]")
                        .value(hasSize(checkpoints.values().stream().reduce(0, Integer::sum))));

        for (Map.Entry<Integer, Integer> entry : checkpoints.entrySet()) {
            int status = entry.getKey();
            int count = entry.getValue();
            resultActions.andExpect(
                    jsonPath("$.orders[0].delivery.shipments[*].tracks[*].checkpoints[?(@.deliveryStatus=='%d')]",
                            status)
                            .value(Matchers.hasSize(count)));
        }
    }

    private MockHttpServletRequestBuilder builderWithShowReturnStatuses(
            Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier, boolean showReturnStatuses
    ) {
        return requestBuilderSupplier.get()
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, Boolean.toString(showReturnStatuses));
    }
}
