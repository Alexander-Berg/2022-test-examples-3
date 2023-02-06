package ru.yandex.market.billing.imports.globalorder.dao;

import java.time.OffsetDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderDeliveryStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderPaymentStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderShopStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.imports.globalorder.dao.GlobalTestObjects.defaultGlobalOrder;
import static ru.yandex.market.billing.imports.globalorder.dao.GlobalTestObjects.defaultGlobalOrderItem;

@ParametersAreNonnullByDefault
class GlobalOrderDaoTest extends FunctionalTest {

    @Autowired
    private GlobalOrderDao globalOrderDao;

    @Test
    @DbUnitDataSet(before = "GlobalOrderDao.saveOrders.before.csv", after = "GlobalOrderDao.saveOrders.after.csv")
    void saveOrders() {
        globalOrderDao.saveOrders(List.of(
                defaultGlobalOrder(4)
                        .setCreatedAt(OffsetDateTime.parse("2021-11-17T03:34:00+03").toInstant())
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-17T03:34:00+03").toInstant())
                        .setFreeDeliveryForShop(true)
                        .build(),
                defaultGlobalOrder(5)
                        .setCreatedAt(OffsetDateTime.parse("2021-11-17T03:44:00+03").toInstant())
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-17T03:44:00+03").toInstant())
                        .setItemsTotal(25000L)
                        .setSubsidyTotal(500L)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "GlobalOrderDao.saveOrders.before.csv",
            after = "GlobalOrderDao.saveOrders_withUpdate.after.csv"
    )
    void saveOrders_withUpdate() {
        globalOrderDao.saveOrders(List.of(
                defaultGlobalOrder(3)
                        .setShopId(13L)
                        .setCreatedAt(OffsetDateTime.parse("2021-11-17T03:09:00+03").toInstant())
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-17T03:15:00+03").toInstant())
                        .setItemsTotal(300000L)
                        .setSubsidyTotal(0L)
                        .setStatus(GlobalOrderStatus.CANCELED)
                        .setDeliveryStatus(GlobalOrderDeliveryStatus.ORDER_CANCELED)
                        .setPaymentStatus(GlobalOrderPaymentStatus.CANCELED)
                        .setShopStatus(GlobalOrderShopStatus.CANCELED)
                        .build(),
                defaultGlobalOrder(4)
                        .setCreatedAt(OffsetDateTime.parse("2021-11-17T03:44:00+03").toInstant())
                        .setProcessInBillingAt(OffsetDateTime.parse("2021-11-17T04:02:00+03").toInstant())
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = {"GlobalOrderDao.saveOrders.before.csv", "GlobalOrderDao.saveOrderItems.before.csv"},
            after = "GlobalOrderDao.saveOrderItems.after.csv"
    )
    void saveOrderItems() {
        globalOrderDao.saveOrderItems(List.of(
                defaultGlobalOrderItem(301, 3)
                        .setOfferName("חלב")
                        .setPrice(10000)
                        .setSubsidy(1000)
                        .build(),
                defaultGlobalOrderItem(302, 3)
                        .setOfferName("בירה")
                        .setPrice(5000)
                        .setSubsidy(2500)
                        .setCount(10)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(
            before = {"GlobalOrderDao.saveOrders.before.csv", "GlobalOrderDao.saveOrderItems.before.csv"},
            after = "GlobalOrderDao.saveOrderItems_withUpdate.after.csv"
    )
    void saveOrderItems_withUpdate() {
        globalOrderDao.saveOrderItems(List.of(
                defaultGlobalOrderItem(211, 2)
                        .setOfferName("ויסקי")
                        .setPrice(200000)
                        .setSubsidy(3000)
                        .setCount(10)
                        .build(),
                defaultGlobalOrderItem(302, 3)
                        .setOfferName("בירה")
                        .setPrice(5000)
                        .setSubsidy(2500)
                        .setCount(10)
                        .build()
        ));
    }
}
