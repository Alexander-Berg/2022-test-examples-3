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
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
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
 * Является обновленной версией {@link GetOrderEventsStrategy_StatusUpdatedEvent_UpdateExistingOrder_Test}.
 * Использует более продовый конфиг.
 */
@DisplayName("Обработка события ORDER_STATUS_UPDATE")
@DbUnitDataSet(before = {"db/datasource.csv", "db/params.csv", "db/environmentZeroDelay.csv"})
class DbGetOrdersExecutor_OrderStatusUpdated_Test extends FunctionalTest
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
            before = {"db/cpa_order_status.csv", "db/testProcessingToCancelled.before.csv"},
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
            before = {"db/cpa_order_status.csv", "db/testProcessingToCancelled.before.csv"},
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
     * Обработку получения инфомрации о предоплатном платеже с {@link PaymentMethod#APPLE_PAY}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже с APPLE_PAY")
    @Test
    @DbUnitDataSet(
            before = {
                    "db/currency_rate.csv",
                    "db/deliveryTypes.csv",
                    "db/supplier.csv",
                    "db/cpa_order_status.csv"
            },
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
     * Обработку получения инфомрации о предоплатном платеже с {@link PaymentMethod#GOOGLE_PAY}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже с GOOGLE_PAY")
    @Test
    @DbUnitDataSet(
            before = {
                    "db/currency_rate.csv",
                    "db/deliveryTypes.csv",
                    "db/supplier.csv",
                    "db/cpa_order_status.csv"
            },
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
     * Обработку получения инфомрации о предоплатном платеже с {@link PaymentMethod#TINKOFF_CREDIT}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже с TINKOFF_CREDIT")
    @Test
    @DbUnitDataSet(
            before = {
                    "db/currency_rate.csv",
                    "db/deliveryTypes.csv",
                    "db/supplier.csv",
                    "db/cpa_order_status.csv"
            },
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
     * Обработку получения инфомрации о предоплатном платеже с {@link PaymentMethod#SBP}.
     * Для простоты заказ переходит из {@link OrderStatus#UNPAID} в {@link OrderStatus#PROCESSING}.
     */
    @DisplayName("UNPAID -> PROCESSING. Сохраняем информацию о платеже по СБП")
    @Test
    @DbUnitDataSet(
            before = {
                    "db/currency_rate.csv",
                    "db/deliveryTypes.csv",
                    "db/supplier.csv",
                    "db/cpa_order_status.csv"
            },
            after = "db/StoreTransactions.unpaidToProcessing.Sbp.after.csv"
    )
    void test_SbpTransactionProcessing() throws IOException {
        runTest(
                "status_update/new_order/",
                "unpaidToProcessing-paymentSbp-deliveryTypeIsFake.json",
                DATE_2017_01_01
        );
    }

    @DisplayName("PROCESSING -> DELIVERY Для фулфилмент заказа - проверка сохранения информации о посылках. " +
            "Информации по посылкам в базе для данного заказа еще нет.")
    @Test
    @DbUnitDataSet(
            before = {"db/cpa_order_status.csv", "db/testNewFFParcelsCreation.before.csv"},
            after = "db/testNewFFParcelsCreation.after.csv"
    )
    void testFFParcelsCreationOnDelivery() throws IOException {
        runTest(
                "status_update/existing_order/",
                "createFFParcelsOnChangeStatusToDelivery.json",
                DATETIME_2017_01_01_01_02_03
        );
    }

    @DisplayName("PROCESSING -> DELIVERY Для фулфилмент заказа - проверка сохранения информации о посылках. " +
            "Информации по посылкам уже есть в базе.")
    @Test
    @DbUnitDataSet(
            before = {"db/cpa_order_status.csv", "db/testParcelsUpdate.before.csv"},
            after = "db/testParcelsUpdate.after.csv"
    )
    void testFFParcelsUpdateOnDelivery() throws IOException {
        runTest(
                "status_update/existing_order/",
                "updateFFParcelsOnChangeStatusToDelivery.json",
                DATETIME_2017_01_01_01_02_03
        );
    }

    @Test
    @DbUnitDataSet(
            before = {"db/cpa_order_status.csv", "db/testNotFFParcelsCreation.before.csv"},
            after = "db/testNotFFParcelsCreation.after.csv"
    )
    void testNotFFParcelsCreationOnDelivery() throws IOException {
        runTest(
                "status_update/existing_order/",
                "createNotFFParcelsOnChangeStatusToDelivery.json",
                DATETIME_2017_01_01_01_02_03
        );
    }

    @Test
    @DisplayName("RESERVED -> UNPAID -> PENDING -> PROCESSING")
    @DbUnitDataSet(
            before = {"db/cpa_order_status.csv", "db/testReservedToUnpaidToPendingToProcessing.before.csv"},
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
            before = {"db/cpa_order_status.csv", "db/testReservedUnpaidPendingToCancelled.before.csv"},
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
            before = {"db/cpa_order_status.csv", "db/testDeliveryToCustomerReturn.before.csv"},
            after = "db/testDeliveryToCustomerReturn.after.csv"
    )
    void testDeliveryToCustomerReturn() throws IOException {
        runTest(
                "status_update/existing_order/",
                "delivery_to_customer_return.json",
                DATETIME_2017_02_26_19_22_37
        );
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
