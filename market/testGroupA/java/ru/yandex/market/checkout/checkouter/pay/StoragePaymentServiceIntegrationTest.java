package ru.yandex.market.checkout.checkouter.pay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Throwables;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.cashier.model.PassParams;
import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.checkers.CreateBalanceOrderParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CreateProductParams.product;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;


public class StoragePaymentServiceIntegrationTest extends AbstractServicesTestBase {

    private static final Map<Long, ShopMetaData> FF_SHOP_MAP = new HashMap<>();
    private static final ShopMetaData VIRTUAL_SHOP_META =
            ShopSettingsHelper.createCustomNewPrepayMeta(Long.valueOf(SHOP_ID).intValue());

    private static final Validator FULFILMENT_VALIDATOR = new FulfilmentValidator();

    static {
        FF_SHOP_MAP.put(774L, ShopSettingsHelper.createCustomNewPrepayMeta(774));
        FF_SHOP_MAP.put(123L, ShopSettingsHelper.createCustomNewPrepayMeta(123));
        FF_SHOP_MAP.put(124L, ShopSettingsHelper.createCustomNewPrepayMeta(124));
    }

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ShopService shopService;
    private Order saved;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{"yandexMarket", createDropshipOrder(), FULFILMENT_VALIDATOR},
                new Object[]{"fulfilment", createFfOrder(), FULFILMENT_VALIDATOR}
        ).stream().map(Arguments::of);
    }

    private static Order createDropshipOrder() {
        Order order = OrderProvider.getPostPaidOrder();
        order.setFulfilment(false);
        order.setPaymentType(PaymentMethod.CASH_ON_DELIVERY.getPaymentType());
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.getItems().forEach(i -> i.setSupplierId(order.getShopId()));
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order.getDelivery().setDeliveryServiceId(106L);
        order.setRgb(Color.BLUE);
        return order;
    }

    private static Order createFfOrder() {
        Order fulfilmentOrder = OrderProvider.getFulfilmentOrder();
        fulfilmentOrder.setRgb(Color.BLUE);
        fulfilmentOrder.setPaymentType(PaymentMethod.CASH_ON_DELIVERY.getPaymentType());
        fulfilmentOrder.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        fulfilmentOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        return fulfilmentOrder;
    }

    @AfterEach
    public void tearDown() {
        trustMockConfigurer.resetAll();
    }

    @ParameterizedTest(name = TEST_DISPLAY_NAME)
    @MethodSource("parameterizedTestData")
    public void createCashPayment(String caseName, Order order, Validator validator) throws Exception {
        setNewPrepayMetaForShops(order);

        trustMockConfigurer.mockWholeTrust();
        saved = orderServiceHelper.saveOrder(order);

        if (saved.getStatus() == OrderStatus.PENDING) {
            saved = orderUpdateService.updateOrderStatus(saved.getId(), OrderStatus.PROCESSING);
        }

        orderUpdateService.updateOrderStatus(saved.getId(), OrderStatus.DELIVERY);
        orderUpdateService.updateOrderStatus(saved.getId(), OrderStatus.DELIVERED);

        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_CASH_PAYMENT, saved.getId());

        Order fromDb = orderService.getOrder(saved.getId());

        List<Payment> cashPayments = paymentService.getPayments(
                saved.getId(),
                ClientInfo.SYSTEM,
                PaymentGoal.ORDER_POSTPAY
        );

        validator.validatePayments(fromDb, cashPayments);
        validator.validateBalanceInteraction(fromDb, trustMockConfigurer.eventsIterator());
    }

    private void setNewPrepayMetaForShops(Order order) {
        if (order.isFulfilment()) {
            order.getItems().stream()
                    .map(OrderItem::getSupplierId)
                    .filter(Objects::nonNull)
                    .forEach(id -> shopService.updateMeta(
                            id,
                            ShopSettingsHelper.createCustomNewPrepayMeta(id.intValue()))
                    );
        }
        shopService.updateMeta(
                order.getShopId(),
                ShopSettingsHelper.createCustomNewPrepayMeta(order.getShopId().intValue())
        );
    }

    private interface Validator {

        void validatePayments(Order order, List<Payment> payments);

        void validateBalanceInteraction(Order order, OneElementBackIterator<ServeEvent> eventsIterator)
                throws Exception;
    }

    private static class FulfilmentValidator implements Validator {

        @Override
        public void validatePayments(Order order, List<Payment> cashPaymentsList) {
            Assertions.assertNotNull(cashPaymentsList);
            assertThat(cashPaymentsList, hasSize(1));
            Payment cashPayment = cashPaymentsList.get(0);

            Assertions.assertEquals(order.getBuyerItemsTotal().add(order.getDelivery().getBuyerPrice()),
                    cashPayment.getTotalAmount(), "order.buyerTotal");
        }

        @Override
        public void validateBalanceInteraction(Order order, OneElementBackIterator<ServeEvent> servedEvents)
                throws Exception {
            List<CreateBalanceOrderParams> balanceOrderParams = new ArrayList<>();
            List<Pair<OrderItem, String>> items = new ArrayList<>();
            // Проверяем для item'ов заказа вызовы LoadPartner, CreateServiceProduct
            order.getItems().forEach(item -> {
                Long shopId = item.getSupplierId();
                ShopMetaData meta = FF_SHOP_MAP.get(item.getSupplierId());
                try {
                    checkLoadPartnerCall(servedEvents, meta.getClientId());
                    String serviceProductId = meta.getCampaignId() + "_" + meta.getClientId();
                    checkOptionalCreateServiceProductCall(
                            servedEvents, product(meta.getClientId(),
                                    meta.getClientId() + "-" + meta.getCampaignId(), serviceProductId)
                    );

                    OrderItem orderItem = order.getItems().stream().filter(i -> shopId.equals(i.getSupplierId()))
                            .findAny()
                            .orElseThrow(() -> new RuntimeException("No items in order? O_o"));

                    String serviceOrderIdProposal = "" + order.getId() + "-item-" + orderItem.getId();

                    balanceOrderParams.add(new CreateBalanceOrderParams(
                            meta.getAgencyCommission(),
                            serviceProductId,
                            order.getDisplayOrderId(),
                            notNullValue(PassParams.class),
                            serviceOrderIdProposal,
                            order.getCreationDate()
                    ));
                    items.add(Pair.of(orderItem, serviceOrderIdProposal));
                } catch (Exception ex) {
                    throw Throwables.propagate(ex);
                }
            });

            // Проверяем для доставки вызовы LoadPartner, CreateServiceProduct
            long clientId = VIRTUAL_SHOP_META.getClientId();

            checkOptionalLoadPartnerCall(servedEvents, clientId);
            checkOptionalCreateServiceProductCall(
                    servedEvents, product(PaymentTestHelper.MARKET_PARTNER_ID, "BLUE_MARKET_DELIVERY",
                            "BLUE_MARKET_DELIVERY", 1)
            );

            String serviceOrderIdProposal = "" + order.getId() + "-delivery";
            order.getDelivery().setBalanceOrderId(serviceOrderIdProposal);
            balanceOrderParams.add(new CreateBalanceOrderParams(
                    null,
                    "BLUE_MARKET_DELIVERY",
                    order.getDisplayOrderId(),
                    notNullValue(PassParams.class),
                    serviceOrderIdProposal,
                    order.getCreationDate()
            ));

            // Проверяем для вызов CreateOrdersBatch по всем накопленным данным

            checkBatchServiceOrderCreationCall(servedEvents, order.getUid(), balanceOrderParams);

            CreateBasketParams createBasket = createBasket()
                    .withUid(order.getUid())
                    .withYandexUid(order.getBuyer().getYandexUid())
                    .withPayMethodId("cash-0")
                    .withBackUrl(stringContainsInOrder(Arrays.asList("/payments", "/notify")))
                    .withPassParams(notNullValue(String.class))
                    .withCurrency(Currency.RUR)
                    .withOrdersByItemsAndDelivery(order, false)
                    .withDeveloperPayload("{\"ProcessThroughYt\":1,\"call_preview_payment\":\"card_info\"}");
            TrustBasketKey basketKey = checkCreateBasketCall(servedEvents, createBasket);
            checkPayBasketCall(servedEvents, order.getUid(), basketKey);

            Assertions.assertFalse(servedEvents.hasNext());
        }
    }
}
