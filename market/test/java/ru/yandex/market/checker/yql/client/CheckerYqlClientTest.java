package ru.yandex.market.checker.yql.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checker.AbstractWebTest;
import ru.yandex.market.checker.yql.CheckerYqlClient;
import ru.yandex.market.checker.yql.model.OperationDto;
import ru.yandex.market.checker.yql.model.OperationStatus;
import ru.yandex.market.checker.yql.model.ProcessOperationDto;

import static org.assertj.core.api.Assertions.assertThat;

class CheckerYqlClientTest extends AbstractWebTest {

    @Value("${yql.base.url}")
    private String baseUrlFormat;
    @Value("${yql.token}")
    private String yqlToken;

    private YqlClient yqlClient;

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("%s:%s", baseUrlFormat, mockBackEnd.getPort());
        yqlClient = new CheckerYqlClient(getWebClient(), baseUrl, yqlToken);
    }

    @Test
    void submitOperation_shouldReceiveOperationIdWhenGiven() throws JsonProcessingException, InterruptedException {
        OperationDto expectedDto = OperationDto.builder()
                .setId("someId")
                .setStatus(OperationStatus.PENDING)
                .build();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(getObjectMapper().writeValueAsString(expectedDto))
                .addHeader("Content-Type", "application/json")
        );

        ProcessOperationDto requestBody = ProcessOperationDto.builder()
                .setContent("SELECT 2+2")
                .setAction("RUN")
                .setType("SQL")
                .build();
        OperationDto receivedDto = yqlClient.submitOperation(requestBody).block();

        assertThat(receivedDto).isEqualTo(expectedDto);

        RecordedRequest recordedRequest = mockBackEnd.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeaders().get("Authorization")).isEqualTo("OAuth test-token");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v2/operations");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(getObjectMapper().writeValueAsString(requestBody));

    }

    @Test
    void getOperationStatus_shouldReceiveOperationStatus() throws JsonProcessingException, InterruptedException {
        OperationDto expectedDto = OperationDto.builder()
                .setId("someId")
                .setStatus(OperationStatus.COMPLETED)
                .build();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(getObjectMapper().writeValueAsString(expectedDto))
                .addHeader("Content-Type", "application/json")
        );

        OperationDto responseDto = yqlClient.getOperationStatus("someId").block();
        RecordedRequest recordedRequest = mockBackEnd.takeRequest();

        assertThat(responseDto).isEqualTo(expectedDto);
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getHeaders().get("Authorization")).isEqualTo("OAuth test-token");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v2/operations/someId");
    }
}
