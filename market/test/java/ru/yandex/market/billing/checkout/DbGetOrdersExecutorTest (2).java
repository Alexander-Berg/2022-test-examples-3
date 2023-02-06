package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.checkout.handler.EventExceptionHandlerException;
import ru.yandex.market.billing.checkout.handler.EventHandlerServiceImpl;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = {"db/cpa_order_status.csv", "db/deliveryTypes.csv", "db/environmentZeroDelay.csv"})
class DbGetOrdersExecutorTest extends FunctionalTest
        implements ResourceHttpUtilitiesMixin, ResourceUtilitiesMixin {

    public static final String RESOURCES_PREFIX = "resources/checkouter_response/events/";

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void setup() {
        this.strategy = new GetOrderEventsStrategy(eventProcessorSupportFactory.createSupport());
    }

    @Test
    @DbUnitDataSet(
            before = "db/newSubsidyEvent.before.csv",
            after = "db/newSubsidyEvent.after.csv"
    )
    @DisplayName("Новый субсидийный платеж.")
    void newSubsidyEvent() throws IOException {
        mockClientWithResource("subsidies_update/checkout.json");
        List<OrderHistoryEvent> events = events("subsidies_update/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/newSubsidyEvent.before.csv",
            after = "db/newSubsidyEvent.after.csv"
    )
    @DisplayName("Новый субсидийный платеж, не обрабатываем повторно.")
    void newSubsidyEventDuplicate() throws IOException {
        mockClientWithResource("subsidies_update/checkout.json");
        List<OrderHistoryEvent> events = events("subsidies_update/events.json");
        strategy.process(events, DATE_2017_01_01);

        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Тестирование событий {@link HistoryEventType#RECEIPT_PRINTED RECEIPT_PRINTED},
     * {@link HistoryEventType#RECEIPT_GENERATED RECEIPT_GENERATED} по которым импортируем связку
     * товарная позиция - транзакция из чеков чекаутера.
     */
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newReceiptEvent.before.csv"},
            after = "db/newReceiptEvent.after.csv"
    )
    @DisplayName("Проверка событий генерации и распечатки чека: RECEIPT_PRINTED RECEIPT_PRINTED.")
    void newReceiptPrintedEvent() throws IOException {
        mockClientWithResource("receipt/checkout.json");
        List<OrderHistoryEvent> events = events("receipt/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Проверяем, что при обработкие событий относящихся к чекам, корректно обрабатывается ситуация, когда
     * запись о чеке по платежу уже существует.
     */
    @DisplayName("Учет уже существующей информации о чеке для платежа")
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newReceiptEvent.withExistingPaymentReceipt.before.csv"},
            after = "db/newReceiptEvent.withExisting.after.csv"
    )
    void test_newReceiptPrintedEventWhenExistingPayment() throws IOException {
        mockClientWithResource("receipt/checkout.existingPaymentReceipt.json");
        List<OrderHistoryEvent> events = events("receipt/events.existingPaymentReceipt.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Проверяем, что не будет импортирован чек с типом OFFSET_ADVANCE_ON_DELIVERED тк он не несёт для
     * нас никакой полезной информации, но при этом мы дублируем у себя записи
     */
    @DisplayName("Пропускаем информацию о OFFSET_ADVANCE_ON_DELIVERED чеках")
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newReceiptEvent.withExistingPaymentReceipt.before.csv"},
            after = "db/offsetAdvanceOnDeliveredReceipt.withExisting.after.csv"
    )
    void test_shouldSkipReceipt_whenOffsetAdvanceOnDeliveryReceiptType() throws IOException {
        mockClientWithResource("receipt/checkout.offsetAdvanceOnDeliveredReceipt.json");
        List<OrderHistoryEvent> events = events("receipt/events.newOffsetAdvanceOnDeliveredReceipt.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Проверяем, что в случае, когда в чеке с типом INCOME было более одного заказа, при обработке чека
     * с типом INCOME_RETURN из базы будут удалены только перечисленные в этом чеке товары.
     */
    @DisplayName("Учёт уже существующей информации о чеке для платежа с несколькими заказами")
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newReceiptEvent.removeOnlySpecifiedItems.before.csv"},
            after = "db/newReceiptEvent.removeOnlySpecifiedItems.after.csv"
    )
    void test_newReceiptEventRemoveOnlySpecifiedItems() throws IOException {
        mockClientWithResource("receipt/checkout.removeOnlySpecifiedItems.json");
        List<OrderHistoryEvent> events = events("receipt/events.removeOnlySpecifiedItems.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Проверяем, что при обработке чека с типом INCOME_RETURN из базы будет удалена запись по доставке.
     */
    @DisplayName("Удаление доставки из чека")
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newReceiptEvent.removeDelivery.before.csv"},
            after = "db/newReceiptEvent.removeDelivery.after.csv"
    )
    void test_newReceiptEventRemoveDelivery() throws IOException {
        mockClientWithResource("receipt/checkout.removeDelivery.json");
        List<OrderHistoryEvent> events = events("receipt/events.removeDelivery.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Проверяем, что при обработкие событий относящихся к чекам, наличие записи о чеке для рефанда приводит к ошибке.
     */
    @DisplayName("Учёт уже существующей информации о чеке для рефанда")
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newReceiptEvent.withExistingRefundReceipt.before.csv"},
            after = "db/newReceiptEvent.withExistingRefundReceipt.after.csv")
    void test_newReceiptPrintedEventWhenExistingRefund() throws IOException {
        mockClientWithResource("receipt/checkout.existingRefundReceipt.json");
        List<OrderHistoryEvent> events = events("receipt/events.existingRefundReceipt.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Тестирование события {@link HistoryEventType#NEW_CASH_PAYMENT} для постоплатных заказов.
     * Проверяем, что импортируются все транзакции.
     */
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newCashPaymentEvent.before.csv"},
            after = "db/newCashPaymentEvent.after.csv"
    )
    @DisplayName("Новый платеж наличными для постоплатных заказов. NEW_CASH_PAYMENT")
    void newCashPaymentEvent() throws IOException {
        mockClientWithResource("new_cash_payment/checkout.json");
        List<OrderHistoryEvent> events = events("new_cash_payment/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Тестирование события {@link HistoryEventType#NEW_COMPENSATION} для постоплатных заказов.
     * Проверяем, что импортируются все транзакции и чеки.
     */
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newCompensationEvent.before.csv"},
            after = "db/newCompensationEvent.after.csv"
    )
    @DisplayName("Компенсация для постоплатных заказов. NEW_COMPENSATION")
    void newCompensationEvent() throws IOException {
        mockClientWithResource("new_compensation/checkout.json");
        List<OrderHistoryEvent> events = events("new_compensation/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Тестирование события {@link HistoryEventType#REFUND} для заказов, которые были куплены в кредит.
     * Проверяем, что импортируются рефандовые транзакции.
     */
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newCreditRefund.before.csv"},
            after = "db/newCreditRefund.after.csv"
    )
    @DisplayName("Новый рефанд на кредит. REFUND")
    void newCreditRefund() throws IOException {
        mockClientWithResource("new_credit_refund/checkout.json");
        List<OrderHistoryEvent> events = events("new_credit_refund/events.json");
        strategy.process(events, DATE_2017_01_01); }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/newSupplierPaymentEvent.before.csv"},
            after = "db/newSupplierPaymentEvent.after.csv"
    )
    @DisplayName("Новый платеж поставщику в рамках кредита.")
    void newSupplierPaymentEvent() throws IOException {
        mockClientWithResource("new_supplier_payment/checkout.json");
        List<OrderHistoryEvent> events = events("new_supplier_payment/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Тестирование события {@link HistoryEventType#NEW_COMPENSATION} для постоплатных заказов.
     * Проверяем, что импортируются все транзакции и чеки при повторной обработке ивента.
     */
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/repeatCompensationEvent.before.csv"},
            after = "db/repeatCompensationEvent.after.csv"
    )
    @DisplayName("Повторный забор компенсации, сохраняем все чеки и транзакции.")
    void repeatCompensationEvent() throws IOException {
        mockClientWithResource("repeat_compensation/checkout.json");
        List<OrderHistoryEvent> events = events("repeat_compensation/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @DisplayName("Проверка обновления посылок в заказе при получении событий " +
            "PARCEL_BOXES_CHANGED, ORDER_DELIVERY_UPDATED")
    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/testParcelsUpdateOnEvents.before.csv"},
            after = "db/testParcelsUpdateOnEvents.after.csv"
    )
    void testParcelsUpdateEvents() throws IOException {
        List<OrderHistoryEvent> events = events("parcels_info_update/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @DisplayName("Проверка, что джоба не падает, если пытается обновить посылки для заказов, неимпортированных в mbi")
    @Test
    @DbUnitDataSet(before = {"db/datasource.csv", "db/testParcelsUpdateOnEvents.before.csv"})
    void testNonExistingOrderParcelsUpdate() throws IOException {
        List<OrderHistoryEvent> events = events("non_existing_order_parcels/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DisplayName("Провека обновления информации о доставке в заказе при получении события ORDER_DELIVERY_UPDATED")
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/testDeliveryUpdateOnEvents.before.csv"},
            after = "db/testDeliveryUpdateOnEvents.after.csv"
    )
    void test_deliveryUpdateEvent() throws IOException {
        List<OrderHistoryEvent> events = events("delivery_info_update/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DisplayName("Провека обновления, shopOrderId=null не перезаписывает значение в order_num")
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/testOrderNumNullUpdateEvent.before.csv"},
            after = "db/testOrderNumNullUpdateEvent.after.csv"
    )
    void test_orderNumNullUpdateEvent() throws IOException {
        List<OrderHistoryEvent> events = events("order_num_null_update/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DisplayName("Провека обновления, shopOrderId записывается в order_num")
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/testOrderNumUpdateEvent.before.csv"},
            after = "db/testOrderNumUpdateEvent.after.csv"
    )
    void test_orderNumUpdateEvent() throws IOException {
        List<OrderHistoryEvent> events = events("order_num_update/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.dsbs.before.csv"},
            after = "db/GetOrderEventsExecutor.dsbs.after.csv"
    )
    @DisplayName("Новый чек по DSBS заказу.")
    void newDropshipBySellerReceiptEvent() throws IOException {
        mockClientWithResource("new_dsbs_receipt/checkout.json");
        List<OrderHistoryEvent> events = events("new_dsbs_receipt/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.dsbs.newOrder.before.csv"} ,
            after = "db/GetOrderEventsExecutor.dsbs.newOrder.after.csv"
    )
    @DisplayName("Новый DSBS заказ.")
    void newDropshipBySellerOrder() throws IOException {
        List<OrderHistoryEvent> events = events("new_dsbs_order/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.dsbs.newOrder.before.csv"} ,
            after = "db/GetOrderEventsExecutor.dsbs.newOrder.cash.only.after.csv"
    )
    @DisplayName("Новый заказ dsbs cash-only.")
    void newDSBSCashOnlyOrder() throws IOException {
        List<OrderHistoryEvent> events = events("new_dsbs_order/events_cash_only.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.dsbs.newOrder.before.csv"} ,
            after = "db/GetOrderEventsExecutor.dsbs.newOrder.not.cash.only.after.csv"
    )
    @DisplayName("Новый заказ dsbs, не cash-only.")
    void newDSBSNotCashOnlyOrder() throws IOException {
        List<OrderHistoryEvent> events = events("new_dsbs_order/events_not_cash_only.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/GetOrderEventsExecutor.multiOrder.before.csv"},
            after = "db/GetOrderEventsExecutor.multiOrder.after.csv"
    )
    @DisplayName("Забор чековых позиций для мультизаказа.")
    void newMultiOrderReceiptEvent() throws IOException {
        mockClientWithResource("new_multi_order_receipt/checkout.json");
        List<OrderHistoryEvent> events = events("new_multi_order_receipt/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "db/params.csv",
                    "db/currency_rate.csv",
                    "db/datasource.csv",
                    "db/GetOrderEventsExecutor.saveAgencyCommissionTrantime.before.csv"
            },
            after = "db/GetOrderEventsExecutor.saveAgencyCommissionTrantime.after.csv"
    )
    @DisplayName("Сохранение записей по АВ для обилливания.")
    void saveAgencyCommissionTrantime() throws IOException {
        mockClientWithResource("save_agency_commission/checkout_save.json");
        List<OrderHistoryEvent> events = events("save_agency_commission/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "db/params.csv",
                    "db/currency_rate.csv",
                    "db/datasource.csv",
                    "db/GetOrderEventsExecutor.saveAgencyCommissionTrantime.before.csv"
            },
            after = "db/GetOrderEventsExecutor.notSaveAgencyCommissionTrantime.after.csv"
    )
    @DisplayName("Не сохраняем белый постоплатный заказ.")
    void saveAgencyCommissionWhitePostpaidOrder() throws IOException {
        mockClientWithResource("save_agency_commission/checkout_save.json");
        List<OrderHistoryEvent> events = events("save_agency_commission/events_white_postpaid.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "db/params.csv",
                    "db/currency_rate.csv",
                    "db/datasource.csv",
                    "db/GetOrderEventsExecutor.saveAgencyCommissionTrantime.before.csv"
            },
            after = "db/GetOrderEventsExecutor.notSaveAgencyCommissionTrantime.after.csv"
    )
    @DisplayName("Не сохраняем событие по АВ, если нет даты изменения статуса платежа.")
    void saveAgencyCommissionNoDateTrantime() throws IOException {
        mockClientWithResource("save_agency_commission/checkout_no_date.json");
        List<OrderHistoryEvent> events = events("save_agency_commission/events_no_payment.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "db/params.csv",
                    "db/currency_rate.csv",
                    "db/datasource.csv",
                    "db/GetOrderEventsExecutor.saveAgencyCommissionTrantime.before.csv"
            },
            after = "db/GetOrderEventsExecutor.notSaveAgencyCommissionTrantime.after.csv"
    )
    @DisplayName("Не сохраняем событие по АВ, если нет платежа.")
    void saveAgencyCommissionEmptyPaymentTrantime() throws IOException {
        mockClientWithResource("save_agency_commission/checkout_empty_payment.json");
        List<OrderHistoryEvent> events = events("save_agency_commission/events_no_payment.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "db/params.csv",
                    "db/currency_rate.csv",
                    "db/datasource.csv",
                    "db/GetOrderEventsExecutor.saveAgencyCommissionTrantime.before.csv"
            },
            after = "db/GetOrderEventsExecutor.notSaveAgencyCommissionTrantime.after.csv"
    )
    @DisplayName("Не обрабатываем передачу платежа в баланс для белых постоплатных заказов.")
    void notSaveAgencyCommissionTrantime() throws IOException {
        mockClientWithResource("save_agency_commission/checkout_not_save.json");
        List<OrderHistoryEvent> events = events("save_agency_commission/events_not_save.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            after = "db/GetOrderEventsExecutor.handleErrorEventTest.after.xml",
            type = DataSetType.XML
    )
    @DisplayName("Корректная обработка ошибок при заборе событий из чекаутера.")
    void handleErrorEvent() {
        List<OrderHistoryEvent> events = makeEvents(2, true);

        EventProcessorSupport supportSpy = Mockito.spy(eventProcessorSupportFactory.createSupport());
        Mockito.when(supportSpy.getOrderService())
                .thenThrow(IllegalStateException.class);

        new GetOrderEventsStrategy(supportSpy)
                .process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            after = "db/GetOrderEventsExecutor.handleIgnoredEvent.after.xml",
            type = DataSetType.XML
    )
    @DisplayName("Игнорирование события из чекаутера.")
    void handleIgnoredEvent() {
        List<OrderHistoryEvent> events = makeEvents(2, false);

        EventProcessorSupport supportSpy = Mockito.spy(eventProcessorSupportFactory.createSupport());
        Mockito.when(supportSpy.getOrderService())
                .thenThrow(IllegalStateException.class);

        GetOrderEventsStrategy strategy = new GetOrderEventsStrategy(supportSpy);
        strategy.process(events, DATE_2017_01_01);
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DisplayName("Превышение лимита игнорирования событий.")
    void overLimitOnHandleEvents() {
        List<OrderHistoryEvent> events = makeEvents(1, false);

        EventProcessorSupport supportSpy = Mockito.spy(eventProcessorSupportFactory.createSupport());
        EventHandlerServiceImpl handlerServiceMock = Mockito.mock(EventHandlerServiceImpl.class);

        Mockito.doThrow(EventExceptionHandlerException.class)
                .when(handlerServiceMock)
                .handleExceptionOrderEvent(any(), any());
        Mockito.doNothing()
                .when(handlerServiceMock)
                .handleIgnoredOrderEvent(any());

        Mockito.when(supportSpy.getOrderService())
                .thenThrow(IllegalStateException.class);
        Mockito.when(supportSpy.getEventExceptionHandlerService())
                .thenReturn(handlerServiceMock);
        Mockito.when(supportSpy.getEventExceptionHandlerService())
                .thenReturn(handlerServiceMock);

        GetOrderEventsStrategy strategy = new GetOrderEventsStrategy(supportSpy);
        // Падаем в первый раз
        Assertions.assertThrows(
                EventExceptionHandlerException.class,
                () -> strategy.process(events, DATE_2017_01_01)
        );
        // Эта же ошибка должна воспроизводиться
        Assertions.assertThrows(
                EventExceptionHandlerException.class,
                () -> strategy.process(events, DATE_2017_01_01)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkVirtualBnplPayment.before.csv",
            after = "db/checkVirtualBnplPayment.after.csv"
    )
    @DisplayName("Добавление платежа виртуальный BNPL")
    void checkVirtualBnplPayment() throws IOException {
        mockClientWithResource("bnpl/checkout.json");
        List<OrderHistoryEvent> events = events("bnpl/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkVirtualBnplRefund.before.csv",
            after = "db/checkVirtualBnplRefund.after.csv"
    )
    @DisplayName("Добавление REFUND BNPL")
    void checkVirtualBnplRefund() throws IOException {
        mockClientWithResource("bnpl_refund/checkout.json");
        List<OrderHistoryEvent> events = events("bnpl_refund/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedProcessing.before.csv",
            after = "db/checkPaymentClearedProcessing.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED")
    void checkPaymentClearedProcessing() throws IOException {
        mockClientWithResource("payment_cleared/events.json");
        List<OrderHistoryEvent> events = events("payment_cleared/payment_cleared_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedProcessingBnpl.before.csv",
            after = "db/checkPaymentClearedProcessingBnpl.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED для BNPL")
    void checkPaymentClearedBnplProcessing() throws IOException {
        mockClientWithResource("payment_cleared_bnpl/events.json");
        List<OrderHistoryEvent> events = events("payment_cleared_bnpl/payment_cleared_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedProcessing.mbiControlEnabled.before.csv",
            after = "db/checkPaymentClearedProcessing.emptyAccruals.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED с mbi_control_enabled = false")
    void checkPaymentClearedProcessingMbiControlEnabledFalse() throws IOException {
        mockClientWithResource("payment_cleared/events_mbi_control_enabled_false.json");
        List<OrderHistoryEvent> events = events("payment_cleared/payment_cleared_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedProcessing.envFalse.before.csv",
            after = "db/checkPaymentClearedProcessing.emptyAccruals.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED с env = false")
    void checkPaymentClearedProcessingAccrualEnvFalse() throws IOException {
        mockClientWithResource("payment_cleared/events.json");
        List<OrderHistoryEvent> events = events("payment_cleared/payment_cleared_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedProcessing.processingFlagFalse.before.csv",
            after = "db/checkPaymentClearedProcessing.processingFlagFalse.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED с isProcessPaymentClearedEvent = false")
    void checkPaymentClearedProcessingFlagFalse() throws IOException {
        mockClientWithResource("payment_cleared/events.json");
        List<OrderHistoryEvent> events = events("payment_cleared/payment_cleared_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptGeneratedEvent.before.csv"},
            after = "db/receiptGeneratedEvent.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_GENERATED. Refund")
    void receiptGeneratedEvent() throws IOException {
        mockClientWithResource("receipt/checkout.json");
        List<OrderHistoryEvent> events = events("receipt/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptGeneratedEventBnpl.before.csv"},
            after = "db/receiptGeneratedEventBnpl.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_GENERATED. Payment BNPL")
    void receiptGeneratedEventBnpl() throws IOException {
        mockClientWithResource("receipt/checkoutBnpl.json");
        List<OrderHistoryEvent> events = events("receipt/receiptGeneratedEventBnpl.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptGeneratedEventSubsidy.before.csv"},
            after = "db/receiptGeneratedEventSubsidy.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_GENERATED. Payment Subsidy")
    void receiptGeneratedEventSubsidy() throws IOException {
        mockClientWithResource("receipt/checkoutSubsidy.json");
        List<OrderHistoryEvent> events = events("receipt/receiptGeneratedEventSubsidy.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/receiptGeneratedEventFor613Refund.before.csv",
            after = "db/receiptGeneratedEventFor613Refund.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_GENERATED. Refund по 613 сервису")
    void receiptGeneratedEventFor613Refund() throws IOException {
        mockClientWithResource("receipt/checkout.receiptGeneratedFor613Refund.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptGeneratedFor613Refund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/receiptGeneratedEventForOld613Refund.before.csv",
            after = "db/receiptGeneratedEventForOld613Refund.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_GENERATED. Refund по 613 сервису через Баланс")
    void receiptGeneratedEventForOld613Refund() throws IOException {
        mockClientWithResource("receipt/checkout.receiptGeneratedForOld613Refund.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptGeneratedForOld613Refund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptPrintedEventForRefund.before.csv"},
            after = "db/receiptPrintedEventForRefund.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_PRINTED. Refund")
    void receiptPrintedEventForRefund() throws IOException {
        mockClientWithResource("receipt/checkout.refundForReceiptPrinted.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptPrintedForRefund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/receiptPrintedEventFor613Refund.before.csv",
            after = "db/receiptPrintedEventFor613Refund.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_PRINTED. Refund по 613 сервису")
    void receiptPrintedEventFor613Refund() throws IOException {
        mockClientWithResource("receipt/checkout.receiptPrintedFor613Refund.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptPrintedFor613Refund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/receiptPrintedEventForOld613Refund.before.csv",
            after = "db/receiptPrintedEventForOld613Refund.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_PRINTED. Refund по 613 сервису через Баланс")
    void receiptPrintedEventForOld613Refund() throws IOException {
        mockClientWithResource("receipt/checkout.receiptPrintedForOld613Refund.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptPrintedForOld613Refund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptPrintedEventForBnplPayment.before.csv"},
            after = "db/receiptPrintedEventForBnplPayment.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_PRINTED. Payment BNPL")
    void receiptPrintedEventForBnplPayment() throws IOException {
        mockClientWithResource("receipt/checkout.bnplPaymentForReceiptPrinted.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptPrintedForBnplPayment.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptPrintedEventForSubsidyPayment.before.csv"},
            after = "db/receiptPrintedEventForSubsidyPayment.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_PRINTED. Payment Subsidy")
    void receiptPrintedEventForSubsidyPayment() throws IOException {
        mockClientWithResource("receipt/checkout.subsidyPaymentForReceiptPrinted.json");
        List<OrderHistoryEvent> events = events("receipt/events.receiptPrintedForSubsidyPayment.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/cashRefundReceiptPrintedEventForRefund.before.csv"},
            after = "db/cashRefundReceiptPrintedEventForRefund.after.csv"
    )
    @DisplayName("Проверка событий чека: CASH_REFUND_RECEIPT_PRINTED. Refund")
    void cashRefundReceiptPrintedEventForRefund() throws IOException {
        mockClientWithResource("receipt/checkout.refundForCashRefundReceiptPrinted.json");
        List<OrderHistoryEvent> events = events("receipt/events.cashRefundReceiptPrintedForRefund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/cashRefundReceiptPrintedEventFor613Refund.before.csv",
            after = "db/cashRefundReceiptPrintedEventFor613Refund.after.csv"
    )
    @DisplayName("Проверка событий чека: CASH_REFUND_RECEIPT_PRINTED. Refund по 613 сервису")
    void cashRefundReceiptPrintedEventFor613Refund() throws IOException {
        mockClientWithResource("receipt/checkout.cashRefundReceiptPrintedFor613Refund.json");
        List<OrderHistoryEvent> events = events("receipt/events.cashRefundReceiptPrintedFor613Refund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/cashRefundReceiptPrintedEventForOld613Refund.before.csv",
            after = "db/cashRefundReceiptPrintedEventForOld613Refund.after.csv"
    )
    @DisplayName("Проверка событий чека: CASH_REFUND_RECEIPT_PRINTED. Refund по 613 сервису через Баланс")
    void cashRefundReceiptPrintedEventForOld613Refund() throws IOException {
        mockClientWithResource("receipt/checkout.cashRefundReceiptPrintedForOld613Refund.json");
        List<OrderHistoryEvent> events = events("receipt/events.cashRefundReceiptPrintedForOld613Refund.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/cashRefundReceiptPrintedEventForBnplPayment.before.csv"},
            after = "db/cashRefundReceiptPrintedEventForBnplPayment.after.csv"
    )
    @DisplayName("Проверка событий чека: CASH_REFUND_RECEIPT_PRINTED. Payment BNPL")
    void cashRefundReceiptPrintedEventForBnplPayment() throws IOException {
        mockClientWithResource("receipt/checkout.bnplPaymentForCashRefundReceiptPrinted.json");
        List<OrderHistoryEvent> events = events("receipt/events.cashRefundReceiptPrintedForBnplPayment.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/cashRefundReceiptPrintedEventForSubsidyPayment.before.csv"},
            after = "db/cashRefundReceiptPrintedEventForSubsidyPayment.after.csv"
    )
    @DisplayName("Проверка событий чека: CASH_REFUND_RECEIPT_PRINTED. Payment Subsidy")
    void cashRefundReceiptPrintedEventForSubsidyPayment() throws IOException {
        mockClientWithResource("receipt/checkout.subsidyPaymentForCashRefundReceiptPrinted.json");
        List<OrderHistoryEvent> events = events("receipt/events.cashRefundReceiptPrintedForSubsidyPayment.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedProcessingWithReceiptFilter.before.csv",
            after = "db/checkPaymentClearedProcessingWithReceiptFilter.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED с фильтрацией чеков")
    void checkPaymentClearedProcessingWithReceiptFilter() throws IOException {
        mockClientWithResource("payment_cleared_receipt/events.json");
        List<OrderHistoryEvent> events = events("payment_cleared_receipt/payment_cleared_receipt_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/spasiboCorrectionReceipt.before.csv",
            after = "db/spasiboCorrectionReceipt.after.csv"
    )
    @DisplayName("Изменение spasibo для позиции чека")
    void spasiboCorrectionReceipt() throws IOException {
        mockClientWithResource("spasibo_correction/checkout.json");
        List<OrderHistoryEvent> events = events("spasibo_correction/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/cashbackCorrectionReceipt.before.csv",
            after = "db/cashbackCorrectionReceipt.after.csv"
    )
    @DisplayName("Изменение yandex_cashback для позиции чека")
    void cashbackCorrectionReceipt() throws IOException {
        mockClientWithResource("cashback_correction/checkout.json");
        List<OrderHistoryEvent> events = events("cashback_correction/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/payment_method_change.before.csv",
            after = "db/payment_method_change.after.csv"
    )
    @DisplayName("Изменение payment_method")
    void paymentMethodChange() throws IOException {
        List<OrderHistoryEvent> events = events("payment_method_change/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/payment_submethod_change.before.csv",
            after = "db/payment_submethod_change.after.csv"
    )
    @DisplayName("Изменение payment_submethod")
    void paymentSubmethodChange() throws IOException {
        List<OrderHistoryEvent> events = events("payment_submethod_change/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/payment_submethod_save.before.csv",
            after = "db/payment_submethod_save.after.csv"
    )
    @DisplayName("Сохранение payment_submethod")
    void paymentSubmethodSave() throws IOException {
        List<OrderHistoryEvent> events = events("payment_submethod_save/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkInsertIntoPayout.before.csv",
            after = "db/checkInsertIntoPayout.after.csv"
    )
    @DisplayName("Создание order_payout_trantime для заказа, который перешел в статус Delivered")
    void saveOrderPayoutTrantime() throws IOException {
        List<OrderHistoryEvent> events = events("payout_delivered/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/DbGetOrdersExecutorTest.saveProcessedOrderPayoutTrantimeB2bAccountPrepayment.before.csv",
            after = "db/DbGetOrdersExecutorTest.saveProcessedOrderPayoutTrantimeB2bAccountPrepayment.after.csv"
    )
    @DisplayName("Создание НЕ обработанного order_payout_trantime для типа оплаты B2B_ACCOUNT_PREPAYMENT")
    void saveProcessedOrderPayoutTrantimeB2bAccountPrepayment() throws IOException {
        List<OrderHistoryEvent> events = events("payout_delivered/b2b_account_prepayment_events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkInsertIntoPayoutNonDelivered.before.csv",
            after = "db/checkInsertIntoPayoutNonDelivered.after.csv"
    )
    @DisplayName("Если заказ не перешел в статус Delivered, order_payout_trantime не создается")
    void doNotSaveOrderPayoutTrantime() throws IOException {
        List<OrderHistoryEvent> events = events("payout_nondelivered/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkSaveInstallmentRefund.before.csv",
            after = "db/checkSaveInstallmentRefund.after.csv"
    )
    @DisplayName("Сохранение installment refund")
    void saveInstallmentRefund() throws IOException {
        mockClientWithResource("installment_refund/checkout.json");
        List<OrderHistoryEvent> events = events("installment_refund/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkB2b.before.csv",
            after = "db/checkB2bPayment.after.csv"
    )
    @DisplayName("Добавление платежа B2B")
    void checkB2bPayment() throws IOException {
        mockClientWithResource("b2b/checkout.json");
        List<OrderHistoryEvent> events = events("b2b/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkB2b.before.csv",
            after = "db/checkB2bRefund.after.csv"
    )
    @DisplayName("Добавление REFUND B2B")
    void checkB2bRefund() throws IOException {
        mockClientWithResource("b2b_refund/checkout.json");
        List<OrderHistoryEvent> events = events("b2b_refund/events.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/checkPaymentClearedB2b.before.csv",
            after = "db/checkPaymentClearedB2b.after.csv"
    )
    @DisplayName("Обработка события PAYMENT_CLEARED для B2b")
    void checkPaymentClearedB2bProcessing() throws IOException {
        mockClientWithResource("payment_cleared_b2b/events.json");
        List<OrderHistoryEvent> events = events("payment_cleared_b2b/payment_cleared_event.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/datasource.csv", "db/receiptGeneratedEventB2b.before.csv"},
            after = "db/receiptGeneratedEventB2b.after.csv"
    )
    @DisplayName("Проверка событий чека: RECEIPT_GENERATED. B2b")
    void receiptGeneratedEventB2b() throws IOException {
        mockClientWithResource("receipt/checkoutB2b.json");
        List<OrderHistoryEvent> events = events("receipt/receiptGeneratedEventB2b.json");
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

    private List<OrderHistoryEvent> makeEvents(long count, boolean differentOrders) {
        return LongStream.range(0, count)
                .mapToObj(l -> {
                    OrderHistoryEvent event = new OrderHistoryEvent();
                    event.setId(l);
                    event.setType(HistoryEventType.ARCHIVED);

                    Order orderAfter = new Order();
                    orderAfter.setId(differentOrders ? l : 1L);
                    event.setOrderAfter(orderAfter);

                    return event;
                }).collect(Collectors.toList());
    }

    @Override
    public RestTemplate getRestTemplate() {
        return checkouterRestTemplate;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String getResourcePrefix() {
        return RESOURCES_PREFIX;
    }
}
