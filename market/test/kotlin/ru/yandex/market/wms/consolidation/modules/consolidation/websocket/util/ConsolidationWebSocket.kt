package ru.yandex.market.wms.consolidation.modules.consolidation.websocket.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.BigDecimalComparator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.CloseContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ContainerInfoResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ItemsCountResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.MoveCancelledItemRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.NextStepResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanCellRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanContainerRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitRequest
import ru.yandex.market.wms.consolidation.modules.consolidation.controller.ScanUitResponse
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.ConsolidationException
import ru.yandex.market.wms.consolidation.modules.consolidation.exception.WmsErrorDto
import ru.yandex.market.wms.consolidation.modules.consolidation.model.WebSocketMessage
import ru.yandex.market.wms.trace.request.RequestId
import ru.yandex.market.wms.trace.request.RequestUtils
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ConsolidationWebSocket {
    companion object {
        const val TIMEOUT_SECONDS: Long = 10
    }

    @Value("\${local.server.port}")
    private var port = 0

    @Autowired
    private lateinit var mapper: ObjectMapper

    internal val queue = LinkedBlockingQueue<JsonNode>()
    private val handler = WebSocketHandler()
    private val client = WebSocketStompClient(SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient()))))
        .apply { messageConverter = MappingJackson2MessageConverter() }

    private lateinit var user: String
    private lateinit var session: StompSession
    private lateinit var subscription: StompSession.Subscription

    private val url get() = "ws://localhost:$port/consolidation/ws"

    fun connect(user: String, putwall: String, ex: ConsolidationException? = null): ConsolidationWebSocket {
        this.user = user
        this.session = client.connect(url, null as WebSocketHttpHeaders?, stompHeaders(null), handler)
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        this.subscription = subscribe(putwall, ex)
        return this
    }

    fun disconnect() {
        subscription.unsubscribe()
        session.disconnect()
    }

    private fun subscribe(putwall: String, ex: ConsolidationException?): StompSession.Subscription {
        val subscription = session.subscribe(stompHeaders("/app/user/cons/putwall/$putwall"), handler)
        val type = WebSocketMessage.Type.PUTWALL
        receive(ex?.let { WebSocketMessage.error(type, WmsErrorDto.of(it)) } ?: WebSocketMessage.ok(type))
        return subscription
    }

    fun getContainerInfo(request: ScanContainerRequest, expectedPayload: ContainerInfoResponse) = send(
        "/app/cons/get-container-info", request,
        WebSocketMessage.ok(WebSocketMessage.Type.SCAN_CONTAINER, expectedPayload)
    )

    fun getContainerInfoWithError(request: ScanContainerRequest, ex: ConsolidationException) = send(
        "/app/cons/get-container-info", request,
        WebSocketMessage.error(WebSocketMessage.Type.SCAN_CONTAINER, WmsErrorDto.of(ex))
    )

    fun scanUit(request: ScanUitRequest, expectedPayload: ScanUitResponse) = send(
        "/app/cons/scan-uit", request,
        WebSocketMessage.ok(WebSocketMessage.Type.SCAN_UIT, expectedPayload)
    )

    fun scanUitWithError(request: ScanUitRequest, ex: ConsolidationException) = send(
        "/app/cons/scan-uit", request,
        WebSocketMessage.error(WebSocketMessage.Type.SCAN_UIT, WmsErrorDto.of(ex))
    )

    fun undoScanUit() = send(
        "/app/cons/undo-scan-uit", "",
        WebSocketMessage.ok(WebSocketMessage.Type.UNDO_SCAN_UIT)
    )

    fun scanCell(request: ScanCellRequest, expectedPayload: ItemsCountResponse) = send(
        "/app/cons/scan-putwall-cell", request,
        WebSocketMessage.ok(WebSocketMessage.Type.SCAN_CELL, expectedPayload)
    )

    fun scanCellWithError(request: ScanCellRequest, ex: ConsolidationException) = send(
        "/app/cons/scan-putwall-cell", request,
        WebSocketMessage.error(WebSocketMessage.Type.SCAN_CELL, WmsErrorDto.of(ex))
    )

    fun receiveButtonEvent(expectedPayload: ItemsCountResponse) = receive(
        WebSocketMessage.ok(WebSocketMessage.Type.PUSH_BUTTON, expectedPayload)
    )

    fun nextStep(request: NextStepRequest, expectedPayload: NextStepResponse) = send(
        "/app/cons/next-step", request,
        WebSocketMessage.ok(WebSocketMessage.Type.NEXT_STEP, expectedPayload)
    )

    fun moveCancelledItem(request: MoveCancelledItemRequest, expectedPayload: ItemsCountResponse) = send(
        "/app/cons/move-cancelled-item", request,
        WebSocketMessage.ok(WebSocketMessage.Type.MOVE_CANCELLED_ITEM, expectedPayload)
    )

    fun closeContainer(request: CloseContainerRequest) = send(
        "/app/cons/close-container", request,
        WebSocketMessage.ok(WebSocketMessage.Type.CLOSE_CONTAINER)
    )

    private inline fun <reified T> send(
        destination: String, payload: Any = "", expectedResponse: WebSocketMessage<T>
    ): WebSocketMessage<T> {
        session.send(stompHeaders(destination), payload)
        return receive(expectedResponse)
    }

    private inline fun <reified T> receive(expectedResponse: WebSocketMessage<T>): WebSocketMessage<T> {
        val response = pollQueue<T>()
        assertThat(response)
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
        return response
    }

    private inline fun <reified T> pollQueue(): WebSocketMessage<T> = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        ?.let { mapper.convertValue(it, object : TypeReference<WebSocketMessage<T>>() {}) }
        ?: throw IllegalStateException("Could not poll message from queue")

    private fun stompHeaders(destination: String?) = StompHeaders().apply {
        this.destination = destination
        this.login = user
        this[RequestUtils.REQUEST_ID_HEADER] = RequestId.generateBase()
    }

    inner class WebSocketHandler : StompSessionHandlerAdapter() {

        override fun getPayloadType(headers: StompHeaders): Type = JsonNode::class.java

        override fun handleFrame(headers: StompHeaders, payload: Any?) {
            queue.add(payload as JsonNode)
        }

        override fun handleException(
            session: StompSession,
            command: StompCommand?,
            headers: StompHeaders,
            payload: ByteArray,
            exception: Throwable
        ) {
            exception.printStackTrace()
        }

        override fun handleTransportError(session: StompSession, exception: Throwable) {
            exception.printStackTrace()
        }
    }
}

