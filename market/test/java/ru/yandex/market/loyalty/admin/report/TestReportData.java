package ru.yandex.market.loyalty.admin.report;

import ru.yandex.market.checkout.checkouter.order.Order;

public class TestReportData {
    public long promoId;
    public String couponCode;
    public Order order;

    public TestReportData(long promoId, String couponCode, Order order) {
        this.promoId = promoId;
        this.couponCode = couponCode;
        this.order = order;
    }
}
