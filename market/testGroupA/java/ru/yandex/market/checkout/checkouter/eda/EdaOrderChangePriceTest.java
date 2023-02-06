package ru.yandex.market.checkout.checkouter.eda;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.eda.EdaOrderService;
import ru.yandex.market.checkout.checkouter.storage.eda.OrderPriceDao;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EdaOrderChangePriceTest extends AbstractWebTestBase {

    @Autowired
    private EdaOrderService edaOrderService;

    @Autowired
    private OrderPriceDao orderPriceDao;

    @Test
    void orderPriceIsUnmodifiable() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .buildResponse(DeliveryResponse::new));
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.getReportParameters().setIsEda(true);
        Order order = orderCreateHelper.createOrder(parameters);
        //250.0
        BigDecimal initialPrice = order.getBuyerItemsTotal();

        BigDecimal delta = edaOrderService.getPriceDelta(order.getId());
        assertEquals(BigDecimal.ZERO, delta);

        /*
            Цена:Дельта:[Список дельт] ->
            250:0:[] ->
            10:-240:[-240] ->
            10:-240:[-240] ->
            11:-239:[-240, -239] ->
            10:-240:[-240, -239, -240]
         */

        BigDecimal price1 = new BigDecimal(10);
        //проверяем идемпотентность
        for (int i = 0; i < 2; i++) {
            checkOrderPriceChanging(
                    order.getId(),
                    price1,
                    initialPrice,
                    Collections.singletonList(
                            price1.subtract(initialPrice)
                    )
            );
        }

        BigDecimal price2 = new BigDecimal(11);
        checkOrderPriceChanging(
                order.getId(),
                price2,
                initialPrice,
                Arrays.asList(
                        price1.subtract(initialPrice),
                        price2.subtract(initialPrice)
                )
        );

        BigDecimal price3 = new BigDecimal(10);
        checkOrderPriceChanging(
                order.getId(),
                price3,
                initialPrice,
                Arrays.asList(
                        price1.subtract(initialPrice),
                        price2.subtract(initialPrice),
                        price3.subtract(initialPrice)
                )
        );
    }

    @Test
    void orderNotFound() {
        changeOrderPrice(1L, new BigDecimal(10), ClientRole.SHOP, status().isNotFound());
    }


    @Test
    void allowedOnlyForProcessing() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        parameters.setPushApiDeliveryResponse(DeliveryProvider.shopSelfDelivery()
                .buildResponse(DeliveryResponse::new));
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.getReportParameters().setIsEda(true);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        changeOrderPrice(order.getId(), new BigDecimal(10), ClientRole.SHOP, status().isBadRequest());
    }

    @Test
    void allowedOnlyForEdaShop() {
        changeOrderPrice(1L, new BigDecimal(10), ClientRole.SYSTEM, status().isForbidden());
    }

    private void changeOrderPrice(long orderId, BigDecimal price, ClientRole clientRole, ResultMatcher expectedStatus) {
        try {
            mockMvc.perform(patch("/orders/{orderId}/price", orderId)
                    .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                    .param(CheckouterClientParams.CLIENT_ID, "123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"price\": " + price + "}")
            ).andExpect(expectedStatus);
        } catch (Exception e) {
            fail("Changing order price failed", e);
        }
    }

    private void checkOrderPriceChanging(long orderId, BigDecimal price, BigDecimal initialPrice,
                                         List<BigDecimal> expectedDeltas) {
        changeOrderPrice(orderId, price, ClientRole.SHOP, status().isOk());
        BigDecimal currentDelta = edaOrderService.getPriceDelta(orderId);
        assertEquals(price.subtract(initialPrice), currentDelta);
        List<BigDecimal> deltas = orderPriceDao.getOrderPriceHistoryDeltas(orderId);
        assertEquals(expectedDeltas.size(), deltas.size());
        for (int i = 0; i < expectedDeltas.size(); i++) {
            assertEquals(expectedDeltas.get(i), deltas.get(i));
        }
    }
}
