package ru.yandex.market.crm.operatorwindow.utils;

import org.mockito.Mockito;

import ru.yandex.market.crm.operatorwindow.domain.loyalty.CouponReason;
import ru.yandex.market.crm.operatorwindow.services.loyalty.OrderCouponsService;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyCoupon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

public class MockCouponScriptServiceApi extends AbstractMockService<OrderCouponsService> {
    private final OrderCouponsService api;

    public MockCouponScriptServiceApi(OrderCouponsService api) {
        super(api);
        this.api = api;
    }

    public void setupCouponCreation(String bonusReason) {
        Mockito.when(api.createAndSaveCoupon(
                        any(),
                        eq(CouponReason.forDefaultPromoId(bonusReason)),
                        any()))
                .thenReturn(Mockito.mock(LoyaltyCoupon.class));
    }

    public void verifyCouponCreation(String bonusReason) {
        Mockito.verify(api, times(1))
                .createAndSaveCoupon(
                        any(),
                        eq(CouponReason.forDefaultPromoId(bonusReason)),
                        any());
    }

    public void verifyNoCouponCreationRequest() {
        Mockito.verify(api, times(0))
                .createAndSaveCoupon(
                        any(),
                        any(),
                        any());
    }
}
