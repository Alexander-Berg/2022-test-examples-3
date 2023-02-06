package ru.yandex.chemodan.app.telemost.services;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.telemost.TelemostBaseContextTest;
import ru.yandex.chemodan.xiva.XivaSecretSign;

public class WebsocketUrlServiceTest extends TelemostBaseContextTest {

    @Autowired
    private WebsocketUriService websocketUriService;

    @Test
    public void testUriBuilding() throws URISyntaxException {
        XivaSecretSign secretSign = new XivaSecretSign("sign-1", "ts-1");
        String userId = "user-1";
        String sessionId = "session-1";
        String uriString = websocketUriService.buildWebsocketUri(secretSign, userId, sessionId);
        Assert.assertNotNull(uriString);
        URI uri = new URI(uriString);
        Assert.assertEquals("wss", uri.getScheme());
        Assert.assertEquals("push-sandbox.yandex.ru", uri.getHost());
        Assert.assertEquals("/v2/subscribe/websocket", uri.getPath());
        MapF<String, String> parameters = Cf.list(uri.getQuery().split("&"))
                .map(part -> part.split("=")).toMap(parts -> parts[0], parts -> parts[1]);
        Assert.assertEquals(userId, parameters.getOrElse("user", ""));
        Assert.assertEquals("telemost", parameters.getOrElse("client", ""));
        Assert.assertEquals(sessionId, parameters.getOrElse("session", ""));
        Assert.assertEquals(secretSign.getSign(), parameters.getOrElse("sign", ""));
        Assert.assertEquals(secretSign.getTs(), parameters.getOrElse("ts", ""));
    }
}
