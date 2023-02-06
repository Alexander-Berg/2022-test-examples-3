package ru.yandex.market.loyalty.core.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class SqlMonitorServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    private static final Date TODAY = DateUtil.getToday();
    private static final Date YESTERDAY = DateUtil.prevDay(TODAY);
    @Autowired
    SqlMonitorService sqlMonitorService;
    @Autowired
    PromoManager promoManager;

    @Test
    public void shouldReturnsOkWhenDatesValid() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        ComplicatedMonitoring.Result result = sqlMonitorService.checkDbState();

        assertEquals(MonitoringStatus.OK, result.getStatus());
        assertTrue(result.getMessage().isEmpty());
    }


    @Test
    public void shouldReturnsCriticalPromoWhenDatesInvalid() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndDate(YESTERDAY)
                .setStartDate(TODAY)
        );

        ComplicatedMonitoring.Result result = sqlMonitorService.checkDbState();

        assertEquals(MonitoringStatus.CRITICAL, result.getStatus());
        assertEquals(
                String.format("Found invalid dates in  promos with ids: {%s}", promo.getId()),
                result.getMessage()
        );
    }

    @Test
    public void shouldReturnsCriticalCouponEmissionWhenDatesInvalid() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse()
                .setEndEmissionDate(YESTERDAY)
                .setStartEmissionDate(TODAY));

        ComplicatedMonitoring.Result result = sqlMonitorService.checkDbState();

        assertEquals(MonitoringStatus.CRITICAL, result.getStatus());
        assertEquals(
                String.format("Found invalid coupon emission dates in params of promos with ids: {%s}", promo.getId()),
                result.getMessage()
        );
    }

    @Override
    protected boolean shouldCheckConsistence() {
        return false;
    }

}
