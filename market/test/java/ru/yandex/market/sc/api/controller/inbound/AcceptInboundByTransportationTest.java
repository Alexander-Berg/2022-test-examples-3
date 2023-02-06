package ru.yandex.market.sc.api.controller.inbound;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.core.domain.inbound.model.ApiAcceptInboundDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AcceptInboundByTransportationTest extends BaseApiControllerTest {

    private final JdbcTemplate jdbcTemplate;
    private final InboundRepository inboundRepository;

    private static final String API_INBOUND_ACCEPT_BY_TRANSPORTATION_ID = "/api/inbounds/{0}/acceptByTransportation";

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;

    private Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        warehouse = testFactory.storedWarehouse();
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Поставка на сегодня, в статусе создана.")
    void acceptInboundExpectedToday() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        var expected = new ApiAcceptInboundDto(inboundExternalId, transportationId, warehouse.getIncorporation());
        var actual = acceptInboundByTransportation(transportationId)
                .andExpect(status().is2xxSuccessful())
                .getResponseAsClass(ApiAcceptInboundDto.class);
        assertThat(actual).isEqualTo(expected);
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Поставка на сегодня, в статусе создана. С префиксом")
    void acceptInboundExpectedTodayWithTMPrefix() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "TMtransportationId-2738712";
        String transportationIdToSearch = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        var expected = new ApiAcceptInboundDto(inboundExternalId, transportationId, warehouse.getIncorporation());
        var actual = acceptInboundByTransportation(transportationIdToSearch)
                .andExpect(status().is2xxSuccessful())
                .getResponseAsClass(ApiAcceptInboundDto.class);
        assertThat(actual).isEqualTo(expected);
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Поставка на вчера, в статусе создана.")
    void acceptInboundExpectedYesterday() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock).minusDays(1))//на вчера
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        var expected = new ApiAcceptInboundDto(inboundExternalId, transportationId, warehouse.getIncorporation());
        var actual = acceptInboundByTransportation(transportationId)
                .andExpect(status().is2xxSuccessful())
                .getResponseAsClass(ApiAcceptInboundDto.class);
        assertThat(actual).isEqualTo(expected);
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Поставка на завтра, в статусе создана.")
    void acceptInboundExpectedTomorrow() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock).plusDays(1))//на завтра
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        var expected = new ApiAcceptInboundDto(inboundExternalId, transportationId, warehouse.getIncorporation());
        var actual = acceptInboundByTransportation(transportationId)
                .andExpect(status().is2xxSuccessful())
                .getResponseAsClass(ApiAcceptInboundDto.class);
        assertThat(actual).isEqualTo(expected);
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Поставка на сегодня, в статусе принята.")
    void acceptInboundAlreadyAccepted() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock).plusDays(1))//на завтра
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        var expected = new ApiAcceptInboundDto(inboundExternalId, transportationId, warehouse.getIncorporation());
        var actual = acceptInboundByTransportation(transportationId)
                .andExpect(status().is2xxSuccessful())
                .getResponseAsClass(ApiAcceptInboundDto.class);
        assertThat(actual).isEqualTo(expected);
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
        //и принимаем еще раз
        acceptInboundByTransportation(transportationId)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{" +
                        "\"status\":400," +
                        "\"error\":" + "\"" + ScErrorCode.INBOUND_CANT_BE_ACCEPTED + "\"," +
                        "\"message\": \"Прибытие поставки " + transportationId + " уже отмечено ранее.\"" +
                        "}", false));
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Сканирование несуществующей транспортировки.")
    void acceptInboundNonExisting() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
         acceptInboundByTransportation("non_existing_transportation")
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{" +
                        "\"status\":400," +
                        "\"error\":" + "\"" + ScErrorCode.INBOUND_NOT_FOUND_BY_TRANSPORTATION_ID + "\"," +
                        "\"message\": \"Не найдена поставка для транспортировки с id = non_existing_transportation\"" +
                        "}", false));
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CREATED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Сканирование уже зафиксированной поставки.")
    void acceptInboundAcceptFixed() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of("registry_1", List.of(Pair.of("o-1", "p-1"))))
                .build();
        var inbound = testFactory.createInbound(params);
        acceptInboundByTransportation(transportationId)
                .andExpect(status().is2xxSuccessful());
        testFactory.finishInbound(inbound);
        acceptInboundByTransportation(transportationId)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{" +
                        "\"status\":400," +
                        "\"error\":" + "\"" + ScErrorCode.INBOUND_CANT_BE_ACCEPTED + "\"," +
                        "\"message\": \"Найденная поставка " + inboundExternalId + " для данной транспортировки " +
                        transportationId +" уже зафиксирована. Нельзя ее принять.\"" +
                        "}", false));
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Сканирование уже отмененной поставки.")
    void acceptInboundAcceptCanceled() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        inbound.setInboundStatus(InboundStatus.CANCELLED);
        inboundRepository.save(inbound);
        acceptInboundByTransportation(transportationId)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{" +
                        "\"status\":400," +
                        "\"error\":" + "\"" + ScErrorCode.INBOUND_CANT_BE_ACCEPTED + "\"," +
                        "\"message\": \"Найденная поставка " + inboundExternalId + " для данной транспортировки " +
                        transportationId +" отменена. Нельзя ее принять.\"" +
                        "}", false));
        inbound = testFactory.getInbound(inbound.getId());
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CANCELLED);
    }

    @Test
    @SneakyThrows
    @DisplayName("Приемка поставки по Id транспортировки. Сканирование ШК транспортировки для поставки с другого сц.")
    void acceptInboundFromAnotherSc() {
        String inboundExternalId = "inboundExtId";
        String transportationId = "transportationId-2738712";
        var params = TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(sortingCenter)
                .registryMap(Map.of())
                .build();
        var inbound = testFactory.createInbound(params);
        String inboundExternalId2 = "inboundExtId2";
        String transportationId2 = "transportationId-from_another_sc-3482734";
        var params2= TestFactory.CreateInboundParams.builder()
                .inboundExternalId(inboundExternalId2)
                .inboundType(InboundType.DS_SC)
                .fromDate(OffsetDateTime.now(clock))
                .warehouseFromExternalId(warehouse.getYandexId())
                .transportationId(transportationId2)
                .toDate(OffsetDateTime.now(clock))
                .sortingCenter(testFactory.storedSortingCenter(777L))
                .registryMap(Map.of())
                .build();
        testFactory.createInbound(params2);
        acceptInboundByTransportation(transportationId2)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{" +
                        "\"status\":400," +
                        "\"error\":" + "\"" + ScErrorCode.INBOUND_NOT_FOUND_BY_TRANSPORTATION_ID + "\"," +
                        "\"message\": \"Не найдена поставка для транспортировки с id = " + transportationId2 + "\"" +
                        "}", false));
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CREATED);
    }


    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#acceptInbound(String, ScanLogContext, User)}
     */
    @SneakyThrows
    public SneakyResultActions acceptInboundByTransportation(String transportationId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        put(MessageFormat.format(API_INBOUND_ACCEPT_BY_TRANSPORTATION_ID, transportationId))
                                .headers(auth())
                                .contentType(MediaType.APPLICATION_JSON)
                ));
    }

    private HttpHeaders auth() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + UID);
        return headers;
    }
}
