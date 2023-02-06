package ru.yandex.market.abo.cpa.lms.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 14.12.2021
 */
class LogisticPointRepoTest @Autowired constructor(
    private val logisticPointRepo: LogisticPointRepo,
    private val logisticPointBatchUpdater: PgBatchUpdater<LogisticPoint>
): EmptyTest() {
    @Test
    fun `repo test`() {
        val point = LogisticPoint(1, 123, listOf(1, 2, 3, 4, 5))
        logisticPointBatchUpdater.insertWithoutUpdate(listOf(point))
        flushAndClear()

        assertThat(point)
            .usingRecursiveComparison()
            .isEqualTo(logisticPointRepo.findAll()[0])
    }
}
