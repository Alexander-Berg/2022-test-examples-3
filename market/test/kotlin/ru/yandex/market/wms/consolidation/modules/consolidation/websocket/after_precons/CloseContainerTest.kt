package ru.yandex.market.wms.consolidation.modules.consolidation.websocket.after_precons

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseTearDown
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.common.model.enums.OrderType
import ru.yandex.market.wms.common.spring.enums.SortingType
import ru.yandex.market.wms.consolidation.core.async.MoveToLostProducer
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.CloseContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ContainerInfoResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.model.enum.ConsolidationStep
import ru.yandex.market.wms.consolidation.modules.consolidation.websocket.WebSocketIntegrationTest
import java.math.BigDecimal

class CloseContainerTest : WebSocketIntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var moveToLostProducer: MoveToLostProducer

    @Test
    @DatabaseSetup("/websocket/after-precons/close-container/before.xml")
    @ExpectedDatabase(
        value = "/websocket/after-precons/close-container/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Close container with 1 item when shorts disabled`() {
        val putwall = "S01"
        val line = "CONS01"
        val waveKey = "W0001"
        val containerId = "PLT1"
        val socket = createSocket().connect("TEST", putwall)
        socket.nextStep(NextStepRequest(putwall), NextStepResponse.line(line, waveKey))

        // scan container
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal.ONE, waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )

        socket.closeContainer(CloseContainerRequest(containerId, line, putwall))

        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))
        socket.disconnect()

        Mockito.verifyNoInteractions(moveToLostProducer)
    }

    @Test
    @DatabaseSetup(value = [
        "/websocket/after-precons/close-container/before.xml",
        "/websocket/after-precons/close-container/enable-shorts.xml",
    ])
    @ExpectedDatabase(
        value = "/websocket/after-precons/close-container/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Close container with 1 item when shorts enabled`() {
        val putwall = "S01"
        val line = "CONS01"
        val waveKey = "W0001"
        val containerId = "PLT1"
        val socket = createSocket().connect("TEST", putwall)
        socket.nextStep(NextStepRequest(putwall), NextStepResponse.line(line, waveKey))

        // scan container
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal.ONE, waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )

        socket.closeContainer(CloseContainerRequest(containerId, line, putwall))

        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))
        socket.disconnect()

        Mockito.verify(moveToLostProducer).produce(putwall, containerId, "TEST")
        Mockito.verifyNoMoreInteractions(moveToLostProducer)
    }
}
