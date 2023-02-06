package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CreateProductParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.FREE_DELIVERY_THRESHOLD;
import static ru.yandex.market.checkout.checkouter.pay.builders.AbstractPaymentBuilder.DELIVERY_TITLE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;

public class CreateWhiteOrderTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "PROMO-CODE";

    @Autowired
    private OrderPayHelper orderPayHelper;

    @Autowired
    private ShopService shopService;

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReceiptService receiptService;

    @Test
    public void shouldCreateWhiteOrderWithShopDelivery() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        assertEquals(DELIVERY, order.getDelivery().getType());

        orderPayHelper.payForOrder(order);

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        OrderRequest orderRequest = OrderRequest.builder(order.getId()).withArchived(false).build();
        order = client.getOrder(requestClientInfo, orderRequest);

        assertThat(order.getDelivery().getBalanceOrderId(), notNullValue());
    }

    @Test
    public void shouldCreateWhiteOrderWithSubsidy() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setupPromo(PROMO_CODE);
        whiteParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(whiteParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
    }

    @Test
    public void shouldCreateShopDeliveryForWhiteWithShopServiceProduct() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());

        orderPayHelper.payForOrder(order);
        OneElementBackIterator<ServeEvent> callIterator = trustMockConfigurer.eventsIterator();
        ShopMetaData shop = shopService.getMeta(order.getShopId());
        assertEquals(trustMockConfigurer.servedGatewayEvents().size(), 2);
        TrustCallsChecker.checkLoadPartnerCall(callIterator, shop.getClientId());
        TrustCallsChecker.checkOptionalCreateServiceProductCall(callIterator, CreateProductParams.product(
                shop.getClientId(),
                "" + shop.getClientId() + "-" + shop.getCampaignId(),
                shop.getCampaignId() + "_" + shop.getClientId()
        ));

        List<Payment> pagedPayments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.ORDER_PREPAY);
        assertThat(pagedPayments, hasSize(1));
        Payment payment = Iterables.getOnlyElement(pagedPayments);
        assertThat(payment.getStatus(), equalTo(PaymentStatus.HOLD));

        List<Receipt> receipts = receiptService.findByPayment(payment, ReceiptType.INCOME);
        assertThat(receipts, hasSize(1));

        ReceiptItem deliveryReceiptItem = Iterables.getOnlyElement(receipts).getItems()
                .stream().filter(it -> it.getItemTitle().equals(DELIVERY_TITLE)).findFirst().orElseThrow();

        assertEquals(1, deliveryReceiptItem.getCount());
        assertEquals(0, deliveryReceiptItem.getPrice().compareTo(BigDecimal.TEN));
    }


    @Test
    public void shouldCreateWhiteOrderWithFreeShopDelivery() throws Exception {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setMockLoyalty(true);
        whiteParameters.setFreeDelivery(true);
        whiteParameters.setCheckCartErrors(false);
        whiteParameters.getLoyaltyParameters()
                .addDeliveryDiscount(LoyaltyDiscount.discountFor(10, FREE_DELIVERY_THRESHOLD));
        MultiCart cart = orderCreateHelper.cart(whiteParameters);

        MultiOrder orders = orderCreateHelper.checkout(cart, whiteParameters);
        Order order = orders.getOrders().iterator().next();

        assertEquals(0, order.getDelivery().getPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getDelivery().getPrices().getSubsidy().compareTo(BigDecimal.valueOf(10)));
        assertThat(order.getDelivery().getPromos(), contains(
                ItemPromo.createWithSubsidy(PromoDefinition.builder()
                        .type(PromoType.FREE_DELIVERY_THRESHOLD)
                        .build(), BigDecimal.valueOf(10))));


        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);
        OrderRequest orderRequest = OrderRequest.builder(order.getId()).withArchived(false).build();
        order = client.getOrder(requestClientInfo, orderRequest);

        assertThat(order.getDelivery().getSubsidyBalanceOrderId(), notNullValue());
        assertEquals(0, order.getDelivery().getPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getDelivery().getPrices().getSubsidy().compareTo(BigDecimal.valueOf(10)));
        assertThat(order.getDelivery().getPromos(), contains(
                ItemPromo.createWithSubsidy(PromoDefinition.builder()
                        .type(PromoType.FREE_DELIVERY_THRESHOLD)
                        .build(), BigDecimal.valueOf(10))));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void shouldFailOnCreateWhiteOrderWithIncorrectAddressRegion(boolean enable) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_ADDRESS_REGION_ACTUALIZER, enable);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ADDRESS_REGION_POSTPROCESSOR_ENABLED, true);

        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        MultiCart cart = orderCreateHelper.cart(whiteParameters);
        Boolean deliveryAvailable = cart.getCarts().stream()
                .anyMatch(c -> c.getDeliveryOptions().stream().anyMatch(d -> d.getType() == DELIVERY));
        assertEquals(false, deliveryAvailable);
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void shouldCreateWhiteOrderWithCorrectAddressRegionAlpha(boolean enable) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_ADDRESS_REGION_ACTUALIZER, enable);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ADDRESS_REGION_POSTPROCESSOR_ENABLED, true);

        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        ((AddressImpl) whiteParameters.getOrder().getDelivery().getBuyerAddress()).setCity("Москва");
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(DELIVERY, order.getDelivery().getType());
        assertEquals(213L, order.getDelivery().getRegionId());
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    public void shouldCreateWhiteOrderWithCorrectAddressRegionBeta(boolean enable) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_ADDRESS_REGION_ACTUALIZER, enable);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ADDRESS_REGION_POSTPROCESSOR_ENABLED, true);

        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        ((AddressImpl) whiteParameters.getOrder().getDelivery().getBuyerAddress()).setCity("Москва и Московская " +
                "область");
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(DELIVERY, order.getDelivery().getType());
        assertEquals(213L, order.getDelivery().getRegionId());
    }
}
