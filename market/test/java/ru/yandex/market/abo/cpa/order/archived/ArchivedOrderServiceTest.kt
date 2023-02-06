package ru.yandex.market.abo.cpa.order.archived

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

/**
 * @author komarovns
 */
open class ArchivedOrderServiceTest @Autowired constructor(
    private val archivedOrderService: ArchivedOrderService
) : EmptyTest() {

    @Test
    open fun `archive and deArchive`() {
        assertFalse(archivedOrderService.isArchived(ORDER_ID))

        archivedOrderService.archive(listOf(ORDER_ID))
        assertTrue(archivedOrderService.isArchived(ORDER_ID))
        archivedOrderService.archive(listOf(ORDER_ID))
        assertTrue(archivedOrderService.isArchived(ORDER_ID))

        archivedOrderService.deArchive(listOf(ORDER_ID))
        assertFalse(archivedOrderService.isArchived(ORDER_ID))
        archivedOrderService.deArchive(listOf(ORDER_ID))
        assertFalse(archivedOrderService.isArchived(ORDER_ID))
    }

    @Test
    open fun filterArchived() {
        archivedOrderService.archive(listOf(1, 2, 3))
        assertEquals(hashSetOf<Long>(2, 3), archivedOrderService.filterArchived(listOf(2, 3, 4)).toHashSet())
    }

    companion object {
        private const val ORDER_ID = 1L
    }
}
