package ru.yandex.market.loyalty.admin.test;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.loyalty.admin.report.TestReportData;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesRequest;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.ANOTHER_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;

/**
 * @author dinyat
 * 25/08/2017
 */
public abstract class LoyaltyAdminReportTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {
    private static final BigDecimal SUBSIDY = BigDecimal.valueOf(50);
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private DiscountUtils discountUtils;


    protected static final String SHOP_URL = "http://shop-url";

    protected TestReportData prepareCouponDataForReport(RuleType<?> couponRule) {
        long orderId = 123144231L;
        Order order = order(orderId);

        when(mbiApiClient.getShop(anyLong())).thenReturn(shop());
        when(checkouterClient.getOrders(any(), any())).thenReturn(paged(order));

        String couponKey = "coupon-key";
        Promo promo;
        if (RuleType.SINGLE_USE_COUPON_RULE == couponRule) {
            promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        } else if (RuleType.INFINITY_USE_COUPON_RULE == couponRule) {
            promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultInfiniteUse());
        } else {
            throw new UnsupportedOperationException();
        }

        Coupon coupon;
        if (RuleType.SINGLE_USE_COUPON_RULE == couponRule) {
            CouponCreationRequest request = CouponCreationRequest.builder(couponKey, promo.getId())
                    .identity(new Uid(1L))
                    .build();
            coupon = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());
            couponService.activateCouponsFromInactive(Collections.singletonMap(coupon.getCode(), "none"));
        } else {
            coupon = couponService.getCouponByPromo(promo);
        }

        OrderWithBundlesRequest orderRequest = orderRequestWithBundlesBuilder()
                .withOrderItem()
                .withOrderId(String.valueOf(orderId))
                .build();
        discountService.spendDiscount(
                DiscountRequestWithBundlesBuilder.builder(orderRequest).withCoupon(coupon.getCode()).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        return new TestReportData(promo.getId(), coupon.getCode(), order);
    }

    private static Shop shop() {
        return new Shop(0L, SHOP_URL, null, null, null, null, null, null, false, false, null, null, null, false);
    }

    private static Order order(long id) {
        OrderItem orderItem1 = CheckouterUtils.defaultOrderItem()
                .setDiscount(SUBSIDY.multiply(CheckouterUtils.DEFAULT_ITEMS_COUNT))
                .setItemKey(DEFAULT_ITEM_KEY)
                .build();
        OrderItem orderItem2 = CheckouterUtils.defaultOrderItem()
                .setItemKey(ANOTHER_ITEM_KEY)
                .setDiscount(SUBSIDY.multiply(CheckouterUtils.DEFAULT_ITEMS_COUNT))
                .build();
        return CheckouterUtils.defaultOrder(OrderStatus.PENDING)
                .setOrderId(id)
                .addItems(Arrays.asList(orderItem1, orderItem2))
                .build();
    }

    private static PagedOrders paged(Order... orders) {
        PagedOrders pagedOrders = new PagedOrders();
        pagedOrders.setItems(Arrays.asList(orders));
        return pagedOrders;
    }
}
