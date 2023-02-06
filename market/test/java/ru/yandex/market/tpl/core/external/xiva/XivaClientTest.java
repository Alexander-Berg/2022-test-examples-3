package ru.yandex.market.tpl.core.external.xiva;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.api.model.notification.PushRedirect;
import ru.yandex.market.tpl.api.model.notification.PushRedirectType;
import ru.yandex.market.tpl.common.util.TestUtil;
import ru.yandex.market.tpl.core.config.external.XivaConfiguration;
import ru.yandex.market.tpl.core.domain.push.notification.model.PushNotificationPayload;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushSubscribeRequest;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushSubscriptionPlatform;
import ru.yandex.market.tpl.core.domain.push.subscription.model.PushUnsubscribeRequest;
import ru.yandex.market.tpl.core.external.xiva.model.PushEvent;
import ru.yandex.market.tpl.core.external.xiva.model.PushSendRequest;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author valter
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "external.xiva.url=https://test-xiva.ru",
        "external.xiva.partner.carrier.url=https://test-xiva.ru",
        "external.xiva.service=market-tpl",
        "external.xiva.partner.carrier.service=market-partner-carrier",
        "external.xiva.tokenSend=none_send",
        "external.xiva.tokenListen=none_listen",
        "external.xiva.connectTimeoutMillis=1000",
        "external.xiva.readTimeoutMillis=2000",
        "external.xiva.maxTotal=10",
        "external.xiva.partner.carrier.connectTimeoutMillis=1000",
        "external.xiva.partner.carrier.readTimeoutMillis=2000",
        "external.xiva.partner.carrier.maxTotal=10"
})
@MockBean(classes = TvmClient.class)
@ContextConfiguration(classes = {
        XivaConfiguration.class
})
class XivaClientTest {

    private MockRestServiceServer server;

    @Autowired
    private RestTemplate xivaRestTemplate;

    @Autowired
    private XivaClient xivaClient;

    @BeforeEach
    void initMockServer() {
        server = MockRestServiceServer.bindTo(xivaRestTemplate).build();
    }

    // ResponseCreator.toString has variable representation due to identity hash code
    //  it is excluded from the test name to make the test name stable
    @ParameterizedTest(name = "[{index}] request={0}, response=..., expectedResult={2}")
    @MethodSource("subscribeArguments")
    void subscribe(PushSubscribeRequest request, ResponseCreator serverResponse, boolean expectedResult) {
        server.expect(
                once(),
                requestTo(Matchers.endsWith(
                        "https://test-xiva.ru" +
                                "/v2/subscribe/app?user=" + request.getUid() +
                                "&service=market-tpl" +
                                "&uuid=" + request.getUuid() +
                                "&app_name=" + request.getAppName() +
                                "&platform=" + request.getPlatform().getTransportService()
                ))
        )
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Xiva none_listen"))
                .andExpect(content().string("push_token=" + request.getPushToken()))
                .andRespond(serverResponse);

        assertThat(xivaClient.subscribe(request)).isEqualTo(expectedResult);
    }

    private static List<Arguments> subscribeArguments() {
        ResponseCreator okResponse = withSuccess("OK", MediaType.TEXT_HTML)
                .headers(sendResponseHeaders());
        return List.of(
                Arguments.of(subscribeRequest(), okResponse, true),
                Arguments.of(subscribeRequest(), withServerError(), false),
                Arguments.of(subscribeRequest(), withBadRequest(), false)
        );
    }

    private static PushSubscribeRequest subscribeRequest() {
        long uid = 123234L;
        return new PushSubscribeRequest(
                String.valueOf(uid), "mu_uuid", uid,
                "my_push_token", PushSubscriptionPlatform.FCM, "my_app"
        );
    }

    // ResponseCreator.toString has variable representation due to identity hash code
    //  it is excluded from the test name to make the test name stable
    @ParameterizedTest(name = "[{index}] request={0}, response=..., expectedResult={2}")
    @MethodSource("unsubscribeArguments")
    void unsubscribe(PushUnsubscribeRequest request, ResponseCreator serverResponse, boolean expectedResult) {
        server.expect(
                once(),
                requestTo(Matchers.endsWith(
                        "https://test-xiva.ru" +
                                "/v2/unsubscribe/app?user=" + request.getXivaUserId() +
                                "&service=market-tpl" +
                                "&uuid=" + request.getUuid()
                ))
        )
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Xiva none_listen"))
                .andRespond(serverResponse);

        assertThat(xivaClient.unsubscribe(request)).isEqualTo(expectedResult);
    }

