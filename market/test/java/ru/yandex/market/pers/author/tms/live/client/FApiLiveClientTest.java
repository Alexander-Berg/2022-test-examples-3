package ru.yandex.market.pers.author.tms.live.client;

import java.util.Arrays;

import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.author.tms.live.client.model.fapi.YaMessengerChat;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

class FApiLiveClientTest {

    private HttpClient httpClient = mock(HttpClient.class);
    private FApiLiveClient fApiLiveClient =
            new FApiLiveClient("http://target:90", "http://target:90",
                    new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)), "oauth_token");

    @Test
    public void testCreateChat() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/fapi/create_chat_answer.json",
                withQueryParam("name", "resolveCreateChat"));

        YaMessengerChat chat = fApiLiveClient.createChat("test_name", "test_description", Arrays.asList("1234",
                "12345"));

        Assertions.assertEquals("0/24/10083039-c025-4fa7-9b2a-2faee120f363", chat.getChatId());
        Assertions.assertEquals("eaf2a9c7-0b9d-479e-bdba-4c8ceb885bd4", chat.getInviteHash());
    }

    @Test
    public void testRemoveChatMembers() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/fapi/remove_chat_members.json",
                withQueryParam("name", "removeChatMembers"));

        boolean success = fApiLiveClient.removeChatMembers("0/24/10083039-c025-4fa7-9b2a-2faee120f363");
        Assertions.assertTrue(success);
    }

    @Test
    public void testRemoveChatMembersIncorrect() {
        HttpClientMockUtils.mockResponseWithFile(httpClient, "/live/fapi/empty.json",
                withQueryParam("name", "removeChatMembers"));

        boolean success = fApiLiveClient.removeChatMembers("0/24/10083039-c025-4fa7-9b2a-2faee120f363");
        Assertions.assertFalse(success);
    }

}
