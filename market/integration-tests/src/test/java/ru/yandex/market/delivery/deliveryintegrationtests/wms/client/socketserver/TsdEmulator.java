package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.tsdscreens.login.InforSceLoginTsdScreen;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA01;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA02;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver.WsRequestTypeEnum.NSPRFMETA03;

@Resource.Classpath({"wms/socketserver.properties", "wms/infor.properties"})
public class TsdEmulator implements Closeable {
    private final Logger log = LoggerFactory.getLogger("TSD");

    @Property("infor.host")
    private String host;

    @Property("socketserver.sessionTimeout")
    private int sessionTimeout;

    @Property("socketserver.requestTimeout")
    private int requestTimeout;

    private Map<String, String> session;

    private WebsocketConnection wsConnection;

    private AbstractTsdScreen currentScreen;

    public TsdEmulator() {
        PropertyLoader.newInstance().populate(this);

        this.session = new HashMap<>();
        this.session.put("host", host);
        this.wsConnection = new WebsocketConnection("ws://" + host + "/scprd_webrf", sessionTimeout, requestTimeout);
        currentScreen = new InforSceLoginTsdScreen(this);
    }

    public String[] exec(WsRequestTypeEnum wsRequestType, String queryParams) {
        if (wsRequestType == NSPRFMETA01 || wsRequestType == NSPRFMETA02 || wsRequestType == NSPRFMETA03) {
            return null; // try
        }
        for (String name : session.keySet()) {
            queryParams = queryParams.replace("$" + name, session.get(name));
        }

        // генерируем 6-значный id для взаимодействия с WMS
        String id = RandomUtil.randomStringNumbersOnly(6);

        // перед любой операцией сначала PING-PONG
        String response = wsConnection.sendMessage(id + " PING");
        if (!response.equals(id + " PONG")) {
            throw new RuntimeException("PING-PONG operation error (Response: '" + response +
                    "', but expected: '" + id + " PONG')");
        }

        // добавляем этот же id к уже смысловому запросу
        String messageWithId = String.format("%s EXEC %s %s", id, wsRequestType.name(), queryParams);
        log.debug("=========================================================================");
        log.debug("REQUEST: " + messageWithId);
        response = wsConnection.sendMessage(messageWithId);
        String decodedResponse = java.net.URLDecoder.decode(response);

        String[] parts = decodedResponse.split(" ");
        String[] indexAndId = parts[0].split(",");
        if (indexAndId.length != 2 || !indexAndId[1].equals(id)) {
            throw new RuntimeException("Error response: " + response + "\n    on request:" + messageWithId);
        }

        String[] responseContent = parts[1].split(",");
        for (int i = 0; i < responseContent.length; i++) {
            if (!responseContent[i].equals("%s")) {
                responseContent[i] = java.net.URLDecoder.decode(responseContent[i]);
            }
            if (!responseContent[i].isEmpty()) {
                log.debug("response[{}]={}", i, responseContent[i]);
            }
        }
        log.debug("=========================================================================");
        return responseContent;
    }

    public void setSessionValue(String name, Object value) {
        if (session.containsKey(name) && session.get(name).equals(value)) {
            return;
        }

        log.debug(session.containsKey(name) ? "UPDATE VARIABLE IN TSD-SESSION" : "ADD VARIABLE TO TSD-SESSION");

        this.session.put(name, value.toString());

        // print session-content
        log.debug("|| TSD-SESSION VARIABLES:");
        for (String key : session.keySet()) {
            log.debug("||   ${} = {}   {}", key, session.get(key), key.equals(name) ? "- IS NEW" : "");
        }
    }

    public AbstractTsdScreen getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(AbstractTsdScreen currentScreen) {
        this.currentScreen = currentScreen;
        log.debug("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.debug("XXX   CURRENT SCREEN - " + currentScreen.getClass().getSimpleName() + "                     XXX");
        log.debug("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    @Override
    public void close() {
        wsConnection.close();
    }
}
