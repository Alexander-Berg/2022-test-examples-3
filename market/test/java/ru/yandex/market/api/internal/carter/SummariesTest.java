package ru.yandex.market.api.internal.carter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.domain.v2.DiscountType;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.cart.CartItemV2;
import ru.yandex.market.api.domain.v2.cart.DeliveryInfo;
import ru.yandex.market.api.domain.v2.cart.ServiceInfo;
import ru.yandex.market.api.domain.v2.cart.Summary;
import ru.yandex.market.api.internal.loyalty.ThresholdResponse;
import ru.yandex.market.api.matchers.SummaryMatcher;
import ru.yandex.market.api.user.order.DeliveryOption;
import ru.yandex.market.api.user.order.OutletDeliveryOption;
import ru.yandex.market.api.user.order.PostDeliveryOption;
import ru.yandex.market.api.user.order.ServiceDeliveryOption;
import ru.yandex.market.api.user.order.ShopOrderItem;
import ru.yandex.market.api.user.order.bnpl.BnplInformation;
import ru.yandex.market.api.user.order.bnpl.PlanDetails;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.BnplPlanDetails;
import ru.yandex.market.checkout.checkouter.cart.BnplRegularPayment;
import ru.yandex.market.checkout.checkouter.cart.BnplVisualProperties;
import ru.yandex.market.checkout.checkouter.cart.MultiCartTotals;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPresentationFields;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.LiftingOptions;
import ru.yandex.market.checkout.checkouter.order.HelpingHand;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.loyalty.api.model.CashbackPermision;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SummariesTest {
    private static final BigDecimal FREE_DELIVERY_FROM_INF = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal FREE_DELIVERY_FROM_ONE_HUNDRED = BigDecimal.valueOf(100L);

    @Test
    public void zeroInitSummary() {
        List<CartItemV2> items = Collections.singletonList(item(null));

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.valueOf(10000L), null));
        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery(), SummaryMatcher.deliveryIsFree());
    }

    @Test
    public void ignoreOldOffer() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("12", null, null),
                        delivery(true, null)
                )),
                item(offer(
                        "offer-2",
                        price("32", null, null),
                        delivery(true, null)
                )),
                item(null)
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.ZERO, FREE_DELIVERY_FROM_INF, null));

        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(44)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery(), SummaryMatcher.deliveryIsFree());
    }

    @Test
    public void costWithDelivery() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("20.1", null, null),
                        delivery(false, "1")
                )),
                item(offer(
                        "offer-2",
                        price("10", null, null),
                        delivery(true, null)
                )),
                item(offer(
                        "offer-3",
                        price("30", null, null),
                        delivery(false, "8")
                ))
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.valueOf(1000), FREE_DELIVERY_FROM_INF, null));

        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(60.1)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery().getFreeThreshold(), is(FREE_DELIVERY_FROM_INF));
    }

    @Test
    public void costWithDiscount() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("4.9", "51", "10"),
                        delivery(true, null)
                )),
                item(offer(
                        "offer-2",
                        price("30", null, null),
                        delivery(true, null)
                )),
                item(offer(
                        "offer-3",
                        price("5.2", "74", "20.2"),
                        delivery(true, null)
                ))
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.ZERO, FREE_DELIVERY_FROM_INF, null));

        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(60.2)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(20.1)));
        assertThat(summary.getDelivery(), SummaryMatcher.deliveryIsFree());
    }


    @Test
    public void shouldEmptyToFreeDelvieryWhenDeliveryIsFree() {
        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        item(offer(
                                "offer",
                                price("20", null, null),
                                delivery(true, null)
                        ))
                ),
                new ThresholdResponse(BigDecimal.ZERO, FREE_DELIVERY_FROM_ONE_HUNDRED, null));
        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(20)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery().getFreeThreshold(), is(BigDecimal.valueOf(100)));
    }

    @Test
    public void fullAmountCostLessThanFreeDeliveryFrom() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("20", null, null),
                        delivery(false, "1")
                )),
                item(offer(
                        "offer-2",
                        price("30", null, null),
                        delivery(false, "2")
                ))
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.valueOf(50), FREE_DELIVERY_FROM_ONE_HUNDRED, DiscountType.THRESHOLD));

        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(50)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery().getFreeThreshold(), is(BigDecimal.valueOf(100)));
    }

    @Test
    public void fullAmountCostEqualFreeDeliveryFrom() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("20", null, null),
                        delivery(false, "90")
                )),
                item(offer(
                        "offer-2",
                        price("80", null, null),
                        delivery(false, "10")
                ))
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.valueOf(0), FREE_DELIVERY_FROM_ONE_HUNDRED, null));

        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(100)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery().getFreeThreshold(), is(BigDecimal.valueOf(100)));
    }


    @Test
    public void fullAmountCostMoreThanFreeDeliveryFrom() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("80", null, null),
                        delivery(false, "50")
                )),
                item(offer(
                        "offer-2",
                        price("40", null, null),
                        delivery(false, "120")
                ))
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.valueOf(0), FREE_DELIVERY_FROM_ONE_HUNDRED, null));
        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(120)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery().getFreeThreshold(), is(BigDecimal.valueOf(100)));
    }

    @Test
    public void summaryForCartWithRepeating() {
        List<CartItemV2> items = Arrays.asList(
                item(offer(
                        "offer-1",
                        price("40", null, null),
                        delivery(false, "1")
                ), 5),
                item(offer(
                        "offer-2",
                        price("20", null, null),
                        delivery(false, "2")
                ))
        );

        Summary summary = Summaries.calculateSummary(
                items,
                new ThresholdResponse(BigDecimal.valueOf(1000), FREE_DELIVERY_FROM_INF, null));

        assertThat(summary.getBaseAmount(), is(BigDecimal.valueOf(220)));
        assertThat(summary.getDiscountAmount(), is(BigDecimal.valueOf(0)));
        assertThat(summary.getDelivery().getFreeThreshold(), is(FREE_DELIVERY_FROM_INF));
    }

    @Test
    public void orderEmpty() {
        // вызов системы
        List<ShopOrderItem> items = Lists.newArrayList();
        List<DeliveryOption> deliveryOptions = Lists.newArrayList();

        Summary result = Summaries.calculateSummary(
                items,
                deliveryOptions,
                null,
                Collections.emptyList(),
                null,
                new ThresholdResponse(FREE_DELIVERY_FROM_INF, BigDecimal.ZERO, null),
                null,
                false,
                null,
                BigDecimal.ZERO,
                null,
                null,
                BigDecimal.ZERO
        );
        // проверка утверждений
        Assert.assertEquals(BigDecimal.valueOf(0), result.getBaseAmount());
        Assert.assertEquals(BigDecimal.valueOf(0), result.getTotalAmount());
        Assert.assertEquals(BigDecimal.valueOf(0), result.getDiscountAmount());
    }

    @Test
    public void orderWithSelectedDelivery() {
        // вызов системы
        List<ShopOrderItem> items = Lists.newArrayList();
        items.add(orderItem(11, 2, 5));
        items.add(orderItem(13, 3, 7));

        String selectedDeliveryOption = "123";
        List<DeliveryOption> deliveryOptions = Lists.newArrayList();
        deliveryOptions.add(serviceDeliveryOption(selectedDeliveryOption, 17));
        deliveryOptions.add(serviceDeliveryOption("456", 19));

        Summary result = Summaries.calculateSummary(
                items,
                deliveryOptions,
                new DeliveryPointId(selectedDeliveryOption),
                Collections.emptyList(),
                null,
                new ThresholdResponse(FREE_DELIVERY_FROM_INF, BigDecimal.valueOf(1000), null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO);
        // проверка утверждений
        Assert.assertEquals(BigDecimal.valueOf(11 * 5 + 13 * 7), result.getBaseAmount());
        Assert.assertEquals(BigDecimal.valueOf(2 * 5 + 3 * 7 + 17), result.getTotalAmount());
        Assert.assertEquals(BigDecimal.valueOf((11 - 2) * 5 + (13 - 3) * 7), result.getDiscountAmount());
    }

    @Test
    public void orderWithoutSelectedDelivery() {
        // вызов системы
        List<ShopOrderItem> items = Lists.newArrayList();
        items.add(orderItem(11, 2, 5));
        items.add(orderItem(13, 3, 7));

        List<DeliveryOption> deliveryOptions = Lists.newArrayList();
        deliveryOptions.add(serviceDeliveryOption("123", 17));

        Summary result = Summaries.calculateSummary(
                items,
                deliveryOptions,
                new DeliveryPointId("notExists"),
                Collections.emptyList(),
                null,
                new ThresholdResponse(BigDecimal.ZERO, FREE_DELIVERY_FROM_INF, null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO);
        // проверка утверждений
        Assert.assertEquals(BigDecimal.valueOf(11 * 5 + 13 * 7), result.getBaseAmount());
        Assert.assertEquals(BigDecimal.valueOf(2 * 5 + 3 * 7), result.getTotalAmount());
        Assert.assertEquals(BigDecimal.valueOf((11 - 2) * 5 + (13 - 3) * 7), result.getDiscountAmount());
    }

    @Test
    public void orderWithPromo() {
        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(10, 6, 1)
                ),
                Collections.emptyList(),
                new DeliveryPointId("notExists"),
                Collections.singletonList(
                        promoCode(3, PromoType.MARKET_COUPON)
                ),
                null,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(1), summary.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(3), summary.getPromoDiscount());
        assertEquals(BigDecimal.valueOf(3), summary.getPromoCodeDiscount());
    }

    @Test
    public void orderWithCoin() {
        OrderPromo promoWithCoin = promoCode(2, PromoType.MARKET_COIN);
        OrderPromo promo = promoCode(3, PromoType.MARKET_COUPON);

        String deliveryOptionId = "exist";

        Delivery delivery = new Delivery();
        delivery.setDeliveryOptionId(deliveryOptionId);

        ItemPromo itemPromo = new ItemPromo(
                PromoDefinition.marketCoinPromo("test", null, null, 12L, null, null),
                BigDecimal.valueOf(2),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        delivery.setPromos(Collections.singleton(itemPromo));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(15, 6, 1)
                ),
                Collections.emptyList(),
                new DeliveryPointId(deliveryOptionId, deliveryOptionId),
                Arrays.asList(promoWithCoin, promo),
                Collections.singletonList(delivery),
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.MARKET_COIN),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(4), summary.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(5), summary.getPromoDiscount());
        assertEquals(BigDecimal.valueOf(2), summary.getCoinDiscount());
        assertEquals(BigDecimal.valueOf(3), summary.getPromoCodeDiscount());

        assertThat(
                summary.getDelivery(),
                SummaryMatcher.deliveryIsFree(Matchers.is(DiscountType.MARKET_COIN))
        );
    }

    @Test
    public void orderWithBlueMarketAndCoinAndPromoCodeAndSecretSale() {
        OrderPromo coin = promoCode(5, PromoType.MARKET_COIN);
        OrderPromo promo = promoCode(6, PromoType.MARKET_COUPON);
        OrderPromo secretSale = promoCode(7, PromoType.SECRET_SALE);
        OrderPromo marketBlue = promoCode(8, PromoType.MARKET_BLUE);

        String deliveryOptionId = "exist";

        Delivery delivery = new Delivery();
        delivery.setDeliveryOptionId(deliveryOptionId);

        ItemPromo itemPromo = new ItemPromo(
                PromoDefinition.marketCoinPromo("test", null, null, 12L, null, null),
                BigDecimal.valueOf(2),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        delivery.setPromos(Collections.singleton(itemPromo));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(100, 74, 1)
                ),
                Collections.emptyList(),
                new DeliveryPointId(deliveryOptionId, deliveryOptionId),
                Arrays.asList(coin, promo, secretSale, marketBlue),
                Collections.singletonList(delivery),
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.MARKET_COIN),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(15), summary.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(11), summary.getPromoDiscount());
        assertEquals(BigDecimal.valueOf(5), summary.getCoinDiscount());
        assertEquals(BigDecimal.valueOf(6), summary.getPromoCodeDiscount());

        assertThat(
                summary.getDelivery(),
                SummaryMatcher.deliveryIsFree(Matchers.is(DiscountType.MARKET_COIN))
        );
    }

    @Test
    public void multiCartWithSpendCashbackOptionTotalAmount() {
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(BigDecimal.valueOf(12345));
        Cashback applicableCashbackOptions = new Cashback(
                null,
                new CashbackOptions(
                        "promoKey",
                        1,
                        BigDecimal.valueOf(10000),
                        null,
                        Collections.emptyList(),
                        CashbackPermision.ALLOWED,
                        null,
                        null,
                        null,
                        null
                )

        );

        Summary summary = Summaries.calculateMultiCartSummary(
                Collections.emptyList(),
                multiCartTotals,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.YANDEX_PLUS),
                null,
                null,
                BigDecimal.TEN,
                CashbackOption.SPEND,
                applicableCashbackOptions,
                null,
                null,
                null,
                false);

        assertEquals(BigDecimal.valueOf(12345), summary.getTotalAmount());
        assertEquals(BigDecimal.valueOf(2345), summary.getTotalMoneyAmount());
    }

    @Test
    public void multiCartWithLiftingOptions() {
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(BigDecimal.valueOf(12345));
        Cashback applicableCashbackOptions = new Cashback(
                null,
                new CashbackOptions(
                        "promoKey",
                        1,
                        BigDecimal.valueOf(10000),
                        null,
                        Collections.emptyList(),
                        CashbackPermision.ALLOWED,
                        null,
                        null,
                        null,
                        null
                )

        );

        Summary summary = Summaries.calculateMultiCartSummary(
                Arrays.asList(
                        shopOptionWithLifting(1, "1", 100, LiftType.ELEVATOR),
                        shopOptionWithLifting(2, "2", -1, LiftType.UNKNOWN),
                        shopOption(4, "4", 100),
                        shopOptionWithLifting(3, "3", 0, LiftType.CARGO_ELEVATOR)
                ),
                multiCartTotals,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.YANDEX_PLUS),
                null,
                null,
                BigDecimal.TEN,
                CashbackOption.SPEND,
                applicableCashbackOptions,
                null,
                null,
                null,
                false);

        List<DeliveryInfo.Lifting> liftings = summary.getDelivery().getLiftingServices();

        assertEquals(3, liftings.size());

        assertEquals(1, liftings.get(0).getShopId());
        assertEquals(BigDecimal.valueOf(100), liftings.get(0).getPrice());
        assertEquals(LiftType.ELEVATOR, liftings.get(0).getLiftType());

        assertEquals(2, liftings.get(1).getShopId());
        assertNull(liftings.get(1).getPrice());
        assertEquals(LiftType.UNKNOWN, liftings.get(1).getLiftType());

        assertEquals(3, liftings.get(2).getShopId());
        assertEquals(BigDecimal.valueOf(0), liftings.get(2).getPrice());
        assertEquals(LiftType.CARGO_ELEVATOR, liftings.get(2).getLiftType());
    }

    @Test
    public void ensureFirstServiceOption() {
        List<DeliveryOption> deliveryOptions = Lists.newArrayList();
        deliveryOptions.add(postDeliveryOption("345", 125));
        deliveryOptions.add(outletDeliveryOption("789", 345));
        deliveryOptions.add(serviceDeliveryOption("123", 17));
        deliveryOptions.add(serviceDeliveryOption("012", 18));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(15, 6, 1)
                ),
                deliveryOptions,
                null,
                Collections.emptyList(),
                null,
                new ThresholdResponse(BigDecimal.TEN, BigDecimal.valueOf(10000L), null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(17), summary.getDelivery().getPrice());
    }

    @Test
    public void ensureFirstOutletOption() {
        List<DeliveryOption> deliveryOptions = Lists.newArrayList();
        deliveryOptions.add(postDeliveryOption("345", 125));
        deliveryOptions.add(outletDeliveryOption("789", 345));
        deliveryOptions.add(outletDeliveryOption("012", 11));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(15, 6, 1)
                ),
                deliveryOptions,
                null,
                Collections.emptyList(),
                null,
                new ThresholdResponse(BigDecimal.TEN, BigDecimal.valueOf(10000L), null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(345), summary.getDelivery().getPrice());
    }

    @Test
    public void ensureFirstPostOption() {
        List<DeliveryOption> deliveryOptions = Lists.newArrayList();
        deliveryOptions.add(postDeliveryOption("345", 125));
        deliveryOptions.add(postDeliveryOption("789", 345));
        deliveryOptions.add(postDeliveryOption("012", 11));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(15, 6, 1)
                ),
                deliveryOptions,
                null,
                Collections.emptyList(),
                null,
                new ThresholdResponse(BigDecimal.TEN, BigDecimal.valueOf(10000L), null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(125), summary.getDelivery().getPrice());
    }

    @Test
    public void ensureCheapestOption() {
        List<DeliveryOption> deliveryOptions = Lists.newArrayList();
        deliveryOptions.add(serviceDeliveryOption("123", 17));
        deliveryOptions.add(postDeliveryOption("345", 125));
        deliveryOptions.add(serviceDeliveryOption("012", 10));
        deliveryOptions.add(outletDeliveryOption("789", 345));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(15, 6, 1)
                ),
                deliveryOptions,
                null,
                Collections.emptyList(),
                null,
                new ThresholdResponse(BigDecimal.TEN, BigDecimal.valueOf(10000L), null),
                null,
                true,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertEquals(BigDecimal.valueOf(10), summary.getDelivery().getPrice());
    }

    @Test
    public void checkCheapestAsGiftPromoDiscountReturnedAsDiscountAmount() {
        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(10, 5, 1)
                ),
                Collections.emptyList(),
                new DeliveryPointId("notExists"),
                Collections.singletonList(
                        promoCode(5, PromoType.CHEAPEST_AS_GIFT)
                ),
                null,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertThat(summary.getDiscountAmount(), equalTo(BigDecimal.valueOf(5)));
        assertThat(summary.getPromoDiscount(), equalTo(BigDecimal.valueOf(0)));
    }

    @Test
    public void checkBlueSetPromoDiscountReturnedAsDiscountAmount() {
        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(10, 5, 1)
                ),
                Collections.emptyList(),
                new DeliveryPointId("notExists"),
                Collections.singletonList(
                        promoCode(5, PromoType.BLUE_SET)
                ),
                null,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                null,
                BigDecimal.ZERO
        );

        assertThat(summary.getDiscountAmount(), equalTo(BigDecimal.valueOf(5)));
        assertThat(summary.getPromoDiscount(), equalTo(BigDecimal.valueOf(0)));
    }

    @Test
    public void multiCartWithHelpIsNearInfo() {
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(BigDecimal.valueOf(98765));
        HelpingHand helpingHand = HelpingHand.enabled(1234);

        Summary summary = Summaries.calculateMultiCartSummary(
                Collections.emptyList(),
                multiCartTotals,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.YANDEX_PLUS),
                null,
                null,
                BigDecimal.TEN,
                CashbackOption.SPEND,
                null,
                helpingHand,
                null,
                null,
                false);

        assertEquals(helpingHand.getStatus().name(), summary.getYandexHelpInfo().getYandexHelpStatus());
        assertEquals(helpingHand.getDonationAmount(), summary.getYandexHelpInfo().getYandexHelpDonationAmount());
    }

    @Test
    public void bnplSummaryTest() {
        Instant instant = Instant.now();
        BnplPlanDetails bnplPlanDetails1 = createBnplPlanDetails("split_2_month", instant, "base_split");
        BnplPlanDetails bnplPlanDetails2 = createBnplPlanDetails("split_4_month", instant,"long_split");
        BnplPlanDetails bnplPlanDetails3 = createBnplPlanDetails("split_6_month", instant,"long_split");

        BnplInfo bnplInfo = new BnplInfo();
        bnplInfo.setSelected(true);
        bnplInfo.setAvailable(false);
        bnplInfo.setBnplPlanDetails(bnplPlanDetails1);
        bnplInfo.setSelectedPlan("split_4_month");
        bnplInfo.setPlans(Arrays.asList(bnplPlanDetails1, bnplPlanDetails2, bnplPlanDetails3));

        Summary summary = Summaries.calculateSummary(
                Collections.singletonList(
                        orderItem(15, 6, 1)
                ),
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                null,
                new ThresholdResponse(BigDecimal.TEN, BigDecimal.valueOf(10000L), null),
                null,
                false,
                null,
                BigDecimal.TEN,
                null,
                new BnplInformation(bnplInfo),
                BigDecimal.ZERO
        );

        assertTrue(summary.getBnplInformation().isSelected());
        assertFalse(summary.getBnplInformation().isAvailable());
        assertThat(summary.getBnplInformation().getSelectedPlan(), equalTo("split_4_month"));
        PlanDetails planDetails = summary.getBnplInformation().getDetails();
        assertNotNull(planDetails);
        List<PlanDetails> plans = new ArrayList<>(summary.getBnplInformation().getPlans());
        assertThat(plans.get(0), equalTo(planDetails));
        assertThat(plans.get(1).getConstructor(), equalTo("split_4_month"));
        assertThat(plans.get(1).getDetailsUrl(), equalTo("https://split.yandex.ru/#longsplit"));
        assertThat(plans.get(2).getConstructor(), equalTo("split_6_month"));
        assertThat(plans.get(2).getDetailsUrl(), equalTo("https://split.yandex.ru/#longsplit"));
        assertThat(planDetails.getDeposit(), equalTo(BigDecimal.ONE));
        assertThat(planDetails.getPayments().size(), equalTo(1));
        assertThat(planDetails.getFee(), equalTo(BigDecimal.TEN));
        assertThat(planDetails.getType(), equalTo("base_split"));
        assertThat(planDetails.getConstructor(), equalTo("split_2_month"));
        assertThat(planDetails.getDetailsUrl(), equalTo("https://split.yandex.ru/market"));
        planDetails.getPayments().forEach(regularPayment -> {
            assertThat(regularPayment.getAmount(), equalTo(new BigDecimal(42)));
            assertThat(regularPayment.getDateTime(), equalTo(instant));
        });
        assertNotNull(planDetails.getVisualProperties());
        assertThat(planDetails.getVisualProperties().getShortTitle(), equalTo("сплит на 4 месяца"));
        assertThat(planDetails.getVisualProperties().getNextDatesDescription(), equalTo("далее 3 платежа"));
        assertThat(planDetails.getVisualProperties().getNextPaymentsDescription(), equalTo("по 1000 рублей"));
        assertThat(planDetails.getVisualProperties().getColors().size(), equalTo(1));
        assertThat(planDetails.getVisualProperties().getColors().get("canceled"), equalTo("#E1E3E8"));
    }

    private BnplPlanDetails createBnplPlanDetails(String constructor, Instant instant, String type) {
        BnplRegularPayment bnplRegularPayment = new BnplRegularPayment();
        bnplRegularPayment.setAmount(BigDecimal.valueOf(42L));
        bnplRegularPayment.setDatetime(instant);
        BnplPlanDetails bnplPlanDetails = new BnplPlanDetails();
        bnplPlanDetails.setDeposit(BigDecimal.ONE);
        bnplPlanDetails.setPayments(Collections.singletonList(bnplRegularPayment));
        bnplPlanDetails.setFee(BigDecimal.TEN);
        bnplPlanDetails.setType(type);
        bnplPlanDetails.setConstructor(constructor);
        BnplVisualProperties visualProperties = new BnplVisualProperties();
        visualProperties.setShortTitle("сплит на 4 месяца");
        visualProperties.setNextDatesDescription("далее 3 платежа");
        visualProperties.setNextPaymentsDescription("по 1000 рублей");
        HashMap<String, String> colors = new HashMap<>();
        colors.put("canceled", "#E1E3E8");
        visualProperties.setColors(colors);
        bnplPlanDetails.setVisualProperties(visualProperties);
        return bnplPlanDetails;
    }

    @Test
    public void multiCartWithBnplInfo() {
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(BigDecimal.valueOf(98765));
        HelpingHand helpingHand = HelpingHand.enabled(1234);

        BnplRegularPayment bnplRegularPayment = new BnplRegularPayment();
        bnplRegularPayment.setAmount(BigDecimal.valueOf(42L));
        bnplRegularPayment.setDatetime(Instant.now());
        BnplPlanDetails bnplPlanDetails = new BnplPlanDetails();
        bnplPlanDetails.setDeposit(BigDecimal.ONE);
        bnplPlanDetails.setPayments(Collections.singletonList(bnplRegularPayment));

        BnplInfo bnplInfo = new BnplInfo();
        bnplInfo.setSelected(false);
        bnplInfo.setAvailable(true);
        bnplInfo.setBnplPlanDetails(bnplPlanDetails);

        Summary summary = Summaries.calculateMultiCartSummary(
                Collections.emptyList(),
                multiCartTotals,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.YANDEX_PLUS),
                null,
                null,
                BigDecimal.TEN,
                CashbackOption.SPEND,
                null,
                helpingHand,
                new BnplInformation(bnplInfo),
                null,
                false);

        assertFalse(summary.getBnplInformation().isSelected());
        assertTrue(summary.getBnplInformation().isAvailable());
        PlanDetails planDetails = summary.getBnplInformation().getDetails();
        assertNotNull(planDetails);
        assertThat(planDetails.getDeposit(), equalTo(bnplPlanDetails.getDeposit()));
        assertThat(planDetails.getPayments().size(), equalTo(bnplPlanDetails.getPayments().size()));
        planDetails.getPayments().forEach(regularPayment -> {
            assertThat(regularPayment.getAmount(), equalTo(bnplRegularPayment.getAmount()));
            assertThat(regularPayment.getDateTime(), equalTo(bnplRegularPayment.getDatetime()));
        });
    }

    @Test
    public void multiCartSummaryWithServiceTest() {
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(BigDecimal.valueOf(12345));
        multiCartTotals.setServicesTotal(BigDecimal.TEN);

        Summary summary = Summaries.calculateMultiCartSummary(
                Arrays.asList(
                        shopOptionWithService(1, 5),
                        shopOptionWithService(2, 4),
                        shopOption(4, "4", 100)
                ),
                multiCartTotals,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.YANDEX_PLUS),
                null,
                null,
                BigDecimal.TEN,
                CashbackOption.SPEND,
                null,
                null,
                null,
                null,
                false
        );

        assertNotNull(summary.getServiceInfo());
        assertEquals(2, summary.getServiceInfo().getTotalCount());
        assertEquals(BigDecimal.TEN, summary.getServiceInfo().getTotalPrice());
    }

    @Test
    public void multiCartSummaryWithoutServiceTest() {
        MultiCartTotals multiCartTotals = new MultiCartTotals();
        multiCartTotals.setBuyerTotal(BigDecimal.valueOf(12345));
        multiCartTotals.setServicesTotal(BigDecimal.TEN);

        Summary summary = Summaries.calculateMultiCartSummary(
                Arrays.asList(
                        shopOption(1, "5", 200),
                        shopOption(4, "4", 100)
                ),
                multiCartTotals,
                new ThresholdResponse(BigDecimal.ZERO, BigDecimal.ZERO, DiscountType.YANDEX_PLUS),
                null,
                null,
                BigDecimal.TEN,
                CashbackOption.SPEND,
                null,
                null,
                null,
                null,
                false
        );

        assertNull(summary.getServiceInfo());
    }

    private static OrderPromo promoCode(int discount, PromoType promoType) {
        OrderPromo promo = new OrderPromo();
        promo.setType(promoType);
        promo.setBuyerItemsDiscount(new BigDecimal(discount));
        return promo;
    }

    private static OfferPriceV2 price(String value,
                                      String discount,
                                      String base) {
        return new OfferPriceV2(value, discount, base);
    }

    private static DeliveryV2 delivery(boolean isFree,
                                       String value) {
        DeliveryV2 delivery = DeliveryV2.withExplicitDefaults();
        delivery.setFree(isFree);
        delivery.setPrice(price(value, null, null));
        return delivery;
    }

    private static OfferV2 offer(String id,
                                 OfferPriceV2 offerPrice,
                                 DeliveryV2 delivery) {
        OfferV2 offer = new OfferV2();
        offer.setId(new OfferId(id, ""));
        offer.setPrice(offerPrice);
        offer.setDelivery(delivery);
        return offer;
    }

    private static ShopOrderItem orderItem(int basePrice, int price, int count) {
        ShopOrderItem result = new ShopOrderItem();
        result.setBasePrice(BigDecimal.valueOf(basePrice));
        result.setPrice(BigDecimal.valueOf(price));
        result.setCount(count);
        return result;
    }

    private static DeliveryOption serviceDeliveryOption(String id, int price) {
        DeliveryOption result = new ServiceDeliveryOption();
        result.setId(new DeliveryPointId(id));
        result.setPrice(BigDecimal.valueOf(price));
        return result;
    }

    private static DeliveryOption serviceDeliveryOptionWithLifting(String id, int price, int liftingPrice, LiftType liftType) {
        ServiceDeliveryOption result = new ServiceDeliveryOption();
        result.setId(new DeliveryPointId(id));
        result.setPrice(BigDecimal.valueOf(price));

        if (liftingPrice >= 0) {
            result.setLiftPrice(BigDecimal.valueOf(liftingPrice));
        }
        if (liftType != null) {
            result.setLiftType(liftType);
        }

        DeliveryPresentationFields fields = new DeliveryPresentationFields();
        LiftingOptions liftingOptions = new LiftingOptions();
        fields.setLiftingOptions(liftingOptions);

        result.setDeliveryPresentationFields(fields);

        return result;
    }

    private static DeliveryOption outletDeliveryOption(String id, int price) {
        DeliveryOption result = new OutletDeliveryOption();
        result.setId(new DeliveryPointId(id));
        result.setPrice(BigDecimal.valueOf(price));
        return result;
    }

    private static DeliveryOption postDeliveryOption(String id, int price) {
        DeliveryOption result = new PostDeliveryOption();
        result.setId(new DeliveryPointId(id));
        result.setPrice(BigDecimal.valueOf(price));
        return result;
    }

    private static CartItemV2 item(OfferV2 offer) {
        return item(offer, 1);
    }

    private static CartItemV2 item(OfferV2 offer, int count) {
        CartItemV2 item = new CartItemV2();
        item.setOffer(offer);
        item.setCount(count);
        return item;
    }

    private static OrderOptionsResponse.ShopOptions shopOptionWithService(long shopId, int price) {
        OrderOptionsResponse.ShopOptions shop = new OrderOptionsResponse.ShopOptions();
        shop.setShopId(shopId);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setTotalPrice(BigDecimal.valueOf(price));
        serviceInfo.setTotalCount(1);

        Summary summary = new Summary();
        summary.setBaseAmount(BigDecimal.valueOf(shopId).multiply(BigDecimal.TEN));
        summary.setDiscountAmount(BigDecimal.ZERO);
        summary.setPromoDiscount(BigDecimal.ZERO);
        summary.setPromoCodeDiscount(BigDecimal.ZERO);
        summary.setCoinDiscount(BigDecimal.ZERO);
        summary.setServiceInfo(serviceInfo);

        shop.setSummary(summary);

        return shop;
    }

    private static OrderOptionsResponse.ShopOptions shopOptionWithLifting(long shopId,
                                                                          String deliveryId,
                                                                          int price,
                                                                          LiftType liftType) {
        OrderOptionsResponse.ShopOptions shop = new OrderOptionsResponse.ShopOptions();
        shop.setShopId(shopId);
        shop.setDeliveryOptions(serviceDeliveryOptionWithLifting(deliveryId, price, price, liftType));

        Summary summary = new Summary();
        summary.setBaseAmount(BigDecimal.valueOf(shopId).multiply(BigDecimal.TEN));
        summary.setDiscountAmount(BigDecimal.ZERO);
        summary.setPromoDiscount(BigDecimal.ZERO);
        summary.setPromoCodeDiscount(BigDecimal.ZERO);
        summary.setCoinDiscount(BigDecimal.ZERO);

        shop.setSummary(summary);

        return shop;
    }

    private static OrderOptionsResponse.ShopOptions shopOption(long shopId, String deliveryId, int price) {
        OrderOptionsResponse.ShopOptions shop = new OrderOptionsResponse.ShopOptions();
        shop.setShopId(shopId);
        shop.setDeliveryOptions(serviceDeliveryOption(deliveryId, price));

        Summary summary = new Summary();
        summary.setBaseAmount(BigDecimal.valueOf(shopId).multiply(BigDecimal.TEN));
        summary.setDiscountAmount(BigDecimal.ZERO);
        summary.setPromoDiscount(BigDecimal.ZERO);
        summary.setPromoCodeDiscount(BigDecimal.ZERO);
        summary.setCoinDiscount(BigDecimal.ZERO);

        shop.setSummary(summary);

        return shop;
    }
}
