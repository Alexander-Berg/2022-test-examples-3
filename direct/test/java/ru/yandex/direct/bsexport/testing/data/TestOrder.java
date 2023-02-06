package ru.yandex.direct.bsexport.testing.data;

import ru.yandex.direct.bsexport.model.ContentType;
import ru.yandex.direct.bsexport.model.Order;

/**
 * Данные по заказам
 */
public class TestOrder {
    private TestOrder() {
    }

    public static final Order text1Base = Order.newBuilder()
            .setEID(38965517L)
            .setID(23233775L)
            .setStop(1)
            .setUpdateInfo(0)
            .build();

    public static final Order textWithUpdateInfo1Full = text1Base.toBuilder()
            .setUpdateInfo(1)
            .setOrderType(1)
            .setGroupOrder(0)
            .setGroupOrderID(11903457)
            .setClientID(22069311)
            .setAgencyID(50537)
            .setManagerUID(0)
            .setContentType(ContentType.text)
            .setIndependentBids(0)
            .setQueueSetTime("20190516055554")
            .setDescription("38965517: nsh-srch-cat-nz-computers")
            .setAttributionType(4)
            .setAutoOptimization(0)
            .setMaxCPC(0)
            .build();

    public static final Order cpmBannerWithUpdateInfo1Full = Order.newBuilder()
            .setEID(39192675L)
            .setID(23366890L)
            .setStop(0)
            .setUpdateInfo(1)
            .setOrderType(1)
            .setGroupOrder(0)
            .setGroupOrderID(15580972)
            .setClientID(33265089)
            .setAgencyID(0)
            .setManagerUID(0)
            .setContentType(ContentType.reach)
            .setIndependentBids(1)
            .setQueueSetTime("20190814204520")
            .setDescription("39192675:  Газпром Космические Системы [RTB] [45-54]")
            .setAttributionType(4)
            .setAutoOptimization(0)
            .setMaxCPC(0)
            .setBillingOrders(TestBillingAggregates.billingAggregates1)
            .build();
}
