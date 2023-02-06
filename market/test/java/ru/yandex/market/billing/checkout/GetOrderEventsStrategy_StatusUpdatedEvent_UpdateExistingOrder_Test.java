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
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexerclient.HttpDeliveryCalculatorIndexerClient;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.ShopDeliveryCostResponse;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATETIME_2017_01_01_01_02_03;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.buildPartiallyMockedStrategy;

/**
 * Обработка события {@link HistoryEventType#ORDER_STATUS_UPDATED} для заказа существующего в базе.
 * Новые тесты добавлять в {@link DbGetOrdersExecutor_OrderStatusUpdated_Test} и эти хорошо бы перенести туда же.
 */
@DbUnitDataSet(before = {"db/cpa_order_status.csv", "db/datasource.csv",
        "db/GetOrderEventsStrategy_StatusUpdatedEvent_UpdateExistingOrder_Test.csv", "db/environmentZeroDelay.csv"})
public class GetOrderEventsStrategy_StatusUpdatedEvent_UpdateExistingOrder_Test extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final BigDecimal DELIVERY_COST_KOP = BigDecimal.valueOf(123400L);
    private static final BigDecimal INSURANCE_COST_KOP = BigDecimal.valueOf(32100L);
    private static final double CHARGEABLE_WEIGHT_KG = 1.0;
    private static final String RESOURCE_PREFIX = "resources/checkouter_response/events/status_update/existing_order/";
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
     * Переход из {@link OrderStatus#DELIVERY} в {@link OrderStatus#CANCELLED}.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только общий пайплайн.
     * Детально следует покрыть дополнительно.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery.after.csv"
    )
    void test_process_when_orderExistingInProcessing_should_updateToCancelled_for_market_delivery() throws IOException {
        ShopDeliveryCostResponse shopDeliveryCosts = new ShopDeliveryCostResponse();
        shopDeliveryCosts.setDeliveryCost(DELIVERY_COST_KOP);
        shopDeliveryCosts.setInsuranceCost(INSURANCE_COST_KOP);
        shopDeliveryCosts.setCalculatedWeight(CHARGEABLE_WEIGHT_KG);
        Mockito.doReturn(shopDeliveryCosts).when(deliveryCalculatorIndexerClient).getShopDeliveryCost(any());

        List<OrderHistoryEvent> events = events("deliveryToCancelled-with-itemsAndDelivery-paymentYandex_MarketDelivery.json");

        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERY} в {@link OrderStatus#DELIVERED}.
     * Тест не покрывает всех моментов связанных с доставкой и скидками, только общий пайплайн.
     * Детально следует покрыть дополнительно.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue.after.csv"
    )
    void test_process_when_orderExistingInProcessing_should_updateToDelivered_for_market_delivery_blue() throws IOException {
        ShopDeliveryCostResponse shopDeliveryCosts = new ShopDeliveryCostResponse();
        shopDeliveryCosts.setDeliveryCost(DELIVERY_COST_KOP);
        shopDeliveryCosts.setInsuranceCost(INSURANCE_COST_KOP);
        shopDeliveryCosts.setCalculatedWeight(CHARGEABLE_WEIGHT_KG);
        Mockito.doReturn(shopDeliveryCosts).when(deliveryCalculatorIndexerClient).getShopDeliveryCost(any());

        List<OrderHistoryEvent> events = events("deliveryToDelivered-with-itemsAndDelivery-paymentYandex_MarketDelivery_Blue.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERED} в {@link OrderStatus#CANCELLED}.
     * Тест проверяет, что сохраняется trantime для fee_cancellation.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_FulfillmentBlue.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_FulfillmentBlue.after.csv"
    )
    void test_process_when_FulfillmentBlueOrderExistingInDelivered_should_updateToCancelled() throws IOException {
        List<OrderHistoryEvent> events = events("deliveredToCancelled-with-itemsAndDelivery-paymentYandex_MarketDelivery_FulfillmentBlue.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#PENDING} в {@link OrderStatus#CANCELLED}.
     * Ожидается, что для синего нефулфиллмент заказа, у которого isMarketDelivery = true и подстатусы SHOP_FAILED,
     * PENDING_EXPIRED, WAREHOUSE_FAILED_TO_SHIP или MISSING_ITEM, будет сохранен CANCELLED_ORDER_FEE
     * в market_billing.order_trantimes.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Dropship.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Dropship.after.csv"
    )
    void test_process_when_orderExistingInPending_should_updateToCancelled_for_market_delivery_dropship() throws IOException {
        List<OrderHistoryEvent> events = events("pendingToCancelled-with-itemsAndDelivery-paymentYandex_MarketDelivery_Dropship.json");

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
        List<OrderHistoryEvent> events = events("processingToDelivery-with-itemsAndDelivery-paymentYandex.json");

        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERY} в {@link OrderStatus#DELIVERED}.
     * Тест проверяет, что сохраняется trantime для loyalty_participation_fee и ff_processing.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue_loyalty_participation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue_loyalty_participation.after.csv"
    )
    void test_process_when_orderExistingInDelivery_should_updateToDelivered_for_market_delivery_blue_loyalty_participation() throws IOException {
        List<OrderHistoryEvent> events = events("deliveryToDelivered-with-itemsAndDelivery-paymentYandex_MarketDelivery_Blue_loyalty_participation.json");

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
        List<OrderHistoryEvent> events = events("processingToDelivery-with-itemsAndDelivery-paymentYandex_MarketDelivery_Blue_Fulfillment_loyalty_participation.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    /**
     * Переход из {@link OrderStatus#DELIVERED} в {@link OrderStatus#CANCELLED}.
     * Тест проверяет, что сохраняется trantime для loyalty_participation_fee_cancellation.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_FulfillmentBlue_loyalty_participation.before.csv",
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_FulfillmentBlue_loyalty_participation.after.csv"
    )
    void test_process_when_FulfillmentBlueOrderExistingInDelivered_should_updateToCancelled_loyalty_participation() throws IOException {
        List<OrderHistoryEvent> events = events("deliveredToCancelled-with-" +
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
                    "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue.before.csv",
                    "db/ignored_orders.before.csv"
            },
            after = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToDelivered_MarketDelivery_Blue.ignored.after.csv"
    )
    void test_process_when_orderExistingInProcessingAndIgnored_shouldNot_beProcessed() throws IOException {
        List<OrderHistoryEvent> events = events(
                "deliveryToDelivered-with-itemsAndDelivery-paymentYandex_MarketDelivery_Blue.json");

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Blue.before.csv",
            after  = "db/GetOrderEventsStrategy_updateOrderStatus_UpdateExistingOrder_ToCancelled_MarketDelivery_Blue.after.csv"
    )
    @DisplayName("Billing status не меняется, когда заказ из DELIVERY переходит в CANCELLED.")
    void test_process_when_orderFromDeliveredToCancelled_shouldNot_changeBilledStatus() throws IOException {
        List<OrderHistoryEvent> events = events(
                "deliveredToCancelled-with-itemsAndDelivery-paymentYandex_MarketDelivery_Blue.json"
        );

        strategy.process(events, DATETIME_2017_01_01_01_02_03);
    }

    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_processingToCancelled_without_substatus.before.csv",
            after = "db/GetOrderEventsStrategy_processingToCancelled_without_substatus.after.csv"
    )
    void test_processingToCancelled_without_substatus() throws IOException {
        OrderHistoryEvent events = events("processingToCancelled-without-substatus.json").get(0);
        strategy.processIfNecessary(events, DATETIME_2017_01_01_01_02_03);
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
