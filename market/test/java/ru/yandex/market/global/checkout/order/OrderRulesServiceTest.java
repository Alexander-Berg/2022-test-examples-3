package ru.yandex.market.global.checkout.order;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.configuration.ConfigurationProperties;
import ru.yandex.market.global.checkout.domain.event.EventRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRulesService;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCourierSearchProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.OrderPlusRewardTopUpProducer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.order.rules.OrderRulesTest;
import ru.yandex.market.global.checkout.order.rules.OrderRulesTestBuilder;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderEvent;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

import static ru.yandex.market.global.checkout.order.rules.OrderRulesTestBuilder.TestEvent.te;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_CANCEL_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_DELIVERED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PICKUP;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_RETURN_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_RETURN_REPORTED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_SEARCH_COURIER_FAIL;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_SEARCH_COURIER_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.MANUAL_DELIVERY_CANCELED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.MANUAL_DELIVERY_DELIVERED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.MANUAL_SHOP_NEW;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.MODE_SWITCH_TO_MANUAL;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_AUTHORIZE_START;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_CLEAR_START;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.SHOP_CANCEL;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.SHOP_READY;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderRulesServiceTest extends BaseFunctionalTest {
    private static final long TEST_SHOP_ID = 2L;
    private static final long COMMON_SHOP_ID = 3L;

    private final OrderRepository orderRepository;
    private final OrderRulesService orderRulesService;
    private final EventRepository eventRepository;
    private final ConfigurationService configurationService;
    private final TestOrderFactory testOrderFactory;

    @Test
    public void testNormalProcessing() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0),
                        te(EOrderEvent.SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.FINISHED,
                EDeliveryOrderState.ORDER_DELIVERED,
                EPaymentOrderState.CLEARED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
        test.assertThatProducerInvokedNTimes(OrderPlusRewardTopUpProducer.class, 1);
    }

    @Test
    public void testNormalProcessingWithShopReadyAfterDelivered() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0),
                        te(EOrderEvent.SHOP_READY, 60)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.FINISHED,
                EDeliveryOrderState.ORDER_DELIVERED,
                EPaymentOrderState.CLEARED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testShopCancelProcessing() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0),
                        te(EOrderEvent.SHOP_CANCEL, 2)
                ))
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_OK, 5)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.CANCELED,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.CANCELED
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testShopCancelSwitchToManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0),
                        te(EOrderEvent.SHOP_CANCEL, 2)
                ))
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_OK, 5)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.ORDER_PLACED,
                EPaymentOrderState.NEW,
                EShopOrderState.CANCELED
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testScheduledProcessing() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.SCHEDULED,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 60),
                        te(EOrderEvent.SHOP_READY, 62)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.FINISHED,
                EDeliveryOrderState.ORDER_DELIVERED,
                EPaymentOrderState.CLEARED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testScheduledCancelProcessing() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.SCHEDULED,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_CANCEL_START, 30)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.CANCELED,
                EDeliveryOrderState.NEW,
                EPaymentOrderState.NEW,
                EShopOrderState.NEW
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testSearchCourierCancel() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_FAIL, 2)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.CANCELED,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.NEW
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testSearchCourierCancelSwitchToManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_FAIL, 2)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.NEW
        );

        test.assertAllProducersInvokedOnlyOnce();
    }


    @Test
    public void testSearchCourierCancelSwitchToManualForTestShopOnly() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "FOR_TEST_SHOP");
        configurationService.mergeValue(ConfigurationProperties.TEST_SHOP_ID, TEST_SHOP_ID);

        OrderRulesTestBuilder testBuilder = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setShopId(TEST_SHOP_ID)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_FAIL, 2)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0)
                ));

        OrderRulesTest switchToManualTest = testBuilder.build();

        switchToManualTest.run();

        switchToManualTest.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.NEW
        );

        switchToManualTest.assertAllProducersInvokedOnlyOnce();

        OrderRulesTest canceledTest = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setShopId(COMMON_SHOP_ID)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_FAIL, 2)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.ORDER_NEW, 0)
                ))
                .build();

        canceledTest.run();

        canceledTest.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.CANCELED,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.NEW
        );

        canceledTest.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testManualFinish() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.CANCELED
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.MANUAL_SHOP_NEW, 1),
                        te(EOrderEvent.SHOP_READY, 2),
                        te(EOrderEvent.PAYMENT_AUTHORIZE_START, 3),
                        te(PAYMENT_CLEAR_START, 10),
                        te(EOrderEvent.MANUAL_DELIVERY_DELIVERED, 12)

                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.FINISHED,
                EDeliveryOrderState.ORDER_DELIVERED,
                EPaymentOrderState.CLEARED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testManualCancel() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.CANCELED
                )
                .setExternalEvents(List.of(
                        te(EOrderEvent.MANUAL_SHOP_NEW, 1),
                        te(EOrderEvent.SHOP_READY, 2),
                        te(EOrderEvent.PAYMENT_AUTHORIZE_START, 3),
                        te(EOrderEvent.PAYMENT_CANCEL_START, 10),
                        te(EOrderEvent.MANUAL_DELIVERY_CANCELED, 12),
                        te(EOrderEvent.MANUAL_ORDER_CANCELED, 14)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.CANCELED,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.CANCELED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testSwitchToManualOk() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.SEARCHING_COURIER,
                        EPaymentOrderState.AUTHORIZED,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(MODE_SWITCH_TO_MANUAL, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.SEARCHING_COURIER,
                EPaymentOrderState.AUTHORIZED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testSwitchToManualIgnoredForCanceledOrder() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.CANCELED,
                        EDeliveryOrderState.ORDER_CANCELED,
                        EPaymentOrderState.NOT_AUTHORIZED,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(MODE_SWITCH_TO_MANUAL, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.CANCELED,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NOT_AUTHORIZED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testScheduledIgnoreAllEventsExceptNewCancelAndSwitchToManual() {
        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.SCHEDULED,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(Arrays.stream(EOrderEvent.values())
                        .filter(e -> e != EOrderEvent.ORDER_NEW)
                        .filter(e -> e != EOrderEvent.ORDER_CANCEL_START)
                        .filter(e -> e != EOrderEvent.MODE_SWITCH_TO_MANUAL)
                        .map(OrderRulesTestBuilder.TestEvent::te)
                        .collect(Collectors.toList())
                )
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.AUTO,
                EOrderState.SCHEDULED,
                EDeliveryOrderState.NEW,
                EPaymentOrderState.NEW,
                EShopOrderState.NEW
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testAvailableEventsInManualMode() {
        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                        .setupOrder(o -> o
                                .setProcessingMode(EProcessingMode.MANUAL)
                                .setShopState(EShopOrderState.CANCELED)
                                .setPaymentState(EPaymentOrderState.NEW)
                                .setDeliveryState(EDeliveryOrderState.DELIVERING_ORDER)
                                .setOrderState(EOrderState.PROCESSING)
                        )
                        .build()
                ).getOrder();

        List<EOrderEvent> events = orderRulesService.getAvailableManualOrderEvents(order.getId());
        Assertions.assertThat(events).containsExactlyInAnyOrder(
                MANUAL_DELIVERY_DELIVERED,
                MANUAL_DELIVERY_CANCELED,
                PAYMENT_AUTHORIZE_START,
                SHOP_READY,
                SHOP_CANCEL,
                MANUAL_SHOP_NEW
        );
    }

    @Test
    public void testAvailableEventsInAutoMode() {
        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(EProcessingMode.AUTO)
                        .setOrderState(EOrderState.PROCESSING)
                        .setShopState(EShopOrderState.READY)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.SEARCHING_COURIER)
                )
                .build()
        ).getOrder();

        List<EOrderEvent> events = orderRulesService.getAvailableManualOrderEvents(order.getId());
        Assertions.assertThat(events).containsExactlyInAnyOrder(
                MODE_SWITCH_TO_MANUAL, SHOP_CANCEL
        );
    }

    @Test
    public void testSwitchToManualWhenReturning() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.DELIVERING_ORDER,
                        EPaymentOrderState.AUTHORIZED,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_RETURN_REPORTED, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.RETURNING_ORDER,
                EPaymentOrderState.AUTHORIZED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testReturningInManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.DELIVERING_ORDER,
                        EPaymentOrderState.AUTHORIZED,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_RETURN_REPORTED, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.RETURNING_ORDER,
                EPaymentOrderState.AUTHORIZED,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testCourierFoundInManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_SEARCH_COURIER_OK, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.COURIER_FOUND,
                EPaymentOrderState.NEW,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testDeliveringFoundInManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_PICKUP, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.DELIVERING_ORDER,
                EPaymentOrderState.NEW,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testDeliveredFoundInManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_DELIVERED, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.ORDER_DELIVERED,
                EPaymentOrderState.NEW,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testReturnedFoundInManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_RETURN_OK, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

    @Test
    public void testCancelledFoundInManual() {
        configurationService.mergeValue(ConfigurationProperties.SWITCH_TO_MANUAL_MODE, "ALWAYS");

        OrderRulesTest test = new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setInitialOrderState(
                        EProcessingMode.MANUAL,
                        EOrderState.PROCESSING,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.READY
                )
                .setExternalEvents(List.of(
                        te(DELIVERY_CANCEL_OK, 10)
                ))
                .build();

        test.run();

        test.assertFinalOrderStateIs(
                EProcessingMode.MANUAL,
                EOrderState.PROCESSING,
                EDeliveryOrderState.ORDER_CANCELED,
                EPaymentOrderState.NEW,
                EShopOrderState.READY
        );

        test.assertAllProducersInvokedOnlyOnce();
    }

}
