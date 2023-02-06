package ru.yandex.market.global.checkout.order;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.event.EventRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.queue.task.DeliveryCourierSearchProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentAuthorizeProducer;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentClearProducer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentCancellationProducer;
import ru.yandex.market.global.checkout.domain.queue.task.receipt.PaymentConfirmationProducer;
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

import static ru.yandex.market.global.checkout.order.rules.OrderRulesTestBuilder.TestEvent.te;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PICKUP;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PLACE_ORDER_FAIL;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_RETURN_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_RETURN_START;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_SEARCH_COURIER_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.ORDER_NEW;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_AUTHORIZE_FAIL;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_AUTHORIZE_START;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_CLEAR_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.PAYMENT_CLEAR_START;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.SHOP_READY;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PaymentInvoiceRulesCoverageTest extends BaseFunctionalTest {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final ConfigurationService configurationService;
    private final TestOrderFactory testOrderFactory;

    private OrderRulesTestBuilder get1StageBuilder(OrderRepository orderRepository,
                                                   EventRepository eventRepository,
                                                   ConfigurationService configurationService,
                                                   TestOrderFactory testOrderFactory) {
        return new OrderRulesTestBuilder(
                orderRepository, eventRepository, configurationService, testOrderFactory
        )
                .setOrderIdProducer(PaymentAuthorizeProducer.class, List.of(
                        te(PAYMENT_CLEAR_OK)
                ));
    }

    @Test
    public void test2StageClear() {
        OrderRulesTest test = new OrderRulesTestBuilder(orderRepository, eventRepository, configurationService,
                testOrderFactory)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(ORDER_NEW, 0),
                        te(SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }

    @Test
    public void test2StageAuthorize() {
        OrderRulesTest test = new OrderRulesTestBuilder(orderRepository, eventRepository, configurationService,
                testOrderFactory)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(ORDER_NEW, 0),
                        te(SHOP_READY, 2)
                ))
                .setOrderIdProducer(PaymentClearProducer.class)
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }

    @Test
    public void test1StageClear() {
        OrderRulesTest test = get1StageBuilder(orderRepository, eventRepository, configurationService,
                testOrderFactory)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(ORDER_NEW, 0),
                        te(SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }


    @Test
    public void test1StageCancel() {
        OrderRulesTest test = get1StageBuilder(orderRepository, eventRepository, configurationService, testOrderFactory)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_OK, 5),
                        te(DELIVERY_PICKUP, 5),
                        te(DELIVERY_PLACE_ORDER_FAIL, 10),
                        te(DELIVERY_RETURN_START, 12),
                        te(DELIVERY_RETURN_OK, 15)
                ))
                .setExternalEvents(List.of(
                        te(ORDER_NEW, 0),
                        te(SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 1);
    }

    @Test
    public void test2StageCancel() {
        OrderRulesTest test = new OrderRulesTestBuilder(orderRepository, eventRepository, configurationService,
                testOrderFactory)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setOrderIdProducer(DeliveryCourierSearchProducer.class, List.of(
                        te(DELIVERY_SEARCH_COURIER_OK, 5),
                        te(DELIVERY_PICKUP, 5),
                        te(DELIVERY_PLACE_ORDER_FAIL, 10),
                        te(DELIVERY_RETURN_START, 12),
                        te(DELIVERY_RETURN_OK, 15)
                ))
                .setExternalEvents(List.of(
                        te(ORDER_NEW, 0),
                        te(SHOP_READY, 2)
                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 1);
    }

    @Test
    public void testPaymentAuthFail() {
        OrderRulesTest test = new OrderRulesTestBuilder(orderRepository, eventRepository, configurationService,
                testOrderFactory)
                .setInitialOrderState(
                        EProcessingMode.AUTO,
                        EOrderState.NEW,
                        EDeliveryOrderState.NEW,
                        EPaymentOrderState.NEW,
                        EShopOrderState.NEW
                )
                .setExternalEvents(List.of(
                        te(ORDER_NEW, 0),
                        te(SHOP_READY, 2)
                ))
                .setOrderIdProducer(PaymentAuthorizeProducer.class, List.of(
                        te(PAYMENT_AUTHORIZE_FAIL)
                ))
                .setOrderIdProducer(PaymentClearProducer.class)
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 0);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }

    @Test
    public void testManualCancelled() {
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
                        te(PAYMENT_AUTHORIZE_START, 3),
                        te(EOrderEvent.PAYMENT_CANCEL_START, 10),
                        te(EOrderEvent.MANUAL_DELIVERY_CANCELED, 12),
                        te(EOrderEvent.MANUAL_ORDER_CANCELED, 14)
                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 1);
    }

    @Test
    public void testManualAuth() {
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
                        te(EOrderEvent.MANUAL_SHOP_NEW, 1),
                        te(EOrderEvent.SHOP_READY, 2),
                        te(PAYMENT_AUTHORIZE_START, 3),
                        te(EOrderEvent.MANUAL_DELIVERY_DELIVERED, 14)
                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }

    @Test
    public void testManualAuthClear() {
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
                        te(PAYMENT_AUTHORIZE_START, 3),
                        te(PAYMENT_CLEAR_START, 10),
                        te(EOrderEvent.MANUAL_DELIVERY_DELIVERED, 12)

                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 1);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }

    @Test
    public void testManualAuthFail() {
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
                .setOrderIdProducer(PaymentAuthorizeProducer.class, List.of(
                        te(PAYMENT_AUTHORIZE_FAIL)
                ))
                .setExternalEvents(List.of(
                        te(EOrderEvent.MANUAL_SHOP_NEW, 1),
                        te(EOrderEvent.SHOP_READY, 2),
                        te(PAYMENT_AUTHORIZE_START, 3),
                        te(EOrderEvent.MANUAL_ORDER_CANCELED, 12)

                ))
                .build();

        test.run();

        test.assertThatProducerInvokedNTimes(PaymentConfirmationProducer.class, 0);
        test.assertThatProducerInvokedNTimes(PaymentCancellationProducer.class, 0);
    }


}
