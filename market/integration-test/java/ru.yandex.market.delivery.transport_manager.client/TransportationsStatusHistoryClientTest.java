package ru.yandex.market.delivery.transport_manager.client;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.StatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoRequestDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class TransportationsStatusHistoryClientTest extends AbstractClientTest {

    @Autowired
    private TransportManagerClient transportManagerClient;

    @DisplayName("Получение истории изменения статусов")
    @Test
    void getTransportationsStatusHistory() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/transportations/status-history"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(
                withSuccess(
                    extractFileContent("response/transportations/status_history/success.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        List<TransportationStatusHistoryInfoDto> actual = transportManagerClient.getTransportationsStatusHistory(
            new TransportationStatusHistoryInfoRequestDto()
                .setTransportationIds(List.of(1L, 2L))
                .setGetUnitsHistory(true)
        );

        List<TransportationStatusHistoryInfoDto> expected = getTransportationsStatusHistoryInfo();

        softly.assertThat(actual).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);

    }

    @Nonnull
    private List<TransportationStatusHistoryInfoDto> getTransportationsStatusHistoryInfo() {
        return List.of(
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(1L)
                .setStatusHistoryList(getStatusHistoryInfo(30))
                .setOutboundStatusHistoryList(getUnitStatusHistoryInfo())
                .setMovementStatusHistoryList(getUnitStatusHistoryInfo())
                .setInboundStatusHistoryList(getUnitStatusHistoryInfo()),
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(2L)
                .setStatusHistoryList(getStatusHistoryInfo(31))
        );
    }

    @Nonnull
    private List<StatusHistoryInfoDto> getStatusHistoryInfo(int day) {
        return List.of(
            new StatusHistoryInfoDto()
                .setNewStatus("COULD_NOT_BE_MATCHED")
                .setChangedAt(LocalDateTime.of(2020, 12, day, 14, 56, 40).toInstant(ZoneOffset.UTC)),
            new StatusHistoryInfoDto()
                .setNewStatus("SCHEDULED_WAITING_RESPONSE")
                .setChangedAt(LocalDateTime.of(2020, 12, day, 14, 46, 40).toInstant(ZoneOffset.UTC)),
            new StatusHistoryInfoDto()
                .setNewStatus("SCHEDULED")
                .setChangedAt(LocalDateTime.of(2020, 12, day, 14, 6, 40).toInstant(ZoneOffset.UTC))
        );
    }

    private List<StatusHistoryInfoDto> getUnitStatusHistoryInfo() {
        return List.of(
            new StatusHistoryInfoDto()
                .setNewStatus("NEW")
                .setChangedAt(LocalDateTime.of(2020, 12, 30, 14, 46, 40).toInstant(ZoneOffset.UTC))
        );
    }
}
