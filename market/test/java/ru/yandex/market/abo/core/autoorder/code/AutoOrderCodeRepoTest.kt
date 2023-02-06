package ru.yandex.market.abo.core.autoorder.code

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

open class AutoOrderCodeRepoTest @Autowired constructor(
    private val autoOrderCodeRepo: AutoOrderCodeRepo
) : EmptyTest() {

    @Test
    fun `delete by creation time`() {
        val now = LocalDateTime.now()
        autoOrderCodeRepo.saveAll(listOf(
            AutoOrderCode(0, "").apply { creationTime = now },
            AutoOrderCode(1, "").apply { creationTime = now.minusDays(10) }
        ))
        flushAndClear()
        autoOrderCodeRepo.deleteByCreationTimeBefore(now.minusDays(1))
        flushAndClear()
        assertThat(autoOrderCodeRepo.findAll())
            .extracting<Long> { it.hypId }
            .containsExactly(0L)
    }
}
