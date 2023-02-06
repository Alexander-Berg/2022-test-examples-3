package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.DiscountHistoryRecordType;
import ru.yandex.market.loyalty.api.model.discount.OrderWithDeliveriesRequest;
import ru.yandex.market.loyalty.back.controller.discount.DiscountControllerGenerationSupportedTest;
import ru.yandex.market.loyalty.core.dao.coupon.CouponHistoryDao;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coupon.CouponHistoryRecord;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.math.BigDecimal;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.generateWith;
import static ru.yandex.market.loyalty.core.utils.BuildCustomizer.Util.same;
import static ru.yandex.market.loyalty.core.utils.DiscountRequestBuilder.builder;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContext;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_QUANTITY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.OrderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.keyOf;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderItemBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.quantity;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_COUPON_VALUE;
import static ru.yandex.market.loyalty.core.utils.SequenceCustomizer.compose;

/**
 * @author ukchuvrus
 */
public class ConcurrentDiscountControllerTest extends DiscountControllerGenerationSupportedTest {
    private static final int ITEMS_IN_ORDER = 3;
    private static final BigDecimal REQUESTED_SUBSIDY = DEFAULT_COUPON_VALUE.multiply(BigDecimal.valueOf(CPU_COUNT));
    private static final BigDecimal BUDGET = REQUESTED_SUBSIDY.multiply(BigDecimal.valueOf(0.5));// размер
    // достаточный на половину
    private static final BigDecimal EMISSION_BUDGET =
            BigDecimal.valueOf(CPU_COUNT).multiply(BigDecimal.valueOf(0.75)); // размер достаточный на 3/4

    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private CouponHistoryDao couponHistoryDao;

    @Test
    @Repeat(5)
    public void testSpendBudgetConcurrent() throws Exception {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setBudget(BUDGET)
                .setPlatform(CoreMarketPlatform.BLUE)
                .setEmissionBudget(EMISSION_BUDGET)
        );

        assertEquals(0, couponHistoryDao.getAllRecords().size());

        testConcurrency(() ->
                () -> {
                    CouponDto coupon;
                    try {
                        coupon = createActivatedCoupon(promo.getId());
                    } catch (MarketLoyaltyException ignored) {
                        //часть запросов отдает бросает бизнесовые исключения
                        return;
                    }
                    OrderRequestBuilder orderRequestBuilder = orderRequestBuilder();
                    generateWith(same(orderItemBuilder(
                            quantity(DEFAULT_QUANTITY),
                            price(BigDecimal.valueOf(1000))
                    )), ITEMS_IN_ORDER, compose(keyOf(), OrderRequestUtils::itemKey))
                            .forEach(orderRequestBuilder::withOrderItem);
                    OrderWithDeliveriesRequest order = orderRequestBuilder.build();
                    marketLoyaltyClient.spendDiscount(
                            builder(order).withCoupon(coupon.getCode()).withOperationContext(uidOperationContext()).build());
                }
        );

        long creationCount = couponHistoryDao.getAllRecords()
                .stream()
                .map(CouponHistoryRecord::getRecordType)
                .filter(Predicate.isEqual(DiscountHistoryRecordType.CREATION))
                .count();
        assertEquals(CPU_COUNT * 3 / 4, creationCount);

        long usageCount = couponHistoryDao.getAllRecords()
                .stream()
                .map(CouponHistoryRecord::getRecordType)
                .filter(Predicate.isEqual(DiscountHistoryRecordType.USAGE))
                .count();
        assertEquals(CPU_COUNT / 2, usageCount);

        Promo promoStored = promoService.getPromo(promo.getId());

        BigDecimal usedSpentBudget = DEFAULT_COUPON_VALUE.multiply(BigDecimal.valueOf(usageCount));
        assertThat(promoStored.getCurrentBudget(), comparesEqualTo(BUDGET.subtract(usedSpentBudget)));
        assertThat(promoStored.getSpentBudget(), comparesEqualTo(usedSpentBudget));

        assertThat(promoStored.getCurrentEmissionBudget(),
                comparesEqualTo(EMISSION_BUDGET.subtract(BigDecimal.valueOf(creationCount))));
        assertThat(promoStored.getSpentEmissionBudget(), comparesEqualTo(BigDecimal.valueOf(creationCount)));
    }
}
