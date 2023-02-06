package ru.yandex.market.checkout.checkouter.orderItem;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.OptionalAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.fee.FeeChangeResolution;
import ru.yandex.market.checkout.checkouter.order.fee.OrderItemFeeChangeRequest;
import ru.yandex.market.checkout.checkouter.order.fee.OrderItemFeeChangeResponse;
import ru.yandex.market.checkout.checkouter.order.fee.OrderItemsChangeFeeRequest;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

@ParametersAreNonnullByDefault
class PutOrderItemsEditFeeTest extends AbstractPaymentTestBase {

    private static final ClientInfo CLIENT_INFO = ClientInfo.SYSTEM;

    @Autowired
    private ObjectMapper checkouterAnnotationObjectMapper;
    @Autowired
    private OrderReadingDao orderReadingDao;

    @DisplayName("Проверяет, что был ставка на одну из позиций в заказе, сделанным залогенненым пользователем, была " +
            "изменена.")
    @Test
    void putOrderItemsEditFeeLoggedUser() throws Exception {
        // создаем заказы
        Order startOrder = createOrderWithItems(PROCESSING, 40L, 144L,
                List.of(
                        OrderItemProvider.buildOrderItem("vitek140L", 0, 40L),
                        OrderItemProvider.buildOrderItem("vitek240L", 0, 40L)
                )
        );

        List<OrderItemFeeChangeResponse> actualIgnored = processPutFeeChange(
                "vitek140L",
                startOrder,
                0,
                9999,
                null
        );

        assertThat(actualIgnored)
                .isEmpty();

        // проверяем ставку в заказе
        OptionalAssert<Order> optionalAssert = assertThat(orderReadingDao.getOrder(startOrder.getId(), CLIENT_INFO))
                .isPresent();
        assertFeeInt(optionalAssert, new OfferItemKey("vitek140L", 40L, null), 9999);
        assertFeeInt(optionalAssert, new OfferItemKey("vitek240L", 40L, null), 0);
    }

    @DisplayName("Проверяет, что на запрос изменение ставки для товара, у которого нет возможности это сделать, " +
            "ничего не поменяет и ответ пришел с правильным статусом.")
    @ParameterizedTest(name = "resolution: {5}; modification: {6}")
    @CsvSource({
            "vitek60L,60,666,60,0,ORDER_DELIVERED,false",
            "vitek70L,70,777,70,30,FEE_EXISTED,null",
            "vitek80L,80,888,80,30,FEE_EXISTED,false"
    })
    void putOrderItemsEditFeeCannotBePerform(String offerId,
                                             long shopId,
                                             long uid,
                                             long feedId,
                                             int oldFee,
                                             FeeChangeResolution resolution,
                                             Boolean modification) throws Exception {
        // создаем заказы
        Order startOrder = createOrderWithItems(
                resolution == FeeChangeResolution.FEE_EXISTED ? PROCESSING : DELIVERED,
                shopId,
                uid,
                List.of(
                        OrderItemProvider.buildOrderItem(offerId, oldFee, feedId)
                )
        );

        List<OrderItemFeeChangeResponse> actualIgnored = processPutFeeChange(
                offerId,
                startOrder,
                oldFee,
                1,
                modification
        );

        // проверяем ставку в заказе
        OptionalAssert<Order> optionalAssert = assertThat(orderReadingDao.getOrder(startOrder.getId(), CLIENT_INFO))
                .isPresent();
        assertFeeInt(optionalAssert, new OfferItemKey(offerId, feedId, null), oldFee * 100);

        assertThat(actualIgnored)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        new OrderItemFeeChangeResponse(startOrder.getId(),
                                startOrder.getItem(new OfferItemKey(offerId, feedId, null))
                                        .getId(),
                                resolution
                        )
                );
    }

    @DisplayName("Проверяет, что ставка у товара в заказе уже была задана, но мы ее изменили.")
    @Test
    void putOrderItemsEditFeeLoggedUserChangeFee() throws Exception {
        // создаем заказы
        Order startOrder = createOrderWithItems(PROCESSING, 95L, 5944L,
                List.of(
                        OrderItemProvider.buildOrderItem("vitek6450L", 53, 694L)
                )
        );

        List<OrderItemFeeChangeResponse> actualIgnored = processPutFeeChange(
                "vitek6450L",
                startOrder,
                53,
                9999,
                true
        );

        assertThat(actualIgnored)
                .isEmpty();

        // проверяем ставку в заказе
        OptionalAssert<Order> optionalAssert = assertThat(orderReadingDao.getOrder(startOrder.getId(), CLIENT_INFO))
                .isPresent();
        assertFeeInt(optionalAssert, new OfferItemKey("vitek6450L", 694L, null), 9999);
    }

    private List<OrderItemFeeChangeResponse> processPutFeeChange(String offerId,
                                                                 Order startOrder,
                                                                 int oldFee,
                                                                 int fee,
                                                                 @Nullable Boolean modification) throws Exception {
        // создаем запрос на установку ставки
        OrderItemsChangeFeeRequest request = new OrderItemsChangeFeeRequest(
                getChangeFeeRequests(startOrder, offerId, fee,
                        orderReadingDao.getOrder(startOrder.getId(), CLIENT_INFO)
                                .orElseThrow()
                                .getItems()
                ),
                modification
        );

        // проверяем, что ставка у заказа 0
        orderReadingDao.getOrder(startOrder.getId(), CLIENT_INFO)
                .map(Order::getItems)
                .stream()
                .flatMap(Collection::stream)
                .forEach(i -> assertThat(i.getFeeInt()).isEqualTo(oldFee * 100));

        // запрос на изменения ставки
        String response = putFeeChange(request)
                .getResponse()
                .getContentAsString();

        return checkouterAnnotationObjectMapper.readValue(
                response,
                new TypeReference<List<OrderItemFeeChangeResponse>>() {
                }
        );
    }

    @Nonnull
    private Collection<OrderItemFeeChangeRequest> getChangeFeeRequests(Order order,
                                                                       String offerId,
                                                                       Integer fee,
                                                                       Collection<OrderItem> items) {
        return items.stream()
                .filter(item -> offerId.equals(item.getOfferId()))
                .map(item -> new OrderItemFeeChangeRequest(
                                order.getId(),
                                item.getId(),
                                fee
                        )
                ).collect(Collectors.toList());
    }

    @NotNull
    private MvcResult putFeeChange(OrderItemsChangeFeeRequest request) throws Exception {
        String content = checkouterAnnotationObjectMapper.writeValueAsString(request);
        return mockMvc.perform(put("/orders/items/edit-fee")
                        .param(CheckouterClientParams.CLIENT_ROLE, CLIENT_INFO.getRole().name())
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn();
    }

    @Nonnull
    private Order createOrderWithItems(OrderStatus status,
                                       Long shopId,
                                       Long uid,
                                       List<OrderItem> items) {
        Buyer buyer = new Buyer();
        buyer.setFirstName("First");
        buyer.setLastName("Last");
        buyer.setPhone("+799999999" + shopId);
        buyer.setEmail("user@yandex.ru");
        buyer.setUid(uid);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(items);
        parameters.setShopId(shopId);
        parameters.setBuyer(buyer);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, status);
        return order;
    }

    private void assertFeeInt(OptionalAssert<Order> optionalAssert,
                              OfferItemKey offerItemKey,
                              int expectedFee) {
        optionalAssert.map(order -> order.getItem(offerItemKey))
                .map(OrderItem::getFeeInt)
                .isPresent()
                .get()
                .isEqualTo(expectedFee);
    }
}
