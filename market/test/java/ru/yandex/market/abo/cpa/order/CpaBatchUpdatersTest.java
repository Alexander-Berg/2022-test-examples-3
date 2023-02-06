package ru.yandex.market.abo.cpa.order;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.order.delivery.OrderDelivery;
import ru.yandex.market.abo.cpa.order.delivery.OrderDeliveryRepo;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.abo.cpa.order.service.CpaOrderStatRepo;
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author komarovns
 */
class CpaBatchUpdatersTest extends EmptyTest {
    @Autowired
    PgBatchUpdater<CpaOrderStat> cpaOrderStatUpdater;
    @Autowired
    CpaOrderStatRepo cpaOrderStatRepo;
    @Autowired
    PgBatchUpdater<OrderDelivery> orderDeliveryUpdater;
    @Autowired
    OrderDeliveryRepo orderDeliveryRepo;

    @Test
    void cpaOrderStatTest() {
        var stat = new CpaOrderStat();
        stat.setOrderId(1);
        stat.setShopOrderId("2");
        stat.setShopId(3);
        stat.setUserId(4);
        stat.setCreationDate(new Date());
        stat.setPending(new Date());
        stat.setProcessing(new Date());
        stat.setDelivery(new Date());
        stat.setPickup(new Date());
        stat.setDelivered(new Date());
        stat.setCancelled(new Date());
        stat.setCancelledSubstatus(OrderSubstatus.SHOP_PENDING_CANCELLED);
        stat.setCancelledRole(ClientRole.SHOP_USER);
        stat.setDeliveryType(DeliveryType.DIGITAL);
        stat.setRgb(Color.BLUE);
        stat.setReadyToShip(new Date());
        stat.setShipped(new Date());
        stat.setStatus(OrderStatus.PROCESSING);
        stat.setSubstatus(OrderSubstatus.STARTED);
        stat.setRealDeliveryDate(new Date());

        cpaOrderStatUpdater.insertOrUpdate(List.of(stat));
        var dbStat = cpaOrderStatRepo.findByIdOrNull(stat.getOrderId());

        assertEquals(stat.getOrderId(), dbStat.getOrderId());
        assertEquals(stat.getShopOrderId(), dbStat.getShopOrderId());
        assertEquals(stat.getShopId(), dbStat.getShopId());
        assertEquals(stat.getUserId(), dbStat.getUserId());
        assertEquals(stat.getCreationDate().getTime(), dbStat.getCreationDate().getTime());
        assertEquals(stat.getPending().getTime(), dbStat.getPending().getTime());
        assertEquals(stat.getProcessing().getTime(), dbStat.getProcessing().getTime());
        assertEquals(stat.getDelivery().getTime(), dbStat.getDelivery().getTime());
        assertEquals(stat.getPickup().getTime(), dbStat.getPickup().getTime());
        assertEquals(stat.getDelivered().getTime(), dbStat.getDelivered().getTime());
        assertEquals(stat.getCancelled().getTime(), dbStat.getCancelled().getTime());
        assertEquals(stat.getCancelledSubstatus(), dbStat.getCancelledSubstatus());
        assertEquals(stat.getCancelledRole(), dbStat.getCancelledRole());
        assertEquals(stat.getDeliveryType(), dbStat.getDeliveryType());
        assertEquals(stat.getRgb(), dbStat.getRgb());
        assertEquals(stat.getReadyToShip().getTime(), dbStat.getReadyToShip().getTime());
        assertEquals(stat.getShipped().getTime(), dbStat.getShipped().getTime());
        assertEquals(stat.getStatus(), dbStat.getStatus());
        assertEquals(stat.getSubstatus(), dbStat.getSubstatus());
        assertEquals(stat.getRealDeliveryDate().getTime(), dbStat.getRealDeliveryDate().getTime());
    }

    @Test
    void orderDeliveryTest() {
        var delivery = new OrderDelivery(
                1, new Date(), new Date(), DeliveryPartnerType.YANDEX_MARKET, true
        );
        orderDeliveryUpdater.insertWithoutUpdate(List.of(delivery));

        var dbDelivery = orderDeliveryRepo.findByIdOrNull(delivery.getOrderId());
        assertEquals(delivery.getOrderId(), dbDelivery.getOrderId());
        assertEquals(delivery.getByOrder().getTime(), dbDelivery.getByOrder().getTime());
        assertEquals(delivery.getByShipment().getTime(), dbDelivery.getByShipment().getTime());
        assertEquals(delivery.getDeliveryPartnerType(), dbDelivery.getDeliveryPartnerType());
        assertEquals(delivery.getExpress(), dbDelivery.getExpress());
    }
}
