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
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.MoveCancelledItemRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanCellRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.OrderCancelledException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.OrderCancelledForUitException
import ru.yandex.market.wms.consolidation.modules.consolidation.model.enum.ConsolidationStep
import ru.yandex.market.wms.consolidation.modules.consolidation.websocket.WebSocketIntegrationTest
import java.math.BigDecimal

class CancelledOrderTest : WebSocketIntegrationTest() {

    @Test
    @DatabaseSetup("/websocket/after-precons/cancelled/with-pd/before.xml")
    @ExpectedDatabase(
        value = "/websocket/after-precons/cancelled/with-pd/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Normal order and cancelled order with pickdetails`() {
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
                containerId, BigDecimal(2), waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )

        // scan uit 1, there is alive order for the SKU
        socket.scanUit(
            ScanUitRequest("UIT0001", containerId),
            ScanUitResponse(
                "UIT0001", waveKey, putwall, containerId, "ORD0001", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf(), false
            )
        )
        socket.scanCell(
            ScanCellRequest("UIT0001", containerId, "ORD0001", "0001", "S01-01"),
            ItemsCountResponse(containerId, BigDecimal(1))
        )

        // scan uit 2, same SKU, but there is only cancelled order for it
        socket.scanUit(
            ScanUitRequest("UIT0002", containerId),
            ScanUitResponse(
                "UIT0002", waveKey, putwall, containerId, "ORD0002", "0001",
                ScanUitResponse.OrderStatus.CANCELLED, listOf(), listOf(), false
            )
        )
        socket.scanCellWithError(
            ScanCellRequest("UIT0002", containerId, "ORD0002", "0001", "S01-02"),
            OrderCancelledException("ORD0002")
        )
        socket.moveCancelledItem(
            MoveCancelledItemRequest("UIT0002", containerId, "CART123"),
            ItemsCountResponse(containerId, BigDecimal.ZERO)
        )

        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))
        socket.disconnect()
    }

    @Test
    @DatabaseSetup("/websocket/after-precons/cancelled/without-pd/before.xml")
    @DatabaseTearDown(value = ["/websocket/tear-down.xml"], type = DatabaseOperation.DELETE_ALL)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `Normal order and cancelled order without pickdetails`() {
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
                containerId, BigDecimal(2), waveKey, OrderType.BATCH_ORDER.name,
                SortingType.SORT, putwall, line
            )
        )

        // scan uit 1, there is alive order for the SKU
        socket.scanUit(
            ScanUitRequest("UIT0001", containerId),
            ScanUitResponse(
                "UIT0001", waveKey, putwall, containerId, "ORD0001", "0001",
                ScanUitResponse.OrderStatus.OK, listOf(), listOf(), false
            )
        )
        socket.scanCell(
            ScanCellRequest("UIT0001", containerId, "ORD0001", "0001", "S01-01"),
            ItemsCountResponse(containerId, BigDecimal(1))
        )

        // scan uit 2, same SKU, but without pickdetails
        socket.scanUit(
            ScanUitRequest("UIT0002", containerId),
            ScanUitResponse(
                "UIT0002", "", "", containerId, "", "",
                ScanUitResponse.OrderStatus.CANCELLED, listOf(), listOf(), false
            )
        )
        socket.scanCellWithError(
            ScanCellRequest("UIT0002", containerId, "ORD0002", "0001", "S01-02"),
            OrderCancelledForUitException("UIT0002")
        )

        socket.moveCancelledItem(
            MoveCancelledItemRequest("UIT0002", containerId, "CART123"),
            ItemsCountResponse(containerId, BigDecimal.ZERO)
        )

        socket.nextStep(NextStepRequest(putwall, waveKey, line), NextStepResponse(ConsolidationStep.PUTWALL))
        socket.disconnect()
    }
}
