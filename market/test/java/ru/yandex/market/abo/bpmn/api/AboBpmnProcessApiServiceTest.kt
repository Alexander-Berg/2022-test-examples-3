package ru.yandex.market.abo.bpmn.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.bpmn.AbstractFunctionalTest
import ru.yandex.market.abo.bpmn.api.exception.NotFoundException
import ru.yandex.market.abo.bpmn.util.checkIncidents
import ru.yandex.market.abo.bpmn.util.waitUntilNoActiveJobs
import ru.yandex.mj.generated.server.api.AboBpmnProcessApiDelegate
import ru.yandex.mj.generated.server.model.ProcessSearchRequest
import ru.yandex.mj.generated.server.model.ProcessStartRequest
import ru.yandex.mj.generated.server.model.ProcessType.HELLO_WORLD

/**
 * @author zilzilok
 */
class AboBpmnProcessApiServiceTest @Autowired constructor(
    private val aboBpmnProcessApiService: AboBpmnProcessApiService
) : AbstractFunctionalTest() {

    @Test
    fun `start and get process`() {
        val startRequest = createDefaultStartRequest()
        val processInstance = aboBpmnProcessApiService.startProcess(startRequest).body!!.result
        assertTrue(waitUntilNoActiveJobs(processEngine, processInstance.processInstanceId))

        val process = aboBpmnProcessApiService.getProcess(processInstance.processInstanceId).body!!

        assertEquals("Hello abo!", process.result.params?.get("message"))
        checkIncidents(processEngine, processInstance.processInstanceId)
    }

    @Test
    fun `search process`() {
        val startRequest = createDefaultStartRequest()
        val processInstance = aboBpmnProcessApiService.startProcess(startRequest).body!!.result
        assertTrue(waitUntilNoActiveJobs(processEngine, processInstance.processInstanceId))

        val searchRequest = ProcessSearchRequest().processInstanceId(processInstance.processInstanceId)
        val processes = aboBpmnProcessApiService.searchProcess(searchRequest).body!!.result

        assertEquals(1, processes.size)
        checkIncidents(processEngine, processInstance.processInstanceId)
    }

    @Test
    fun `process not found`() {
        assertThrows(NotFoundException::class.java) {
            aboBpmnProcessApiService.getProcess("no way").body
        }
    }

    companion object {
        private fun createDefaultStartRequest() = ProcessStartRequest()
            .processType(HELLO_WORLD)
            .businessKey("123")
            .params(
                mapOf(
                    "name" to "abo"
                )
            )
    }
}
