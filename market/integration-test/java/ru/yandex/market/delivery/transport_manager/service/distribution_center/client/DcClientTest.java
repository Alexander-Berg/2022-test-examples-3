package ru.yandex.market.delivery.transport_manager.service.distribution_center.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.AxaptaMovementRequestDto;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.document.SendDocumentsBody;
import ru.yandex.market.delivery.transport_manager.dto.distribution_center.document.SendDocumentsBodyResultStatus;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class DcClientTest extends AbstractContextualTest {

    protected MockRestServiceServer mockServer;

    @Autowired
    private RestTemplate dcClientRestTemplate;

    @Autowired
    private DcClient dcClient;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(dcClientRestTemplate);
    }

    @Test
    void sendDocuments() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(header("X-Ya-Service-Ticket", "test-service-ticket"))
            .andExpect(header("X-Ya-User-Ticket", "test-user-ticket"))
            .andExpect(requestTo("https://dc-host/TM/sortingCenterPoint/2/outbounds/3/docs"))
            .andExpect(jsonRequestContent("client/distribution_center/send_documents.json"))
            .andRespond(withSuccess());

        SendDocumentsBody body = body(1L, SendDocumentsBodyResultStatus.SUCCESS, List.of("a", "b"));
        dcClient.sendDocuments(body, 2L, "3");
    }

    @Test
    @SneakyThrows
    void sendAxaptaMovementRequestId() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(header("X-Ya-Service-Ticket", "test-service-ticket"))
            .andExpect(header("X-Ya-User-Ticket", "test-user-ticket"))
            .andExpect(requestTo(
                "https://dc-host/TM/sortingCenterPoint/402/inbounds/37012311/axaptaRequestId?axaptaRequestId=" +
                URLEncoder.encode("Зпер123", StandardCharsets.UTF_8)
            ))
            .andRespond(withSuccess());

        dcClient.sendAxaptaMovementRequestId(
            new AxaptaMovementRequestDto()
                .setAxaptaMovementOrderId("Зпер123")
                .setInboundYandexId("37012311"),
            402L
        );
    }

    private static SendDocumentsBody body(Long registryId, SendDocumentsBodyResultStatus status, List<String> docs) {
        return new SendDocumentsBody()
            .setRegistryId(registryId)
            .setStatus(status)
            .setDocs(docs);
    }
}
