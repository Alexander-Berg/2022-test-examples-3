package ru.yandex.market.billing.bcl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import one.util.streamex.StreamEx;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static ru.yandex.market.billing.bcl.BclApiService.RAIFFEISEN_BIK;

@ParametersAreNonnullByDefault
class BclApiServiceTest {
    public static final RecursiveComparisonConfiguration CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoredFields("id", "statusLastChange").build();

    private MockWebServer bclApi;
    private BclApiService service;

    @BeforeEach
    void setUp() throws IOException {
        bclApi = new MockWebServer();
        bclApi.start();
        service = new BclApiService(WebClient.create("http://localhost:" + bclApi.getPort()));
    }

    @AfterEach
    void tearDown() throws IOException {
        bclApi.close();
    }

    @Test
    void sendCreditorsToCheck_singlePerson() {
        var creditor = defaultCreditorInfo(1);
        var creditors = List.of(creditor);

        bclApi.enqueue(successfulResponse(creditor));

        var fetchedClients = service.sendCreditorsToCheck(RAIFFEISEN_BIK, creditors);

        assertAll(
                () -> assertThat(bclApi.getRequestCount()).isEqualTo(1),
                () -> {
                    var request = bclApi.takeRequest();
                    var expectedBody = String.format(
                            "{\"bik\":\"%s\",\"name\":\"%s\",\"inn\":\"%s\",\"ogrn\":\"%s\",\"kpp\":\"%s\"}",
                            RAIFFEISEN_BIK, creditor.getFullName(), creditor.getInn(), creditor.getOgrn(),
                            creditor.getKpp()
                    );
                    assertAll(
                            () -> assertThat(request.getPath()).isEqualTo("/proxy/creditors/"),
                            () -> assertThat(request.getMethod()).isEqualTo(HttpMethod.POST.name()),
                            () -> assertThat(request.getBody().readUtf8()).isEqualTo(expectedBody)
                    );
                },
                () -> assertThat(fetchedClients)
                        .usingRecursiveFieldByFieldElementComparator(CONFIGURATION)
                        .containsExactlyInAnyOrder(getCreditorStateDto(creditor))
        );
    }

    @Test
    void sendCreditorsToCheck_singlePerson_repeatIfError() {
        var creditor = defaultCreditorInfo(1);
        var creditors = List.of(creditor);

        bclApi.enqueue(errorResponse(500));
        bclApi.enqueue(errorResponse(404));
        bclApi.enqueue(successfulResponse(creditor));

        var fetchedClients = service.sendCreditorsToCheck(RAIFFEISEN_BIK, creditors);

        assertAll(
                () -> assertThat(bclApi.getRequestCount()).isEqualTo(3),
                () -> assertThat(fetchedClients)
                        .usingRecursiveFieldByFieldElementComparator(CONFIGURATION)
                        .containsExactlyInAnyOrder(getCreditorStateDto(creditor))
        );
    }

    @Nonnull
    private MockResponse errorResponse(int i) {
        return new MockResponse().setResponseCode(i);
    }

    @Test
    void sendCreditorsToCheck_multiplePersons() {
        var suffererId = 3;
        var creditors = List.of(
                defaultCreditorInfo(1),
                defaultCreditorInfo(suffererId),
                defaultCreditorInfo(5)
        );

        bclApi.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                JsonNode bodyJson = null;
                try {
                    bodyJson = new ObjectMapper().readTree(request.getBody().readUtf8());
                } catch (IOException e) {
                    Assertions.fail(e.getMessage());
                }
                var innInRequest = bodyJson.get("inn").asText();

                // при отправке страдальца всегда получаем ошибку
                if (innInRequest.equals(inn(suffererId))) {
                    return errorResponse(500);
                }

                var sentPerson = creditors.stream()
                        .filter(p -> innInRequest.equals(p.getInn()))
                        .findFirst()
                        .orElseThrow();

                return successfulResponse(sentPerson);
            }
        });

        var fetchedClients = service.sendCreditorsToCheck(RAIFFEISEN_BIK, creditors);

        var expectedResult = StreamEx.of(creditors)
                .filter(p -> !inn(suffererId).equals(p.getInn()))
                .map(this::getCreditorStateDto)
                .toList();
        assertAll(
                // по одному запросу на везунчиков, и 4 - на страдальца
                () -> assertThat(bclApi.getRequestCount()).isEqualTo(6),
                () -> assertThat(fetchedClients)
                        .usingRecursiveFieldByFieldElementComparator(CONFIGURATION)
                        .containsExactlyInAnyOrderElementsOf(expectedResult)
        );
    }

    @Test
    void sendCreditorsToCheck_singlePerson_resending() {
        var creditor = defaultCreditorInfo(1);

        bclApi.enqueue(resendingResponse());

        var fetchedClients = service.sendCreditorsToCheck(RAIFFEISEN_BIK, List.of(creditor));

        assertAll(
                () -> assertThat(bclApi.getRequestCount()).isEqualTo(1),
                () -> assertThat(fetchedClients)
                        .usingRecursiveFieldByFieldElementComparator(CONFIGURATION)
                        .containsExactlyInAnyOrder(getCreditorStateDto(creditor))
        );
    }

    @Nonnull
    private MockResponse successfulResponse(CreditorInfo sentCreditor) {
        return new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(buildSuccessfulResponse(sentCreditor));
    }

    private MockResponse resendingResponse() {
        return new MockResponse()
                .setResponseCode(500)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(buildResendingResponse());
    }

    private static String inn(long personId) {
        return "770435790" + personId;
    }

    private CreditorInfo defaultCreditorInfo(long personId) {
        return new CreditorInfo("Test " + personId, inn(personId), "116774649139" + personId, personId + "123");
    }

    private CreditorStateDto getCreditorStateDto(CreditorInfo creditor) {
        return CreditorStateDto.builder()
                .setId("")
                .setName(creditor.getFullName())
                .setInn(creditor.getInn())
                .setOgrn(creditor.getOgrn())
                .setStatus(CreditorStatus.FETCHED)
                .setStatusLastChange("")
                .build();
    }

    private String buildSuccessfulResponse(CreditorInfo creditor) {
        return String.format(
                "{\"meta\": {\"built\": \"2021-10-20T23:49:12.091\"}, \"data\": %s, \"errors\": []}",
                buildFetchedCreditorJson(creditor)
        );
    }

    private static String buildFetchedCreditorJson(CreditorInfo creditor) {
        return String.format(
                "{\"id\": \"%s\", \"fullNameRus\": \"%s\", \"inn\": %s, \"ogrn\": %s, \"state\": {" +
                        "\"status\": \"FETCHED\", \"statusDate\": \"2021-10-21T12:12:12.111111\"" +
                        "}}",
                UUID.randomUUID(), creditor.getFullName(), creditor.getInn(), creditor.getOgrn()
        );
    }

    private static String buildResendingResponse() {
        var message = String.format(
                "Such combination of inn, ogrn and fullNameRus is already exists for creditor with id: %s",
                UUID.randomUUID()
        );
        return String.format(
                "{\"meta\": {\"built\": \"2021-10-20T23:49:12.091\"}, \"data\": {}, \"errors\": [%s]}",
                "{\"message\":\"" + message + "\"}"
        );
    }
}
