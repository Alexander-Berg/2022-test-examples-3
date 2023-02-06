package ru.yandex.market.markup3.yang.repositories

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.yang.dto.TolokaRecoverQueueRow
import java.time.Instant

class TolokaRecoverQueueRepositoryTest : CommonTaskTest() {

    @Autowired
    lateinit var tolokaRecoverQueueRepository: TolokaRecoverQueueRepository

    @Test
    fun testComplexKey() {
        val taskId = createTestTask(createTestTaskGroup().id)
        val now = Instant.now()

        val item = TolokaRecoverQueueRow(taskId, "qwe", 1, now)
        tolokaRecoverQueueRepository.insertOrUpdate(item)

        var findAll = tolokaRecoverQueueRepository.findAll()
        findAll shouldHaveSize 1
        findAll[0].poolId shouldBe 1

        tolokaRecoverQueueRepository.insertOrUpdate(TolokaRecoverQueueRow(taskId, "qwe", 2, now))

        findAll = tolokaRecoverQueueRepository.findAll()
        findAll shouldHaveSize 1
        findAll[0].poolId shouldBe 2

        tolokaRecoverQueueRepository.insertOrUpdate(TolokaRecoverQueueRow(taskId, "qwe2", 1, now))
        findAll = tolokaRecoverQueueRepository.findAll()
        findAll shouldHaveSize 2
    }
}
