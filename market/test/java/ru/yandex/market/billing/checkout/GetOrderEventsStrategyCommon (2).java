package ru.yandex.market.billing.checkout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.common.util.date.DateUtil.asDate;


class GetOrderEventsStrategyCommon {

    static final Date DATE_2017_01_01 = asDate(LocalDate.of(2017, 01, 01));
    static final Date DATETIME_2017_01_01_01_02_03 = asDate(LocalDateTime.of(2017, 01, 01, 01, 02, 03));
    static final Date DATETIME_2017_02_26_19_22_37 = asDate(LocalDateTime.of(2017, 2, 26, 19, 22, 37));
    static final String LAST_EVENT_ID = "1234";
    static final Long SOME_SHOP_ID = 774L;
    static final Long LOCAL_DELIVERY_REGION = 213L;


    /**
     * Отключаем логику {@link GetOrderEventsStrategy#processOrderTransactions}, для {@link org.mockito.Spy} объектов.
     * {@link GetOrderEventsStrategy#processOrderTransactions} вызывается внутри
     * {@link GetOrderEventsStrategy#process}} и требует большого количества моков и конфигурации дял теста.
     * В идеале как-то модульно разнести, чтобы можно было одельно тестировать логику хэндлеров:
     * - {@link GetOrderEventsStrategy#processOrderStatus}
     * - {@link GetOrderEventsStrategy#updateOrderItems}
     */
    private static void disableProcessOrderTransactions(GetOrderEventsStrategy strategy) {
        doNothing()
                .when(strategy).processOrderTransactions(any());
    }

    static GetOrderEventsStrategy buildPartiallyMockedStrategy(EventProcessorSupport support) {
        GetOrderEventsStrategy strategy = Mockito.spy(new GetOrderEventsStrategy(support));
        disableProcessOrderTransactions(strategy);
        return strategy;
    }

}
