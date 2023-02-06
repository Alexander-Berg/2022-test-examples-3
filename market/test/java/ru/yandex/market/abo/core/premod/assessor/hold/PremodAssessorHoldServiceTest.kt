package ru.yandex.market.abo.core.premod.assessor.hold

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class PremodAssessorHoldServiceTest @Autowired constructor(
    private val premodAssessorHoldService: PremodAssessorHoldService,
    private val premodAssessorHoldRepo: PremodAssessorHoldRepo
) : EmptyTest() {

    @Test
    fun `release by time`() {
        val now = LocalDateTime.now()
        premodAssessorHoldRepo.saveAll(listOf(
            PremodAssessorHold(0, now.minusHours(12), now.plusHours(12)),
            PremodAssessorHold(1, now.minusHours(12), now.minusHours(6))
        ))
        flushAndClear()

        premodAssessorHoldService.releaseAllByTime()

        assertEquals(listOf(0L), premodAssessorHoldRepo.findAll().map { it.ticketId })
    }
}
