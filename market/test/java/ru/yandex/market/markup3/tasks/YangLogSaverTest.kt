package ru.yandex.market.markup3.tasks

import com.fasterxml.jackson.databind.node.TextNode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import ru.yandex.market.markup3.core.dto.Task
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.testutils.BaseAppTest
import ru.yandex.market.markup3.users.profile.TolokaProfileRow
import ru.yandex.market.markup3.yang.dto.TolokaTaskInfo
import ru.yandex.market.mbo.http.YangLogStorage.YangLogStoreRequest
import ru.yandex.market.mbo.http.YangLogStorage.YangLogStoreResponse
import ru.yandex.market.mbo.http.YangLogStorage.YangTaskType.BLUE_CLASSIFICATION
import ru.yandex.market.mbo.http.YangLogStorageService
import kotlin.properties.Delegates

class YangLogSaverTest : BaseAppTest() {
    private var yangLogStorageService by Delegates.notNull<YangLogStorageService>()
    private var requestCaptor by Delegates.notNull<KArgumentCaptor<YangLogStoreRequest>>()
    private var yangLogSaver by Delegates.notNull<YangLogSaver>()

    @Before
    fun setup() {
        yangLogStorageService = mock()
        doReturn(YangLogStoreResponse.newBuilder().build()).`when`(yangLogStorageService).yangLogStore(any())
        requestCaptor = argumentCaptor()
        yangLogSaver = YangLogSaver(yangLogStorageService)
    }

    @Test
    fun `Builds request correctly`() {
        val tolokaPoolName = "testPool"
        val taskInfo = TolokaTaskInfo(
            id = 123,
            poolId = 456,
            taskId = 777,
            taskKey = "888",
            tolokaTaskId = "900",
            assignmentId = "100"
        )
        val profile = TolokaProfileRow(
            workerId = "wid001",
            staffLogin = "test",
            uid = 987,
        )
        val taskCategoryId = 765L
        val task = Task(
            input = "input",
            state = "state",
            taskRow = TaskRow(213, stage = "stage", state = TextNode("state"), taskGroupId = 1)
        )
        val taskType = BLUE_CLASSIFICATION

        yangLogSaver.yangLogStore(
            tolokaPoolName = tolokaPoolName,
            tolokaTaskInfo = taskInfo,
            tolokaProfile = profile,
            taskCategoryId = taskCategoryId,
            task = task,
        ) {
            it.taskType = taskType
        }

        val request = lastCapture()
        request.contractorInfo.poolName shouldBe tolokaPoolName
        request.contractorInfo.uid shouldBe profile.uid
        request.contractorInfo.poolId shouldBe taskInfo.poolId.toString()
        request.contractorInfo.taskId shouldBe taskInfo.tolokaTaskId
        request.contractorInfo.assignmentId shouldBe taskInfo.assignmentId
        request.contractorInfo.taskSuiteCreatedDate shouldBe taskInfo.created.toString()
        request.id shouldBe YangLogSaver.getYangLogStoreTaskId(task.id)
        request.hitmanId shouldBe YangLogSaver.getExternalTaskId(task.id)
        request.categoryId shouldBe taskCategoryId
        request.taskType shouldBe taskType
    }

    private fun lastCapture(): YangLogStoreRequest {
        verify(yangLogStorageService, atLeastOnce()).yangLogStore(requestCaptor.capture())
        return requestCaptor.lastValue
    }
}
