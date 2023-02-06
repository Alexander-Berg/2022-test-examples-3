package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static ru.yandex.common.util.date.DateUtil.asDate;

/**
 * Tests for {@link GetOrderEventsStrategy}
 */
@DbUnitDataSet(before = "db/environmentZeroDelay.csv")
class GetOrderEventsStrategyTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final Date DATE_2017_01_01 = asDate(LocalDate.of(2017, 1, 1));

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    private GetOrderEventsStrategy strategy;

    private static final String PREFIX = "resources/checkouter_response/events/";

    @BeforeEach
    void before() {
        strategy = buildPartiallyMockedStrategy(eventProcessorSupportFactory.createSupport());
    }

    @Test
    @DisplayName("Импорт cash_only заказа.")
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.dsbs.newOrder.before.csv"},
            after = "db/GetOrderEventsExecutor.dsbs.newOrder.cash.only.after.csv"
    )
    void testCashOnlySave() throws IOException {
        List<OrderHistoryEvent> events = events("status_update/cash_only_order.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DisplayName("Импорт не cash_only заказа.")
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.dsbs.newOrder.before.csv"},
            after = "db/GetOrderEventsExecutor.dsbs.newOrder.not.cash.only.after.csv"
    )
    void testNotCashOnlySave() throws IOException {
        List<OrderHistoryEvent> events = events("status_update/events_not_cash_only.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/datasource.csv",
            after = "db/GetOrderEventsStrategy.payment_method_save.after.csv"
    )
    @DisplayName("Сохранение payment_method")
    void paymentMethodSave() throws IOException {
        List<OrderHistoryEvent> events = events("payment_method/payment_method_save.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy.payment_method_change.before.csv",
            after = "db/GetOrderEventsStrategy.payment_method_change.after.csv"
    )
    @DisplayName("Изменение payment_method")
    void paymentMethodChange() throws IOException {
        List<OrderHistoryEvent> events = events("payment_method/payment_method_change.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy.payment_submethod_change.before.csv",
            after = "db/GetOrderEventsStrategy.payment_submethod_change.after.csv"
    )
    @DisplayName("Изменение payment_submethod")
    void paymentSubmethodChange() throws IOException {
        List<OrderHistoryEvent> events = events("payment_submethod/payment_submethod_change.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/datasource.csv",
            after = "db/GetOrderEventsExecutor.payment.submethod.save.after.csv"
    )
    @DisplayName("Сохранение payment_submethod")
    void paymentSubmethodSave() throws IOException {
        List<OrderHistoryEvent> events = events("payment_submethod/payment_submethod_save.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/itemFeeChanged.before.csv",
            after = "db/itemFeeChanged.after.csv"
    )
    @DisplayName("Изменилась рекламная ставка на позицию в заказе")
    void itemFeeChanged() throws IOException {
        List<OrderHistoryEvent> events = events("item_fee_changed/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Override
    public String getResourcePrefix() {
        return PREFIX;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    static GetOrderEventsStrategy buildPartiallyMockedStrategy(EventProcessorSupport support) {
        GetOrderEventsStrategy strategy = Mockito.spy(new GetOrderEventsStrategy(support));
        disableProcessOrderTransactions(strategy);
        return strategy;
    }

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
}
