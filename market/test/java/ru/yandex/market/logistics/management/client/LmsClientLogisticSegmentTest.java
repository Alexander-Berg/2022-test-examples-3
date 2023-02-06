package ru.yandex.market.logistics.management.client;

import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.BaseLogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentCreateDto;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentSequenceFilter;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentUpdateDto;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.SearchDropshipLogisticMovementsFilter;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.UpdateDropshipLogisticMovementRequest;
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceCreateDto;
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceUpdateDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticMovement;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.EdgesFrozen;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("CRUD и поиск логистических сегментов через клиент LMS")
class LmsClientLogisticSegmentTest extends AbstractClientTest {

    private static final String URI = "/externalApi/logistic-segments";
    private static final String URI_SEARCH = URI + "/search";
    private static final String URI_SEARCH_PAGED = URI + "/search-paged";
    private static final String URI_SEARCH_SEQUENCE = URI + "/searchSequence";
    private static final String URI_SEARCH_DROPSHIP_LOGISTIC_MOVEMENTS = URI + "/search/dropship-logistic-movements";
    private static final String URI_SEARCH_PAGED_DROPSHIP_LOGISTIC_MOVEMENTS =
        URI + "/search-paged/dropship-logistic-movements";
    private static final String URI_DROPSHIP_LOGISTIC_MOVEMENT = URI + "/dropship-logistic-movement";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Запрос на поиск сегмента")
    void searchLogisticSegments() throws JsonProcessingException {
        LogisticSegmentFilter filter = LogisticSegmentFilter.builder()
            .setIds(Set.of(10001L, 10002L, 10005L, 10006L, 10007L))
            .setLogisticsPointIds(Set.of(101L, 102L))
            .setPartnerIds(Set.of(1L, 3L))
            .setTypes(Set.of(LogisticSegmentType.WAREHOUSE, LogisticSegmentType.HANDING))
            .build();

        mockServer.expect(requestTo(uri + URI_SEARCH))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/search_response.json"))
            );

        List<LogisticSegmentDto> actualResponse = client.searchLogisticSegments(filter);

