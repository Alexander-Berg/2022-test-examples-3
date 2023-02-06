package ru.yandex.market.wms.consolidation.modules.consolidation.websocket.after_precons

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseTearDown
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.common.model.enums.OrderType
import ru.yandex.market.wms.common.spring.enums.SortingType
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ContainerInfoResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ItemsCountResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.PutWallLineDto
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanCellRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.CellOccupiedException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.NoBatchOrdersInContainerException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.UitNotFoundException
import ru.yandex.market.wms.consolidation.modules.consolidation.model.enum.ConsolidationStep
import ru.yandex.market.wms.consolidation.modules.consolidation.websocket.WebSocketIntegrationTest
import java.math.BigDecimal

class NormalFlowTest : WebSocketIntegrationTest() {

    @Test
    @DatabaseSetup("/websocket/after-precons/normal/1-container/before.xml")
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
        val socket = createSocket().connect("TEST", putwall)
        socket.nextStep(NextStepRequest(putwall), NextStepResponse.line(line, waveKey))

        // scan container
        socket.getContainerInfoWithError(
            ScanContainerRequest(putwall, line, "XYZ", waveKey),
            NoBatchOrdersInContainerException("XYZ")
        )
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal(3), waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )

        // scan uit 1
        socket.scanUitWithError(ScanUitRequest("XYZ", containerId), UitNotFoundException("XYZ"))
        val scanUit1Req = ScanUitRequest("UIT0001", containerId)
        val scanUit1Resp = ScanUitResponse(
            "UIT0001", waveKey, putwall, containerId, "ORD0001", "0001",
            ScanUitResponse.OrderStatus.OK, listOf(), listOf("S01-01", "S01-02", "S01-03"), false
        )
        socket.scanUit(scanUit1Req, scanUit1Resp)
        socket.undoScanUit()
        socket.scanUit(scanUit1Req, scanUit1Resp)

        socket.scanCell(
            ScanCellRequest("UIT0001", containerId, "ORD0001", "0001", "S01-01"),
            ItemsCountResponse(containerId, BigDecimal(2))
        )

        // scan uit 2
        socket.scanUitWithError(ScanUitRequest("234t42g433", containerId), UitNotFoundException("234t42g433"))
        socket.scanUit(
            ScanUitRequest("UIT0002", containerId),
            ScanUitResponse(
                "UIT0002", waveKey, putwall, containerId, "ORD0002", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf("S01-02", "S01-03"), false
            )
        )
        var scanCellRequest = ScanCellRequest("UIT0002", containerId, "ORD0002", "0001", "S01-02")
        socket.scanCellWithError(
            scanCellRequest.copy(cell = "S01-01"),
            CellOccupiedException("S01-01", "ORD0001", "UIT0002")
        )
        socket.scanCell(scanCellRequest, ItemsCountResponse(containerId, BigDecimal("1.0")))

        // scan uit 3
        socket.scanUitWithError(ScanUitRequest("4363636", containerId), UitNotFoundException("4363636"))
        socket.scanUit(
            ScanUitRequest("UIT0003", containerId),
            ScanUitResponse(
                "UIT0003", waveKey, putwall, containerId, "ORD0001", "0002",
                ScanUitResponse.OrderStatus.OK, listOf("S01-01"), listOf(), false
            )
        )
        scanCellRequest = ScanCellRequest("UIT0003", containerId, "ORD0001", "0002", "S01-01")
        socket.scanCellWithError(
            scanCellRequest.copy(cell = "S01-02"),
            CellOccupiedException("S01-02", "ORD0002", "UIT0003")
        )
        socket.scanCell(scanCellRequest, ItemsCountResponse(containerId, BigDecimal.ZERO))

        // container is empty, what is next step?
        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))
        socket.disconnect()
    }

    @Test
    @DatabaseSetup("/websocket/after-precons/normal/2-containers-same-line/before.xml")
    @ExpectedDatabase(
        value = "/websocket/after-precons/normal/2-containers-same-line/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Normal flow after preconsolidation, 2 containers in the same line`() {
        val putwall = "S01"
        val line = "CONS01"
        val waveKey = "W0001"
        var containerId = "PLT1"
        val socket = createSocket().connect("TEST", putwall)
        socket.nextStep(NextStepRequest(putwall), NextStepResponse.line(line, waveKey))

        // scan container 1
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal.ONE, waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )
        socket.scanUit(
            ScanUitRequest("UIT0001", containerId),
            ScanUitResponse(
                "UIT0001", waveKey, putwall, containerId, "ORD0001", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf(), false
            )
        )
        socket.scanCell(
            ScanCellRequest("UIT0001", containerId, "ORD0001", "0001", "S01-01"),
            ItemsCountResponse(containerId, BigDecimal.ZERO)
        )
        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.CONTAINER))

        // scan container 2
        containerId = "PLT2"
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal.ONE, waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )
        socket.scanUit(
            ScanUitRequest("UIT0002", containerId),
            ScanUitResponse(
                "UIT0002", waveKey, putwall, containerId, "ORD0002", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf(), false
            )
        )
        socket.scanCell(
            ScanCellRequest("UIT0002", containerId, "ORD0002", "0001", "S01-02"),
            ItemsCountResponse(containerId, BigDecimal.ZERO)
        )
        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))

        socket.disconnect()
    }

    @Test
    @DatabaseSetup("/websocket/after-precons/normal/2-containers-dif-line/before.xml")
    @ExpectedDatabase(
        value = "/websocket/after-precons/normal/2-containers-dif-line/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Normal flow after preconsolidation, 2 containers in different lines`() {
        val putwall = "S01"
        var line = "CONS01"
        var waveKey = "W0001"
        var containerId = "PLT1"
        val socket = createSocket().connect("TEST", putwall)
        socket.nextStep(NextStepRequest(putwall), NextStepResponse.line(line, waveKey))

        // scan container 1
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal.ONE, waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )
        socket.scanUit(
            ScanUitRequest("UIT0001", containerId),
            ScanUitResponse(
                "UIT0001", waveKey, putwall, containerId, "ORD0001", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf(), false
            )
        )
        socket.scanCell(
            ScanCellRequest("UIT0001", containerId, "ORD0001", "0001", "S01-01"),
            ItemsCountResponse(containerId, BigDecimal.ZERO)
        )
        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse.line("CONS02", "W0002"))

        // scan container 2
        line = "CONS02"
        waveKey = "W0002"
        containerId = "PLT2"
        socket.getContainerInfo(
            ScanContainerRequest(putwall, line, containerId, waveKey),
            ContainerInfoResponse(
                containerId, BigDecimal.ONE, waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )
        socket.scanUit(
            ScanUitRequest("UIT0002", containerId),
            ScanUitResponse(
                "UIT0002", waveKey, putwall, containerId, "ORD0002", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf(), false
            )
        )
        socket.scanCell(
            ScanCellRequest("UIT0002", containerId, "ORD0002", "0001", "S01-02"),
            ItemsCountResponse(containerId, BigDecimal.ZERO)
        )
        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))

        socket.disconnect()
    }
}
