package ru.yandex.market.fulfillment.wrap.marschroute.service.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.service.util.status.GenericStatusHistoryFormatter;
import ru.yandex.market.fulfillment.wrap.marschroute.service.util.status.adapter.GenericOrderStatusAdapter;
import ru.yandex.market.fulfillment.wrap.marschroute.service.util.status.strategy.OrderStatusHistoryFormatterStrategy;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.OrderStatuses.orderStatus;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.OrderStatuses.orderStatuses;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ERROR;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_CANCELLED_FF;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_CREATED_FF;

class GenericOrderStatusHistoryFormatterTest {

    private static final LocalDateTime TEST_TIME = LocalDateTime.of(1970, 1, 1, 12, 0, 0);

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of( // 0
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(24)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(2), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "2"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(2), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1/2/3")
                )
            ),
            Arguments.of( // 1
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(24)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(2), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "3"),
                    orderStatus(ORDER_CREATED_FF, TEST_TIME, "2"),
                    orderStatus(ORDER_CREATED_FF, TEST_TIME.minusHours(1), "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(2), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "3/4"),
                    orderStatus(ORDER_CREATED_FF, TEST_TIME.minusHours(1), "1/2")
                )
            ),
            Arguments.of( // 2
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(24)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME.plusHours(2), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "3"),
                    orderStatus(ORDER_CREATED_FF, TEST_TIME, "2"),
                    orderStatus(ERROR, TEST_TIME.minusHours(1), "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME.plusHours(2), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "3"),
                    orderStatus(ORDER_CREATED_FF, TEST_TIME, "2"),
                    orderStatus(ERROR, TEST_TIME.minusHours(1), "1")
                )
            ),
            Arguments.of( // 3
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(24)),
                //Actual
                orderStatuses(orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME.plusHours(2), "1")),
                //Expected
                orderStatuses(orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME.plusHours(2), "1"))
            ),
            Arguments.of( // 4
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(24)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME.plusHours(2), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "2"),
                    orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME.plusHours(2), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(1), "2"),
                    orderStatus(ORDER_ARRIVED_TO_PICKUP_POINT, TEST_TIME, "1")
                )
            ),
            Arguments.of( // 5
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(1)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusMinutes(20), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusMinutes(10), "2"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusMinutes(20), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1/2/3")
                )
            ),
            Arguments.of( // 6
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(1)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusMinutes(71), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusMinutes(10), "2"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusMinutes(71), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1/2")
                )
            ),
            Arguments.of( // 7
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(6)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(13), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(8), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(4), "2"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(13), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(8), "3/4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1/2")
                )
            ),
            Arguments.of( // 8
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(6)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(12), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(6), "3"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(4), "2"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME.plusHours(12), "4"),
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1/2/3")
                )
            ),
            Arguments.of( // 9
                new GenericStatusHistoryFormatter<>(new OrderStatusHistoryFormatterStrategy(6)),
                //Actual
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                ),
                //Expected
                orderStatuses(
                    orderStatus(ORDER_CANCELLED_FF, TEST_TIME, "1")
                )
            )
        );
    }

    @MethodSource("data")
    @ParameterizedTest
    void testDuplicateOrderStatusHandling(
        GenericStatusHistoryFormatter<OrderStatus, GenericOrderStatusAdapter> handler,
        List<OrderStatus> before,
        List<OrderStatus> after
    ) {
        before = handler.formatHistory(before);

        assertThat(before.size()).isEqualTo(after.size());
        for (int i = 0; i < after.size(); i++) {
            OrderStatus actualStatus = before.get(i);
            OrderStatus expectedStatus = after.get(i);

            assertThat(actualStatus.getStatusCode())
                .as("Asserting status code value, iteration: " + i)
                .isEqualTo(expectedStatus.getStatusCode());

            assertThat(actualStatus.getSetDate())
                .as("Asserting set date value, iteration: " + i)
                .isEqualTo(expectedStatus.getSetDate());

            assertThat(actualStatus.getMessage())
                .as("Asserting message value, iteration: " + i)
                .isEqualTo(expectedStatus.getMessage());
        }
    }
}
