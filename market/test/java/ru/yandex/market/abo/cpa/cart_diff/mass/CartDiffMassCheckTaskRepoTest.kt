package ru.yandex.market.abo.cpa.cart_diff.mass

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import java.time.LocalDateTime
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckState.FINISHED
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckTask
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckTaskRepo

class CartDiffMassCheckTaskRepoTest @Autowired constructor(
    private val cartDiffMassCheckTaskRepo: CartDiffMassCheckTaskRepo
) : EmptyTest() {
    @Test
    fun deleteAllByProcessedTimeBeforeTest() {
        val now = LocalDateTime.now()
        val cartDiffMassCheckTasks = listOf(
            CartDiffMassCheckTask(1, 1).apply {
                processedTime = now
            },
            CartDiffMassCheckTask(2, 2).apply {
                processedTime = now.minusDays(2)
                state = FINISHED
            }
        )
        cartDiffMassCheckTaskRepo.saveAll(cartDiffMassCheckTasks)
        flushAndClear()
        cartDiffMassCheckTaskRepo.deleteAllByProcessedTimeBefore(now.minusDays(1), FINISHED)
        flushAndClear()
        val dbCartDiffMassCheckTasks = cartDiffMassCheckTaskRepo.findAll()
        assertEquals(1, dbCartDiffMassCheckTasks.size)
        assertEquals(cartDiffMassCheckTasks[0].id, dbCartDiffMassCheckTasks[0].id)
    }
}
