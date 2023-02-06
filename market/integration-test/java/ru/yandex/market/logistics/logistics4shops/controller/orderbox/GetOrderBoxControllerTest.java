package ru.yandex.market.logistics.logistics4shops.controller.orderbox;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.logistics.logistics4shops.client.api.OrderBoxApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxesDto;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.logging.code.CheckouterFallbackCode;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;

import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;

@DisplayName("Получение грузомест")
@DatabaseSetup("/controller/orderbox/get/before/get_order_boxes_before.xml")
@ParametersAreNonnullByDefault
class GetOrderBoxControllerTest extends AbstractOrderBoxControllerTest {

    protected static final long CHECKOUTER_SHOP_ID = 200100;
    protected static final List<Long> CHECKOUTER_ORDER_IDS = List.of(100103L, 876543L);

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получить коробки по маркетному идентификатору заказа")
    void getOrderBoxes(
        @SuppressWarnings("unused") String name,
        long orderId,
        long mbiPartnerId,
        OrderBoxesDto expected
    ) {
        OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), mbiPartnerId, true)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderBoxesDto).isEqualTo(expected);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получить коробки по маркетному идентификатору заказа, если их нет в БД L4S")
    void getOrderBoxesNoL4SBoxes(
        @SuppressWarnings("unused") String name,
        long orderId,
        long mbiPartnerId,
        boolean checkouterFallback,
        OrderBoxesDto expected
    ) {
        mockCheckouter(orderId, mbiPartnerId);

        OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), mbiPartnerId, checkouterFallback)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderBoxesDto).isEqualTo(expected);

        if (checkouterFallback) {
            assertLogs().anyMatch(logEqualsTo(
                TskvLogRecord.info("Not found boxes, using checkouter for order " + orderId)
                    .setLoggingCode(CheckouterFallbackCode.GET_BOXES_CHECKOUTER_FALLBACK)
                    .setEntities(Map.of(
                        "order", List.of(String.valueOf(orderId)),
                        "mbiPartnerId", List.of(String.valueOf(mbiPartnerId))
                    ))
            ));
            verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
        }
    }

    @Test
    @DisplayName("Ошибка получения коробок: нет партнера в L4S")
    void getOrderBoxesNoPartner() {
        ApiError error = apiOperation("1L", 999999L, true)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Failed to find [MBI_PARTNER] with id [999999]");
    }

    @Test
    @DisplayName("Ошибка получения коробок: попытка сходить в чекаутер с нечисловым идентификатором заказа")
    void getOrderBoxesNoLongCheckouterOrderId() {
        ApiError error = apiOperation("my-best-order-ever-111L", CHECKOUTER_SHOP_ID, true)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_BAD_REQUEST)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage())
            .isEqualTo("Invalid format of id my-best-order-ever-111L: must be Long");
    }

    @Test
    @DisplayName("Ошибка получения коробок: нет заказа в чекаутере")
    void getOrderBoxesNoCheckouterOrder() {
        when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
            .thenThrow(new OrderNotFoundException(999999L));

        ApiError error = apiOperation("998877", CHECKOUTER_SHOP_ID, true)
            .execute(validatedWith(shouldBeCode(HttpStatus.SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage())
            .isEqualTo("Failed to find [ORDER] with id [998877]");

        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Nonnull
    private OrderBoxApi.GetOrderBoxesOper apiOperation(String orderId, long mbiPartnerId, boolean checkouterFallback) {
        return apiClient.orderBox().getOrderBoxes()
            .orderIdPath(orderId)
            .mbiPartnerIdQuery(mbiPartnerId)
            .checkouterFallbackQuery(checkouterFallback);
    }

    @Nonnull
    private static Stream<Arguments> getOrderBoxes() {
        return Stream.of(
            Arguments.of(
                "Несколько коробок в заказе",
                100100,
                200100,
                new OrderBoxesDto()
                    .orderId("100100")
                    .boxes(List.of(
                            buildOrderBox(1000L, "100-1", 500L, 100L, 132L, 161L, List.of(
                                buildBoxItem(123451L, 1),
                                buildBoxItem(123452L, 5)
                            )),
                            buildOrderBox(1001L, "100-2", 600L, 140L, 112L, 176L, List.of()),
                            buildOrderBox(1002L, "100-3", 1L, 1L, 1L, 1L, List.of())
                        )
                    )
            ),
            Arguments.of(
                "Одна коробка в заказе",
                100101,
                200100,
                new OrderBoxesDto()
                    .orderId("100101")
                    .boxes(List.of(buildOrderBox(1100L, "101-1", 100L, 200L, 300L, 400L, List.of())))
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> getOrderBoxesNoL4SBoxes() {
        return Stream.of(
            Arguments.of(
                "Заказ без коробок",
                100102,
                200102,
                true,
                new OrderBoxesDto().orderId("100102").boxes(List.of())
            ),
            Arguments.of(
                "Заказ без коробок в L4S, но с фоллбэком и коробками в чекаутере",
                CHECKOUTER_ORDER_IDS.get(0),
                CHECKOUTER_SHOP_ID,
                true,
                new OrderBoxesDto()
                    .orderId(String.valueOf(CHECKOUTER_ORDER_IDS.get(0)))
                    .boxes(List.of(
                        buildOrderBox(
                            9100L,
                            "100103-1",
                            901L,
                            902L,
                            904L,
                            903L,
                            List.of(
                                buildBoxItem(9091L, 9),
                                buildBoxItem(9092L, 99)
                            )
                        ))
                    )
            ),
            Arguments.of(
                "Заказ без коробок в L4S и без фоллбэка на чекаутер",
                CHECKOUTER_ORDER_IDS.get(0),
                CHECKOUTER_SHOP_ID,
                false,
                new OrderBoxesDto().orderId(String.valueOf(CHECKOUTER_ORDER_IDS.get(0))).boxes(List.of())
            ),
            Arguments.of(
                "Заказа нет в L4S, но с фоллбэком и коробками в чекаутере",
                CHECKOUTER_ORDER_IDS.get(1),
                CHECKOUTER_SHOP_ID,
                true,
                new OrderBoxesDto()
                    .orderId(String.valueOf(CHECKOUTER_ORDER_IDS.get(1)))
                    .boxes(List.of(
                        buildOrderBox(
                            9100L,
                            "100103-1",
                            901L,
                            902L,
                            904L,
                            903L,
                            List.of(
                                buildBoxItem(9091L, 9),
                                buildBoxItem(9092L, 99)
                            )
                        )))
            ),
            Arguments.of(
                "Заказа нет в L4S и без фоллбэка на чекаутер",
                CHECKOUTER_ORDER_IDS.get(1),
                CHECKOUTER_SHOP_ID,
                false,
                new OrderBoxesDto().orderId(String.valueOf(CHECKOUTER_ORDER_IDS.get(1))).boxes(List.of())
            ),
            Arguments.of(
                "Заказ другого партнера",
                100100,
                200102,
                false,
                new OrderBoxesDto().orderId("100100").boxes(List.of())
            )
        );
    }

    private void mockCheckouter(long orderId, long mbiPartnerId) {
        if (CHECKOUTER_ORDER_IDS.contains(orderId) && mbiPartnerId == CHECKOUTER_SHOP_ID) {
            when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
                .thenReturn(CheckouterFactory.buildOrder(CHECKOUTER_ORDER_IDS.get(0), 200200L));
        } else {
            Order order = new Order();
            order.setId(999999L);
            when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
                .thenReturn(order);
        }
    }
}
