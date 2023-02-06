package ru.yandex.market.checkout.checkouter.items;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod.PUSH_API;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.ClientHelper.shopClientFor;

@Deprecated
public class AbstractChangeOrderItemsTestBase extends AbstractWebTestBase {

    @Autowired
    protected TestSerializationService serializationService;
    @Autowired
    protected OrderPayHelper paymentHelper;
    @Autowired
    protected ChangeOrderItemsHelper changeOrderItemsHelper;
    @Autowired
    protected QueuedCallService queuedCallService;
    protected Order order;
    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;
    @Autowired
    private CipherService reportCipherService;

    @BeforeEach
    public void prepare() throws Exception {
        trustMockConfigurer.mockWholeTrust();
    }

    protected Order refreshOrder() {
        return order = orderService.getOrder(order.getId());
    }

    protected Collection<OrderItem> orderItemsWithCorrectWareMD5() {
        Collection<OrderItem> items = OrderItemProvider.getDefaultItems();
        items.forEach(item -> {
            item.setWareMd5(item.getWareMd5() + item.getOfferId());
            OrderItemProvider.patchShowInfo(item, reportCipherService);
        });
        return items;
    }

    protected Order createOrder(OrderConfig config) {
        Order orderTemplate = OrderProvider.getColorOrder(config.color);
        orderTemplate.setItems(config.items);
        orderTemplate.setAcceptMethod(config.acceptMethod);
        orderTemplate.setDelivery(DeliveryProvider.getEmptyDelivery());

        Parameters parameters;
        switch (config.color) {
            case BLUE:
                parameters = defaultBlueOrderParameters(orderTemplate);
                if (config.fulfillment) {
                    fulfillmentConfigurer.configure(parameters);
                    parameters.setWeight(BigDecimal.valueOf(1));
                    parameters.setDimensions("10", "10", "10");
                }
                parameters.setDeliveryPartnerType(config.deliveryPartnerType);
                break;
            default:
                parameters = new Parameters(orderTemplate);
        }
        if (config.paymentType == PaymentType.PREPAID) {
            parameters.setPaymentMethod(PaymentMethod.YANDEX);
        } else {
            parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        }
        if (config.promo) {
            parameters.setupPromo("SOME-PROMO-CODE");
        }
        parameters.setFreeDelivery(config.freeDelivery);
        parameters.setDeliveryType(config.deliveryType);
        parameters.setDeliveryServiceId(null);
        return order = orderCreateHelper.createOrder(parameters);
    }

    protected ResultActions changeOrderItems(Collection<OrderItem> newItems, ClientInfo client, long orderId) throws
            Exception {
        return changeOrderItemsHelper.changeOrderItems(newItems, client, orderId);
    }

    protected ResultActions changeOrderItems(Collection<OrderItem> newItems, ClientInfo client) throws Exception {
        return changeOrderItems(newItems, client, order.getId());
    }

    protected ResultActions changeOrderItems(Collection<OrderItem> newItems) throws Exception {
        return changeOrderItems(newItems, shopClientFor(order));
    }

    private ResultActions checkChangeItemsResponse(ResultActions result, Order orderBefore,
                                                   Map<OfferItemKey, Integer> itemsNewCount,
                                                   Collection<OrderItem> expectedItems) throws Exception {
        changeOrderItemsHelper.checkChangeOrderItemsResponse(result, orderBefore, itemsNewCount, expectedItems);
        refreshOrder();
        return result;
    }

    void changeItemsAndCheckResult(Map<OfferItemKey, Integer> itemsNewCount, ClientInfo clientInfo, HttpStatus expected)
            throws Exception {
        Order orderBefore = order;
        Collection<OrderItem> newItems = order.getItems().stream().map(OrderItem::clone).collect(toList());
        newItems.forEach(i -> i.setCount(itemsNewCount.getOrDefault(i.getOfferItemKey(), i.getCount())));
        final ResultActions result = changeOrderItems(newItems, clientInfo)
                .andExpect(MockMvcResultMatchers.status().is(expected.value()));
        if (expected == HttpStatus.OK) {
            checkChangeItemsResponse(result, orderBefore, itemsNewCount, newItems);
            checkChangeItemsResponse(changeOrderItemsHelper.getOrderItems(order.getId()), orderBefore, itemsNewCount,
                    newItems);
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_PARTIAL_UNHOLD);
        }
    }

    void changeItemsAndCheckResult(Map<OfferItemKey, Integer> itemsNewCount, ClientInfo clientInfo)
            throws Exception {
        changeItemsAndCheckResult(itemsNewCount, clientInfo, HttpStatus.OK);
    }

    void changeItemsAndCheckResult(Map<OfferItemKey, Integer> itemsNewCount) throws Exception {
        changeItemsAndCheckResult(itemsNewCount, shopClientFor(order), HttpStatus.OK);
    }

    static class OrderConfig {

        Collection<OrderItem> items = OrderItemProvider.getDefaultItems();
        PaymentType paymentType = PaymentType.POSTPAID;
        OrderAcceptMethod acceptMethod = PUSH_API;
        DeliveryPartnerType deliveryPartnerType = DeliveryPartnerType.SHOP;
        DeliveryType deliveryType = null;
        boolean promo = false;
        boolean fulfillment = false;
        boolean freeDelivery = false;
        Color color = Color.WHITE;

        static OrderConfig defaultConfig() {
            return new OrderConfig();
        }

        OrderConfig with(Collection<OrderItem> items) {
            this.items = items;
            return this;
        }

        OrderConfig with(PaymentType paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        OrderConfig with(OrderAcceptMethod acceptMethod) {
            this.acceptMethod = acceptMethod;
            return this;
        }

        OrderConfig with(DeliveryPartnerType deliveryPartnerType) {
            this.deliveryPartnerType = deliveryPartnerType;
            return this;
        }

        OrderConfig with(DeliveryType deliveryType) {
            this.deliveryType = deliveryType;
            return this;
        }

        OrderConfig withPromo(boolean promo) {
            this.promo = promo;
            return this;
        }

        OrderConfig freeDelivery() {
            this.freeDelivery = true;
            return this;
        }

        OrderConfig fulfillment(boolean fulfillment) {
            this.fulfillment = fulfillment;
            return this;
        }

        OrderConfig withColor(Color color) {
            this.color = color;
            return this;
        }
    }
}
