package ru.yandex.market.ocrm.module.loyalty.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.loyalty.api.model.CouponValueType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.coin.OCRMUserCoinResponse;
import ru.yandex.market.loyalty.api.model.ocrm.Coupon;
import ru.yandex.market.loyalty.api.model.ocrm.CouponRestrictions;
import ru.yandex.market.loyalty.api.model.ocrm.OrderCoins;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyUsedCoupon;
import ru.yandex.market.ocrm.module.loyalty.MarketLoyaltyService;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
@SpringJUnitConfig(LoyaltyUsedCouponTest.Configuration.class)
public class LoyaltyUsedCouponTest {

    @Inject
    private MarketLoyaltyService marketLoyaltyService;
    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private OrderTestUtils orderTestUtils;

    @Test
    public void usedCouponExists() {
        Order order = orderTestUtils.createOrder();
        final BigDecimal discount = BigDecimal.valueOf(11);
        final BigDecimal nominal = BigDecimal.valueOf(123L);
        final CouponValueType couponValueType = CouponValueType.FIXED;
        final OffsetDateTime startDate = OffsetDateTime.of(
                LocalDate.of(2020, 9, 1),
                LocalTime.of(12, 01, 0),
                ZoneOffset.UTC);
        final OffsetDateTime dueDate = OffsetDateTime.of(
                LocalDate.of(2020, 10, 1),
                LocalTime.of(12, 01, 0),
                ZoneOffset.UTC);
        Coupon coupon = createCoupon(
                discount,
                nominal,
                couponValueType,
                startDate,
                dueDate);
        setupLoyaltyClient(
                Collections.emptyList(),
                Collections.emptyList(),
                coupon,
                order);

        List<LoyaltyUsedCoupon> actualUsedCoupons = getUsedCoupons(order.getOrderId());

        Assertions.assertEquals(1, actualUsedCoupons.size());
        final LoyaltyUsedCoupon actualUsedCoupon = actualUsedCoupons.get(0);
        Assertions.assertEquals(couponValueType.name(), actualUsedCoupon.getCouponValueType().getCode());
        Assertions.assertEquals(nominal, actualUsedCoupon.getNominal());
        Assertions.assertEquals(discount, actualUsedCoupon.getDiscount());
        Assertions.assertEquals(
                startDate.toInstant(),
                actualUsedCoupon.getStartDate().toInstant());
        Assertions.assertEquals(
                dueDate.toInstant(),
                actualUsedCoupon.getDueDate().toInstant());
        Assertions.assertTrue(actualUsedCoupon.getRestrictions().contains("Минимальная сумма заказа:"));
    }

    @Test
    public void thereIsNoUsedCoupon() {
        Order order = orderTestUtils.createOrder();
        setupLoyaltyClient(
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                order);

        List<LoyaltyUsedCoupon> coupons = getUsedCoupons(order.getOrderId());

        Assertions.assertTrue(coupons.isEmpty());
    }

    private List<LoyaltyUsedCoupon> getUsedCoupons(Long orderId) {
        Query query = Query.of(LoyaltyUsedCoupon.FQN)
                .withFilters(Filters.eq(LoyaltyUsedCoupon.ORDERS, orderId));
        return entityStorageService.list(query);
    }

    private void setupLoyaltyClient(List<OCRMUserCoinResponse> issuedCoin,
                                    List<OCRMUserCoinResponse> usedCoin,
                                    Coupon usedCoupon,
                                    Order order) {
        OrderCoins result = new OrderCoins(issuedCoin, usedCoin, usedCoupon);
        Mockito.when(marketLoyaltyService.getIssuedAndUsedCoinsForOrder(Mockito.eq(order.getOrderId()))).thenReturn(result);
    }

    private Coupon createCoupon(BigDecimal discount,
                                BigDecimal nominal,
                                CouponValueType couponValueType,
                                OffsetDateTime startDate,
                                OffsetDateTime dueDate) {
        return new Coupon("couponCode",
                discount,
                nominal,
                couponValueType,
                Date.from(
                        startDate.toInstant()),
                Date.from(
                        dueDate.toInstant()),
                new CouponRestrictions(
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(1000),
                        true,
                        Set.of(UsageClientDeviceType.DESKTOP),
                        "some coupon description"
                ),
                Collections.emptyList()
        );
    }

    @Import(ModuleLoyaltyTestConfiguration.class)
    @org.springframework.context.annotation.Configuration
    public static class Configuration {

    }
}
