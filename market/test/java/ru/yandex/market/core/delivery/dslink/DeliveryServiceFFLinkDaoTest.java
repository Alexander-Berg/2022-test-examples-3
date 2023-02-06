package ru.yandex.market.core.delivery.dslink;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceFFLink;
import ru.yandex.market.core.delivery.MarketDeliveryShipmentType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeliveryServiceFFLinkDaoTest extends FunctionalTest {

    @Autowired
    private DeliveryServiceFFLinkDao deliveryServiceFFLinkDao;

    @Test
    @DbUnitDataSet(after = "DeliveryServiceFFLinkDaoTest.after.csv")
    void testInsert() {
        deliveryServiceFFLinkDao.insert(generate());
    }

    @Test
    @DbUnitDataSet(before = "DeliveryServiceFFLinkDaoTest.update.before.csv",
            after = "DeliveryServiceFFLinkDaoTest.after.csv")
    void testUpdate() {
        deliveryServiceFFLinkDao.update(generate());
    }

    @Test
    @DbUnitDataSet(before = "DeliveryServiceFFLinkDaoTest.delete.before.csv",
            after = "DeliveryServiceFFLinkDaoTest.after.csv")
    void testDelete() {
        deliveryServiceFFLinkDao.delete(List.of(5L, 6L, 7L, 8L, 9L));
    }

    @Test
    @DbUnitDataSet(before = "DeliveryServiceFFLinkDaoTest.refresh.before.csv",
            after = "DeliveryServiceFFLinkDaoTest.after.csv")
    void testRefresh() {
        deliveryServiceFFLinkDao.refreshDeliveryServicesLink(generate());
    }

    @Test
    @DbUnitDataSet(before = "DeliveryServiceFFLinkDaoTest.exists.before.csv")
    void testExistsBySupplierId() {
        assertTrue(deliveryServiceFFLinkDao.existsBySupplierId(12L));
        assertTrue(deliveryServiceFFLinkDao.existsBySupplierId(13L));
        assertTrue(deliveryServiceFFLinkDao.existsBySupplierId(14L));

        assertFalse(deliveryServiceFFLinkDao.existsBySupplierId(11L));
        assertFalse(deliveryServiceFFLinkDao.existsBySupplierId(15L));
        assertFalse(deliveryServiceFFLinkDao.existsBySupplierId(1001L));
    }

    private List<DeliveryServiceFFLink> generate() {
        return List.of(DeliveryServiceFFLink.builder()
                        .id(1L).fromFFId(1001L).toFFid(2001L).shipmentType(MarketDeliveryShipmentType.INTAKE)
                        .cutoffTimeHour(12).toLogisticsPointId(4001L).build(),
                DeliveryServiceFFLink.builder().id(2L).fromFFId(1002L).toFFid(2002L).build(),
                DeliveryServiceFFLink.builder()
                        .id(3L).fromFFId(1003L).toFFid(2003L)
                        .cutoffTimeHour(12).toLogisticsPointId(4003L).build(),
                DeliveryServiceFFLink.builder()
                        .id(4L).fromFFId(1004L).toFFid(2004L).shipmentType(MarketDeliveryShipmentType.SELF_EXPORT)
                        .cutoffTimeHour(14).toLogisticsPointId(4004L).build()
        );
    }
}
