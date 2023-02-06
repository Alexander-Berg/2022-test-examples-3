package ru.yandex.market.abo.core.lom

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.lom.LomOrderCheckpoint.Key
import ru.yandex.market.abo.cpa.order.checkpoint.CheckpointType.SORTING_CENTER_AT_START
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.abo.util.kotlin.toLocalDateTimeBySecondsOffset

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 13.09.2021
 */
open class LomOrderCheckpointUpdaterTest @Autowired constructor(
    private val lomOrderCheckpointBatchUpdater: PgBatchUpdater<LomOrderCheckpoint>,
    private val lomOrderCheckpointRepo: LomOrderCheckpointRepo
) : EmptyTest() {

    @Test
    fun `save test`() {
        val checkpointTime = LocalDateTime.now()
        val checkpoint = LomOrderCheckpoint(
            key = Key(1111, SORTING_CENTER_AT_START),
            id = 25,
            checkpointTime = checkpointTime,
            checkpointRealTime = checkpointTime.toInstant(ZoneOffset.ofHours(3)).toLocalDateTimeBySecondsOffset(28800),
            locationId = 66
        )
        lomOrderCheckpointBatchUpdater.insertOrUpdate(listOf(checkpoint))
        flushAndClear()
        assertThat(checkpoint)
            .usingRecursiveComparison()
            .isEqualTo(lomOrderCheckpointRepo.findByIdOrNull(Key(1111, SORTING_CENTER_AT_START)))

        val nextCheckpoint = LomOrderCheckpoint(
            key = Key(1111, SORTING_CENTER_AT_START),
            id = 28,
            checkpointTime = checkpointTime.plusHours(1),
            checkpointRealTime = checkpointTime.toInstant(ZoneOffset.ofHours(3)).toLocalDateTimeBySecondsOffset(28800),
            locationId = 66
        )
        lomOrderCheckpointBatchUpdater.insertOrUpdate(listOf(nextCheckpoint))
        flushAndClear()
        assertThat(checkpoint)
            .usingRecursiveComparison()
            .isEqualTo(lomOrderCheckpointRepo.findByIdOrNull(Key(1111, SORTING_CENTER_AT_START)))

        val previousCheckpoint = LomOrderCheckpoint(
            key = Key(1111, SORTING_CENTER_AT_START),
            id = 21,
            checkpointTime = checkpointTime.minusHours(1),
            checkpointRealTime = checkpointTime.toInstant(ZoneOffset.ofHours(3)).toLocalDateTimeBySecondsOffset(28800),
            locationId = 66
        )
        lomOrderCheckpointBatchUpdater.insertOrUpdate(listOf(previousCheckpoint))
        flushAndClear()
        assertThat(previousCheckpoint)
            .usingRecursiveComparison()
            .isEqualTo(lomOrderCheckpointRepo.findByIdOrNull(Key(1111, SORTING_CENTER_AT_START)))
    }
}
