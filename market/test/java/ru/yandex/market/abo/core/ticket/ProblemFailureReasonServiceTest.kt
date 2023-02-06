package ru.yandex.market.abo.core.ticket

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.problem.model.ProblemFailureReason
import ru.yandex.market.abo.core.problem.model.ProblemStatus
import ru.yandex.market.abo.core.problem.model.ProblemTypeId
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupFailureReasonType
import ru.yandex.market.abo.core.ticket.repository.ProblemFailureReasonRepo
import java.util.Arrays

/**
 * @author agavrikov
 * @date 29.11.18
 */
class ProblemFailureReasonServiceTest @Autowired constructor(
    private val problemFailureReasonService: ProblemFailureReasonService,
    private val problemFailureReasonRepo: ProblemFailureReasonRepo,
) : AbstractCoreHierarchyTest() {

    @Test
    fun testSaveAndDelete() {
        val problem = createProblem(1, 1, ProblemTypeId.PAYMENT_PROBLEM, ProblemStatus.APPROVED)
        val reason1 = ProblemFailureReason(problem.id, AboRegionGroupFailureReasonType.COURIER_CARD)
        val reason2 = ProblemFailureReason(problem.id, AboRegionGroupFailureReasonType.COURIER_CASH)
        problemFailureReasonService.save(Arrays.asList(reason1, reason2))

        val dbProblemFailureReasons = problemFailureReasonRepo.findAll()
        assertEquals(2, dbProblemFailureReasons.size)
        assertTrue(dbProblemFailureReasons.contains(reason1))
        assertTrue(dbProblemFailureReasons.contains(reason2))

        problemFailureReasonService.delete(problem.id)
        assertTrue(problemFailureReasonRepo.findAll().isEmpty())
    }

    @Test
    fun deleteAllByProblemId() {
        val problemIds = listOf(
            createProblem(1, 1, ProblemTypeId.PAYMENT_PROBLEM, ProblemStatus.APPROVED),
            createProblem(2, 1, ProblemTypeId.PAYMENT_PROBLEM, ProblemStatus.APPROVED),
        ).map { it.id }

        val problemFailureReasons = listOf(
            ProblemFailureReason(problemIds[0], AboRegionGroupFailureReasonType.COURIER_CARD),
            ProblemFailureReason(problemIds[1], AboRegionGroupFailureReasonType.COURIER_CARD),
        )
        problemFailureReasonRepo.saveAll(problemFailureReasons)
        flushAndClear()
        problemFailureReasonRepo.deleteAllByProblemId(problemIds[1])
        flushAndClear()

        val dbProblemFailureReasons = problemFailureReasonRepo.findAll()
        assertEquals(1, dbProblemFailureReasons.size)
        assertEquals(problemFailureReasons[0].id, dbProblemFailureReasons[0].id)
    }

}
