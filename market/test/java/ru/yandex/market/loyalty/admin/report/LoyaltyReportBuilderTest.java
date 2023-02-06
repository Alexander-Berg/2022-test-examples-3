package ru.yandex.market.loyalty.admin.report;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.loyalty.admin.test.LoyaltyAdminReportTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor;
import ru.yandex.market.loyalty.api.model.CouponCreationRequest;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.core.model.coupon.Coupon;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coupon.CouponService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 14.06.17
 */
@TestFor(CheckouterEventRestProcessor.class)
public class LoyaltyReportBuilderTest extends LoyaltyAdminReportTest {
    @Autowired
    private LoyaltyReportBuilder loyaltyReportBuilder;
    @Autowired
    private PromoService promoService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private DiscountUtils discountUtils;


    @Test
    public void spendingHistoryReportEmpty() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        CouponCreationRequest request = CouponCreationRequest.builder("coupon-key", promo.getId())
                .identity(new Uid(2L))
                .build();
        Coupon coupon = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload());

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(promo.getId(), coupon.getCode(),
                null, null);
        assertTrue(report.isEmpty());
    }

    @Test
    public void testSpendingHistoryReportWithoutCouponCode() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.SINGLE_USE_COUPON_RULE);

        Promo promo = promoService.getPromo(data.promoId);
        CouponCreationRequest request = CouponCreationRequest.builder("coupon-key-2", promo.getId())
                .identity(new Uid(2L))
                .build();
        String couponCode2 = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload()).getCode();
        couponService.activateCouponsFromInactive(Collections.singletonMap(couponCode2, "none"));
        discountService.spendDiscount(DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem().build()
                ).withCoupon(couponCode2).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, null, null, null);

        assertThat(report, containsInAnyOrder(
                allOf(
                        hasProperty("couponCode", equalTo(couponCode2)),
                        hasProperty("shopUrl", nullValue())
                ),
                allOf(
                        hasProperty("couponCode", equalTo(data.couponCode)),
                        hasProperty("shopUrl", equalTo(SHOP_URL))
                )
        ));
    }

    @Test
    public void testSpendingHistoryReportWithCouponCode() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.SINGLE_USE_COUPON_RULE);

        Promo promo = promoService.getPromo(data.promoId);
        CouponCreationRequest request = CouponCreationRequest.builder("coupon-key-2", promo.getId())
                .identity(new Uid(2L))
                .build();
        String couponCode2 = couponService.createOrGetCoupon(request, discountUtils.getRulesPayload()).getCode();
        couponService.activateCouponsFromInactive(Collections.singletonMap(couponCode2, "none"));
        discountService.spendDiscount(DiscountRequestWithBundlesBuilder.builder(
                orderRequestWithBundlesBuilder().withOrderItem().build()
                ).withCoupon(couponCode2).build(),
                configurationService.currentPromoApplicabilityPolicy(),
                null
        );

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, data.couponCode,
                null, null);

        assertEquals(1, report.size());
        assertEquals(data.couponCode, report.get(0).getCouponCode());
        assertEquals(SHOP_URL, report.get(0).getShopUrl());
        assertNotNull(report.get(0).getOrderId());
    }

    @Test
    public void testSpendingHistoryReportAfterRevertOnSingleUseCouponRule() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.SINGLE_USE_COUPON_RULE);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, data.order);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, data.couponCode,
                null, null);

        assertEquals(1, report.size());
        assertThat(report.get(0).getDiscountTotal(), comparesEqualTo(BigDecimal.ZERO));
        assertNotNull(report.get(0).getRevertTime());
        assertNull(report.get(0).getCommitTime());
    }

    @Test
    public void testSpendingHistoryReportAfterRevertOnInfinityUseCouponRule() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.INFINITY_USE_COUPON_RULE);

        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED, data.order);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, data.couponCode,
                null, null);

        assertEquals(1, report.size());
        assertThat(report.get(0).getDiscountTotal(), comparesEqualTo(BigDecimal.ZERO));
        assertNotNull(report.get(0).getRevertTime());
        assertNull(report.get(0).getCommitTime());
    }

    @Test
    public void testSpendingHistoryReportAfterOrderDeliver() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.SINGLE_USE_COUPON_RULE);

        processEvent(OrderStatus.DELIVERY, HistoryEventType.NEW_SUBSIDY, data.order.getId());

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, data.couponCode,
                null, null);

        assertEquals(1, report.size());
        assertThat(report.get(0).getDiscountTotal(), comparesEqualTo(BigDecimal.valueOf(300)));
        assertNull(report.get(0).getRevertTime());
        assertNotNull(report.get(0).getCommitTime());
    }

    @Test
    public void testSpendingHistoryReportIfOrderCanceledAfterDeliverOnSingleUseCouponRule() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.SINGLE_USE_COUPON_RULE);

        processEvent(OrderStatus.DELIVERY, HistoryEventType.NEW_SUBSIDY,
                data.order);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED,
                data.order);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.SUBSIDY_REFUND,
                data.order);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, data.couponCode,
                null, null);

        assertEquals(1, report.size());
        assertThat(report.get(0).getDiscountTotal(), comparesEqualTo(BigDecimal.ZERO));
        assertNotNull(report.get(0).getRevertTime());
        assertNotNull(report.get(0).getCommitTime());
    }

    @Test
    public void testSpendingHistoryReportIfOrderCanceledAfterDeliverOnInfinityUseCouponRule() throws Exception {
        TestReportData data = prepareCouponDataForReport(RuleType.INFINITY_USE_COUPON_RULE);

        processEvent(OrderStatus.DELIVERY, HistoryEventType.NEW_SUBSIDY,
                data.order);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.ORDER_STATUS_UPDATED,
                data.order);
        processEvent(OrderStatus.CANCELLED, HistoryEventType.SUBSIDY_REFUND,
                data.order);
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);

        List<LoyaltyReportRow> report = loyaltyReportBuilder.spendingHistoryReport(data.promoId, data.couponCode,
                null, null);

        assertEquals(1, report.size());
        assertThat(report.get(0).getDiscountTotal(), comparesEqualTo(BigDecimal.ZERO));
        assertNotNull(report.get(0).getRevertTime());
        assertNotNull(report.get(0).getCommitTime());
    }
}
