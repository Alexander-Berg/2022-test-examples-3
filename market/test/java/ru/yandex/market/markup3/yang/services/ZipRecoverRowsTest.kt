package ru.yandex.market.markup3.yang.services

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.markup3.yang.dto.TolokaRecoverQueueRow
import ru.yandex.market.markup3.yang.services.recover.RecoverTaskService
import ru.yandex.market.markup3.yang.services.recover.zipRecoverRows
import java.time.Instant

class ZipRecoverRowsTest {

    @Test
    fun testZipRecoverRows() {
        val baseInstant = Instant.now()
        val poolId = 1

        var listToZip = listOf(
            TolokaRecoverQueueRow(1, "asd", poolId, baseInstant),
        )
        var zipped = zipRecoverRows(listToZip)
        zipped shouldHaveSize 1
        zipped[0].rows shouldHaveSize 1
        zipped[0].from shouldBe baseInstant.minusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER)
        zipped[0].to shouldBe baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER)

        listToZip = listOf(
            TolokaRecoverQueueRow(1, "asd", poolId, baseInstant),
            TolokaRecoverQueueRow(1, "asd", poolId, baseInstant.plusSeconds(1)),
            TolokaRecoverQueueRow(1, "asd", poolId, baseInstant.plusSeconds(2)),
            TolokaRecoverQueueRow(
                1,
                "asd",
                poolId,
                baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER + 1)
            ),
            TolokaRecoverQueueRow(
                1,
                "asd",
                poolId,
                baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER * 5)
            ),
            TolokaRecoverQueueRow(
                1,
                "asd",
                poolId,
                baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER * 5)
            ),
        )
        zipped = zipRecoverRows(listToZip)
        zipped shouldHaveSize 2
        zipped[0].rows shouldHaveSize 4
        zipped[0].from shouldBe baseInstant.minusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER)
        zipped[0].to shouldBe baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER + 1)
            .plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER)

        zipped[1].rows shouldHaveSize 2
        zipped[1].from shouldBe baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER * 5)
            .minusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER)
        zipped[1].to shouldBe baseInstant.plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER * 5)
            .plusSeconds(RecoverTaskService.DELTA_TO_SEARCH_FOR_RECOVER)

        listToZip = listOf()
        zipped = zipRecoverRows(listToZip)
        zipped shouldHaveSize 0
    }
}
