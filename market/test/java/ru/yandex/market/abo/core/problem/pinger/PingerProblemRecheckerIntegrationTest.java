package ru.yandex.market.abo.core.problem.pinger;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest;
import ru.yandex.market.abo.core.ticket.model.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author artemmz
 * @date 22/02/19.
 */
class PingerProblemRecheckerIntegrationTest extends AbstractCoreHierarchyTest {
    @Autowired
    private PingerProblemRechecker pingerProblemRechecker;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void loadCurrentProblemsWithoutTickets() {
        PingerProblemType type = PingerProblemType.CONTENT_SIZE;

        Problem pingerProblem = createProblem(0, type.getGenId(), type.getId(), ProblemStatus.APPROVED);
        createProblem(0, type.getGenId(), type.getId(), ProblemStatus.NEW);

        entityManager.flush();
        entityManager.clear();

        Collection<Problem> problems = pingerProblemRechecker.loadCurrentProblems().values();
        assertEquals(1, problems.size());
        Problem fromDb = problems.iterator().next();
        assertEquals(pingerProblem.getProblemTypeId(), fromDb.getProblemTypeId());

        Ticket ticket = fromDb.getTicket();
        assertFalse(isInitialized(ticket));
    }
}
