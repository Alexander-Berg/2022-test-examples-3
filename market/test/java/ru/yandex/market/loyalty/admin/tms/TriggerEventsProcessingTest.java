package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;
import ru.yandex.market.loyalty.core.dao.trigger.TriggerEventDao;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventProcessedResult;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.market.loyalty.admin.utils.MultiStageTestUtils.buildOrder;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.courierDelivery;
import static ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils.withPrice;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;

public class TriggerEventsProcessingTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private MultiStageTestUtils multiStageTestUtils;
    @Autowired
    private TriggerEventDao triggerEventDao;

    @Test
    public void shouldProcessOrderPaidSeparately() {
        configurationService.set(ConfigurationService.MOVE_ORDER_PAID_PROCESSING_TO_ANOTHER_TMS, true);

        sendOrderEvents();
        List<TriggerEvent> all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE))
        ));
        triggerEventTmsProcessor.processOrderPaidTriggerEvents(Duration.ofSeconds(10));
        all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE))
        ));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ofSeconds(10));
        triggerEventTmsProcessor.processOrderTerminationTriggerEvents(Duration.ofSeconds(10));
        all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.NO_TRIGGERS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS))
        ));
    }

    @Test
    public void shouldProcessOrderTerminationSeparately() {
        configurationService.set(ConfigurationService.MOVE_ORDER_PAID_PROCESSING_TO_ANOTHER_TMS, true);

        sendOrderEvents();
        List<TriggerEvent> all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE))
        ));
        triggerEventTmsProcessor.processOrderPaidTriggerEvents(Duration.ofSeconds(10));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ofSeconds(10));
        all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.NO_TRIGGERS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE))
        ));
        triggerEventTmsProcessor.processOrderTerminationTriggerEvents(Duration.ofSeconds(10));
        all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.NO_TRIGGERS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS))
        ));
    }

    @Test
    public void shouldProcessOrderPaidWithOthers() {
        configurationService.set(ConfigurationService.MOVE_ORDER_PAID_PROCESSING_TO_ANOTHER_TMS, false);

        sendOrderEvents();
        List<TriggerEvent> all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE))
        ));
        triggerEventTmsProcessor.processOrderPaidTriggerEvents(Duration.ofSeconds(10));
        all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.IN_QUEUE))
        ));
        triggerEventTmsProcessor.processTriggerEvents(Duration.ofSeconds(10));
        all = triggerEventDao.getAll();
        assertThat(all, containsInAnyOrder(
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.NO_TRIGGERS)),
                hasProperty("processedResult", equalTo(TriggerEventProcessedResult.SUCCESS))
        ));
    }

    private void sendOrderEvents() {
        int orderId = new Random().nextInt(1000);
        MultiCartWithBundlesDiscountResponse discountResponse =
                multiStageTestUtils.spendRequest(DiscountRequestWithBundlesBuilder
                        .builder(orderRequestWithBundlesBuilder()
                                .withOrderId(Integer.toString(orderId))
                                .withOrderItem(
                                        warehouse(MARKET_WAREHOUSE_ID),
                                        itemKey(DEFAULT_ITEM_KEY),
                                        price(500),
                                        quantity(3)
                                )
                                .withPaymentType(PaymentType.BANK_CARD)
                                .withPaymentSystem(PaymentSystem.MASTERCARD)
                                .withDeliveries(courierDelivery(
                                        withPrice(BigDecimal.valueOf(350)),
                                        builder -> builder.setSelected(true)
                                ))
                                .build())
                        .build());
        processEvent(
                buildOrder(OrderStatus.PROCESSING, (long) orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        processEvent(
                buildOrder(OrderStatus.DELIVERED, (long) orderId, null,
                        DEFAULT_ITEM_KEY, BigDecimal.valueOf(500), BigDecimal.valueOf(3), 1).build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        clock.spendTime(20, ChronoUnit.SECONDS);
    }
}
