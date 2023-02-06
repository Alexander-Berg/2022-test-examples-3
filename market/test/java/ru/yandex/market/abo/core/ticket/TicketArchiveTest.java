package ru.yandex.market.abo.core.ticket;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemFailureReason;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupFailureReasonType;
import ru.yandex.market.abo.core.ticket.model.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketArchiveTest extends AbstractCoreHierarchyTest {

    @Autowired
    ProblemFailureReasonService problemFailureReasonService;

    @Test
    public void archive() {
        long shopId = 1L;
        long id = createTicket(shopId, 2);
        Ticket ticket = ticketService.loadTicketById(id);
        Problem problem = createProblem(shopId, 2, ProblemTypeId.PRICE_DIFFERS, ProblemStatus.APPROVED);
        problemFailureReasonService.save(Collections.singletonList(
                new ProblemFailureReason(problem.getId(), AboRegionGroupFailureReasonType.NO_DELIVERY)
        ));
        flushAndClear();

        jdbcTemplate.execute("select archive_core_data(current_date + 1)");
        flushAndClear();
        assertNull(ticketService.loadTicketById(id));
        assertNull(problemService.loadProblem(problem.getId()));
        assertEquals(0, getCountCoreProblemFailureReason());

        jdbcTemplate.execute("select dearchive_core_by_date(current_date + 2)");
        flushAndClear();
        assertNull(ticketService.loadTicketById(id));
        assertNull(problemService.loadProblem(problem.getId()));
        assertEquals(0, getCountCoreProblemFailureReason());

        jdbcTemplate.execute("select dearchive_core_by_date(current_date - 1)");
        flushAndClear();
        assertEquals(problem, problemService.loadProblem(problem.getId()));
        assertEquals(ticket, ticketService.loadTicketById(id));
        assertEquals(1, getCountCoreProblemFailureReason());
    }

    int getCountCoreProblemFailureReason() {
        return jdbcTemplate.queryForObject(
                "select count(*) from core_problem_failure_reason", Integer.class);
    }
}
