package ru.yandex.market.hrms.tms.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

@DbUnitDataSet(schema = "public", before = "OutstaffDomainIdPopulationManagerTest.before.csv")
@DbUnitDataSet(schema = "public", after = "OutstaffDomainIdPopulationManagerTest.after.csv")
class OutstaffDomainIdPopulationManagerTest extends AbstractTmsTest {

    @Autowired
    private OutstaffDomainIdPopulationManager manager;

    @Test
    void populateOutstaffDomainId() {
        manager.populateOutstaffDomainId();
    }
}
