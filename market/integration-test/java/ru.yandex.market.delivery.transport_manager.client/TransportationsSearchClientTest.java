package ru.yandex.market.delivery.transport_manager.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class TransportationsSearchClientTest extends AbstractClientTest {
    @Autowired
    private TransportManagerClient transportManagerClient;

    @Test
    @DisplayName("Поиск перемещений")
    void searchTransportations() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/transportations/search")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/transportations/search/by_everything.json"))
            .andExpect(queryParam("size", "10"))
            .andExpect(queryParam("page", "0"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/transportations/search/by_everything.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        TransportationSearchFilter filter = TransportationSearchFilter.builder()
            .outboundPartnerIds(Set.of(123L))
            .outboundDateFrom(LocalDate.of(2021, 3, 2))
            .outboundDateTo(LocalDate.of(2021, 3, 4))
            .outboundStatuses(Set.of(TransportationUnitStatus.SENT))
            .movementExcludePartnerIds(Set.of(234L))
            .build();

        Page<TransportationSearchDto> actual = transportManagerClient.searchTransportations(
            filter,
            new PageRequest(0, 10)
        );

        TransportationSearchDto searchDto = (TransportationSearchDto) new TransportationSearchDto()
            .setId(1L)
            .setOutbound(TransportationsFactory.newOutboundUnit(123L, "Partner"))
            .setInbound(TransportationsFactory.newInboundUnit())
            .setMovement(TransportationsFactory.newMovement(123L, "Partner"));

        Page<TransportationSearchDto> expected = new Page<TransportationSearchDto>()
            .setData(
                List.of(
                    searchDto
                        .setScheme(TransportationScheme.UNKNOWN)
                        .setDeleted(false)
                        .setHash("")
                        .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT)
                        .setTransportationType(TransportationType.ORDERS_OPERATION)
                        .setPlannedLaunchTime(LocalDateTime.of(2021, 3, 2, 12, 0))
                )
            )
            .setTotalPages(1)
            .setPage(0)
            .setTotalElements(1)
            .setSize(10);

        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
