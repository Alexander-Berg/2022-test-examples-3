package ru.yandex.market.checkout.checkouter.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.controllers.service.ShootingController;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.support.ShootingOrderRef;
import ru.yandex.market.checkout.checkouter.support.ShootingStatistics;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class ShootingControllerTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @AfterEach
    private void cleanUp() {
        clearFixed();
    }

    @Test
    public void getShootingOrdersSmokeTest() throws Exception {
        mockMvc.perform(get("/shooting/orders")
                        .param("fromDate", Instant.now(getClock()).minusSeconds(1800).toString())
                        .param("pageSize", String.valueOf(50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void getShootingProgressSmokeTest() throws Exception {
        mockMvc.perform(get("/shooting/progress")
                        .param("fromDate", Instant.now(getClock()).minusSeconds(1800).toString())
                        .param("toDate", Instant.now(getClock()).plusSeconds(1800).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").exists());
    }

    @Test
    public void getShootingStatistics() throws Exception {
        Instant creationDate = Instant.parse("2118-07-23T16:30:00Z");
        setFixedTime(creationDate);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        parameters.getBuyer().setUid(ShootingController.SHOOTING_USER_ID_RANGE.lowerEndpoint());
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);

        Instant pushTime = creationDate.plusSeconds(60);
        setFixedTime(pushTime);

        orderDeliveryHelper.updateOrderDelivery(order.getId(), DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                ParcelProvider.createParcelWithTracks(
                        TrackProvider.createTrack()
                )
        ));

        MvcResult mvcResult = mockMvc.perform(get("/shooting/statistics")
                        .param("fromDate", creationDate.minus(30, ChronoUnit.MINUTES).toString())
                        .param("toDate", creationDate.plus(30, ChronoUnit.MINUTES).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryTimings").exists())
                .andExpect(jsonPath("$.deliveryTimings.duration80").exists())
                .andExpect(jsonPath("$.deliveryTimings.duration90").exists())
                .andExpect(jsonPath("$.deliveryTimings.duration95").exists())
                .andExpect(jsonPath("$.orders").exists())
                .andExpect(jsonPath("$.orders.splitByOffers").exists())
                .andExpect(jsonPath("$.orders.splitMultiByOrders").exists())
                .andReturn();

        ShootingStatistics result = testSerializationService.deserializeCheckouterObject(
                mvcResult.getResponse().getContentAsString(), ShootingStatistics.class);
        assertNotNull(result);
    }

    @Test
    public void getShootingAdjustCoefficientSmokeTest() throws Exception {
        mockMvc.perform(get("/shooting/adjust_coefficient")
                        .param("fromDate", Instant.now(getClock()).minusSeconds(1800).toString()))
                .andExpect(status().isOk());
    }

    @Test
    public void getErrorCountSmokeTest() throws Exception {
        mockMvc.perform(get("/shooting/error_percent")
                        .param("fromDate", Instant.now(getClock()).minusSeconds(1800).toString())
                        .param("toDate", Instant.now(getClock()).plusSeconds(1800).toString()))
                .andExpect(status().isOk());
    }

    @Test
    public void bulkCreateCancellationRequestsSuccess() throws Exception {
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(new Parameters(2));
        Order order1 = multiOrder.getOrders().get(0);
        Order order2 = multiOrder.getOrders().get(1);

        List<ShootingOrderRef> entity = new ArrayList<>();
        entity.add(new ShootingOrderRef(order1.getId(), order1.getBuyer().getUid()));
        entity.add(new ShootingOrderRef(order2.getId(), order2.getBuyer().getUid() + 1)); // wrong uid
        entity.add(new ShootingOrderRef(0L, BuyerProvider.UID)); // order not exists

        RequestBuilder req = post("/shooting/orders/cancellation-request")
                .param("clientRole", "SYSTEM")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testSerializationService.serializeCheckouterObject(entity));

        mockMvc.perform(req)
                .andExpect(status().is2xxSuccessful())
                // проверяем только размер коллекций, из-за неопределённого типа чисел в jsonPath
                .andExpect(jsonPath("$.cancelledOrders").value(hasSize(1)))
                .andExpect(jsonPath("$.cancelledOrdersInfo").value(hasSize(1)))
                .andExpect(jsonPath("$.failedOrders").value(hasSize(2)));
    }

    @Test
    public void bulkCreateCancellationRequestsRequiresSystem() throws Exception {
        RequestBuilder req = post("/shooting/orders/cancellation-request")
                .param("clientRole", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"orderId\":1,\"uid\":123}");

        mockMvc.perform(req)
                .andExpect(status().isBadRequest());
    }
}
