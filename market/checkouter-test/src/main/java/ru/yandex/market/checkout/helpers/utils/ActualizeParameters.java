package ru.yandex.market.checkout.helpers.utils;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.providers.ActualItemProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;

public class ActualizeParameters {

    private final ActualItem actualItem;

    private final long uid = BuyerProvider.UID;
    private Order order;
    private ReportGeneratorParameters reportParameters;
    private boolean mockPushApi = true;
    private String perkPromoId;

    public ActualizeParameters() {
        this(ActualItemProvider.buildActualItem());
    }

    public ActualizeParameters(ActualItem actualItem) {
        this.actualItem = actualItem;
        init(actualItem);
    }

    private void init(ActualItem actualItem) {
        order = mapActualItemToOrder(actualItem);
        reportParameters = new ReportGeneratorParameters(order);
        reportParameters.setRegionId(actualItem.getBuyerRegionId());
    }


    public long getUid() {
        return uid;
    }

    public ActualItem getActualItem() {
        return actualItem;
    }

    public Order getOrder() {
        return order;
    }

    public ReportGeneratorParameters getReportParameters() {
        return reportParameters;
    }

    public void setReportParameters(ReportGeneratorParameters reportParameters) {
        this.reportParameters = reportParameters;
    }

    private Order mapActualItemToOrder(ActualItem item) {
        OrderItem orderItem = new OrderItem(item);
        orderItem.setCount(1);
        // для генератора репортового ответа
        orderItem.setPreorder(item.isPreorder());

        Order cart = new Order();
        if (StringUtils.isNotBlank(item.getPromoKey())) {
            cart.setPaymentMethod(PaymentMethod.YANDEX);
        }
        cart.addItem(orderItem);
        cart.setShopId(item.getShopId());
        cart.setDelivery(item.getDeliveryInfo());
        cart.setFake(false);
        cart.setContext(Context.MARKET);
        cart.setBuyer(new Buyer(uid));
        cart.setNoAuth(item.isNoAuth());
        cart.setRgb(item.getRgb());
        return cart;
    }

    public List<DeliveryResponse> getPushApiDeliveryResponses() {
        return Arrays.asList(
                DeliveryProvider.buildShopDeliveryResponse(DeliveryResponse::new),
                DeliveryProvider.buildPickupDeliveryResponseWithOutletCode(DeliveryResponse::new)
        );
    }

    public boolean isMockPushApi() {
        return mockPushApi;
    }

    public void setMockPushApi(boolean mockPushApi) {
        this.mockPushApi = mockPushApi;
    }

    public String getPerkPromoId() {
        return perkPromoId;
    }

    public void setPerkPromoId(String perkPromoId) {
        this.perkPromoId = perkPromoId;
    }
}
