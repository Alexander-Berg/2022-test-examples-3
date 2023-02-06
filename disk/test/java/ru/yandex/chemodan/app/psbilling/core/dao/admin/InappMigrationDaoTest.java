package ru.yandex.chemodan.app.psbilling.core.dao.admin;

import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingOrdersFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.admin.InappMigration;
import ru.yandex.inside.passport.PassportUid;

import static org.junit.Assert.assertEquals;

public class InappMigrationDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    InappMigrationDao inappMigrationDao;
    @Autowired
    PsBillingOrdersFactory ordersFactory;
    @Test
    public void simpleTest() {
        PassportUid uid = PassportUid.cons(1);

        UUID orderId = ordersFactory.createOrder(uid).getId();
        String status = "status";
        String newStatus = "newStatus";
        String currentCode = "code";
        String targetCode = "targetCode";
        InappMigration inappMigration = inappMigrationDao.create(uid, orderId, status, currentCode);
        assertEquals(status, inappMigration.getStatus());
        assertEquals(currentCode, inappMigration.getCurrentProductPeriodCode());
        assertEquals(orderId, inappMigration.getOrderId());
        assertEquals(newStatus, inappMigrationDao.setStatus(inappMigration.getId(), newStatus).getStatus());
        assertEquals(targetCode, inappMigrationDao.setTargetProductPeriodCode(inappMigration.getId(), targetCode)
                .getTargetProductPeriodCode().get());

    }
}
