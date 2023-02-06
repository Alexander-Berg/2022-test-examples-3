package ru.yandex.travel.suburban.partners.movista;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.opentracing.mock.MockTracer;
import lombok.SneakyThrows;
import org.asynchttpclient.Dsl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.suburban.exceptions.SuburbanRetryableException;
import ru.yandex.travel.suburban.partners.movista.exceptions.MovistaRequestException;
import ru.yandex.travel.suburban.partners.movista.exceptions.MovistaUnknownException;
import ru.yandex.travel.testing.misc.TestResources;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class TestMovistaClient {
    private static final Logger logger = LoggerFactory.getLogger("movistaClientTest");
    protected MovistaClient client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        var clientWrapper = new AsyncHttpClientWrapper(
                Dsl.asyncHttpClient(Dsl.config().build()),
                logger, "testDestination", new MockTracer(),
                Arrays.stream(DefaultMovistaClient.Endpoint.values()).map(Enum::toString).collect(Collectors.toSet())
        );

        var config = new DefaultMovistaClient.Config(
                String.format("http://localhost:%d", wireMockRule.port()),
                "token42",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        client = new DefaultMovistaClient(clientWrapper, config);
    }

    @Test
    public void testBook() {
        /* Прогоняем один из методов, чтобы убедиться в базовой работоспособности клиента */
        stubFor(post("/api/v1/book").willReturn(aResponse()
                .withBody(TestResources.readResource("BookResponse.json"))));

        var bookReq = MovistaModel.BookRequest.builder()
                .date(LocalDate.of(2021, 10, 22))
                .fromExpressId(1)
                .toExpressId(2)
                .fareId(3)
                .firstName("FirstName")
                .lastName("LastName")
                .middleName("MiddleName")
                .docNumber("DocNumber")
                .docType("russian passport")
                .email("Email")
                .phone("Phone")
                .build();

        MovistaModel.OrderResponse bookResp = client.book(bookReq);
        assertThat(bookResp.getOrderId()).isEqualTo(2122);
        assertThat(bookResp.getStatus()).isEqualTo(MovistaOrderStatus.CREATED);

        verify(postRequestedFor(urlPathEqualTo("/api/v1/book"))
                .withHeader("token", new EqualToPattern("token42", false))
                .withHeader("Content-Type", new EqualToPattern("application/json", true))
                .withRequestBody(equalToJson(TestResources.readResource("BookRequest.json")))
        );

        // проверяем, что магия jackson нигде не сломалась
        checkSerialization(bookReq, MovistaModel.BookRequest.class);
        checkSerialization(bookResp, MovistaModel.OrderResponse.class);
    }

    @Test
    public void testErrorResponseSerialization() {
        MovistaModel.ErrorResponse resp = MovistaModel.ErrorResponse.builder()
                .code(MovistaErrorCode.USER_UNAUTHORIZED)
                .message("some message")
                .result("no auth")
                .rid("rid123")
                .build();

        checkSerialization(resp, MovistaModel.ErrorResponse.class);
    }

    @Test
    public void testRequestError() {
        stubErrorResponse(400, 1016, "Не передан ID заказа");
        assertThatThrownBy(() -> client.book(new MovistaModel.BookRequest()))
                .isInstanceOf(MovistaRequestException.class)
                .hasMessageContaining("Request problem: 400")
                .hasMessageContaining("MovistaErrorCode.ORDER_ID_REQUIRED")
                .hasMessageContaining("Не передан ID заказа");
    }

    @Test
    public void testServerError() {
        stubErrorResponse(503, 1500, "Сервис временно недоступен");
        assertThatThrownBy(() -> client.book(new MovistaModel.BookRequest()))
                .isInstanceOf(SuburbanRetryableException.class)
                .hasMessageContaining("Server problem: 503")
                .hasMessageContaining("MovistaErrorCode.SERVER_ERROR");
    }

    @Test
    public void testNoResponse() {
        stubErrorResponse(502, null, null);
        assertThatThrownBy(() -> client.book(new MovistaModel.BookRequest()))
                .isInstanceOf(SuburbanRetryableException.class)
                .hasMessageContaining("Failed to get response: 502");
    }

    @Test
    public void testUnknownError() {
        stubErrorResponse(500, null, null);
        assertThatThrownBy(() -> client.book(new MovistaModel.BookRequest()))
                .isInstanceOf(MovistaUnknownException.class)
                .hasMessageContaining("Unknown status: 500");
    }

    @Test
    public void testTimeoutException() {
        stubFor(post("/api/v1/book").willReturn(aResponse().withFixedDelay(300000)));
        assertThatThrownBy(() -> client.book(new MovistaModel.BookRequest()))
                .isInstanceOf(SuburbanRetryableException.class)
                .hasMessageContaining("timeout")
                .satisfies(ex -> assertThat(ex.getCause()).isInstanceOf(TimeoutException.class));
    }

    @Test
    public void testIOException() {
        stubFor(post("/api/v1/book").willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        assertThatThrownBy(() -> client.book(new MovistaModel.BookRequest()))
                .isInstanceOf(SuburbanRetryableException.class)
                .hasMessageContaining("io")
                .satisfies(ex -> assertThat(ex.getCause()).isInstanceOf(IOException.class));
    }

    private void stubErrorResponse(Integer status, Integer code, String message) {
        stubFor(post("/api/v1/book").willReturn(aResponse()
                .withStatus(status)
                .withBody(String.format(
                        "{ \"code\": \"%s\", \"message\": \"%s\", \"result\": \"ERROR\", \"rid\": \"123\"}",
                        code, message
                ))
        ));
    }

    @SneakyThrows
    private <T> void checkSerialization(T obj, Class<T> cls) {
        var mapper = DefaultMovistaClient.createObjectMapper();
        String serialized = mapper.writeValueAsString(obj);
        T deserialized = mapper.readValue(serialized, cls);
        assertThat(deserialized).isEqualTo(obj);
    }
}
