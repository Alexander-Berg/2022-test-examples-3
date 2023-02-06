package ru.yandex.market.delivery.transport_manager.client;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.TransportationCreationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class TransportationsClientTest extends AbstractClientTest {

    @Autowired
    private TransportManagerClient transportManagerClient;

    private final TransportationDto expected = new TransportationDto()
        .setId(1L)
        .setOutbound(TransportationsFactory.newOutboundUnit(123L, "Partner 1"))
        .setInbound(TransportationsFactory.newInboundUnitWithRegisters())
        .setMovement(TransportationsFactory.newMovement(456L, "Partner 3"));

    @DisplayName("Получение перемещения")
    @Test
    void getTransportation() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/transportations/1"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/transportations/get_transportation.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        TransportationDto actual = transportManagerClient.getTransportation(1L)
            .orElseThrow(IllegalStateException::new);

        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @DisplayName("Получение несуществующего перемещения")
    @Test
    void getNonexistentTransportation() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/transportations/2"))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/transportations/transportation_not_found.json"))
            );

        Optional<TransportationDto> transportation = transportManagerClient.getTransportation(2L);

        softly.assertThat(transportation).isEmpty();
    }

    @Test
    void createTransportation() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/transportations/create"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/transportations/create/create_request.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/transportations/get_transportation.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        TransportationCreationDto creationDto = new TransportationCreationDto()
            .setPartnerFromId(11L)
            .setPartnerToId(12L)
            .setPointFromId(1L)
            .setPointToId(2L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setTransportId(777L)
            .setOutboundStart(LocalDateTime.of(2021, 10, 20, 13, 0))
            .setOutboundEnd(LocalDateTime.of(2021, 10, 20, 15, 0));

        TransportationDto actual = transportManagerClient.createTransportation(creationDto);

        softly.assertThat(actual).isEqualTo(expected);
    }
}
