package ru.yandex.market.checkout.checkouter.b2b;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReasonWithDetails;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.loyalty.model.PromocodeDiscountEntry;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class B2bOrderPayTest extends AbstractWebTestBase {

    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderWritingDao orderWritingDao;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    void init() {
        b2bCustomersMockConfigurer.mockGeneratePaymentInvoice();
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        b2bCustomersMockConfigurer.mockIsClientCanOrder(BuyerProvider.UID,
                B2bCustomersTestProvider.BUSINESS_BALANCE_ID, true);
    }

    @AfterEach
    void resetMocks() {
        b2bCustomersMockConfigurer.resetAll();
    }

    @Test
    void restrictPersonPay() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        Order b2bOrder = orderCreateHelper.createOrder(parameters);
        assertEquals(OrderStatus.UNPAID, b2bOrder.getStatus());

        assertThrows(AssertionError.class, () -> orderPayHelper.payForPersonOrder(b2bOrder, false));
    }

    @Test
    void allowBusinessPay() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        Order b2bOrder = orderCreateHelper.createOrder(parameters);
        assertEquals(OrderStatus.UNPAID, b2bOrder.getStatus());
        orderStatusHelper.proceedOrderToStatus(b2bOrder, OrderStatus.UNPAID);

        orderPayHelper.payForBusinessOrder(b2bOrder);

        assertEquals(OrderStatus.PENDING, orderService.getOrder(b2bOrder.getId()).getStatus());
    }

    @Test
    void createsSubsidyPaymentAfterOrderReachesDeliveryStage() {
        // Given
        // включить создание счета для оплаты заказа
        checkouterFeatureWriter.writeValue(ComplexFeatureType.ACCOUNT_PAYMENT_TOGGLE,
                AccountPaymentFeatureToggle.LOGGING);
        // включить создание счета для компенсации субсидии (скидки по промокоду)
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_B2B_SUBSIDY_PAYMENTS, true);

        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        // мок траста для генерации счета по субсидиям
        trustMockConfigurer.mockWholeTrust();

        // добавление промокода на скидку к заказу
        OrderItem orderItem = parameters.getItems().iterator().next();
        BigDecimal promoCodeDiscount = BigDecimal.valueOf(17L);
        parameters.getLoyaltyParameters()
                .expectPromocode(PromocodeDiscountEntry.promocode("PROMO-CODE", "PROMO-KEY")
                        .discount(Map.of(orderItem.getOfferItemKey(), promoCodeDiscount)));

        // движение заказа по этапам до этапа, где работа QueuedCall сгенерирует платеж по субсидиям
        Order b2bOrder = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(b2bOrder, OrderStatus.UNPAID);
        client.payments().generatePaymentInvoice(b2bOrder.getId());  // сделать счет на оплату
        orderPayHelper.payForBusinessOrder(b2bOrder);  // сказать чекаутеру, что счет был оплачен
        changePendingStatusToNotSendRequestsToReport(b2bOrder);
        orderStatusHelper.proceedOrderToStatus(b2bOrder, OrderStatus.PROCESSING);
        orderStatusHelper.proceedOrderToStatus(b2bOrder, OrderStatus.DELIVERY);

        // When
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        // Then
        List<Payment> subsidies = paymentService.getPayments(
                b2bOrder.getId(),
                ClientInfo.SYSTEM,
                PaymentGoal.SUBSIDY);

        Assertions.assertEquals(1, subsidies.size());
        Payment subsidy = subsidies.get(0);
        Assertions.assertEquals(
                promoCodeDiscount.doubleValue(),
                subsidy.getTotalAmount().doubleValue(),
                0.001);
    }

    /**
     * Подмена статуса, чтобы при переходе со статуса "ждем подтверждения от магазина" в "в обработке" не делать
     * поход в Репорт за обновленной доставкой.
     */
    private void changePendingStatusToNotSendRequestsToReport(Order b2bOrder) {
        Order currentState = orderService.getOrder(b2bOrder.getId());
        if (currentState.getStatus() != OrderStatus.PENDING
                && currentState.getSubstatus() != OrderSubstatus.AWAIT_CONFIRMATION) {
            throw new RuntimeException("Order must be waiting for shop confirmation");
        }
        currentState.setSubstatus(OrderSubstatus.WAITING_FOR_STOCKS);
        transactionTemplate.execute((status) -> {
            orderWritingDao.updateOrderStatus(
                    currentState,
                    ClientInfo.SYSTEM,
                    HistoryEventType.ORDER_STATUS_UPDATED,
                    HistoryEventReasonWithDetails.withNullableFields());
            return null;
        });
    }
}
