package ru.yandex.market.wms.consolidation.modules.consolidation.websocket.after_precons

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseTearDown
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.common.model.enums.OrderType
import ru.yandex.market.wms.common.spring.enums.SortingType
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ContainerInfoResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ItemsCountResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.PutwallOccupiedException
import ru.yandex.market.wms.consolidation.modules.consolidation.model.enum.ConsolidationStep
import ru.yandex.market.wms.consolidation.modules.consolidation.service.PickByLightService.Companion.COLORS
import ru.yandex.market.wms.consolidation.modules.consolidation.websocket.WebSocketIntegrationTest
import ru.yandex.market.wms.pickbylight.client.mock.PickByLightMockClient
import ru.yandex.market.wms.pickbylight.model.ButtonEvent
import ru.yandex.market.wms.pickbylight.model.StationOperation
import ru.yandex.market.wms.pickbylight.model.StationSide
import java.math.BigDecimal

class NormalFlowPickByLightTest : WebSocketIntegrationTest() {

    @Autowired
    @SpyBean
    private lateinit var pickByLightClient: PickByLightMockClient

    @Test
    @DatabaseSetup(value = [
        "/websocket/after-precons/normal/1-container/before.xml",
        "/websocket/enable-pick-by-light.xml",
    ])
    @ExpectedDatabase(
        value = "/websocket/after-precons/normal/1-container/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Normal flow after preconsolidation, single container`() {
        val putwall = "S01"
        val line = "CONS01"
        val waveKey = "W0001"
        val containerId = "PLT1"

        pickByLightClient.stationOperations = listOf(StationOperation(putwall))
        val socket = createSocket().connect("TEST", putwall)
        socket.nextStep(NextStepRequest(putwall), NextStepResponse.line(line, waveKey))

        // scan container
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal(3), waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )

        // scan uit 1
        val scanUit1Request = ScanUitRequest("UIT0001", containerId)
        val scanUit1Response = ScanUitResponse(
            "UIT0001", waveKey, putwall, containerId, "ORD0001", "0001",
            ScanUitResponse.OrderStatus.OK, listOf(), listOf("S01-01", "S01-02", "S01-03"), false
        )
        socket.scanUit(scanUit1Request, scanUit1Response)
        verify(pickByLightClient)
            .switchOn(putwall, StationSide.IN, mapOf(COLORS[1] to listOf("S01-01", "S01-02", "S01-03")))

        // undo scan uit
        socket.undoScanUit()
        verify(pickByLightClient).switchOff(putwall, StationSide.IN, listOf("S01-01", "S01-02", "S01-03"))

        // repeat scan uit 1
        socket.scanUit(scanUit1Request, scanUit1Response)
        verify(pickByLightClient, times(2))
            .switchOn(putwall, StationSide.IN, mapOf(COLORS[1] to listOf("S01-01", "S01-02", "S01-03")))

        // push button
        pickByLightClient.acceptEvent(ButtonEvent(putwall, "S01-01"))
        verify(pickByLightClient, times(2)).switchOff(putwall, StationSide.IN, listOf("S01-01", "S01-02", "S01-03"))
        socket.receiveButtonEvent(ItemsCountResponse(containerId, BigDecimal(2)))

        // scan uit 2
        socket.scanUit(
            ScanUitRequest("UIT0002", containerId),
            ScanUitResponse(
                "UIT0002", waveKey, putwall, containerId, "ORD0002", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf("S01-02", "S01-03"), false
            )
        )
        verify(pickByLightClient).switchOn(putwall, StationSide.IN, mapOf(COLORS[1] to listOf("S01-02", "S01-03")))

        // push button
        pickByLightClient.acceptEvent(ButtonEvent(putwall, "S01-02"))
        verify(pickByLightClient).switchOff(putwall, StationSide.IN, listOf("S01-02", "S01-03"))
        socket.receiveButtonEvent(ItemsCountResponse(containerId, BigDecimal(1)))

        // scan uit 3
        socket.scanUit(
            ScanUitRequest("UIT0003", containerId),
            ScanUitResponse(
                "UIT0003", waveKey, putwall, containerId, "ORD0001", "0002",
                ScanUitResponse.OrderStatus.OK, listOf("S01-01"), listOf(), false
            )
        )
        verify(pickByLightClient).switchOn(
            putwall, StationSide.IN,
            mapOf(COLORS[0] to listOf("S01-01"), COLORS[1] to listOf("S01-03"))
        )

        // push button
        pickByLightClient.acceptEvent(ButtonEvent(putwall, "S01-01"))
        verify(pickByLightClient).switchOff(putwall, StationSide.IN, listOf("S01-01", "S01-03"))
        socket.receiveButtonEvent(ItemsCountResponse(containerId, BigDecimal(0)))

        // container is empty, what is next step?
        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))
        socket.disconnect()
    }

    @Test
    @DatabaseSetup(value = [
        "/websocket/after-precons/normal/1-container/before.xml",
        "/websocket/enable-pick-by-light.xml",
    ])
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Fail to register at occupied putwall`() {
        val putwall = "S01"
        pickByLightClient.stationOperations = listOf(StationOperation(putwall))
        val socket = createSocket().connect("TEST", putwall)
        val socket2 = createSocket().connect("TEST2", putwall, PutwallOccupiedException("TEST2", putwall))
        socket2.disconnect()
        socket.disconnect()
    }
}
