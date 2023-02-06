package ru.yandex.market.billing.pro;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.market.billing.pro.model.EventDto;
import ru.yandex.market.billing.pro.model.EventResponse;
import ru.yandex.market.billing.pro.model.ProBalanceDto;
import ru.yandex.market.billing.pro.model.ProBalanceResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ParametersAreNonnullByDefault
public class ProApiServiceTest {
    private MockWebServer apiServer;
    private ProApiService proApiService;

    @BeforeEach
    void setUp() throws IOException {
        apiServer = new MockWebServer();
        apiServer.start();
        WebClient webClient = WebClient.create("http://localhost:" + apiServer.getPort());
        proApiService = new ProApiService(webClient, webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        apiServer.close();
    }

    @Test
    void checkEventSending() {
        apiServer.enqueue(successfulResponse(getEventResponseBody()));
        EventDto event = getEvent();

        EventResponse response = proApiService.sendEvent(event, "1").block();
        EventResponse expectedResponse = new EventResponse("121");

        assertAll(
                () -> assertThat(apiServer.getRequestCount()).isEqualTo(1),
                () -> {
                    var request = apiServer.takeRequest();
                    var expectedBody = String.format(
                            "{\"profile_id\":\"%s\",\"title\":\"%s\",\"deeplink\":\"%s\",\"event_date\":\"%s\"}",
                            event.getProfileId(), event.getTitle(), event.getDeeplink(), event.getEventDate()
                    );

                    assertAll(
                            () -> assertThat(request.getPath()).isEqualTo("/events/v1?external_id=1"),
                            () -> assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT.name()),
                            () -> assertThat(request.getBody().readUtf8()).isEqualTo(expectedBody)
                    );
                },
                () -> assertEquals(expectedResponse, response)
        );
    }

    @Test
    void checkProBalanceSending() {
        apiServer.enqueue(successfulResponse(getProBalanceResponseBody()));
        ProBalanceDto proBalanceDto = getProBalanceDto();

        ProBalanceResponse response = proApiService.sendPaidAmount(List.of(proBalanceDto)).block();

        assertAll(
                () -> assertThat(apiServer.getRequestCount()).isEqualTo(1),
                () -> {
                    var request = apiServer.takeRequest();
                    String expectedBody = "{\"orders\":[{\"data\":{\"template_entries\":[{\"context\":{\"amount" +
                            "\":\"1000\",\"currency\":\"RUB\",\"event_at\":\"2022-04-05\",\"park_id\":\"4040\"," +
                            "\"contractor_profile_id\":\"3030\",\"external_ref\":\"10\",\"firm_id\":1115555}," +
                            "\"template_name\":\"templateName321\"}],\"event_version\":1,\"schema_version\":\"3\"," +
                            "\"topic_begin_at\":\"2022-04-04\"},\"event_at\":\"2022-04-05\",\"external_ref\":\"10\"," +
                            "\"kind\":\"somekind\",\"topic\":\"/market/billing/courier\"}]}";

                    assertAll(
                            () -> assertThat(request.getPath()).isEqualTo("/v2/process/async"),
                            () -> assertThat(request.getMethod()).isEqualTo(HttpMethod.POST.name()),
                            () -> assertThat(request.getBody().readUtf8()).isEqualTo(expectedBody)
                    );
                },
                () -> assertEquals(getExpectedProBalanceResponse(), response)
        );
    }

    private EventDto getEvent() {
        return new EventDto("profile123", "title123", "deeplink123", "2022-04-05");
    }

    private ProBalanceDto getProBalanceDto() {
        return new ProBalanceDto(
                new ProBalanceDto.Data(
                        List.of(
                                new ProBalanceDto.TemplateEntry(
                                        new ProBalanceDto.TemplateContext(
                                                "1000",
                                                "RUB",
                                                "2022-04-05",
                                                "4040",
                                                "3030",
                                                "10",
                                                1115555L
                                        ),
                                        "templateName321"
                                )
                        ),
                        1,
                        "3",
                        "2022-04-04"
                ),
                "2022-04-05",
                "10",
                "somekind",
                "/market/billing/courier"
        );
    }

    @Nonnull
    private MockResponse successfulResponse(String body) {
        return new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body);
    }

    private String getEventResponseBody() {
        return "{\"id\": \"121\"}";
    }

    private String getProBalanceResponseBody() {
        return "{\"orders\":[{\"topic\":\"/market/billing/courier\",\"external_ref\":\"10\",\"doc_id\":999333}]}";
    }

    private ProBalanceResponse getExpectedProBalanceResponse() {
        return new ProBalanceResponse(
                List.of(
                        new ProBalanceResponse.PaidAmountResponse(
                                "/market/billing/courier",
                                "10",
                                999333
                        )
                )
        );
    }
}
