package ru.yandex.market.checkout.checkouter.promo.spreaddiscount;

import java.math.BigDecimal;
import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.promo.bundles.utils.LoyaltyTestUtils;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyService;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.SPREAD_DISCOUNT_RECEIPT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CLEARED;
import static ru.yandex.market.checkout.checkouter.pay.RefundReason.ORDER_CANCELLED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.ACCEPTED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.helpers.RefundHelper.assertRefund;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.test.providers.OrderProvider.orderBuilder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

/**
 * @author Anastasiya Emelianova / orphie@ / 8/12/21
 */
public class SpreadDiscountReceiptTest extends AbstractWebTestBase {

    private static final String PROMO = "spread-discount-receipt";
    @Autowired
    private LoyaltyService loyaltyService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private ReceiptService receiptService;

    @Test
    public void shouldCheckDiscountAndReturnPromos() {
        final OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(1000)
                .count(2);

        final OrderItemProvider.OrderItemBuilder secondOffer = orderItemWithSortingCenter()
                .offer(SECOND_OFFER)
                .price(1000)
                .count(2);

        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(primaryOffer)
                .itemBuilder(secondOffer)
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectResponseItems(
                        itemResponseFor(primaryOffer)
                                .quantity(2)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(250),
                                        PromoType.SPREAD_RECEIPT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        ANAPLAN_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                        itemResponseFor(secondOffer)
                                .quantity(2)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(250),
                                        PromoType.SPREAD_RECEIPT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        ANAPLAN_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(primaryOffer, requestParameters.getBuiltMultiCart()));
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(secondOffer, requestParameters.getBuiltMultiCart()));

