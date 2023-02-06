package ru.yandex.market.abo.cpa.quality.recheck.problem

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.quality.recheck.problem.model.RecheckProblem

class RecheckProblemRepoTest @Autowired constructor(
    private val recheckProblemRepo: RecheckProblemRepo,
    private val recheckProblemTypeService: RecheckProblemTypeService,
) : EmptyTest() {

    val typeIds = recheckProblemTypeService.all.map { it.id }

    fun getRecheckProblems(): List<RecheckProblem> = listOf(
        RecheckProblem().apply {
            ticketId = 1
            typeId = typeIds[0]
        },
        RecheckProblem().apply {
            ticketId = 2
            typeId = typeIds[1]
        },
    )

    @Test
    fun deleteAllByTicketIdTest() {
        val recheckProblems = getRecheckProblems()
        recheckProblemRepo.saveAll(recheckProblems)
        flushAndClear()
        recheckProblemRepo.deleteAllByTicketId(2)
        flushAndClear()
        val dbRecheckProblems = recheckProblemRepo.findAll()
        assertEquals(1, dbRecheckProblems.size)
        assertEquals(recheckProblems[0].id, dbRecheckProblems[0].id)
    }

    @Test
    fun deleteAllByTicketIdAndTypeIdTest() {
        val recheckProblems = getRecheckProblems()
        recheckProblemRepo.saveAll(recheckProblems)
        flushAndClear()
        recheckProblemRepo.deleteAllByTicketIdAndTypeId(2, typeIds[1])
        flushAndClear()
        val dbRecheckProblems = recheckProblemRepo.findAll()
        assertEquals(1, dbRecheckProblems.size)
        assertEquals(recheckProblems[0].id, dbRecheckProblems[0].id)
    }
}
