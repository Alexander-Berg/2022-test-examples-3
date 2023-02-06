package ru.yandex.market.checkout.checkouter.promo.bundles.returns;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.pay.RefundReason.USER_RETURNED_ITEM;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.helpers.RefundHelper.assertRefund;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueGiftsRefundItemsTest extends AbstractWebTestBase {

    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private ReceiptService receiptService;

    @Test
    public void shouldReturnAllItemsAsRefundable() throws Exception {
        Order order = createTypicalOrderWithBundles();

        Set<Long> itemsSet = order.getItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toSet());

        assertThat(refundHelper.getRefundableItemsFor(order), allOf(
                hasProperty("items", everyItem(hasProperty("id", isIn(itemsSet)))),
                hasProperty("items", hasSize(3))
        ));
    }

    @Test
    public void shouldCreateFullRefunds() throws Exception {
        Order typicalOrder = createTypicalOrderWithBundles();

        Refund refund = refundHelper.refund(typicalOrder, USER_RETURNED_ITEM, SC_OK);
        typicalOrder = orderService.getOrder(typicalOrder.getId());

        assertRefund(refund, SUCCESS, USER_RETURNED_ITEM);
        Assertions.assertEquals(refund.getAmount(), typicalOrder.getRefundActual());
        Assertions.assertEquals(typicalOrder.getRefundActual(), typicalOrder.getTotal());

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(4));
    }

    @Test
    public void shouldCreatePartialRefunds() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        final Order typicalOrder = createTypicalOrderWithBundles();

        RefundableItems refundableItems = refundHelper.getRefundableItemsFor(typicalOrder);

        AtomicReference<BigDecimal> refundAmount = new AtomicReference<>(BigDecimal.ZERO);

        var itemsToRefund = refundableItems.withItems(refundableItems.getItems().stream()
                .filter(item -> PRIMARY_OFFER.equals(item.getOfferId()))
                .filter(item -> PROMO_BUNDLE.equals(item.getBundleId()))
                .peek(item -> refundAmount.updateAndGet(amount -> amount.add(
                        item.getPrice().multiply(BigDecimal.valueOf(item.getCount())))))
                .collect(Collectors.toList()));
        itemsToRefund.setItemServices(
                itemsToRefund.getItems().stream()
                        .flatMap(ri -> typicalOrder.getItem(ri.getId()).getServices().stream())
                        .map(ItemService::getId)
                        .map(refundableItems::getItemServiceById)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
        itemsToRefund.getItemServices()
                .forEach(s -> refundAmount.updateAndGet(amount ->
                        amount.add(s.getPrice().multiply(BigDecimal.valueOf(s.getCount())))
                ));

        Refund refund = refundHelper.refund(itemsToRefund, typicalOrder, USER_RETURNED_ITEM, SC_OK);

        assertRefund(refund, SUCCESS, USER_RETURNED_ITEM);
        assertThat(refund.getAmount(), comparesEqualTo(refundAmount.get()));

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(2));
    }

    private Order createTypicalOrderWithBundles() {
        OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .price(10000)
                .services(
                        Collections.singleton(
                                ItemServiceProvider.builder()
                                        .configure(ItemServiceProvider::applyDefaults)
                                        .paymentMethod(PaymentMethod.YANDEX)
                                        .paymentType(PaymentType.PREPAID)
                                        .count(1)
                                        .build()
                        )
                );

        OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(GIFT_OFFER)
                .price(2000);

        Order typicalOrder = orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(secondaryOffer)
        ), PROMO_KEY, config ->
                config.expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(true),
                                itemResponseFor(secondaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(false),
                                itemResponseFor(primaryOffer)
                        )));


        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(typicalOrder)));
        trustMockConfigurer.mockStatusBasket(checkBasketForClearTask(ImmutableList.of(typicalOrder)), null);

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, OrderStatus.PROCESSING);
        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, OrderStatus.DELIVERY);

        return orderService.getOrder(typicalOrder.getId(), ClientInfo.SYSTEM,
                Collections.singleton(OptionalOrderPart.ITEM_SERVICES));
    }
}