        assertThat(cart.getCarts(), Matchers.hasItem(hasProperty("items", hasItems(
                Matchers.allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("buyerPrice", equalTo(BigDecimal.valueOf(750))),
                        hasProperty("bundleId", Matchers.nullValue()),
                        hasProperty("promos", Matchers.hasItems(
                                hasProperty("promoDefinition", Matchers.allOf(
                                        hasProperty("type", equalTo(SPREAD_DISCOUNT_RECEIPT)),
                                        hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("bundleId", Matchers.nullValue())
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(250)))
                        ))
                ),
                Matchers.allOf(
                        hasProperty("offerId", equalTo(SECOND_OFFER)),
                        hasProperty("buyerPrice", equalTo(BigDecimal.valueOf(750))),
                        hasProperty("bundleId", Matchers.nullValue()),
                        hasProperty("promos", Matchers.hasItems(
                                hasProperty("promoDefinition", Matchers.allOf(
                                        hasProperty("type", equalTo(SPREAD_DISCOUNT_RECEIPT)),
                                        hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("bundleId", Matchers.nullValue())
                                )),
                                hasProperty("buyerDiscount", comparesEqualTo(BigDecimal.valueOf(250)))
                        ))
                )
        ))));
    }

    @Test
    void loyaltyReturnsNoPromo() {
        final OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(10000)
                .count(2);

        final OrderItemProvider.OrderItemBuilder secondOffer = orderItemWithSortingCenter()
                .offer(SECOND_OFFER)
                .price(1000)
                .count(2);

        MultiCart cart = MultiCartProvider.single(orderBuilder()
                .someLabel()
                .stubApi()
                .itemBuilder(primaryOffer)
                .itemBuilder(secondOffer)
        );

        Parameters requestParameters = new Parameters();
        requestParameters.getLoyaltyParameters()
                .expectResponseItems(
                        itemResponseFor(primaryOffer)
                                .quantity(3),
                        itemResponseFor(secondOffer)
                                .quantity(3)
                );

        loyaltyConfigurer.mockCalcsWithDynamicResponse(requestParameters);
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(primaryOffer, requestParameters.getBuiltMultiCart()));
        loyaltyService.applyDiscounts(cart, ImmutableMultiCartParameters.builder().build(),
                createTestContext(secondOffer, requestParameters.getBuiltMultiCart()));

        assertThat(cart.getCarts(), Matchers.hasItem(hasProperty("items", hasItems(
                Matchers.allOf(
                        hasProperty("offerId", equalTo(PRIMARY_OFFER)),
                        hasProperty("buyerPrice", equalTo(BigDecimal.valueOf(10000))),
                        hasProperty("bundleId", Matchers.nullValue()),
                        hasProperty("promos", Matchers.empty())
                ),
                Matchers.allOf(
                        hasProperty("offerId", equalTo(SECOND_OFFER)),
                        hasProperty("buyerPrice", equalTo(BigDecimal.valueOf(1000))),
                        hasProperty("bundleId", Matchers.nullValue()),
                        hasProperty("promos", Matchers.empty())
                )
        ))));
    }

    @Test
    void shouldCancelDeliveringOrder() throws Exception {
        final OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(10000)
                .count(2);

        final OrderItemProvider.OrderItemBuilder secondOffer = orderItemWithSortingCenter()
                .offer(SECOND_OFFER)
                .price(1000)
                .count(2);

        Order typicalOrder = orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(secondOffer)
        ), PROMO_KEY, config ->
                config.expectResponseItems(itemResponseFor(primaryOffer)
                                .quantity(3)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(20),
                                        PromoType.SPREAD_RECEIPT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        ANAPLAN_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                )),
                        itemResponseFor(secondOffer)
                                .quantity(3)
                                .promo(new ItemPromoResponse(
                                        BigDecimal.valueOf(10),
                                        PromoType.SPREAD_RECEIPT,
                                        null,
                                        PROMO_KEY,
                                        SHOP_PROMO_KEY,
                                        null,
                                        ANAPLAN_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                )));

        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(typicalOrder)));
        trustMockConfigurer.mockStatusBasket(checkBasketForClearTask(ImmutableList.of(typicalOrder)), null);

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, DELIVERY);

        assertThat(typicalOrder.getStatus(), is(DELIVERY));
        assertThat(typicalOrder.getPayment().getStatus(), is(CLEARED));

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_CASH_PAYMENT);

        typicalOrder = orderStatusHelper.proceedOrderToStatus(typicalOrder, CANCELLED);

        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND_SUBSIDY_PAYMENT);

        Refund refund = refundHelper.anyRefundFor(typicalOrder, PaymentGoal.ORDER_PREPAY);
        refund = refundHelper.proceedAsyncRefund(refund);
        assertRefund(refund, ACCEPTED, ORDER_CANCELLED);

        refund = refundHelper.processToSuccess(refund, typicalOrder);
        assertRefund(refund, SUCCESS, ORDER_CANCELLED);

        typicalOrder = orderService.getOrder(typicalOrder.getId());

        assertThat(typicalOrder.getStatus(), is(CANCELLED));
        assertThat(refund.getAmount(), comparesEqualTo(typicalOrder.getTotal()));

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(3));
        for (ReceiptItem receiptItem : refundReceipt.getItems()) {
            if (receiptItem.getDeliveryId() == null) {
                assertThat(typicalOrder.getItems(), hasItem(allOf(
                        hasProperty("price", is(receiptItem.getPrice())),
                        hasProperty("promos", hasItem(allOf(
                                hasProperty("promoDefinition", Matchers.allOf(
                                        hasProperty("type", equalTo(SPREAD_DISCOUNT_RECEIPT)),
                                        hasProperty("marketPromoId", equalTo(PROMO_KEY)),
                                        hasProperty("shopPromoId", is(SHOP_PROMO_KEY)),
                                        hasProperty("anaplanId", is(ANAPLAN_ID)),
                                        hasProperty("bundleId", Matchers.nullValue())
                                ))
                        )))
                )));
            }
        }
    }

    private LoyaltyContext createTestContext(OrderItemProvider.OrderItemBuilder orderItemBuilder, MultiCart multiCart) {
        var foundOffers = Collections.singletonList(FoundOfferBuilder.createFrom(orderItemBuilder.build())
                .promoKey(PROMO)
                .promoType(PromoType.SPREAD_RECEIPT.getCode())
                .build());
        return LoyaltyTestUtils.createTestContext(multiCart, foundOffers);
    }
}
