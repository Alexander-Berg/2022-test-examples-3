package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATETIME_2017_01_01_01_02_03;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATETIME_2017_02_26_19_22_37;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;

/**
 * Обработка события {@link HistoryEventType#ORDER_STATUS_UPDATED}.
 */
@DisplayName("Обработка события ORDER_STATUS_UPDATE")
@DbUnitDataSet(before = {"db/datasource.csv", "db/environmentZeroDelay.csv"})
@ActiveProfiles("goe-processing")
class DbGetOrdersExecutorOrderStatusUpdatedTest extends FunctionalTest
        implements ResourceHttpUtilitiesMixin, ResourceUtilitiesMixin {

    private static final String RESOURCE_PREFIX = "resources/checkouter_response/events/";

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    @Autowired
    private RestTemplate checkouterRestTemplate;

    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void onBefore() {
        this.strategy = new GetOrderEventsStrategy(eventProcessorSupportFactory.createSupport());
    }

    /**
     * Тестирует перевод заказа из {@link OrderStatus#PROCESSING} в {@link OrderStatus#CANCELLED}.
     */
    @DisplayName("PROCESSING -> CANCELLED (do not cancel payment transaction)")
    @Test
    @DbUnitDataSet(
            before = "db/testProcessingToCancelled.before.csv",
            after = "db/testProcessingToCancelledWithoutCancelingTransaction.after.csv"
    )
    void testProcessingToCancelledWithoutCancelingTransaction() throws IOException {
        runTest(
                "status_update/existing_order/",
                "processingToCancelledWithoutCancelingTransaction.json",
                DATETIME_2017_02_26_19_22_37
        );
    }

    @DisplayName("PROCESSING -> CANCELLED (cancel payment transaction)")
    @Test
    @DbUnitDataSet(
            before = "db/testProcessingToCancelled.before.csv",
            after = "db/testProcessingToCancelled.after.csv"
    )
    void testProcessingToCancelledWithCancelingTransaction() throws IOException {
        runTest(
                "status_update/existing_order/",
                "processingToCancelledWithCancelingTransaction.json",
                DATETIME_2017_02_26_19_22_37
        );
    }

    /**
     * Обработку получения информации о предоплатном платеже с {@link PaymentMethod#APPLE_PAY}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже с APPLE_PAY")
    @Test
    @DbUnitDataSet(
            before = "db/supplier.csv",
            after = "db/StoreTransactions.unpaidToProcessing.ApplePay.after.csv"
    )
    void test_applePayTransactionProcessing() throws IOException {
        runTest(
                "status_update/new_order/",
                "unpaidToProcessing-paymentApplePay-deliveryTypeIsFake.json",
                DATE_2017_01_01
        );
    }

    /**
     * Обработку получения информации о предоплатном платеже с {@link PaymentMethod#GOOGLE_PAY}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже с GOOGLE_PAY")
    @Test
    @DbUnitDataSet(
            before = "db/supplier.csv",
            after = "db/StoreTransactions.unpaidToProcessing.GooglePay.after.csv"
    )
    void test_googlePayTransactionProcessing() throws IOException {
        runTest(
                "status_update/new_order/",
                "unpaidToProcessing-paymentGooglePay-deliveryTypeIsFake.json",
                DATE_2017_01_01
        );
    }

    /**
     * Обработку получения информации о предоплатном платеже с {@link PaymentMethod#TINKOFF_CREDIT}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже с TINKOFF_CREDIT")
    @Test
    @DbUnitDataSet(
            before = "db/supplier.csv",
            after = "db/StoreTransactions.unpaidToProcessing.TinkoffCredit.after.csv"
    )
    void test_tinkoffCreditTransactionProcessing() throws IOException {
        runTest(
                "status_update/new_order/",
                "unpaidToProcessing-paymentTinkoffCredit-deliveryTypeIsFake.json",
                DATE_2017_01_01
        );
    }


    /**
     * Обработку получения информации о предоплатном платеже с {@link PaymentMethod#SBP}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже по СБП")
    @Test
    @DbUnitDataSet(
            before = "db/supplier.csv",
            after = "db/StoreTransactions.unpaidToProcessing.Sbp.after.csv"
    )
    void test_SbpTransactionProcessing() throws IOException {
        runTest(
                "status_update/new_order/",
                "unpaidToProcessing-paymentSbp-deliveryTypeIsFake.json",
                DATE_2017_01_01
        );
    }

    @Test
    @DisplayName("RESERVED -> UNPAID -> PENDING -> PROCESSING")
    @DbUnitDataSet(
            before = {"db/testReservedToUnpaidToPendingToProcessing.before.csv"},
            after = "db/testReservedToUnpaidToPendingToProcessing.after.csv"
    )
    void testReservedToUnpaidToPendingToProcessing() throws IOException {
        runTest(
                "status_update/existing_order/",
                "reservedToUnpaidToPendingToProcessing.json",
                DATETIME_2017_02_26_19_22_37
        );
    }

    @Test
    @DisplayName("RESERVED -> CANCELLED, UNPAID -> CANCELLED, PENDING -> CANCELLED")
    @DbUnitDataSet(
            before = {"db/testReservedUnpaidPendingToCancelled.before.csv"},
            after = "db/testReservedUnpaidPendingToCancelled.after.csv"
    )
    void testReservedUnpaidPendingToCancel() throws IOException {
        runTest(
                "status_update/existing_order/",
                "reservedUnpaidPendingToCancelled.json",
                DATETIME_2017_02_26_19_22_37
        );
    }

    @Test
    @DisplayName("Проверка сохранения отмены доставки при валидном и не валидном substatus")
    @DbUnitDataSet(
            before = "db/testDeliveryToCustomerReturn.before.csv",
            after = "db/testDeliveryToCustomerReturn.after.csv"
    )
    void testDeliveryToCustomerReturn() throws IOException {
        runTest(
                "status_update/existing_order/",
                "delivery_to_customer_return.json",
                DATETIME_2017_02_26_19_22_37
        );
    }


    /**
     * Переход из {@link OrderStatus#PENDING} в {@link OrderStatus#CANCELLED}.
     * Ожидается, что для синего нефулфиллмент заказа, у которого isMarketDelivery = true и подстатусы SHOP_FAILED,
     * PENDING_EXPIRED, WAREHOUSE_FAILED_TO_SHIP или MISSING_ITEM, будет сохранен CANCELLED_ORDER_FEE
     * в market_billing.order_trantimes.
     */
    @Test
    @DbUnitDataSet(
            before = "db/" +
                    "GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Dropship" +
                    ".before.csv",
            after = "db/" +
                    "GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Dropship" +
                    ".after.csv"
    )
    void test_process_when_orderExistingInPending_should_updateToCancelled_for_market_delivery_dropship()
            throws IOException {
        List<OrderHistoryEvent> events = events("status_update/existing_order/pendingToCancelled-with" +
                "-itemsAndDelivery-paymentYandex_MarketDelivery_Dropship.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#PROCESSING} в {@link OrderStatus#DELIVERY}.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только общий пайплайн.
     * Детально следует покрыть дополнительно.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivery.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivery.after.csv"
    )
    void test_process_when_orderExistingInProcessing_should_updateToDelivery() throws IOException {
        List<OrderHistoryEvent> events = events("status_update/existing_order/processingToDelivery-with" +
                "-itemsAndDelivery-paymentYandex.json");

        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERY} в {@link OrderStatus#DELIVERED}.
     * Тест проверяет, что сохраняется trantime для loyalty_participation_fee и ff_processing.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToDelivered_MarketDelivery_Blue_loyalty_participation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToDelivered_MarketDelivery_Blue_loyalty_participation.after.csv"
    )
    void testProcessWhenOrderExistingInDeliveryShouldUpdateToDeliveredForMarketDeliveryBlueLoyaltyParticipation()
            throws IOException {
        List<OrderHistoryEvent> events = events("status_update/existing_order/deliveryToDelivered-with" +
                "-itemsAndDelivery-paymentYandex_MarketDelivery_Blue_loyalty_participation.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#PROCESSING} в {@link OrderStatus#DELIVERY}.
     * Тест проверяет, что сохраняется trantime для ff_processing.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToDelivered_MarketDelivery_Blue_loyalty_participation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToDelivery_MarketDelivery_Blue_fulfillment_loyalty_participation.after.csv"
    )
    void testProcessOrderInProcessingShouldUpdateToDeliveryForMarketDeliveryFulfillmentBlueLoyaltyParticipation()
            throws IOException {
        List<OrderHistoryEvent> events = events("status_update/existing_order/processingToDelivery-with" +
                "-itemsAndDelivery-paymentYandex_MarketDelivery_Blue_Fulfillment_loyalty_participation.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERY} в {@link OrderStatus#DELIVERED}.
     * Тест проверяет, что сохраняется trantime для cpa_auction.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToDelivered_MarketDelivery_Blue_cpa_auction.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToDelivered_MarketDelivery_Blue_cpa_auction.after.csv"
    )
    void testProcessWhenOrderExistingInDeliveryShouldUpdateToDeliveredForCpaAuction()
            throws IOException {
        List<OrderHistoryEvent> events = events("status_update/existing_order/deliveryToDelivered-with" +
                "-itemsAndDelivery-paymentYandex_MarketDelivery_Blue_cpa_auction.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERED} в {@link OrderStatus#CANCELLED}.
     * Тест проверяет, что сохраняется trantime для loyalty_participation_fee_cancellation.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToCancelled_MarketDelivery_FulfillmentBlue_loyalty_participation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder" +
                    "_ToCancelled_MarketDelivery_FulfillmentBlue_loyalty_participation.after.csv"
    )
    void test_process_when_FulfillmentBlueOrderExistingInDelivered_should_updateToCancelled_loyalty_participation()
            throws IOException {
        List<OrderHistoryEvent> events = events("status_update/existing_order/deliveredToCancelled-with-" +
                "itemsAndDelivery-paymentYandex_MarketDelivery_FulfillmentBlue_loyalty_participation.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * На примере перехода из {@link OrderStatus#DELIVERY} в {@link OrderStatus#DELIVERED}
     * проверяем, что для отмеченного как ignored заказ не обарабтываются события.
     * Тест не покрывает всех возможных комбинаицй, только общий кейс.
     * Детально следует покрыть дополнительно.
     */
    @DisplayName("Заказ отмеченный как ignored в env, не обрабатывается")
    @Test
    @DbUnitDataSet(
            before = {
                    "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue" +
                            ".before.csv",
                    "db/ignored_orders.before.csv"
            },
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue" +
                    ".ignored.after.csv"
    )
    void test_process_when_orderExistingInProcessingAndIgnored_shouldNot_beProcessed() throws IOException {
        List<OrderHistoryEvent> events = events(
                "status_update/existing_order/deliveryToDelivered-with-itemsAndDelivery" +
                        "-paymentYandex_MarketDelivery_Blue.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Blue" +
                    ".before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Blue" +
                    ".after.csv"
    )
    @DisplayName("Billing status не меняется, когда заказ из DELIVERY переходит в CANCELLED.")
    void test_process_when_orderFromDeliveredToCancelled_shouldNot_changeBilledStatus() throws IOException {
        List<OrderHistoryEvent> events = events(
                "status_update/existing_order/deliveredToCancelled-with-itemsAndDelivery" +
                        "-paymentYandex_MarketDelivery_Blue.json"
        );

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_processingToCancelled_without_substatus.before.csv",
            after = "db/GetOrderEventsStrategy_processingToCancelled_without_substatus.after.csv"
    )
    void test_processingToCancelled_without_substatus() throws IOException {
        OrderHistoryEvent events =
                events("status_update/existing_order/processingToCancelled-without-substatus.json").get(0);
        strategy.processIfNecessary(events, DATETIME_2017_01_01_01_02_03);
    }

    private void runTest(String commonPath, String fileName, Date date) throws IOException {
        mockClientWithResource(commonPath + fileName);
        List<OrderHistoryEvent> events = events(commonPath + "events/" + fileName);
        strategy.process(events, date);
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
        return RESOURCE_PREFIX;
    }
}
