package ru.yandex.market.abo.core.premod.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.premod.model.PremodCheckType
import ru.yandex.market.abo.core.premod.model.PremodItem
import ru.yandex.market.abo.core.premod.model.PremodItemStatus
import ru.yandex.market.abo.core.premod.model.PremodItemType
import ru.yandex.market.abo.core.premod.model.PremodProblem
import ru.yandex.market.abo.core.premod.model.PremodTicket

class PremodProblemRepoTest @Autowired constructor(
    private val premodItemRepo: PremodRepo.PremodItemRepo,
    private val premodTicketRepo: PremodRepo.PremodTicketRepo,
    private val premodProblemRepo: PremodProblemRepo,
) : EmptyTest() {

    @Test
    fun deleteAllByItemId() {
        val itemIds = getItemIds()
        val premodProblems = listOf(
            PremodProblem(itemIds[0], 1, 1, ""),
            PremodProblem(itemIds[1], 1, 1, ""),
        )
        premodProblemRepo.saveAll(premodProblems)
        flushAndClear()
        premodProblemRepo.deleteAllByItemId(itemIds[1])
        flushAndClear()
        val dbPremodProblems = premodProblemRepo.findAll()
        assertEquals(1, dbPremodProblems.size)
        assertEquals(premodProblems[0].id, dbPremodProblems[0].id)
    }

    @Test
    fun deleteAllByItemIdAndTypeIdIn() {
        val itemIds = getItemIds()
        val premodProblems = listOf(
            PremodProblem(itemIds[0], 1, 1, ""),
            PremodProblem(itemIds[1], 1, 1, ""),
            PremodProblem(itemIds[1], 2, 1, ""),
            PremodProblem(itemIds[1], 3, 1, ""),
        )
        premodProblemRepo.saveAll(premodProblems)
        flushAndClear()
        premodProblemRepo.deleteAllByItemIdAndTypeIdIn(itemIds[1], listOf(2, 3, 4))
        flushAndClear()
        val dbPremodProblems = premodProblemRepo.findAll()
        assertEquals(dbPremodProblems.map { it.id }, premodProblems.take(2).map { it.id })
    }

    fun getItemIds(): List<Long> {
        val tickets = listOf(
            PremodTicket(0, 0, PremodCheckType.CPC_PREMODERATION),
            PremodTicket(1, 0, PremodCheckType.CPC_PREMODERATION),
        )
        premodTicketRepo.saveAll(tickets)
        val items = listOf(
            PremodItem(tickets[0].id, PremodItemStatus.NEWBORN, PremodItemType.SHOP_INFO_COLLECTED),
            PremodItem(tickets[1].id, PremodItemStatus.NEWBORN, PremodItemType.SHOP_INFO_COLLECTED),
        )
        premodItemRepo.saveAll(items)
        flushAndClear()
        return items.map { it.id }
    }
}
