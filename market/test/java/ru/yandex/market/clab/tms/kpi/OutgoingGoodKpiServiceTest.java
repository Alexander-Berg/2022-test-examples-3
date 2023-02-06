package ru.yandex.market.clab.tms.kpi;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.tms.kpi.stats.OutgoingGoodStats;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:magicnumber")
public class OutgoingGoodKpiServiceTest extends KpiServiceTestBase {

    private OutgoingGoodKpiService outgoingGoodKpiService;

    @Before
    public void setUp() {
        super.setUp();

        outgoingGoodKpiService = new OutgoingGoodKpiService(
            auditRepository,
            goodRepository,
            requestedGoodRepositoryStub,
            healthLog
        );
    }

    @Test
    public void testManualMovement() {
        LocalDateTime start = LocalDateTime.now();

        Good newGood = createAndSaveGood(GoodState.SENT);

        createAndSaveGoodStateAction(start.plusSeconds(10), newGood.getId(),
            null, GoodState.NEW);
        createAndSaveGoodStateAction(start.plusSeconds(20), newGood.getId(),
            GoodState.NEW, GoodState.ACCEPTED);
        createAndSaveGoodStateAction(start.plusSeconds(30), newGood.getId(),
            GoodState.ACCEPTED, GoodState.SORTED_TO_CART);
        createAndSaveGoodStateAction(start.plusSeconds(40), newGood.getId(),
            GoodState.SORTED_TO_CART, GoodState.OUT);
        createAndSaveGoodStateAction(start.plusSeconds(50), newGood.getId(),
            GoodState.OUT, GoodState.SENT);

        outgoingGoodKpiService.countAndWriteStats(start, start.plusHours(1));

        OutgoingGoodStats stats = (OutgoingGoodStats) statsCaptor.getValue();

        assertThat(stats.getCategoryId()).isEqualTo(2L);
        assertThat(stats.getGoodId()).isEqualTo(newGood.getId());
        assertThat(stats.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(stats.getProcessedToPlannedTimeSec()).isEqualTo(null);
        assertThat(stats.getPlannedToOutgoingTimeSec()).isEqualTo(null);
        assertThat(stats.getOutgoingToSentTimeSec()).isEqualTo(10);
        assertThat(stats.getRequestedMovementsCount()).isEqualTo(0);
        assertThat(stats.getInContentLabTimeSec()).isEqualTo(30);
    }

    @Test
    public void testAutomatedMovement() {
        LocalDateTime start = LocalDateTime.now();

        Good newGood = createAndSaveGood(GoodState.SENT);

        RequestedGood requestedGood = createAndSaveRequestedGood(newGood.getId(), RequestedGoodState.DONE);

        createAndSaveRequestedGoodStateAction(start.plusSeconds(10), requestedGood.getId(),
            RequestedGoodState.PROCESSING, RequestedGoodState.PROCESSED);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(20), requestedGood.getId(),
            RequestedGoodState.PROCESSED, RequestedGoodState.PLANNED_OUTGOING);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(30), requestedGood.getId(),
            RequestedGoodState.PLANNED_OUTGOING, RequestedGoodState.PROCESSED);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(40), requestedGood.getId(),
            RequestedGoodState.PROCESSED, RequestedGoodState.PLANNED_OUTGOING);
        createAndSaveRequestedGoodStateAction(start.plusSeconds(50), requestedGood.getId(),
            RequestedGoodState.PLANNED_OUTGOING, RequestedGoodState.OUTGOING);

        createAndSaveGoodStateAction(start.plusSeconds(10), newGood.getId(),
            null, GoodState.NEW);
        createAndSaveGoodStateAction(start.plusSeconds(20), newGood.getId(),
            GoodState.NEW, GoodState.ACCEPTED);
        createAndSaveGoodStateAction(start.plusSeconds(30), newGood.getId(),
            GoodState.ACCEPTED, GoodState.SORTED_TO_CART);
        createAndSaveGoodStateAction(start.plusSeconds(40), newGood.getId(),
            GoodState.SORTED_TO_CART, GoodState.OUT);
        createAndSaveGoodStateAction(start.plusSeconds(60), newGood.getId(),
            GoodState.OUT, GoodState.SENT);

        outgoingGoodKpiService.countAndWriteStats(start, start.plusHours(1));

        OutgoingGoodStats stats = (OutgoingGoodStats) statsCaptor.getValue();

        assertThat(stats.getCategoryId()).isEqualTo(2L);
        assertThat(stats.getGoodId()).isEqualTo(newGood.getId());
        assertThat(stats.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertThat(stats.getProcessedToPlannedTimeSec()).isEqualTo(10);
        assertThat(stats.getPlannedToOutgoingTimeSec()).isEqualTo(30);
        assertThat(stats.getOutgoingToSentTimeSec()).isEqualTo(20);
        assertThat(stats.getRequestedMovementsCount()).isEqualTo(2);
        assertThat(stats.getInContentLabTimeSec()).isEqualTo(40);
    }
}
