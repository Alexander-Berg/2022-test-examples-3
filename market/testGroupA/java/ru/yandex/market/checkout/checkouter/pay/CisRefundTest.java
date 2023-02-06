package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.loyalty.api.model.CashbackType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.providers.BnplTestProvider.CASHBACK_AMOUNT;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;

public class CisRefundTest extends AbstractPaymentTestBase {

    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper checkouterAnnotationObjectMapper;

    @Test
    void shouldRefundOneItemOfFourWithCises() throws Exception {
        Parameters parameters = defaultBnplParameters();
        var itemCount = 4;
        var itemPrice = new BigDecimal("3595.00");
        parameters.getOrders().stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .forEach(orderItems -> {
                    orderItems.setCargoTypes(Set.of(980));
                    orderItems.setCount(itemCount);
                    orderItems.setBuyerPrice(itemPrice);
                });
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        parameters.getBuiltMultiCart().setCashback(
                new Cashback(null, CashbackOptions.allowed(CASHBACK_AMOUNT, "28")));
        Order order = orderCreateHelper.createOrder(parameters);
        Payment basePayment = orderPayHelper.payForOrder(order);

        basePayment = paymentService.getPayment(basePayment.getId(), ClientInfo.SYSTEM);

        assertEquals(PaymentStatus.HOLD, basePayment.getStatus());
        var partitions = basePayment.getPartitions();
        assertThat(partitions, hasSize(2));

        var defaultPartition = getPartition(partitions, PaymentAgent.DEFAULT);
        var cashbackPartition = getPartition(partitions, PaymentAgent.YANDEX_CASHBACK);
        var markupItemNameTemplate = order.getId() + "-item-" + order.getItems().stream()
                .findFirst()
                .map(OrderItem::getId)
                .orElseThrow() + "-";

        assertThat(
                defaultPartition.getAmount(),
                equalTo(
                        itemPrice.multiply(BigDecimal.valueOf(itemCount))
                                .add(BigDecimal.valueOf(100L /* delivery*/))
                                .subtract(BigDecimal.valueOf(28L /* ya plus amount*/)))
        );
        assertThat(
                cashbackPartition.getAmount(),
                equalTo(new BigDecimal("28.00" /* ya plus amount*/))
        );

        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order.getId()), OrderStatus.DELIVERY);

        var refundableItems = refundHelper.getRefundableItemsFor(order)
                .getItems()
                .stream()
                .findFirst()
                .map(item -> {
                    item.setRefundableCount(1);
                    item.setRefundableQuantity(BigDecimal.ONE);
                    var items = new RefundableItems().withItems(List.of(item));
                    items.setItemServices(List.of());
                    return items;
                })
                .orElseThrow();

        var createBasketEvent = trustMockConfigurer.servedEvents()
                .stream()
                .filter(event -> CREATE_BASKET_STUB.equals(event.getStubMapping().getName()))
                .findFirst()
                .orElseThrow();

        var payMethodMarkup = checkouterAnnotationObjectMapper
                .readValue(createBasketEvent.getRequest().getBodyAsString(), JsonNode.class)
                .get("paymethod_markup");

        validateMarkup(
                markupItemNameTemplate,
                itemCount,
                payMethodMarkup,
                3588.0,
                7.0
        );

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        refundHelper.refund(
                refundableItems,
                orderService.getOrder(order.getId()),
                RefundReason.USER_RETURNED_ITEM,
                200,
                true
        );

        var refundEvent = trustMockConfigurer.servedEvents()
                .stream()
                .filter(event -> CREATE_REFUND_STUB.equals(event.getStubMapping().getName()))
                .findFirst()
                .orElseThrow();
        assertThat(refundEvent, notNullValue());
        var refundPayMethodMarkup = checkouterAnnotationObjectMapper
                .readValue(refundEvent.getRequest().getBodyAsString(), JsonNode.class)
                .get("paymethod_markup");

        validateMarkup(
                markupItemNameTemplate,
                1,
                refundPayMethodMarkup,
                3588.00,
                7.00
        );
    }

    public void validateMarkup(
            String itemTemplate,
            int itemCount,
            JsonNode payMethodMarkup,
            Double cardPayment,
            Double yaPlus
    ) {
        IntStream.range(1, itemCount + 1).forEachOrdered(itemIndex -> {
            var line = payMethodMarkup.get(itemTemplate + itemIndex);
            assertThat(line.get("card").asDouble(), equalTo(cardPayment));
            assertThat(line.get("yandex_account").asDouble(), equalTo(yaPlus));
        });
    }

    @Nonnull
    private PaymentPartition getPartition(List<PaymentPartition> partitions, PaymentAgent paymentAgent) {
        return partitions.stream()
                .filter(partition -> partition.getPaymentAgent() == paymentAgent)
                .findFirst()
                .orElseThrow();
    }
}
