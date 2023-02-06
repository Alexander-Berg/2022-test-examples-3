package ru.yandex.market.partner.notification.service.providers.address;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsSendingSettingsExperimentFlagTest extends AbstractFunctionalTest {

    @Autowired
    EnvironmentService environmentService;

    @Test
    void envIsNotSet() {
        var flag = new IsSendingSettingsExperimentFlag(environmentService);

        assertFalse(flag.get(shopDst(111L)));
        assertFalse(flag.get(bizDst(111L)));
        assertFalse(flag.get(uidDst(111L)));
    }

    @Test
    @DbUnitDataSet(before = "IsSendingSettingsExperimentFlagTest.envIsSet.before.csv")
    void envIsSet() {
        var flag = new IsSendingSettingsExperimentFlag(environmentService);

        assertTrue(flag.get(shopDst(11L)));
        assertTrue(flag.get(shopDst(12L)));
        assertFalse(flag.get(shopDst(21L)));
        assertFalse(flag.get(shopDst(31L)));

        assertFalse(flag.get(bizDst(11L)));
        assertTrue(flag.get(bizDst(21L)));
        assertTrue(flag.get(bizDst(22L)));
        assertFalse(flag.get(bizDst(31L)));

        assertFalse(flag.get(uidDst(11L)));
        assertFalse(flag.get(uidDst(21L)));
        assertTrue(flag.get(uidDst(31L)));
        assertTrue(flag.get(uidDst(32L)));
    }

    private MbiDestination shopDst(long shopId) {
        return MbiDestination.create(shopId, null, null, null);
    }

    private MbiDestination bizDst(long businessId) {
        return MbiDestination.create(null, businessId, null, null);
    }

    private MbiDestination uidDst(long uid) {
        return MbiDestination.create(null, null, uid, null);
    }
}
