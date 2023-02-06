package ru.yandex.market.abo.core.callcenter.express

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import ru.yandex.market.abo.core.callcenter.core.CallCenterCaller
import ru.yandex.market.abo.core.callcenter.core.CallCenterTask
import ru.yandex.market.abo.core.callcenter.core.CallCenterTaskService
import ru.yandex.market.abo.core.callcenter.core.CallCenterTaskState
import ru.yandex.market.abo.cpa.order.CheckouterOrdersHelper
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus
import ru.yandex.market.checkout.checkouter.request.OrderRequest
import ru.yandex.market.util.db.ConfigurationService
import java.time.LocalDateTime

/**
 * @author selemilka
 * @since 08.07.2022
 */
internal class CallCenterTaskExpressManagerTest {

    private val checkouterClient: CheckouterAPI = mock()
    private val caller: CallCenterCaller = mock()
    private val taskService: CallCenterTaskService = mock()
    private val checkouterOrdersHelper: CheckouterOrdersHelper = mock()
    private val coreConfigService: ConfigurationService = mock()

    private val callCenterTaskExpressManager = CallCenterTaskExpressManager(
        checkouterOrdersHelper = checkouterOrdersHelper,
        checkouterClient = checkouterClient,
        callCenterTaskService = taskService,
        callCenterCaller = caller,
        coreConfigService = coreConfigService,
        callScriptFile = ClassPathResource("callcenter/callscript.json")
    )

    @BeforeEach
    fun init() {
        whenever(checkouterOrdersHelper.buildOrderRequest(any())).thenReturn(OrderRequest.builder(1L).build())
    }

    @Test
    fun `process new task`() {
        whenever(taskService.loadTasksForProcessing(any(), any())).thenReturn(listOf(getActualTask()))
        whenever(checkouterClient.getOrder(any(), any())).thenReturn(notCollectedOffer)

        val taskBeforeProcessing = getActualTask()
        callCenterTaskExpressManager.processTasks()

        argumentCaptor<CallCenterTask>().apply {
            verify(taskService, times(1)).save(capture())
            val taskAfterProcessing = firstValue

            assertThat(taskAfterProcessing.modificationTime).isAfter(taskBeforeProcessing.modificationTime)
            assertThat(taskAfterProcessing.tryNumber).isEqualTo(1)
        }
    }

    @Test
    fun `end task with collected order`() {
        whenever(taskService.loadTasksForProcessing(any(), any())).thenReturn(listOf(getActualTask()))
        whenever(checkouterClient.getOrder(any(), any())).thenReturn(collectedOffer)

        val taskBeforeProcessing = getActualTask()
        callCenterTaskExpressManager.processTasks()

        argumentCaptor<CallCenterTask>().apply {
            verify(taskService, times(1)).save(capture())
            val taskAfterProcessing = firstValue

            assertThat(taskAfterProcessing.modificationTime).isAfter(taskBeforeProcessing.modificationTime)
            assertThat(taskAfterProcessing.state).isEqualTo(CallCenterTaskState.FINISHED)
        }
    }

    @Test
    fun `end task with not collected order`() {
        whenever(taskService.loadTasksForProcessing(any(), any())).thenReturn(listOf(getNoRetriesLeftActualTask()))
        whenever(checkouterClient.getOrder(any(), any())).thenReturn(notCollectedOffer)

        val taskBeforeProcessing = getNoRetriesLeftActualTask()
        callCenterTaskExpressManager.processTasks()

        argumentCaptor<CallCenterTask>().apply {
            verify(taskService, times(1)).save(capture())
            val taskAfterProcessing = firstValue

            assertThat(taskAfterProcessing.modificationTime).isAfter(taskBeforeProcessing.modificationTime)
            assertThat(taskAfterProcessing.state).isEqualTo(CallCenterTaskState.FINISHED)
        }
    }

    companion object {
        private val notCollectedOffer = Order().apply { substatus = OrderSubstatus.PACKAGING }
        private val collectedOffer = Order().apply { substatus = OrderSubstatus.READY_TO_SHIP }

        private fun getActualTask() =
            CallCenterTask().apply {
                modificationTime =
                    LocalDateTime.now().minusMinutes(11)
            }

        private fun getNoRetriesLeftActualTask() =
            CallCenterTask().apply {
                tryNumber = 3
                modificationTime =
                    LocalDateTime.now().minusMinutes(11)
            }
    }
}
