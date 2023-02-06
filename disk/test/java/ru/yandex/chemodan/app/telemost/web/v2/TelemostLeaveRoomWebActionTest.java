package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.chat.ChatClient;
import ru.yandex.chemodan.app.telemost.chat.ChatClientStub;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.web.TelemostWebActionBaseTest;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.test.Assert;

public class TelemostLeaveRoomWebActionTest extends TelemostWebActionBaseTest {

    private static final boolean USE_STUB = true;

    private final static String TEST_UID = "123";

    @Autowired
    private ChatClient chatClient;

    @Before
    public void initUser() {
        super.before();
        addUsers(Cf.map(Long.parseLong(TEST_UID), UserData.staff("test", Option.of("test"), Option.empty(), Cf.map())));
    }

    @Before
    public void setUp() {
        if (USE_STUB)
            ((ChatClientStub)chatClient).reset();
    }

    @Test
    public void testLeaveRoomAction() throws IOException {
        Map<String, Object> conferenceConnectionData = createConferenceV2(Cf.map("uid", TEST_UID,
                "client_instance_id", "client-instance-1"));
        A3TestHelper helper = getA3TestHelper();
        HttpResponse response = helper.put(String.format("/v2/rooms/%s/leave?peer_id=%s&peer_token=%s&media_session_id=%s",
                conferenceConnectionData.get("room_id"),
                URLEncoder.encode(conferenceConnectionData.get("peer_id").toString(), "UTF-8"),
                URLEncoder.encode(conferenceConnectionData.get("peer_token").toString(), "UTF-8"),
                URLEncoder.encode(conferenceConnectionData.get("media_session_id").toString(), "UTF-8")), null);
        Assert.assertEquals(HttpStatus.SC_200_OK, response.getStatusLine().getStatusCode());
    }

    @Configuration
    public static class Context {
        @Bean
        @Primary
        ChatClient chatClient(ChatClient chatClient) {
            if (USE_STUB)
                return new ChatClientStub();
            return chatClient;
        }
    }
}
