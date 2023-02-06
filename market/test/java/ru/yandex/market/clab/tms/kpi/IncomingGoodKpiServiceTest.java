package ru.yandex.market.clab.tms.kpi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.tms.kpi.stats.IncomingGoodStats;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class IncomingGoodKpiServiceTest extends KpiServiceTestBase {

    private IncomingGoodKpiService incomingGoodKpiService;

    @Before
    public void setUp() {
        super.setUp();

        incomingGoodKpiService = new IncomingGoodKpiService(
            auditRepository,
            goodRepository,
            requestedGoodRepositoryStub,
            healthLog
        );
    }

    @Test
    public void testManualMovement() {
        LocalDateTime start = LocalDateTime.now();

        Good newGood = createAndSaveGood(GoodState.ACCEPTED);

        createAndSaveGoodStateAction(start.plusSeconds(10), newGood.getId(),
            null, GoodState.NEW);
        createAndSaveGoodStateAction(start.plusSeconds(20), newGood.getId(),
            GoodState.NEW, GoodState.ACCEPTED);

        incomingGoodKpiService.countAndWriteStats(start, start.plusHours(1));

        IncomingGoodStats stats = (IncomingGoodStats) statsCaptor.getValue();

        assertThat(stats.getCategoryId()).isEqualTo(2L);
        assertThat(stats.getGoodId()).isEqualTo(newGood.getId());
        assertThat(stats.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(stats.getRequestedToPlannedTimeSec()).isEqualTo(null);
        assertThat(stats.getPlannedToIncomingTimeSec()).isEqualTo(null);
        assertThat(stats.getIncomingToAcceptedTimeSec()).isEqualTo(10);
        assertThat(stats.getRequestedMovementsCount()).isEqualTo(0);
    }

    @Test
    public void testAutomatedMovement() {
        LocalDateTime start = LocalDateTime.now();

        Good newGood = createAndSaveGood(GoodState.ACCEPTED);

        RequestedGood requestedGood = createAndSaveRequestedGood(newGood.getId(), RequestedGoodState.DONE);

        createAndSaveRequestedGoodStateAction(start.plusSeconds(10), requestedGood.getId(),
            null, RequestedGoodState.NEW);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(20), requestedGood.getId(),
            RequestedGoodState.NEW, RequestedGoodState.PLANNED);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(30), requestedGood.getId(),
            RequestedGoodState.PLANNED, RequestedGoodState.NEW);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(40), requestedGood.getId(),
            RequestedGoodState.NEW, RequestedGoodState.PLANNED);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(50), requestedGood.getId(),
            RequestedGoodState.PLANNED, RequestedGoodState.INCOMING);

        createAndSaveGoodStateAction(start.plusSeconds(10), newGood.getId(),
            null, GoodState.NEW);
        createAndSaveGoodStateAction(start.plusSeconds(30), newGood.getId(),
            GoodState.NEW, GoodState.ACCEPTED);

        incomingGoodKpiService.countAndWriteStats(start, start.plusHours(1));

        IncomingGoodStats stats = (IncomingGoodStats) statsCaptor.getValue();

        assertThat(stats.getCategoryId()).isEqualTo(2L);
        assertThat(stats.getGoodId()).isEqualTo(newGood.getId());
        assertThat(stats.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(stats.getRequestedToPlannedTimeSec()).isEqualTo(10);
        assertThat(stats.getPlannedToIncomingTimeSec()).isEqualTo(30);
        assertThat(stats.getIncomingToAcceptedTimeSec()).isEqualTo(20);
        assertThat(stats.getRequestedMovementsCount()).isEqualTo(2);
    }
}
