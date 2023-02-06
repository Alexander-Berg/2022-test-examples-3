package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class WebsocketConnection implements Closeable {
    private final Logger log = LoggerFactory.getLogger("WS");

    private String uri;
    private WebSocketContainer container;

    private Session userSession = null;

    private String request = null;
    private boolean requestIsPing = false;
    private String response = null;
    private boolean isWaitingResponse = false;

    public WebsocketConnection(String uri, int sessionTimeout, int requestTimeout) {
        log.info("Connecting to {}, sessionTimeout={}, requestTimeout={}", uri, sessionTimeout, requestTimeout);
        this.uri = uri;
        try {
            container = ContainerProvider.getWebSocketContainer(); // open websocket
            container.setDefaultMaxSessionIdleTimeout(sessionTimeout);
            container.setAsyncSendTimeout(requestTimeout);
            container.connectToServer(this, new URI(uri));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        log.debug("opening websocket (timeout = {})", userSession.getMaxIdleTimeout());
        this.userSession = userSession;
        if (request != null) {
            userSession.getAsyncRemote().sendText(request);
        }
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        log.warn("closing websocket. Reason: " + reason);
        this.userSession = null;
        // TODO may be try to reconnect?
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("onMessage: {}", message.length() > 50 ? message.substring(0, 50) : message);

        if (message.startsWith("maxframesize") || message.startsWith("{\"ns\":\"org.jwebsocket.plugins.system\",")) {
            return;
        }

        // если запрос был PING, то пропускаем все сообщения пока не будет PONG
        if (requestIsPing && !message.endsWith(" PONG")) {
            return;
        }

        if (isWaitingResponse) {
            response = message;
            isWaitingResponse = false;
        }
    }

    /**
     * Запрос к веб-сокету с ожиданием ответа.
     *
     * @param message
     */
    public synchronized String sendMessage(String message) {
        log.info("SEND: " + message);
        request = message;

        requestIsPing = message.endsWith(" PING");

        isWaitingResponse = true;
        userSession.getAsyncRemote().sendText(message);
        long startTime = System.currentTimeMillis();
        try {
            while (userSession != null) {
                if (response != null) {
                    String temp = response;
                    request = null;
                    response = null;
                    log.info("Processing time = {} sec", (System.currentTimeMillis() - startTime) / 1000.0);
                    return temp;
                }
                Thread.sleep(1);
            }
            System.out.println("TIMEOUT");
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void close() {
        if (userSession != null && userSession.isOpen()) {
            try {
                userSession.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        userSession = null;
        container = null;
    }
}
