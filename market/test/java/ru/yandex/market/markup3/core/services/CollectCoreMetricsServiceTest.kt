package ru.yandex.market.markup3.core.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.inside.solomon.pusher.SolomonPusher
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.misc.monica.solomon.sensors.PushSensorsData
import java.util.concurrent.CompletableFuture


class CollectCoreMetricsServiceTest : CommonTaskTest() {
    @Autowired
    lateinit var collectCoreMetricsService: CollectCoreMetricsService

    @Autowired
    lateinit var solomonPusher: SolomonPusher

    @After
    fun reset() {
        Mockito.reset(solomonPusher)
    }

    @Test
    fun `should not fail on empty data`() {
        collectCoreMetricsService.collectMetrics()
        Mockito.verifyNoMoreInteractions(solomonPusher)
    }

    @Test
    fun `it should log something sensible`() {
        Mockito.doReturn(CompletableFuture.completedFuture(Unit)).whenever(solomonPusher).push(any())
        val taskId = createTestTask(createTestTaskGroup().id)
        taskEventService.sendEvents(SendEvent(taskId, TestEventObject))

        collectCoreMetricsService.collectMetrics()

        val captor = ArgumentCaptor.forClass(PushSensorsData::class.java)
        Mockito.verify(solomonPusher).push(captor.capture())

        val sensors = captor.value
        sensors.sensors shouldHaveSize 7
        sensors.sensors.map { it.labels["sensor"] }.toSet() shouldContainExactly listOf(
            "core.task.active_count",
            "core.event.active_count",
            "core.event.active_with_retry_count",
            "core.event.oldest_seconds",
        )
    }
}

