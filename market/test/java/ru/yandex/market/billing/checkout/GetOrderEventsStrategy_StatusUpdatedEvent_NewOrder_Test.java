package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexerclient.HttpDeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.ShopDeliveryCostResponse;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATETIME_2017_01_01_01_02_03;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.buildPartiallyMockedStrategy;

/**
 * Обработка события {@link HistoryEventType#ORDER_STATUS_UPDATED} для заказа отсутствующего в базе.
 */
@DbUnitDataSet(before = {"db/datasource.csv", "db/supplier.csv", "db/deliveryTypes.csv", "db/cpa_order_status.csv",
        "db/GetOrderEventsStrategy_StatusUpdatedEvent_NewOrder_Test.csv", "db/environmentZeroDelay.csv"})
public class GetOrderEventsStrategy_StatusUpdatedEvent_NewOrder_Test extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final BigDecimal DELIVERY_COST_KOP = BigDecimal.valueOf(123400L);
    private static final BigDecimal INSURANCE_COST_KOP = BigDecimal.valueOf(32100L);
    private static final double CHARGEABLE_WEIGHT_KG = 1.0;
    private static final String RESOURCE_PREFIX = "resources/checkouter_response/events/status_update/new_order/";

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private HttpDeliveryCalculatorIndexerClient deliveryCalculatorIndexerClient;
    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;

    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void before() {
        strategy = buildPartiallyMockedStrategy(eventProcessorSupportFactory.createSupport());
    }

    /**
     * Создание заказа с сохранением в базе в статусе {@link OrderStatus#PROCESSING}.
     * Тип оплаты {@link PaymentMethod#CASH_ON_DELIVERY} - скидка не применяется.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только общий пайплайн.
     * Детально следует покрыть дополнительно.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation.after.csv"
    )
    void test_process_when_newOrder_should_buildAndStoreCorrectly() throws IOException {
        List<OrderHistoryEvent> events = events("nullToProcessing-with-itemsAndDelivery-paymentCashOnDelivery.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Создание заказа с сохранением в базе поля fulfillmentWarehouseId при создании заказа и сохранении orderItems.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только сохранение одного поля.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation.after.csv"
    )
    void test_ff_warehouse_id_when_newOrder_should_buildAndStoreCorrectly() throws IOException {
        List<OrderHistoryEvent> events = events(
                "nullToProcessing-with-itemsAndDelivery-" +
                        "paymentCashOnDelivery-ffWarehouseId.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Создание заказа нового отмененного заказа.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCancelled.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCancelled.after.csv"
    )
    void test_ff_warehouse_id_when_newOrderCancelled_should_buildAndStoreCorrectly() throws IOException {
        List<OrderHistoryEvent> events = events("nullToCancelled-with-itemsAndDelivery" +
                "-paymentCashOnDelivery-ffWarehouseId.json");
        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * На примере события создания заказа с сохранением в базе в статусе {@link OrderStatus#PROCESSING}
     * проверяем, что для заказов отмеченных в env как ignored, не происходит обработка события.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только общий пайплайн.
     */
    @DisplayName("Заказ отмеченный как ignored в env, не обрабатывается")
    @Test
    @DbUnitDataSet(
            before = {
                    "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation.before.csv",
                    "db/ignored_orders.before.csv"
            },
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation.ignored.after.csv"
    )
    void test_process_when_newOrderMarkedAsIgnored_shouldNot_beProcess() throws IOException {
        List<OrderHistoryEvent> events = events("nullToProcessing-with-itemsAndDelivery-paymentCashOnDelivery.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Создание заказа с сохранением в базе в статусе {@link OrderStatus#PROCESSING}.
     * Для заказа с методом оплаты {@link PaymentMethod#YANDEX} - это влияет на применение скидки.
     * Данный заказ с промо купоном.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationPaidWithYandex.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationPaidWithYandex.after.csv"
    )
    void test_process_when_newOrderPaidWithCard_should_buildAndStoreCorrectly() throws IOException {
        List<OrderHistoryEvent> events = events("nullToProcessing-with-itemsAndDelivery-paymentYandex.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Создание заказа с сохранением в базе в статусе {@link OrderStatus#PROCESSING}.
     * Проверяем сохранение данных дистрибуции - clid, vid, distr_type
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationDistributionData.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationDistributionData.after.csv"
    )
    void test_process_when_newOrderPaid_storeDistributionData() throws IOException {
        List<OrderHistoryEvent> events = events("nullToProcessing-with-distribution-data.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Создание заказа с сохранением в базе в статусе {@link OrderStatus#PROCESSING}.
     * Проверяем сохранение данных дистрибуции - clid, vid, distr_type
     *
     * Тест такой же как и выше, но к виду добавлено неправильное окончание, которое должно вырезаться
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationDistributionData.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationDistributionData.after.csv"
    )
    void test_process_when_newOrderPaid_storeDistributionData_fixVid() throws IOException {
        List<OrderHistoryEvent> events = events("nullToProcessing-with-distribution-data-fix-vid.json");
        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Создание заказа с сохранением в базе в статусе {@link OrderStatus#PROCESSING}.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только общий пайплайн.
     * Детально следует покрыть дополнительно.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation_MarketDelivery.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreation_MarketDelivery.after.csv"
    )
    void test_process_when_newOrder_MarketDelivery_should_buildAndStoreCorrectly() throws IOException {
        ShopDeliveryCostResponse shopDeliveryCosts = new ShopDeliveryCostResponse();
        shopDeliveryCosts.setDeliveryCost(DELIVERY_COST_KOP);
        shopDeliveryCosts.setInsuranceCost(INSURANCE_COST_KOP);
        shopDeliveryCosts.setCalculatedWeight(CHARGEABLE_WEIGHT_KG);
        Mockito.doReturn(shopDeliveryCosts).when(deliveryCalculatorIndexerClient).getShopDeliveryCost(any());

        List<OrderHistoryEvent> events = events("nullToProcessing-with-itemsAndDelivery-paymentYandex_MarketDelivery.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationPaidWithYandex.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderCreationPaidWithYandex_partnerMarkups.after.csv"
    )
    void test_process_when_newOrder_PartnerMarkups_should_buildAndStoreCorrectly() throws IOException {
        List<OrderHistoryEvent> events = events("nullToProcessing-with-partnerMarkups.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderPlacingToReserved.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_NewOrderPlacingToReserved.after.csv"
    )
    void test_process_when_newOrder_placingToReserved() throws IOException {
        List<OrderHistoryEvent> events = events("placingToReserved.json");
        strategy.process(events, DATE_2017_01_01);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_eventWithPromo_clid.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_eventWithPromo_clid.after.csv"
    )
    void test_process_eventWithPromo_clid() throws IOException {
        strategy.process(
                events("event_with_clientid_promo.json"),
                DATE_2017_01_01
        );
    }

    @Test
    @DbUnitDataSet(
            after = "db/GetOrderEventsStrategy_updateOrderStatus_loadTesting.after.csv"
    )
    void test_process_loadTesting() throws IOException {
        strategy.process(
                events("event_with_load_testing_order.json"),
                DATE_2017_01_01
        );
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
