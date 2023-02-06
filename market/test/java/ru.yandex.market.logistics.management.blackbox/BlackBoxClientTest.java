package ru.yandex.market.logistics.management.blackbox;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.util.client.HttpTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Если вызов BlackBoxClient.getProfileInfo() возвращает UserTicket, то тикет попадает в RequestContext")
public class BlackBoxClientTest {

    private static final String TEST_USER_TICKET = "test-user-ticket";
    private static final String TEST_SESSION_ID_COOKIE_NAME = "sessionid";
    private static final String TEST_SESSION_ID = "test-session-id";
    private static final String IP = "192.168.0.1";
    private static final String HOST = "host";
    private static final String CONSUMER = "consumer";
    private static final Map<String, String> SESSION_COOKIES = Map.of(TEST_SESSION_ID_COOKIE_NAME, TEST_SESSION_ID);

    private BlackBoxProperties blackBoxProperties = new BlackBoxProperties();
    private Map<String, Set<String>> params = new HashMap<>();

    @Mock
    private HttpTemplate httpTemplate;

    @BeforeEach
    void before() {
        blackBoxProperties.setHost(HOST);
        blackBoxProperties.setConsumer(CONSUMER);
        params.put("userip", Set.of(IP));
        params.put("host", Set.of(HOST));
        params.put("method", Set.of("sessionid"));
        params.put("consumer", Set.of(CONSUMER));
        params.put("format", Set.of("json"));
        params.put("get_user_ticket", Set.of("true"));
        params.put(TEST_SESSION_ID_COOKIE_NAME, Set.of(TEST_SESSION_ID));
    }

    @Test
    @DisplayName("Если пришел юзер тикет, то проверяем что он попадает в BlackBoxProfile")
    void userTicketTest() {
        BlackBoxResponse blackBoxResponse = createBlackBoxResponse(TEST_USER_TICKET);
        when(httpTemplate.executeGet(eq(BlackBoxResponse.class), eq(params))).thenReturn(blackBoxResponse);

        BlackBoxClient blackBoxClient = new BlackBoxClient(blackBoxProperties, httpTemplate);
        BlackBoxProfile blackBoxProfile = blackBoxClient.getProfileInfo(IP, SESSION_COOKIES);
        assertThat(blackBoxProfile.getUserTicket()).isEqualTo(TEST_USER_TICKET);
    }

    @Test
    @DisplayName("Если не пришел юзер тикет, то ничего не должно сломаться")
    void noUserTicketTest() {
        BlackBoxResponse blackBoxResponse = createBlackBoxResponse(null);
        when(httpTemplate.executeGet(eq(BlackBoxResponse.class), eq(params))).thenReturn(blackBoxResponse);

        BlackBoxClient blackBoxClient = new BlackBoxClient(blackBoxProperties, httpTemplate);
        BlackBoxProfile blackBoxProfile = blackBoxClient.getProfileInfo(IP, SESSION_COOKIES);
        assertThat(blackBoxProfile.getUserTicket()).isNull();
    }

    @Test
    @DisplayName("Если не запрашиваем юзер тикет, то ничего не должно сломаться")
    void notNeedUserTicketTest() {
        blackBoxProperties.setGetUserTicketDisabled(true);
        params.put("get_user_ticket", Set.of("false"));
        BlackBoxResponse blackBoxResponse = createBlackBoxResponse(null);
        when(httpTemplate.executeGet(eq(BlackBoxResponse.class), eq(params))).thenReturn(blackBoxResponse);

        BlackBoxClient blackBoxClient = new BlackBoxClient(blackBoxProperties, httpTemplate);
        BlackBoxProfile blackBoxProfile = blackBoxClient.getProfileInfo(IP, SESSION_COOKIES);
        assertThat(blackBoxProfile.getUserTicket()).isNull();
    }

    private BlackBoxResponse createBlackBoxResponse(String userTicket) {
        return new BlackBoxResponse(
            1,
            1,
            "",
            "",
            new Status(),
            new Uid("1", false, false, new HashMap<>()),
            "login",
            false,
            false,
            new Karma(),
            new KarmaStatus(),
            new Auth(),
            "",
            userTicket
        );
    }
}
