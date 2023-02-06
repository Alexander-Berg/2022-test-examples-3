package ru.yandex.market.abo.core.ticket;

import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.pinger.PingerProblemType;
import ru.yandex.market.abo.core.ticket.model.TicketTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 03/04/19.
 */
class ProblemManagerIntegrationTest extends AbstractCoreHierarchyTest {
    @Autowired
    private ProblemManager problemManager;

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void deleteDisapprovedPingerProblems(boolean problemIsOld) {
        PingerProblemType prType = PingerProblemType.CONTENT_SIZE;
        Problem problem = createProblem(1, prType.getGenId(), prType.getId(), ProblemStatus.NEW);
        TicketTag modTag = tagService.createTag(0);
        modTag.setTime(problemIsOld ? new Date(0) : new Date());
        problemManager.logApproveAndSave(problem, ProblemStatus.DISAPPROVED, modTag);

        Long problemId = problem.getId();
        assertNotNull(problemService.loadProblem(problemId));
        assertFalse(problemService.loadHistoryList(problemId).isEmpty());
        assertFalse(problemService.loadApproveLogList(problemId).isEmpty());

        problemManager.deleteDisapprovedPingerProblems();
        entityManager.flush();
        entityManager.clear();

        Problem loaded = problemService.loadProblem(problemId);
        assertEquals(problemIsOld, (loaded == null));
        assertEquals(problemIsOld, problemService.loadHistoryList(problemId).isEmpty());
        assertEquals(problemIsOld, problemService.loadApproveLogList(problemId).isEmpty());
    }
}
