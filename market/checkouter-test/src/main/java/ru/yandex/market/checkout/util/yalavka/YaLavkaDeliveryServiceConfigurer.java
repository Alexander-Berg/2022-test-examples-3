package ru.yandex.market.checkout.util.yalavka;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ru.yandex.market.common.taxi.model.DeliveryOptionsCheckRequest;
import ru.yandex.market.common.taxi.model.DeliveryOptionsCheckResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
public class YaLavkaDeliveryServiceConfigurer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private WireMockServer yaLavkaDeliveryServiceMock;

    private static MappingBuilder getCheckDeliveryOptionsRequestBuilder() {
        return getPostRequestBuilder("/platform/requests/deferred/check");
    }

    private static MappingBuilder getOrderReservationRequestBuilder() {
        return getPostRequestBuilder("/platform/requests/add_simple");
    }

    private static MappingBuilder getOrderCancellationRequestBuilder() {
        return getPostRequestBuilder("/api/platform/requests/cancel");
    }

    private static MappingBuilder getPostRequestBuilder(@Nonnull String url) {
        return post(urlPathEqualTo(url))
                .withHeader("Authorization", equalTo("beru-employer"))
                .withHeader("Content-Type", equalTo("application/json"));
    }

    public DeliveryOptionsCheckRequest getCheckRequestBody() {
        return getRequestBody("/platform/requests/deferred/check", DeliveryOptionsCheckRequest.class);
    }

    private <T> T getRequestBody(String url, Class<T> clazz) {
        try {
            return MAPPER.readValue(
                    yaLavkaDeliveryServiceMock.getAllServeEvents().stream()
                            .map(ServeEvent::getRequest)
                            .filter(request -> Objects.equals(request.getUrl(), url))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Request to URL " + url + " not found"))
                            .getBody(),
                    clazz
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ServeEvent> getAllInteractions() {
        return yaLavkaDeliveryServiceMock.getAllServeEvents();
    }

    public void configureUnsuccessfulCheckDeliveryOptionsRequest() throws Exception {
        MappingBuilder requestBuilder = getCheckDeliveryOptionsRequestBuilder();
        yaLavkaDeliveryServiceMock.stubFor(requestBuilder.willReturn(
                new ResponseDefinitionBuilder()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        ));
    }

    public void configureCheckDeliveryOptionsRequestWithDelay(int delay,
                                                              @Nonnull Integer... availableIntervals) throws Exception {
        MappingBuilder requestBuilder = getCheckDeliveryOptionsRequestBuilder();
        ResponseDefinitionBuilder responseDefinitionBuilder = new ResponseDefinitionBuilder()
                .withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(MAPPER.writeValueAsBytes(new DeliveryOptionsCheckResponse(Set.of(availableIntervals))));
        if (delay > 0) {
            responseDefinitionBuilder.withFixedDelay(delay);
        }
        yaLavkaDeliveryServiceMock.stubFor(requestBuilder.willReturn(responseDefinitionBuilder));
    }

    public void configureCheckDeliveryOptionsRequest(@Nonnull Integer... availableIntervals) throws Exception {
        configureCheckDeliveryOptionsRequestWithDelay(0, availableIntervals);
    }

    public void configureOrderReservationRequest(@Nonnull HttpStatus httpStatus) {
        configureOrderReservationRequest(httpStatus, null);
    }

    public void configureOrderReservationRequest(@Nonnull HttpStatus httpStatus, @Nullable String body) {
        MappingBuilder requestBuilder = getOrderReservationRequestBuilder();
        yaLavkaDeliveryServiceMock.stubFor(requestBuilder.willReturn(
                new ResponseDefinitionBuilder()
                        .withStatus(httpStatus.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(body)
        ));
    }

    public void configureReserveCancellationRequest(@Nonnull HttpStatus httpStatus) {
        MappingBuilder requestBuilder = getOrderCancellationRequestBuilder();
        yaLavkaDeliveryServiceMock.stubFor(requestBuilder.willReturn(
                new ResponseDefinitionBuilder()
                        .withStatus(httpStatus.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        ));
    }

    public void reset() {
        yaLavkaDeliveryServiceMock.resetAll();
    }

    public void assertNoInteractions() {
        assertThat(yaLavkaDeliveryServiceMock.getAllServeEvents(), empty());
    }

    public void assertNoCheckRequests() {
        assertFalse(hasRequest("/platform/requests/deferred/check"));
    }

    public void assertHasCheckRequests() {
        assertHasRequests("/platform/requests/deferred/check");
    }

    public void assertHasOrderReserveRequests() {
        assertHasRequests("/platform/requests/add_simple");
    }

    public void assertHasReserveCancellationRequests() {
        assertHasRequests("/api/platform/requests/cancel");
    }

    public void assertNoReserveCancellationRequests() {
        assertFalse(hasRequest("/api/platform/requests/cancel"));
    }

    public void assertHasRequests(String expectedUrl) {
        assertTrue(hasRequest(expectedUrl));
    }

    private boolean hasRequest(String expectedUrl) {
        return yaLavkaDeliveryServiceMock.getAllServeEvents().stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getUrl)
                .collect(Collectors.toSet())
                .contains(expectedUrl);
    }
}
