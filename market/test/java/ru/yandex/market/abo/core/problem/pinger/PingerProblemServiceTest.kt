package ru.yandex.market.abo.core.problem.pinger

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.pinger.model.PingerContentTask
import ru.yandex.market.abo.core.problem.model.ProblemStatus
import ru.yandex.market.abo.core.ticket.AbstractCoreHierarchyTest
import java.time.LocalDateTime
import java.util.*

/**
 * @author artemmz
 * @date 14/02/19.
 */
class PingerProblemServiceTest @Autowired constructor(
    var pingerProblemService : PingerProblemService
) : AbstractCoreHierarchyTest() {

    @Test
    fun testIsUrlCorrect() {
        val sourceId = 123L
        var task = PingerContentTask.builder()
            .id(sourceId)
            .url(findOffer(1L).getDirectUrl())
            .taskBody("1234")
            .genId(MpGeneratorType.PROBLEM_PRICE_REPING.getId())
            .httpStatus(404).finishTime(LocalDateTime.now()).build()
        var type = PingerProblemType.PRICE
        var problem = createProblem(0, type.getGenId(), type.getId(), ProblemStatus.NEW);
        entityManager.flush();
        entityManager.clear();

        var ticket = ticketService.loadTicketById(problem.getTicketId());
        ticket.getHypothesis().setSourceId(sourceId);

        assertTrue(pingerProblemService.isUrlCorrect(ticket, task));
        assertTrue(pingerProblemService.isPriceCorrect(ticket, task));
        assertTrue(Date().after(PingerProblemService.getPingTime(task)));
    }

    @Test
    fun testLastProblemPingStatuses() {
        var result = pingerProblemService.lastProblemPingStatuses(Collections.singletonList(1L))
        assertNotNull(result)
    }

}
