package ru.yandex.market.mdm.service.task.controller

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.given
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.service.task.TaskBaseTestClass
import ru.yandex.market.mdm.service.task.controller.dto.RemoveCisRequirementsRequest
import ru.yandex.market.mdm.service.task.controller.dto.TaskState
import ru.yandex.market.mdm.service.task.service.CisRequirementsRemover

@Import(ManualTaskController::class)
class ManualTaskControllerTest : TaskBaseTestClass() {

    @MockBean
    lateinit var cisRequirementsRemover: CisRequirementsRemover

    @Autowired
    lateinit var controller: ManualTaskController

    @Test
    fun `should return with successful state when all cis removal was successful`() {
        // given
        val request = RemoveCisRequirementsRequest(mskuIds = listOf(1L), checkOnly = true)
        val serviceResponse =
            CisRequirementsRemover.RemoveCisRequirementsResponse(
                successfulOrderIds = listOf(42),
                failedOrderIds = listOf(),
                state = CisRequirementsRemover.TaskState.SUCCESSFUL
            )
        given(cisRequirementsRemover.removeCisRequirements(any())).willReturn(serviceResponse)

        // when
        val response = controller.removeCisRequirements(request)

        // then
        response.taskState shouldBe TaskState.SUCCESSFUL
        response.successfulOrderIds shouldContainExactly listOf(42L)
        response.failedOrderIds.size shouldBe 0
    }
}
