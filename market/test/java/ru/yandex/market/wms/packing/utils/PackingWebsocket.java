package ru.yandex.market.wms.packing.utils;

import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao;
import ru.yandex.market.wms.common.spring.pojo.Carton;
import ru.yandex.market.wms.packing.controller.WebSocketStompController;
import ru.yandex.market.wms.packing.dto.CheckParcelResponse;
import ru.yandex.market.wms.packing.dto.CloseDropRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelRequest;
import ru.yandex.market.wms.packing.dto.CloseParcelResponse;
import ru.yandex.market.wms.packing.dto.DropParcelRequest;
import ru.yandex.market.wms.packing.dto.HotContainersResponse;
import ru.yandex.market.wms.packing.dto.MoveCancelledItemRequest;
import ru.yandex.market.wms.packing.dto.MoveItemsToLostRequest;
import ru.yandex.market.wms.packing.dto.MoveItemsToLostResponse;
import ru.yandex.market.wms.packing.dto.MoveToPackedRequest;
import ru.yandex.market.wms.packing.dto.PackingHintsDTO;
import ru.yandex.market.wms.packing.dto.ScanItemRequest;
import ru.yandex.market.wms.packing.dto.ScanItemResponse;
import ru.yandex.market.wms.packing.dto.ScanItemsContainerRequest;
import ru.yandex.market.wms.packing.dto.ScanItemsContainerResponse;
import ru.yandex.market.wms.packing.dto.TaskRequest;
import ru.yandex.market.wms.packing.dto.WebsocketResponse;
import ru.yandex.market.wms.packing.pojo.IdleTablesDto;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.PackingTask;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.CANCEL_ITEM;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.CHECK_PARCEL;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.CLOSE_DROP;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.CLOSE_PARCEL;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.DROP_PARCEL;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.HOT_CONTAINERS;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.LOST;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.MOVE_TO_PACKED;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.SCAN_ITEM;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.SCAN_ITEMS_CONTAINER;
import static ru.yandex.market.wms.packing.dto.WebsocketResponse.Type.TASK;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PackingWebsocket {

    private static final String DESTINATION_FORMAT = "/app/user/%s/tickets/%s";

    private final ObjectMapper mapper;
    private final SerialInventoryDao serialInventoryDao;
    private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(20);

    @SpyBean
    @Autowired
    private WebSocketStompController webSocketStompController;

    @Value("${local.server.port}")
    private int port;

    @Getter
    private String user;
    @Getter
    private PackingTable table;
    private StompSession session;
    private StompSession.Subscription subscription;

    public void connect(String user, PackingTable table) throws Exception {
        this.user = user;
        this.table = table;
        this.session = connect();
        this.subscription = subscribe();
    }

    public void disconnect() throws InterruptedException {
        subscription.unsubscribe();
        session.disconnect();

        Thread.sleep(1000); // иначе disconnect не всегда успевает сработать

        verify(webSocketStompController, atLeastOnce()).subscribeToTickets(anyString(), anyString(), any(),
                any(Principal.class), any(SimpMessageHeaderAccessor.class));
        verify(webSocketStompController, atLeastOnce()).handleUnsubscribe(any(SessionUnsubscribeEvent.class));
        verify(webSocketStompController, atLeastOnce()).handleDisconnect(any(SessionDisconnectEvent.class));
    }

    public PackingTask getTask() {
        return getTask(null);
    }

    public PackingTask getTask(String containerId) {
        send("/app/get-task", new TaskRequest(false, containerId));
        WebsocketResponse<PackingTask> response = poll(queue, new TypeReference<>() { });
        assertThat(response).usingRecursiveComparison()
                .ignoringFields("payload")
                .isEqualTo(WebsocketResponse.task(null));
        return response.getPayload();
    }

    public void getTaskThatNotExists(String containerId) {
        getTaskAndExpectError(containerId, containerId);
    }

    public void getTaskAndExpectError(String containerId, String errorTextPart) {
        send("/app/get-task", new TaskRequest(true, containerId));
        assertError(TASK, errorTextPart);
    }

    public void getTaskWhenNoTasks(IdleTablesDto expectedResponse) {
        send("/app/get-task", new TaskRequest(true, null));
        WebsocketResponse<IdleTablesDto> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.idleTables(expectedResponse));
    }

    public void scanFirstItemIntoParcel(
            long ticketId,
            String uit,
            String expectedCartonId,
            String expectedParcelId,
            boolean shouldCloseParcel
    ) {
        scanFirstItemIntoParcel(
                ticketId,
                uit,
                expectedCartonId,
                expectedParcelId,
                List.of(),
                shouldCloseParcel
        );
    }

    public void scanFirstItemIntoParcel(
            long ticketId,
            String uit,
            String expectedCartonId,
            String expectedParcelId,
            List<PackingHintsDTO> expectedHints,
            boolean shouldCloseParcel) {
        send("/app/scan-item", new ScanItemRequest(ticketId, uit, false));
        WebsocketResponse<ScanItemResponse> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(
                SCAN_ITEM,
                expectedScanItemResponse(
                        uit,
                        expectedCartonId,
                        expectedParcelId,
                        expectedHints,
                        shouldCloseParcel
                )
        ));
    }

    public void scanItemsIntoOpenParcel(long ticketId, Collection<String> uits) {
        for (String uit : uits) {
            send("/app/scan-item", new ScanItemRequest(ticketId, uit, true));
            WebsocketResponse<ScanItemResponse> response = poll(queue, new TypeReference<>() { });
            assertThat(response).isEqualTo(WebsocketResponse.ok(
                    SCAN_ITEM,
                    expectedScanItemResponse(uit, null, null, null, false)
            ));
        }
    }

    private ScanItemResponse expectedScanItemResponse(
            String uit,
            String cartonId,
            String parcelId,
            List<PackingHintsDTO> expectedHints,
            boolean shouldCloseParcel
    ) {
        SerialInventory inventory = serialInventoryDao.findBySerialNumber(uit).orElseThrow();
        return ScanItemResponse.builder()
                .itemInfo(ScanItemResponse.ItemInfo.builder()
                        .uid(inventory.getSerialNumber())
                        .storerKey(inventory.getStorerKey())
                        .sku(inventory.getSku())
                        .lot(inventory.getLot())
                        .loc(inventory.getLoc())
                        .containerId(inventory.getId())
                        .build())
                .cartonId(cartonId)
                .parcelId(parcelId)
                .hints(expectedHints)
                .shouldCloseParcel(shouldCloseParcel)
                .build();
    }

    public void scanCancelledItem(long ticketId, String uit, String containerId) {
        var request = MoveCancelledItemRequest.builder().ticketId(ticketId).uit(uit).containerId(containerId).build();
        send("/app/move-cancelled-item", request);
        WebsocketResponse<Void> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(CANCEL_ITEM, null));
    }

    public void scanCancelledItems(long ticketId, Set<String> uits, String containerId) {
        var request = MoveCancelledItemRequest.builder().ticketId(ticketId).uits(uits).containerId(containerId).build();
        send("/app/move-cancelled-item", request);
        WebsocketResponse<Void> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(CANCEL_ITEM, null));
    }

    public void scanWrongItem(long ticketId, String uit) {
        send("/app/scan-item", new ScanItemRequest(ticketId, uit, false));
        assertError(SCAN_ITEM, uit);
    }

    public ScanItemsContainerResponse scanContainerToUseItAsParcel(long ticketId, String containerId) {
        var request = ScanItemsContainerRequest.builder()
                .ticketId(ticketId).containerId(containerId).hasOrderOpenParcel(false).build();
        send("/app/scan-items-container", request);
        WebsocketResponse<ScanItemsContainerResponse> response = poll(queue, new TypeReference<>() { });
        assertThat(response.getType()).isEqualTo(SCAN_ITEMS_CONTAINER);
        assertThat(response.getPayload().getParcelId()).isNotEmpty();
        assertThat(response.getPayload().getCartonId()).isEqualTo(Carton.NONPACK_TYPE);
        return response.getPayload();
    }

    public void checkParcel(CloseParcelRequest request, CheckParcelResponse expectedResponse) {
        send("/app/check-parcel", request);
        WebsocketResponse<CheckParcelResponse> checkParcelResponse = poll(queue, new TypeReference<>() { });
        assertThat(checkParcelResponse).isEqualTo(WebsocketResponse.ok(CHECK_PARCEL, expectedResponse));
    }

    public void closeParcel(CloseParcelRequest request, CloseParcelResponse expectedResponse) {
        send("/app/close-parcel", request);
        WebsocketResponse<CloseParcelResponse> closeParcelResponse = poll(queue, new TypeReference<>() { });
        assertThat(closeParcelResponse).isEqualTo(WebsocketResponse.ok(CLOSE_PARCEL, expectedResponse));
    }

    public void closeParcelWithError(CloseParcelRequest request) {
        send("/app/close-parcel", request);
        WebsocketResponse<Void> closeParcelResponse = poll(queue, new TypeReference<>() { });
        assertThat(closeParcelResponse.getType()).isEqualTo(CLOSE_PARCEL);
        assertThat(closeParcelResponse.getStatus()).isEqualTo(WebsocketResponse.Status.ERROR);
    }

    public void dropParcel(DropParcelRequest request) {
        send("/app/drop-parcel", request);
        WebsocketResponse<Void> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(DROP_PARCEL, null));
    }

    public void closeDrop(CloseDropRequest request) {
        send("/app/close-drop", request);
        WebsocketResponse<Void> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(CLOSE_DROP, null));
    }

    public void hotContainers(HotContainersResponse expectedResponse) {
        send("/app/get-hot-containers", "");
        WebsocketResponse<HotContainersResponse> hotContainersResponse = poll(queue, new TypeReference<>() { });
        assertThat(hotContainersResponse).isEqualTo(WebsocketResponse.ok(HOT_CONTAINERS, expectedResponse));
    }

    public void hotContainersWhenNoTasks(IdleTablesDto expectedResponse) {
        send("/app/get-hot-containers", "");
        WebsocketResponse<IdleTablesDto> idleResponse = poll(queue, new TypeReference<>() { });
        assertThat(idleResponse).isEqualTo(WebsocketResponse.idleTables(expectedResponse));
    }

    public void moveItemsToLost(Long ticketId, List<String> uits, MoveItemsToLostResponse expectedResponse) {
        send("/app/lost", MoveItemsToLostRequest.builder().ticketId(ticketId).uids(uits).build());
        WebsocketResponse<MoveItemsToLostResponse> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(LOST, expectedResponse));
    }

    public void moveParcelToPackedCell(String parcelId, String packedLoc) {
        send("/app/move-to-packed", MoveToPackedRequest.builder().parcelId(parcelId).packedLoc(packedLoc).build());
        WebsocketResponse<Void> response = poll(queue, new TypeReference<>() { });
        assertThat(response).isEqualTo(WebsocketResponse.ok(MOVE_TO_PACKED, null));
    }

    private StompSession connect() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransports()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        String url = "ws://localhost:" + port + "/packing2/packing-stomp";
        return stompClient
                .connect(url, (WebSocketHttpHeaders) null, getStompHeaders(null), new StompSessionHandlerAdapter() { })
                .get(1, SECONDS);
    }

    private void send(String destination, Object payload) {
        final StompHeaders stompHeaders = getStompHeaders(destination);
        session.send(stompHeaders, payload);
    }

    private StompHeaders getStompHeaders(String destination) {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.setLogin(user);
        return headers;
    }

    private List<Transport> createTransports() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
        return List.of(new WebSocketTransport(new StandardWebSocketClient(container)));
    }

    private String getDestination(String loc) {
        return String.format(DESTINATION_FORMAT, user.toLowerCase(), loc);
    }

    private StompSession.Subscription subscribe() {
        final StompHeaders stompHeaders = getStompHeaders(getDestination(table.getLoc()));
        StompSession.Subscription subscript = session.subscribe(stompHeaders, new TestStompFrameHandler(queue::add));
        WebsocketResponse<Void> tableResponse = poll(queue, new TypeReference<>() { });
        assertThat(tableResponse).isEqualTo(WebsocketResponse.table());
        return subscript;
    }

    @SneakyThrows
    private <T> T poll(BlockingQueue<Object> queue, TypeReference<T> typeRef) {
        return mapper.convertValue(queue.poll(5, SECONDS), typeRef);
    }

    private void assertError(WebsocketResponse.Type type, String errorFragment) {
        WebsocketResponse<Void> actual = poll(queue, new TypeReference<>() { });
        WebsocketResponse<Void> expected = WebsocketResponse.error(type, "");
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "error");
        assertThat(actual.getError()).contains(errorFragment);
    }

    @RequiredArgsConstructor
    private static class TestStompFrameHandler implements StompFrameHandler {
        private final Consumer<Object> consumer;

        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return Map.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            this.consumer.accept(o);
        }
    }
}
