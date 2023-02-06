package ru.yandex.market.wms.scheduler.dao;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.spring.dao.entity.DcUnitReadyToShip;
import ru.yandex.market.wms.common.spring.enums.UnitType;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class DcUnitsReadyToShipDaoTest extends SchedulerIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;
    private final Clock clock = Clock.fixed(Instant.parse("2022-03-28T11:00:00Z"), ZoneOffset.UTC);
    private DcUnitsReadyToShipDao dcUnitsReadyToShipDao;

    private static final String BOX_ID = "BOX1";
    private static final String PARENT_PALLET_ID = "PALLET1";
    private static final String EMPTY_PALLET_ID = "PALLET2";
    private static final long WH_ID = 2L;
    private static final long INBOUND_EXTERNAL_ID = 345L;

    @BeforeEach
    public void setupDao() {
        dcUnitsReadyToShipDao = new DcUnitsReadyToShipDao(jdbc, clock);
    }

    @Test
    @DatabaseSetup("/db/dao/dcunitsreadytoship/data.xml")
    @ExpectedDatabase(value = "/db/dao/dcunitsreadytoship/after-insert.xml", assertionMode = NON_STRICT_UNORDERED)
    void insert() {
        DcUnitReadyToShip toInsert = new DcUnitReadyToShip(
                EMPTY_PALLET_ID,
                UnitType.PALLET,
                null,
                clock.instant(),
                INBOUND_EXTERNAL_ID,
                WH_ID,
                clock.instant(), null);
        dcUnitsReadyToShipDao.insert(List.of(toInsert));
    }

    @Test
    @DatabaseSetup("/db/dao/dcunitsreadytoship/after-insert.xml")
    void update() {
        dcUnitsReadyToShipDao.updateDcUnitInfo(BOX_ID, EMPTY_PALLET_ID);
        Optional<DcUnitReadyToShip> dcUnit = dcUnitsReadyToShipDao.findById(BOX_ID);
        Assertions.assertTrue(dcUnit.isPresent());
        Assertions.assertEquals(EMPTY_PALLET_ID, dcUnit.get().parentId());
    }

    @Test
    @DatabaseSetup("/db/dao/dcunitsreadytoship/after-insert.xml")
    @ExpectedDatabase(value = "/db/dao/dcunitsreadytoship/after-mark-deleted.xml", assertionMode = NON_STRICT_UNORDERED)
    void markDeleted() {
        dcUnitsReadyToShipDao.markDeleted(PARENT_PALLET_ID);
    }


    @Test
    @DatabaseSetup("/db/dao/dcunitsreadytoship/after-mark-deleted.xml")
    @ExpectedDatabase(value = "/db/dao/dcunitsreadytoship/after-delete.xml", assertionMode = NON_STRICT_UNORDERED)
    void deleteOlderThanDate() {
        dcUnitsReadyToShipDao.deleteOlderThanDate(LocalDate.now(clock).plus(1, ChronoUnit.DAYS));
    }
}
