package ru.yandex.market.checkout.checkouter.tasks.balance;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentOperations;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType;
import ru.yandex.market.checkout.checkouter.pay.cashier.CreatePaymentContext;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class CancelForgottenPaymentsTaskTest extends AbstractServicesTestBase {

    private static final String BASKET_ID = "58f4abba795be27b78188f1e";
    private static final String PURCHASE_TOKEN = "fbcdba795be27b7817def7653";
    @Autowired
    private ZooTask cancelForgottenPaymentsZooTask;
    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    @Qualifier("routingPaymentOperations")
    private PaymentOperations paymentOperations;

    @BeforeEach
    public void init() {
        trustMockConfigurer.mockWholeTrust(new TrustBasketKey(BASKET_ID, PURCHASE_TOKEN));
        cancelForgottenPaymentsZooTask.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));
    }

    @Test
    public void shouldBeInTestEnvironment() {
        cancelForgottenPaymentsZooTask.setPermittedEnvironmentTypes(null);
        cancelForgottenPaymentsZooTask.init();
        assertThat(cancelForgottenPaymentsZooTask.getPermittedEnvironmentTypes(), hasSize(1));
        assertThat(cancelForgottenPaymentsZooTask.getPermittedEnvironmentTypes(), hasItem(EnvironmentType.TESTING));
        assertThat(cancelForgottenPaymentsZooTask.isValidEnvironment(), is(false));
    }

    @Test
    public void shouldCancelForgottenPayments() {
        Payment oldPayment = paymentService.findPayment(createOldPayment(), ClientInfo.SYSTEM);
        assertThat(oldPayment.getStatus(), not(PaymentStatus.CANCELLED));
        trustMockConfigurer.mockNotFoundStatusBasket();

        cancelForgottenPaymentsZooTask.runOnce();

        Payment afterTaskOldPayment = paymentService.findPayment(oldPayment.getId(), ClientInfo.SYSTEM);
        assertThat(afterTaskOldPayment.getStatus(), is(PaymentStatus.CANCELLED));
    }

    @Test
    public void shouldNotCancelNotOldPayments() {
        Payment oldPayment = paymentService.findPayment(createPayment(), ClientInfo.SYSTEM);
        assertThat(oldPayment.getStatus(), not(PaymentStatus.CANCELLED));
        trustMockConfigurer.mockNotFoundStatusBasket();


        cancelForgottenPaymentsZooTask.runOnce();

        Payment afterTaskOldPayment = paymentService.findPayment(oldPayment.getId(), ClientInfo.SYSTEM);
        assertThat(afterTaskOldPayment.getStatus(), not(PaymentStatus.CANCELLED));
    }


    @Test
    public void shouldNotCancelPaymentsWithAnswer() {
        Payment oldPayment = paymentService.findPayment(createOldPayment(), ClientInfo.SYSTEM);
        assertThat(oldPayment.getStatus(), not(PaymentStatus.CANCELLED));

        cancelForgottenPaymentsZooTask.runOnce();

        Payment afterTaskOldPayment = paymentService.findPayment(oldPayment.getId(), ClientInfo.SYSTEM);
        assertThat(afterTaskOldPayment.getStatus(), not(PaymentStatus.CANCELLED));
    }

    private Long createOldPayment() {
        setFixedTime(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC));
        Long paymentId = createPayment();
        clearFixed();
        return paymentId;
    }

    private Long createPayment() {
        ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID);
        long orderId = OrderServiceHelper.createPrepaidOrder(orderCreateService, orderUpdateService,
                orderCompletionService);

        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        Payment payment = paymentOperations.startPrepayPayment(
                orderId,
                clientInfo,
                "RETURN_PATH",
                null,
                PaymentFormType.DESKTOP,
                false,
                CreatePaymentContext.builder().build()
        );

        return payment.getId();
    }
}
