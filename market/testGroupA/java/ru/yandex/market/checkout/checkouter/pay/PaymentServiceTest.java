package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType;
import ru.yandex.market.checkout.checkouter.pay.cashier.CreatePaymentContext;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.balance.model.notifications.PaymentNotification.checkPaymentNotification;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

public class PaymentServiceTest extends AbstractServicesTestBase {

    private static final String BASKET_ID = "58f4abba795be27b78188f0e";
    private static final String PURCHASE_TOKEN = "fbcdba795be27b7817def7654";
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ShopService shopService;
    @Autowired
    private OrderCompletionService orderCompletionService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    @Qualifier("routingPaymentOperations")
    private PaymentOperations paymentOperations;

    @BeforeEach
    public void init() {
        trustMockConfigurer.mockWholeTrust(new TrustBasketKey(BASKET_ID, PURCHASE_TOKEN));
    }

    @Test
    public void testPayMarket() {
        checkouterProperties.setEnableServicesPrepay(true);
        shopService.updateMeta(OrderProvider.SHOP_ID, ShopSettingsHelper.getDefaultMeta());
        test(order -> {
            assertThat(order.getBalanceOrderId(), nullValue());

            // Проверить, что сохранили Трастовый order id для строки заказа.
            final Iterator<OrderItem> iterator = order.getItems().iterator();
            final OrderItem productItem = iterator.next();
            assertThat(productItem.getBalanceOrderId(), equalTo(order.getId() + "-item-" + order.getItems()
                    .iterator().next().getId()));

            final ItemService itemService = productItem.getServices().iterator().next();
            assertThat(itemService.getBalanceOrderId(), equalTo(order.getId() + "-itemService-"
                    + itemService.getId()));

            // Проверить, что сохранили Трастовый order id для доставки.
            final Delivery delivery = order.getDelivery();
            assertThat(delivery.getBalanceOrderId(), equalTo(order.getId() + "-delivery"));

            // Найти чеки.
            final List<Receipt> receipts = receiptService.findByOrder(order.getId());
            assertThat(receipts, notNullValue());
            assertThat(receipts, hasSize(1));

            // Проверить фильтрацию чеков по статусу
            assertThat(receiptService.findByOrder(order.getId(), ReceiptStatus.NEW), hasSize(1));
            assertThat(receiptService.findByOrder(order.getId(), ReceiptStatus.PRINTED), empty());

            // Проверить корректность заполнения чека.
            final Receipt receipt = receipts.get(0);
            assertThat(receipt.getType(), equalTo(ReceiptType.INCOME));
            assertThat(receipt.getStatus(), equalTo(ReceiptStatus.NEW));
            assertThat(receipt.getPaymentId(), equalTo(order.getPayment().getId()));
            assertThat(receipt.getRefundId(), nullValue());
            assertThat(receipt.getTrustPayload(), nullValue());

            // Проверить строки чека.
            final List<ReceiptItem> items = receipt.getItems();
            assertThat(items, notNullValue());
            assertThat(items, hasSize(3));

            // Проверить строку с товаром.
            final ReceiptItem productReceiptItem = items.stream()
                    .filter(ri -> "OfferName".equalsIgnoreCase(ri.getItemTitle()))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("ReceiptItem with title: OfferName was not found"));
            assertThat(productReceiptItem.getItemId(), equalTo(productItem.getId()));
            assertThat(productReceiptItem.getItemServiceId(), nullValue());
            assertThat(productReceiptItem.getDeliveryId(), nullValue());
            assertThat(productReceiptItem.getItemTitle(), equalTo(productItem.getOfferName()));
            assertThat(productReceiptItem.getCount(), equalTo(productItem.getCount()));
            assertThat(productReceiptItem.getPrice(), equalTo(productItem.getBuyerPrice()));
            assertThat(
                    productReceiptItem.getAmount(),
                    equalTo(productItem.getBuyerPrice().multiply(new BigDecimal(productItem.getCount())))
            );

            // Проверить строку с услугой
            final ReceiptItem serviceReceiptItem = items.stream()
                    .filter(ri -> "ItemServiceName".equalsIgnoreCase(ri.getItemTitle()))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("ReceiptItem with title: ItemServiceName was not " +
                            "found"));
            assertThat(serviceReceiptItem.getItemServiceId(), equalTo(itemService.getId()));
            assertThat(serviceReceiptItem.getItemId(), nullValue());
            assertThat(serviceReceiptItem.getDeliveryId(), nullValue());
            assertThat(serviceReceiptItem.getItemTitle(), equalTo(itemService.getTitle()));
            assertThat(serviceReceiptItem.getCount(), equalTo(itemService.getCount()));
            assertThat(serviceReceiptItem.getPrice(), equalTo(itemService.getPrice()));
            assertThat(
                    serviceReceiptItem.getAmount(),
                    equalTo(itemService.getPrice().multiply(new BigDecimal(itemService.getCount())))
            );

            // Проверить строку с доставкой.
            final ReceiptItem deliveryReceiptItem = items.stream()
                    .filter(ri -> "Доставка".equalsIgnoreCase(ri.getItemTitle()))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("ReceiptItem with title: OfferName was not found"));
            assertThat(deliveryReceiptItem.getItemId(), nullValue());
            assertThat(deliveryReceiptItem.getItemServiceId(), nullValue());
            assertThat(deliveryReceiptItem.getDeliveryId(), equalTo(order.getInternalDeliveryId()));
            assertThat(deliveryReceiptItem.getItemTitle(), equalTo("Доставка"));
            assertThat(deliveryReceiptItem.getCount(), equalTo(1));
            assertThat(deliveryReceiptItem.getPrice(), equalTo(delivery.getBuyerPrice()));
            assertThat(deliveryReceiptItem.getAmount(), equalTo(delivery.getBuyerPrice()));
            List<Long> orderIds = paymentService.loadPayments(List.of(order))
                    .stream()
                    .filter(p -> Set.of(PaymentStatus.INIT, PaymentStatus.HOLD, PaymentStatus.CLEARED)
                            .contains(p.getStatus()))
                    .filter(p -> PaymentGoal.ORDER_PREPAY.equals(p.getType()))
                    .map(Payment::getId)
                    .collect(Collectors.toList());
            assertThat("Should have some CLEARED payments", orderIds.size() > 0);
        });
    }

    @Test
    public void testNotifyFailReasonAndDescription() {
        ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID);
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildFailCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildFailCheckBasket(), null);

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
        Long paymentId = payment.getId();

        paymentService.notifyPayment(checkPaymentNotification(paymentId, false));
        Payment payment1 = paymentService.getPayment(paymentId, ClientInfo.SYSTEM);

        Assertions.assertEquals(TrustMockConfigurer.CHECK_BASKET_STATUS_CODE, payment1.getFailReason());
        Assertions.assertEquals(TrustMockConfigurer.CHECK_BASKET_STATUS_DESC, payment1.getFailDescription());
    }

    /**
     * Проверяет кейс с асинхронным выставлением трастом payment_ts (он же holdTimestamp).
     */
    @Test
    public void testNotifyHoldWithAsyncPaymentTimestamp() {
        ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID);

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildHoldWithoutTsCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildHoldWithoutTsCheckBasket(), null);

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
        Long paymentId = payment.getId();

        paymentService.notifyPayment(checkPaymentNotification(paymentId, false));

        Payment payment1 = paymentService.getPayment(paymentId, ClientInfo.SYSTEM);
        Assertions.assertEquals(payment.getStatus(), payment1.getStatus());

        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildHoldCheckBasket());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildHoldCheckBasket(), null);

        // pretend that I'm PaymentStatusInspector
        paymentService.notifyPayment(checkPaymentNotification(paymentId, false));

        // hold timestamp is present, should change payment's status to HOLD
        Payment payment2 = paymentService.getPayment(paymentId, ClientInfo.SYSTEM);
        Assertions.assertEquals(PaymentStatus.HOLD, payment2.getStatus());
    }

    /**
     * При проведении платежа заказ сохраняет свои посылки
     */
    @Test
    public void testSaveShipmentAndItemsOnPay() {
        shopService.updateMeta(OrderProvider.SHOP_ID, ShopSettingsHelper.getDefaultMeta());
        Order prepaidOrder = OrderProvider.getPrepaidOrder();
        Parcel parcel = new Parcel();
        ParcelItem parcelItem = new ParcelItem(prepaidOrder.getItems().stream().findFirst().get());
        parcel.addParcelItem(parcelItem);
        prepaidOrder.getDelivery().addParcel(parcel);
        long orderId = processOrder(prepaidOrder);

        processPayment(orderId);

        Order order = orderService.getOrder(orderId);

        Delivery delivery = order.getDelivery();
        assertThat(delivery.getParcels(), hasSize(1));
        assertThat(delivery.getParcels().get(0).getParcelItems(), hasSize(1));
    }

    @Test
    public void createAndGetPaymentForSberIdUnderUserRole() {
        // Arrange
        ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID);
        final Order prepaidOrder = OrderProvider.getPrepaidOrder(o -> {
            o.setRgb(Color.BLUE);
            o.setBuyer(BuyerProvider.getSberIdBuyer());
        });
        final ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.SBER_ID);

        // Act
        final long orderId = processOrder(prepaidOrder);
        final Payment payment = processPayment(orderId, clientInfo);
        final Order newOrder = orderService.getOrder(orderId);

        // Assert
        commonCheck(newOrder, payment);
        assertFalse(newOrder.isNoAuth()); // СберИД - это залогин
        assertNull(payment.getUid()); // Но uid на платеже не сохраняем и в Траст не передаём

        // Под ролью USER платёж должен вычитываться без ошибок
        final Payment newPayment = paymentService.findPayment(payment.getId(), clientInfo);
        assertNotNull(newPayment);
    }

    @Test
    public void shouldLoadPaymentsUpdatedMoreWeekAgo() {
        setFixedTime(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC));
        ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID);
        long orderId = OrderServiceHelper.createPrepaidOrder(orderCreateService, orderUpdateService,
                orderCompletionService);

        ClientInfo clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        paymentOperations.startPrepayPayment(
                orderId,
                clientInfo,
                "RETURN_PATH",
                null,
                PaymentFormType.DESKTOP,
                false,
                CreatePaymentContext.builder().build()
        );
        clearFixed();
        paymentOperations.startPrepayPayment(
                orderId,
                clientInfo,
                "RETURN_PATH",
                null,
                PaymentFormType.DESKTOP,
                false,
                CreatePaymentContext.builder().build()
        );
        Collection<Payment> paymentsMonthAgo = paymentService.loadPaymentsUpdatedMoreWeekAgo(PaymentStatus.values(),
                Long.MAX_VALUE, -1);

        assertThat(paymentsMonthAgo.size(), is(1));
    }

    @Test
    public void shouldCancelPaymentStatus() {
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

        assertThat(payment.getStatus(), not(PaymentStatus.CANCELLED));
        paymentService.updatePaymentStatusToCancel(payment);
        Payment canceledPayment = paymentService.findPayment(payment.getId(), ClientInfo.SYSTEM);
        assertThat(canceledPayment.getStatus(), is(PaymentStatus.CANCELLED));
    }


    @Test
    public void shouldCreatePaymentWithCustomCssTheme() {
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
                CreatePaymentContext.builder().cssTheme("awesome_css_theme").build()
        );

        Assertions.assertNotNull(payment);
        List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                postRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        String body = requests.iterator().next().getBodyAsString();
        var devPayload = JsonPath.read(body, "$.developer_payload").toString();
        JsonTest.checkJson(devPayload, "$.call_preview_payment", "card_info");
        JsonTest.checkJson(devPayload, "$.css-theme", "awesome_css_theme");
        JsonTest.checkJson(devPayload, "$.ProcessThroughYt", "1");
        JsonTest.checkJson(body, "$.pass_params.market_blue_3ds_policy", "UNKNOWN");
    }

    @Test
    public void shouldCreatePaymentWithoutPassParams() {
        checkouterProperties.setEnableTrustPassParams(false);
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
                CreatePaymentContext.builder().cssTheme("awesome_css_theme").build()
        );

        Assertions.assertNotNull(payment);
        List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                postRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        String body = requests.iterator().next().getBodyAsString();
        JsonTest.checkJsonNotExist(body, "$.pass_params");
    }

    @Test
    public void shouldCreatePaymentWithoutCssTheme() {
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

        Assertions.assertNotNull(payment);
        List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                postRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        String body = requests.iterator().next().getBodyAsString();
        JsonTest.checkJson(body, "$.pass_params.market_blue_3ds_policy", "UNKNOWN");
    }

    @Test
    public void testDsbsPay() {
        shopService.updateMeta(OrderProvider.SHOP_ID, ShopSettingsHelper.getDsbsShopPrepayMeta());
        test(order -> {
        });
    }


    @Test
    void ignoreCardIdForPrepaid() {
        String cardId = "card-x289zzz";
        checkouterProperties.setIgnoreCardId(true);
        ShopSettingsHelper.createShopSettings(shopService, OrderProvider.SHOP_ID);
        long orderId = OrderServiceHelper.createPrepaidOrder(orderCreateService, orderUpdateService,
                orderCompletionService);
        var clientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        paymentOperations.startPrepayPayment(orderId, clientInfo, "RETURN_PATH",
                cardId, PaymentFormType.DESKTOP, false, CreatePaymentContext.builder().build());

        ServeEvent createBasketEvent = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .findAny().get();
        JsonObject body = getRequestBodyAsJson(createBasketEvent);
        String payMethodId = body.get("paymethod_id").getAsString();
        assertFalse(payMethodId.contains(cardId));
    }

    private void test(final Consumer<Order> test) {
        Order prepaidOrder = OrderProvider.getPrepaidOrder();
        final long orderId = processOrder(prepaidOrder);

        final Payment payment = processPayment(orderId);
        final Order newOrder = orderService.getOrder(orderId, ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));

        commonCheck(newOrder, payment);
        test.accept(newOrder);
    }

    @Test
    public void shouldCreatePaymentWithHelpingHandSign() {
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
                CreatePaymentContext.builder().showCharityLabel(true).build()
        );
        Assertions.assertNotNull(payment);
        List<LoggedRequest> requests = trustMockConfigurer.trustMock().findRequestsMatching(
                postRequestedFor(urlEqualTo(TrustMockConfigurer.TRUST_PAYMENTS_CREATE_BASKET_URL)).build()
        ).getRequests();
        String body = requests.iterator().next().getBodyAsString();
        var devPayload = JsonPath.read(body, "$.developer_payload").toString();
        JsonTest.checkJson(devPayload, "$.call_preview_payment", "card_info");
        JsonTest.checkJson(devPayload, "$.ProcessThroughYt", "1");
        JsonTest.checkJson(devPayload, "$.show_charity_label", "true");
        JsonTest.checkJson(body, "$.pass_params.market_blue_3ds_policy", "UNKNOWN");
    }

    private void commonCheck(final Order order, final Payment payment) {
        assertThat(payment.getBasketKey().getBasketId(), equalTo(BASKET_ID));
        assertThat(payment.getBasketId(), equalTo(BASKET_ID));
        assertThat(payment.getBasketKey().getPurchaseToken(), equalTo(PURCHASE_TOKEN));
        assertThat(order.getPayment(), notNullValue());
    }

    private long processOrder(Order prepaidOrder) {
        final long orderId = orderCreateService.createOrder(prepaidOrder, ClientInfo.SYSTEM);
        Order order = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), prepaidOrder.getDelivery());
        orderCompletionService.completeOrder(order, ClientInfo.SYSTEM);
        return orderId;
    }

    private Payment processPayment(final long orderId) {
        ClientInfo userClientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
        return processPayment(orderId, userClientInfo);
    }

    private Payment processPayment(final long orderId, final ClientInfo clientInfo) {
        return paymentOperations.startPrepayPayment(
                orderId,
                clientInfo,
                "RETURN_PATH",
                null,
                PaymentFormType.DESKTOP,
                false,
                CreatePaymentContext.builder().build()
        );
    }
}
