package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.processing.SchedulingEntityType
import ru.yandex.market.logistics.mqm.entity.processing.SchedulingLock
import ru.yandex.market.logistics.mqm.repository.SchedulingLockRepository
import ru.yandex.market.logistics.mqm.service.SchedulingLockServiceImpl.Companion.MONITORING_SCHEDULING_LOG
import ru.yandex.market.logistics.mqm.utils.tskvGetCode
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

@ExtendWith(MockitoExtension::class)
class SchedulingLockServiceImplTest: AbstractTest() {

    @Mock
    lateinit var repository: SchedulingLockRepository

    @RegisterExtension
    @JvmField
    val backLogCaptor = BackLogCaptor()

    private lateinit var schedulingLockService: SchedulingLockServiceImpl

    @BeforeEach
    private fun setUp() {
        schedulingLockService = SchedulingLockServiceImpl(
            repository = repository,
        )
    }

    @Test
    @DisplayName("Учет новых объектов")
    fun lock() {
        schedulingLockService.lock(TEST_ID, TEST_TYPE)

        verify(repository).saveAll(TEST_LOCKS)
        val log = backLogCaptor.results[0]
        tskvGetCode(log) shouldBe MONITORING_SCHEDULING_LOG
        tskvGetExtra(log) shouldContainExactlyInAnyOrder setOf(
            "count" to "2",
            "action" to "lock",
        )
    }

    @Test
    @DisplayName("Удаление объектов из учета")
    fun releaseLock() {
        schedulingLockService.releaseLock(TEST_ID, TEST_TYPE)

        verify(repository).deleteAll(TEST_LOCKS)
        val log = backLogCaptor.results[0]
        tskvGetCode(log) shouldBe MONITORING_SCHEDULING_LOG
        tskvGetExtra(log) shouldContainExactlyInAnyOrder setOf(
            "count" to "2",
            "action" to "release",
        )
    }

    companion object {
        val TEST_ID = setOf(1L, 2L)
        val TEST_TYPE = SchedulingEntityType.PLAN_FACT
        val TEST_LOCKS = TEST_ID.map { id -> SchedulingLock(entityId = id, entityType = TEST_TYPE) }
    }
}
