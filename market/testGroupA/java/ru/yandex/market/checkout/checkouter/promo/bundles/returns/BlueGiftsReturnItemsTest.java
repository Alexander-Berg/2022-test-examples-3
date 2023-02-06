package ru.yandex.market.checkout.checkouter.promo.bundles.returns;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
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
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.FEED_ID;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class BlueGiftsReturnItemsTest extends AbstractReturnTestBase {

    @Autowired
    private RefundService refundService;

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private RefundHelper refundHelper;

    @BeforeEach
    public void setUp() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
    }

    @Test
    public void shouldCreateOrderReturn() throws Exception {
        Order typicalOrder = createTypicalOrderWithBundles();

        Return ret = processReturnAndPayments(typicalOrder, prepareDefaultReturnRequest(
                typicalOrder,
                DeliveryType.DELIVERY
        ));

        assertThat(ret, not(nullValue()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUNDED));

        Collection<Refund> refunds = refundService.getReturnRefunds(ret);

        assertThat(refunds, hasSize(1));

        Refund refund = refunds.iterator().next();

        assertRefund(refund, SUCCESS, USER_RETURNED_ITEM);

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(3));
    }

    @Test
    public void shouldCreatePartialReturn() throws Exception {
        Order typicalOrder = createTypicalOrderWithBundles();

        OrderItem expectedItem = typicalOrder.getItem(OfferItemKey.of(
                PRIMARY_OFFER,
                FEED_ID,
                PROMO_BUNDLE
        ));

        Return request = prepareDefaultReturnRequest(
                typicalOrder,
                DeliveryType.DELIVERY
        );

        request.setItems(Collections.singletonList(toReturnItem(expectedItem, BigDecimal.valueOf(100))));

        Return ret = processReturnAndPayments(typicalOrder, request);

        assertThat(ret, not(nullValue()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUNDED));

        Collection<Refund> refunds = refundService.getReturnRefunds(ret);

        assertThat(refunds, hasSize(1));

        Refund refund = refunds.iterator().next();

        assertRefund(refund, SUCCESS, USER_RETURNED_ITEM);

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(1));
    }

    @Nonnull
    private Return processReturnAndPayments(Order order, Return retReq) {
        trustMockConfigurer.mockWholeTrust();
        Return ret = returnHelper.initReturn(order.getId(), retReq);
        returnHelper.resumeReturn(order.getId(), ret.getId(), ret);
        // Create refunds
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(ret);
        notifyRefundReceipts(ret);
        // Notify refunds
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);
        ret = getReturnById(ret.getId());
        return ret;
    }


    private Order createTypicalOrderWithBundles() {
        OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .price(10000);

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
        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, OrderStatus.DELIVERED);

        return typicalOrder;
    }
}
