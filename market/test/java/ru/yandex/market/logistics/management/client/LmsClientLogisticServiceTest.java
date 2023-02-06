package ru.yandex.market.logistics.management.client;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceCreateDto;
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceFilter;
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceUpdateDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

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

@DisplayName("CRUD логистических сервисов через клиент LMS")
public class LmsClientLogisticServiceTest extends AbstractClientTest {

    private static final String URI = "/externalApi/logistic-services";
    private static final String URI_SEARCH = "/externalApi/logistic-services/search";
    private static final String URI_SEARCH_PAGED = "/externalApi/logistic-services/search-paged";

    @Test
    @DisplayName("Поиск логистического сервиса")
    void searchLogisticService() throws JsonProcessingException {
        LogisticServiceFilter request = LogisticServiceFilter.builder()
            .setSearchQuery("1")
            .build();

        mockServer.expect(requestTo(getBuilder(uri, URI_SEARCH).toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticService/search_response.json"))
            );

        List<LogisticSegmentServiceDto> services = client.searchLogisticService(request);
        softly.assertThat(services).as("Asserting that search query returned a single service").hasSize(1);
        softly.assertThat(services.get(0).getId()).as("Asserting that found service id is valid").isEqualTo(1);
    }

    @Test
    @DisplayName("Постраничный поиск логистического сервиса")
    void searchLogisticServicePaged() throws JsonProcessingException {
        LogisticServiceFilter request = LogisticServiceFilter.builder()
            .setSearchQuery("1")
            .build();

        mockServer.expect(requestTo(startsWith(getBuilder(uri, URI_SEARCH_PAGED).toUriString())))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("size", "2"))
            .andExpect(queryParam("page", "0"))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticService/search_paged_response.json"))
            );

        PageResult<LogisticSegmentServiceDto> services = client.searchLogisticService(request, new PageRequest(0, 2));
        softly.assertThat(services.getPage()).as("Asserting page number is correct").isEqualTo(0);
        softly.assertThat(services.getSize()).as("Asserting page size is correct").isEqualTo(2);
        softly.assertThat(services.getTotalElements()).as("Asserting total elements count is correct").isEqualTo(1);
        softly.assertThat(services.getTotalPages()).as("Asserting total pages count is valid").isEqualTo(1);

        List<LogisticSegmentServiceDto> data = services.getData();
        softly.assertThat(data).as("Asserting that search query returned a single service").hasSize(1);
        softly.assertThat(data.get(0).getId()).as("Asserting that found service id is valid").isEqualTo(1);
    }

    @Test
    @DisplayName("Создание логистического сервиса")
    void createLogisticSegment() throws JsonProcessingException {
        LogisticServiceCreateDto request = LogisticServiceCreateDto.newBuilder()
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
            .build();

        mockServer.expect(requestTo(getBuilder(uri, URI).toUriString()))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticService/create_response.json"))
            );

        softly.assertThat(client.createLogisticService(request).getId())
            .as("Asserting that the created logistic service id is valid")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Обновление логистического сервиса")
    void updateLogisticSegment() throws JsonProcessingException {
        LogisticServiceUpdateDto request = LogisticServiceUpdateDto.newBuilder()
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
            .build();

        mockServer.expect(requestTo(getBuilder(uri, URI).toUriString()))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(
                LMSClientFactory.createLmsJsonConverter().getObjectMapper().writeValueAsString(request)
            ))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logisticService/update_response.json"))
            );

        softly.assertThat(client.updateLogisticService(request).getId())
            .as("Asserting that the updated logistic service id is valid")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Удаление логистического сервиса")
    void deleteLogisticSegment() {
        mockServer.expect(requestTo(getBuilder(uri, URI + "/1").toUriString()))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(OK));

        softly.assertThatCode(() -> client.deleteLogisticService(1L))
            .as("Asserting that no exceptions are thrown")
            .doesNotThrowAnyException();
    }
}