    private static List<Arguments> unsubscribeArguments() {
        ResponseCreator okResponse = withSuccess("OK", MediaType.TEXT_HTML)
                .headers(sendResponseHeaders());
        return List.of(
                Arguments.of(unsubscribeRequest(), okResponse, true),
                Arguments.of(unsubscribeRequest(), withServerError(), false),
                Arguments.of(unsubscribeRequest(), withBadRequest(), false)
        );
    }

    private static PushUnsubscribeRequest unsubscribeRequest() {
        return new PushUnsubscribeRequest("123234L", "mu_uuid");
    }

    // ResponseCreator.toString has variable representation due to identity hash code
    //  it is excluded from the test name to make the test name stable
    @ParameterizedTest(name = "[{index}] request={0}, jsonFile={1}, response=..., expectedResult={3}")
    @MethodSource("sendArguments")
    void send(
            PushSendRequest notification, String jsonFile,
            ResponseCreator serverResponse, boolean expectedResult
    ) {
        String expectedJson = TestUtil.loadResourceAsString(jsonFile);
        server.expect(
                once(),
                requestTo(Matchers.endsWith(
                        "https://test-xiva.ru" +
                                "/v2/send?user=" + notification.getXivaUserId() +
                                "&service=market-tpl" +
                                "&event=" + notification.getEvent() +
                                (notification.getTtlSec() == null ? "" : "&ttl=" + notification.getTtlSec())
                ))
        )
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Xiva none_send"))
                .andExpect(content().json(expectedJson, false))
                .andRespond(serverResponse);

        if (expectedResult) {
            assertThat(xivaClient.send(notification)).isEqualTo("myTransitId");
        } else {
            assertThatThrownBy(() -> xivaClient.send(notification))
                    .isInstanceOf(HttpStatusCodeException.class);
        }
    }

    private static List<Arguments> sendArguments() {
        ResponseCreator okResponse = withSuccess("OK", MediaType.TEXT_HTML)
                .headers(sendResponseHeaders());
        return List.of(
                Arguments.of(
                        notificationWithTitleAndPayload(),
                        "xiva/test_notification_with_title_and_payload.json",
                        okResponse,
                        true
                ),
                Arguments.of(
                        notificationNoTitleAndPayload(),
                        "xiva/test_notification_no_title_and_payload.json",
                        okResponse,
                        true
                ),
                Arguments.of(
                        notificationNoTitleAndPayload(),
                        "xiva/test_notification_no_title_and_payload.json",
                        withServerError(),
                        false
                ),
                Arguments.of(
                        notificationNoTitleAndPayload(),
                        "xiva/test_notification_no_title_and_payload.json",
                        withBadRequest(),
                        false
                ),
                Arguments.of(
                        notificationSkipFcm(),
                        "xiva/test_notification_skip_fcm.json",
                        okResponse,
                        true
                )
        );
    }

    private static HttpHeaders sendResponseHeaders() {
        var headers = new HttpHeaders();
        headers.add("TransitID", "myTransitId");
        return headers;
    }

    private static PushSendRequest notificationWithTitleAndPayload() {
        return new PushSendRequest(
                "myUserId",
                PushEvent.SYSTEM,
                "Иди развози заказы",
                "Работа стоит",
                60,
                new PushNotificationPayload(
                        new PushRedirect(PushRedirectType.ROUTE_POINT_PAGE, 1L, 1L, null)
                ),
                null
        );
    }

    private static PushSendRequest notificationNoTitleAndPayload() {
        return new PushSendRequest(
                "myUserId",
                PushEvent.SYSTEM,
                null,
                "Ты молодец сегодня!",
                null,
                PushNotificationPayload.EMPTY,
                null
        );
    }

    private static PushSendRequest notificationSkipFcm() {
        return new PushSendRequest(
                "myUserId",
                PushEvent.SYSTEM,
                null,
                "Ты молодец сегодня!",
                null,
                PushNotificationPayload.EMPTY,
                true
        );
    }

}
