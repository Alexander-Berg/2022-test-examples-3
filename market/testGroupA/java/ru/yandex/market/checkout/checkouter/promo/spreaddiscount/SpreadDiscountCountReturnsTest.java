package ru.yandex.market.checkout.checkouter.promo.spreaddiscount;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnOptionsResponse;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.pay.RefundReason.USER_RETURNED_ITEM;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.fbyRequestFor;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.helpers.RefundHelper.assertRefund;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class SpreadDiscountCountReturnsTest extends AbstractReturnTestBase {

    public static final Long DELIVERY_SERVICE_ID = 777L;

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private RefundHelper refundHelper;

    @Test
    void shouldReturnForDiscountMorePrice() throws Exception {
        final OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(100)
                .count(20);

        Order order = orderCreateHelper.createOrder(fbyRequestFor(single(orderWithYandexDelivery()
                .itemBuilder(primaryOffer)
        ), PROMO_KEY, config ->
                config.expectResponseItems(itemResponseFor(primaryOffer)
                        .quantity(20)
                        .price(BigDecimal.valueOf(100))
                        .promo(new ItemPromoResponse(
                                BigDecimal.valueOf(10),
                                PromoType.SPREAD_COUNT,
                                null,
                                PROMO_KEY,
                                SHOP_PROMO_KEY,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        ))
                )));

        assertThat(order.getTotal(), comparesEqualTo(BigDecimal.valueOf(1900)));
        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(ImmutableList.of(order)));
        trustMockConfigurer.mockStatusBasket(checkBasketForClearTask(ImmutableList.of(order)), null);

        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERED);

        Return request = new Return();
        List<ReturnItem> returnItems = order.getItems()
                .stream()
                .map(s -> {
                    ReturnItem returnItem = new ReturnItem();
                    returnItem.setCount(2);
                    returnItem.setQuantity(BigDecimal.valueOf(2));
                    returnItem.setItemId(s.getId());
                    returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                    return returnItem;
                }).collect(toList());
        request.setItems(returnItems);

        returnHelper.mockActualDelivery(request, order, DELIVERY_SERVICE_ID);
        ReturnOptionsResponse returnOptions = client.returns().getReturnOptions(
                order.getId(),
                ClientRole.SYSTEM,
                DELIVERY_SERVICE_ID,
                returnItems
        );

        ReturnDelivery option = returnOptions.getDeliveryOptions().stream()
                .filter(returnDelivery -> returnDelivery.getType() == DeliveryType.DELIVERY)
                .findAny()
                .orElseThrow(() -> new RuntimeException("chosen DeliveryType unavailable; please change report " +
                        "actualDelivery configs"));
        request.setDelivery(convertOptionToDelivery(option));
        request.setBankDetails(ReturnHelper.createDummyBankDetails());

        trustMockConfigurer.mockWholeTrust();
        Return ret = returnHelper.initReturn(order.getId(), request);
        returnHelper.resumeReturn(order.getId(), ret.getId(), ret);
        // Create refunds
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(ret);
        notifyRefundReceipts(ret);
        // Notify refunds
        returnService.processReturnPayments(order.getId(), ret.getId(), ClientInfo.SYSTEM);
        ret = getReturnById(ret.getId());

        assertThat(ret, not(nullValue()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUNDED));

        Collection<Refund> refunds = refundService.getReturnRefunds(ret);

        assertThat(refunds, hasSize(1));

        Refund refund = refunds.iterator().next();

        assertRefund(refund, SUCCESS, USER_RETURNED_ITEM);
        assertThat(refund.getAmount(), comparesEqualTo(BigDecimal.valueOf(180)));

        Receipt refundReceipt = receiptService.findByRefund(refund).iterator().next();

        assertThat(refundReceipt, notNullValue());
        assertThat(refundReceipt.getItems(), hasSize(1));
    }
}
