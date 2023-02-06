package ru.yandex.market.wms.common.spring.dao;


import java.time.Clock;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.dao.implementation.LostLogDao;
import ru.yandex.market.wms.common.spring.enums.LostType;
import ru.yandex.market.wms.common.spring.service.SerialInventoryLostService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

@DatabaseSetup("/db/dao/serial-inventory-lost/lost-db.xml")
public class LostLogDaoTest extends IntegrationTest {

    @Autowired
    private LostLogDao lostLogDao;

    @Autowired
    private SerialInventoryLostService serialInventoryLost;

    @Autowired
    private Clock clock;

    @Test
    @ExpectedDatabase(value = "/db/dao/lost-log/after-operlost-log.xml", assertionMode = NON_STRICT)
    public void insertOperLostLogs() {
        List<SerialInventory> serials = serialInventoryLost.getLost(LostType.OPER);
        lostLogDao.logSerialInventoryState(serials, clock.instant());
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/lost-log/after-fixlost-log.xml", assertionMode = NON_STRICT)
    public void insertFixLostLogs() {
        List<SerialInventory> serials = serialInventoryLost.getLost(LostType.FIX);
        lostLogDao.logSerialInventoryState(serials, clock.instant());
    }
}
