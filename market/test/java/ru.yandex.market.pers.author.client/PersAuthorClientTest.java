package ru.yandex.market.pers.author.client;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.author.client.api.dto.AgitationAddRequest;
import ru.yandex.market.pers.author.client.api.dto.AgitationDto;
import ru.yandex.market.pers.author.client.api.model.AgitationType;
import ru.yandex.market.pers.author.client.api.model.AgitationUserType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.DELAY_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.DURATION_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ENTITY_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.FORCE_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.ORDER_ID_KEY;
import static ru.yandex.market.pers.author.client.api.PersAuthorApiConstants.TYPE_KEY;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithFile;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithString;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withBody;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withQueryParam;

/**
 * @author bahus@ / 26.05.2021
 */
public class PersAuthorClientTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final PersAuthorClient client = new PersAuthorClient("http://localhost:1234",
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void testExisted() {
        long userId = 321L;
        mockResponseWithFile(httpClient,
                200,
                "/data/existed_agitations.json",
                and(
                        withMethod(HttpMethod.GET),
                        withPath("/agitation/UID/" + userId + "/existed"),
                        withQueryParam(TYPE_KEY, 0),
                        withQueryParam(ENTITY_ID_KEY, 123),
                        withQueryParam(ENTITY_ID_KEY, 321)
                ));

        List<AgitationDto> agitations =
                client.getExistedUserAgitationsByUid(userId, AgitationType.MODEL_GRADE, List.of(123L, 321L));
        Assertions.assertEquals(2, agitations.size());
        Assertions.assertEquals(Map.of("some", "thing"), agitations.get(0).getData());
        Assertions.assertEquals(Map.of("some", "other thing"), agitations.get(1).getData());
    }

    @Test
    public void testGetAgitationsBeruByOrder() {
        long userId = 321L;
        long orderId = 123L;
        mockResponseWithFile(httpClient,
                200,
                "/data/get_agitations_by_order.json",
                and(
                        withMethod(HttpMethod.GET),
                        withPath("/agitation/beru/UID/" + userId + "/by-order/" + orderId),
                        withQueryParam(TYPE_KEY, 9),
                        withQueryParam(TYPE_KEY, 13)
                ));

        List<AgitationDto> agitations = client.getAgitationBeruByOrder(
                AgitationUserType.UID,
                Long.toString(userId),
                orderId,
                List.of(AgitationType.ORDER_FEEDBACK, AgitationType.ORDER_CANCELLATION_REJECTED_BY_SHOP),
                1, 10)
                .getData();
        Assertions.assertEquals(2, agitations.size());
        Assertions.assertTrue(agitations.stream().anyMatch(a -> a.getType() == AgitationType.ORDER_FEEDBACK.value()));
        Assertions.assertTrue(agitations.stream().anyMatch(a -> a.getType() == AgitationType.ORDER_CANCELLATION_REJECTED_BY_SHOP.value()));
        agitations.forEach(a -> Assertions.assertEquals(Long.toString(orderId), a.getEntityId()));
        Assertions.assertEquals(Map.of("some", "thing"), agitations.get(0).getData());
        Assertions.assertEquals(Map.of("some", "other thing"), agitations.get(1).getData());
    }

    @Test
    public void testAddOrderAgitation() throws IOException {
        String userId = "321";
        AgitationUserType userType = AgitationUserType.UID;
        long orderId = 1L;
        AgitationType agitationType = AgitationType.ORDER_DELIVERY_DATES_MOVED_BY_SHOP;
        boolean force = true;
        String exprectedPath = "/agitation/order/UID/321";

        mockResponseWithString(httpClient, "OK");

        client.addOrderAgitation(userType, userId, orderId, agitationType, force);

        ArgumentMatcher<HttpUriRequest> requestQueryMatcher = and(
                withMethod(HttpMethod.POST),
                withPath(exprectedPath),
                withQueryParam(TYPE_KEY, agitationType.value()),
                withQueryParam(ORDER_ID_KEY, orderId),
                withQueryParam(FORCE_KEY, force)
        );
        verify(httpClient).execute(argThat(requestQueryMatcher), any(HttpContext.class));
    }

    @Test
    public void testAddOrderAgitationWithDelayAndDuration() throws IOException {
        String userId = "321";
        AgitationUserType userType = AgitationUserType.UID;
        long orderId = 1L;
        AgitationType agitationType = AgitationType.ORDER_DELIVERY_DATES_MOVED_BY_SHOP;
        boolean force = true;
        String exprectedPath = "/agitation/order/UID/321";
        Long delay = 60L;
        Long duration = 31536000L;

        mockResponseWithString(httpClient, "OK");

        client.addOrderAgitation(userType, userId, orderId, agitationType, force, delay, duration);

        ArgumentMatcher<HttpUriRequest> requestQueryMatcher = and(
                withMethod(HttpMethod.POST),
                withPath(exprectedPath),
                withQueryParam(TYPE_KEY, agitationType.value()),
                withQueryParam(ORDER_ID_KEY, orderId),
                withQueryParam(DELAY_KEY, "PT60S"),
                withQueryParam(DURATION_KEY, "PT31536000S")
        );
        verify(httpClient).execute(argThat(requestQueryMatcher), any(HttpContext.class));
    }

    @Test
    public void testAddOrderAgitationWithDelayDurationAndData() throws IOException {
        String userId = "321";
        AgitationUserType userType = AgitationUserType.UID;
        long orderId = 1L;
        AgitationType agitationType = AgitationType.ORDER_DELIVERY_DATES_MOVED_BY_SHOP;
        boolean force = true;
        String exprectedPath = "/agitation/order/UID/321";
        Long delay = 60L;
        Long duration = 31536000L;
        Map<String, String> data = Collections.singletonMap("event", "12345678");

        mockResponseWithString(httpClient, "OK");

        client.addOrderAgitation(userType, userId, orderId, agitationType, data, force, delay, duration);

        ArgumentMatcher<HttpUriRequest> requestQueryMatcher = and(
                withMethod(HttpMethod.POST),
                withPath(exprectedPath),
                withQueryParam(TYPE_KEY, agitationType.value()),
                withQueryParam(ORDER_ID_KEY, orderId),
                withQueryParam(DELAY_KEY, "PT60S"),
                withQueryParam(DURATION_KEY, "PT31536000S"),
                withBody(new AgitationAddRequest(data), AgitationAddRequest.class)
        );
        verify(httpClient).execute(argThat(requestQueryMatcher), any(HttpContext.class));
    }
}
