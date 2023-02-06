package ru.yandex.market.markup3.yang.services

import com.fasterxml.jackson.databind.node.IntNode
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.TaskGroupId
import ru.yandex.market.markup3.core.TolokaSource.TOLOKA
import ru.yandex.market.markup3.core.TolokaSource.YANG
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.repositories.TaskDbService
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.markup3.yang.dto.TolokaPoolInfo
import ru.yandex.market.markup3.yang.dto.TolokaTaskInfo
import ru.yandex.market.markup3.yang.repositories.TolokaPoolInfoRepository
import ru.yandex.market.markup3.yang.repositories.TolokaTaskInfoRepository
import ru.yandex.market.markup3.yang.services.YangFullPoolDeactivationService.Companion.FULL_THRESHOLD_KEY
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import kotlin.properties.Delegates

class YangFullPoolDeactivationServiceTest : BaseAppTest() {
    @Autowired
    lateinit var keyValueService: StorageKeyValueService

    @Autowired
    lateinit var poolInfoRepository: TolokaPoolInfoRepository

    @Autowired
    lateinit var taskInfoRepository: TolokaTaskInfoRepository

    @Autowired
    lateinit var taskDbService: TaskDbService

    private var spiedPoolInfoRepository by Delegates.notNull<TolokaPoolInfoRepository>()
    private var service by Delegates.notNull<YangFullPoolDeactivationService>()

    @Before
    fun setUp() {
        spiedPoolInfoRepository = spy(poolInfoRepository)

        service = YangFullPoolDeactivationService(
            keyValueService,
            TransactionHelper.MOCK,
            spiedPoolInfoRepository,
            taskInfoRepository
        )
    }

    @Test
    fun deactivatesOnlyActivePools() {
        val activePool = poolInfoRepository.insert(TolokaPoolInfo(source = YANG, poolGroupId = "1", active = true))
        val inactivePool = poolInfoRepository.insert(TolokaPoolInfo(source = YANG, poolGroupId = "2", active = false))

        activePool.createTasks(1, 1)
        inactivePool.createTasks(1, 1)

        keyValueService.putValue(FULL_THRESHOLD_KEY, 1)
        service.deactivateFullPools()

        val updateCaptor = argumentCaptor<Collection<TolokaPoolInfo>>()
        verify(spiedPoolInfoRepository, times(1)).updateBatch(updateCaptor.capture())
        assertThat(updateCaptor.firstValue).hasSize(1).first()
            .matches { it.id == activePool.id }
            .matches { !it.active }
    }

    @Test
    fun deactivatesOnlyPoolsOverThreshold() {
        val threshold = 123

        val poolMinusOne = poolInfoRepository.insert(TolokaPoolInfo(source = YANG, poolGroupId = "1", active = true))
        val poolExact = poolInfoRepository.insert(TolokaPoolInfo(source = YANG, poolGroupId = "2", active = true))
        val poolPlusOne = poolInfoRepository.insert(TolokaPoolInfo(source = YANG, poolGroupId = "3", active = true))

        poolMinusOne.createTasks(1, threshold - 1)
        poolExact.createTasks(1, threshold)
        poolPlusOne.createTasks(1, threshold + 1)


        keyValueService.putValue(FULL_THRESHOLD_KEY, threshold)
        service.deactivateFullPools()

        assertThat(poolInfoRepository.findById(poolMinusOne.id)).matches { it.active }
        assertThat(poolInfoRepository.findById(poolExact.id)).matches { !it.active }
        assertThat(poolInfoRepository.findById(poolPlusOne.id)).matches { !it.active }
    }

    @Test
    fun deactivatesOnlyYangPools() {
        val yangPool = poolInfoRepository.insert(TolokaPoolInfo(source = YANG, poolGroupId = "1", active = true))
        val tolokaPool = poolInfoRepository.insert(TolokaPoolInfo(source = TOLOKA, poolGroupId = "2", active = true))
        yangPool.createTasks(1, 3)
        tolokaPool.createTasks(1, 3)

        keyValueService.putValue(FULL_THRESHOLD_KEY, 1)
        service.deactivateFullPools()

        assertThat(poolInfoRepository.findById(yangPool.id)).matches { !it.active }
        assertThat(poolInfoRepository.findById(tolokaPool.id)).matches { it.active }
    }

    private fun TolokaPoolInfo.createTasks(taskGroupId: TaskGroupId, count: Int) {
        generateSequence(0) { i -> (i + 1).takeIf { it < count } }
            .map { i ->
                TaskRow(taskGroupId = taskGroupId, stage = i.toString(), state = IntNode(i))
            }
            .let {
                taskDbService.insertTasks(it.toList())
            }
            .mapIndexed { i, task ->
                TolokaTaskInfo(taskId = task.id, poolId = this.id, taskKey = i.toString())
            }.apply {
                taskInfoRepository.insertBatch(this)
            }
    }
}