        softly.assertThat(actualResponse)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                new LogisticSegmentDto()
                    .setId(10002L)
                    .setType(LogisticSegmentType.MOVEMENT)
                    .setLocationId(1002)
                    .setPartnerId(2L)
                    .setServices(List.of())
                    .setNextSegmentIds(List.of(10003L))
                    .setPreviousSegmentIds(List.of(10001L))
            );
    }

    @Test
    @DisplayName("Запрос на постраничный поиск сегмента")
    void searchLogisticSegmentsPaged() throws JsonProcessingException {
        LogisticSegmentFilter filter = LogisticSegmentFilter.builder()
            .setIds(Set.of(10001L, 10002L, 10005L, 10006L, 10007L))
            .setLogisticsPointIds(Set.of(101L, 102L))
            .setPartnerIds(Set.of(1L, 3L))
            .setTypes(Set.of(LogisticSegmentType.WAREHOUSE, LogisticSegmentType.HANDING))
            .build();

        mockServer.expect(requestTo(startsWith(uri + URI_SEARCH_PAGED)))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam("size", "2"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/search_response_paged.json"))
            );

        PageResult<LogisticSegmentDto> actualResponse = client.searchLogisticSegments(filter, new PageRequest(0, 2));

        softly.assertThat(actualResponse)
            .usingRecursiveComparison()
            .isEqualTo(
                new PageResult<LogisticSegmentDto>()
                    .setData(List.of(
                        new LogisticSegmentDto()
                            .setId(10002L)
                            .setType(LogisticSegmentType.MOVEMENT)
                            .setLocationId(1002)
                            .setPartnerId(2L)
                            .setServices(List.of())
                            .setNextSegmentIds(List.of(10003L))
                            .setPreviousSegmentIds(List.of(10001L))
                    ))
                    .setPage(0)
                    .setSize(2)
                    .setTotalPages(1)
                    .setTotalElements(1)
            );
    }

    @Test
    @DisplayName("Запрос на поиск последовательности сегментов")
    void searchLogisticSegmentsSequence() throws JsonProcessingException {
        BaseLogisticSegmentFilter filter1 = BaseLogisticSegmentFilter.builder().setIds(Set.of(10001L)).build();
        BaseLogisticSegmentFilter filter2 = BaseLogisticSegmentFilter.builder().setIds(Set.of(10002L)).build();
        LogisticSegmentSequenceFilter sequenceFilter = LogisticSegmentSequenceFilter.builder().segmentSequence(
            List.of(filter1, filter2)
        ).build();
        mockServer.expect(requestTo(getBuilder(uri, URI_SEARCH_SEQUENCE).toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(sequenceFilter)))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/search_sequence_response.json"))
            );

        softly.assertThatCode(() -> client.searchLogisticSegmentsSequence(sequenceFilter)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Создание логистического сегмента")
    void createLogisticSegment() throws JsonProcessingException {
        LogisticSegmentCreateDto request = LogisticSegmentCreateDto.newBuilder()
            .name("Сегмент тестовый из теста в тест, чтобы потестировать, потестировать и ещё раз потестировать")
            .type(LogisticSegmentType.WAREHOUSE)
            .partnerId(2L)
            .logisticPointId(102L)
            .locationId(213)
            .edgesFrozen(EdgesFrozen.AUTO)
            .services(List.of(
                LogisticServiceCreateDto.newBuilder()
                    .code(ServiceCodeName.PROCESSING)
                    .status(ActivityStatus.ACTIVE)
                    .duration(10)
                    .schedule(Set.of(
                        new ScheduleDayResponse(null, 1, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 2, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 3, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 4, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 5, LocalTime.of(10, 0), LocalTime.of(19, 0), true)
                    ))
                    .calendarId(1000L)
                    .frozen(true)
                    .build()
            ))
            .build();

        mockServer.expect(requestTo(getBuilder(uri, URI).toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/create_response.json"))
            );

        softly.assertThat(client.createLogisticSegment(request).getId())
            .as("Asserting that the created logistic segment id is valid")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Обновление логистического сегмента")
    void updateLogisticSegment() throws JsonProcessingException {
        LogisticSegmentUpdateDto request = LogisticSegmentUpdateDto.newBuilder()
            .id(1L)
            .name("Сегмент тестовый из теста в тест, чтобы потестировать, потестировать и ещё раз потестировать")
            .logisticPointId(102L)
            .locationId(225)
            .edgesFrozen(EdgesFrozen.AUTO)
            .services(List.of(
                LogisticServiceUpdateDto.newBuilder()
                    .id(1L)
                    .status(ActivityStatus.ACTIVE)
                    .duration(10)
                    .price(10)
                    .schedule(Set.of(
                        new ScheduleDayResponse(null, 1, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 2, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 3, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 4, LocalTime.of(10, 0), LocalTime.of(19, 0), true),
                        new ScheduleDayResponse(null, 5, LocalTime.of(10, 0), LocalTime.of(17, 0), true)
                    ))
                    .calendarId(1000L)
                    .frozen(false)
                    .build()
            ))
            .build();

        mockServer.expect(requestTo(getBuilder(uri, URI).toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/update_response.json"))
            );

        softly.assertThat(client.updateLogisticSegment(request).getId())
            .as("Asserting that the updated logistic segment id is valid")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Удаление логистического сегмента")
    void deleteLogisticSegment() {
        mockServer.expect(requestTo(getBuilder(uri, URI + "/1").toUriString()))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> client.deleteLogisticSegment(1L))
            .as("Asserting that no exceptions are thrown")
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Поиск информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов")
    void searchDropshipLogisticMovements() throws JsonProcessingException {
        SearchDropshipLogisticMovementsFilter filter = SearchDropshipLogisticMovementsFilter.builder()
            .fromPartnerIds(Set.of(1L))
            .toPartnerIds(Set.of(2L))
            .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
            .status(ActivityStatus.ACTIVE)
            .attributes(Set.of(
                SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
            ))
            .build();

        mockServer.expect(requestTo(uri + URI_SEARCH_DROPSHIP_LOGISTIC_MOVEMENTS))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/search_dropship_logistic_movements.json"))
            );

        List<LogisticMovement> actualResponse = client.searchDropshipLogisticMovements(filter);

        softly.assertThat(actualResponse)
            .as("Asserting that the response contains a list of valid logistic movements")
            .containsExactly(getLogisticMovement());
    }

    @Test
    @DisplayName(
        "Постраничный поиск информации о логистических перемещениях (связях партнёров в новой модели) из дропшипов"
    )
    void searchDropshipLogisticMovementsPaged() throws JsonProcessingException {
        SearchDropshipLogisticMovementsFilter filter = SearchDropshipLogisticMovementsFilter.builder()
            .fromPartnerIds(Set.of(1L))
            .toPartnerIds(Set.of(2L))
            .toPartnerTypes(EnumSet.of(PartnerType.SORTING_CENTER))
            .status(ActivityStatus.ACTIVE)
            .attributes(Set.of(
                SearchDropshipLogisticMovementsFilter.Attribute.SHIPMENT_SCHEDULE,
                SearchDropshipLogisticMovementsFilter.Attribute.CUTOFF_TIME
            ))
            .build();

        mockServer.expect(requestTo(startsWith(uri + URI_SEARCH_PAGED_DROPSHIP_LOGISTIC_MOVEMENTS)))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam("size", "1"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/search_dropship_logistic_movements_paged.json"))
            );

        PageResult<LogisticMovement> actualResponse = client.searchDropshipLogisticMovements(
            filter,
            new PageRequest(0, 1)
        );

        softly.assertThat(actualResponse)
            .as("Asserting that the response contains a page with valid logistic movements")
            .isEqualTo(
                new PageResult<LogisticMovement>()
                    .setData(List.of(getLogisticMovement()))
                    .setPage(0)
                    .setSize(1)
                    .setTotalPages(1)
                    .setTotalElements(1)
            );
    }

    @Test
    @DisplayName("Обновление логистического перемещения (связи партнёров в новой модели) из дропшипа")
    void updateDropshipLogisticMovement() throws JsonProcessingException {
        UpdateDropshipLogisticMovementRequest request = UpdateDropshipLogisticMovementRequest.builder()
            .fromPartnerId(1L)
            .toPartnerId(2L)
            .toLogisticsPointId(102L)
            .movementPartnerId(2L)
            .cutoffTime(LocalTime.of(17, 0))
            .shipmentSchedule(Set.of(
                new ScheduleDayResponse(3L, 4, LocalTime.of(15, 0), LocalTime.of(20, 0), false),
                new ScheduleDayResponse(4L, 5, LocalTime.of(0, 0), LocalTime.of(17, 0), false)
            ))
            .status(ActivityStatus.ACTIVE)
            .build();

        mockServer.expect(requestTo(startsWith(uri + URI_DROPSHIP_LOGISTIC_MOVEMENT)))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticSegment/update_dropship_logistic_movement.json"))
            );

        LogisticMovement actualResponse = client.updateDropshipLogisticMovement(request);

        softly.assertThat(actualResponse)
            .as("Asserting that the response is valid")
            .isEqualTo(getLogisticMovement());
    }

    private LogisticMovement getLogisticMovement() {
        return LogisticMovement.builder()
            .fromPartnerId(1L)
            .fromLogisticsPointId(101L)
            .fromSegmentId(10001L)
            .toPartnerId(2L)
            .toLogisticsPointId(102L)
            .toSegmentId(10003L)
            .movementPartnerId(2L)
            .movementSegmentId(10002L)
            .status(ActivityStatus.ACTIVE)
            .shipmentType(ShipmentType.WITHDRAW)
            .shipmentSchedule(List.of(
                new ScheduleDayResponse(3L, 4, LocalTime.of(15, 0), LocalTime.of(20, 0), false),
                new ScheduleDayResponse(4L, 5, LocalTime.of(0, 0), LocalTime.of(17, 0), false)
            ))
            .cutoffTime(LocalTime.of(17, 0))
            .movementDuration(Duration.ofHours(3))
            .build();
    }
}
