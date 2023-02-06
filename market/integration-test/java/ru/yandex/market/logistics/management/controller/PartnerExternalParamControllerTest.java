package ru.yandex.market.logistics.management.controller;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.queue.producer.UpdateDbsPartnerCargoTypesProducer;
import ru.yandex.market.logistics.management.service.client.LogisticsPointService;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

class PartnerExternalParamControllerTest extends AbstractContextualTest {

    @Autowired
    private LogisticsPointService logisticsPointService;

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private UpdateDbsPartnerCargoTypesProducer updateDbsPartnerCargoTypesProducer;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(logbrokerEventTaskProducer).produceTask(any());
        Mockito.doNothing().when(updateDbsPartnerCargoTypesProducer).produceTask(any());
        clock.setFixed(Instant.parse("2021-08-17T14:14:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
        verifyNoMoreInteractions(logbrokerEventTaskProducer, logisticsPointService, updateDbsPartnerCargoTypesProducer);
    }

    @Test
    @DisplayName("Успешное получение параметра")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data.xml")
    void getPartnerExternalParam() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partner/externalParam").param("paramTypes", "TYPE1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(("data/controller/partner/partner_external_params.json")));
    }

    @Test
    @DisplayName("Успешное добавление параметра")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_2_external_params.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addParamSuccessfully() throws Exception {
        tryAddParameter("data/controller/externalParams/create_param_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/create_param_response.json"));
    }

    @Test
    @DisplayName("Успешное добавление второго параметра")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_2_external_params.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addParamRequestDuplication() throws Exception {
        tryAddParameter("data/controller/externalParams/create_param_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/create_param_response.json"));
        tryAddParameter("data/controller/externalParams/create_param_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/create_param_response.json"));
    }

    @Test
    @DisplayName("Успешное обновление параметра")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateParamSuccessfully() throws Exception {
        tryAddParameter("data/controller/externalParams/update_param_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/update_param_response.json"));
    }

    @Test
    @DisplayName("Добавление параметра с невалидным значением")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addParamWithInvalidValue() throws Exception {
        tryAddParameter("data/controller/externalParams/create_param_request_with_invalid_value.json")
            .andExpect(status().isBadRequest())
            .andExpect(testJson(
                "data/controller/externalParams/add_invalid_param_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DisplayName("Добавление параметра с несуществующим типом")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addParamWithNonExistingType() throws Exception {
        tryAddParameter("data/controller/externalParams/create_param_request_with_nonexisting_type.json")
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Успешное добавление параметров")
    @DatabaseSetup({
        "/data/controller/externalParams/partner_with_1_external_param.xml",
        "/data/controller/externalParams/logistics_points_for_partner.xml"
    })
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_2_external_params_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addParamsSuccess() throws Exception {
        addParameters("data/controller/externalParams/set_params_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/set_params_response.json"));

        ArgumentCaptor<Set<LogisticsPoint>> pointsCaptor = ArgumentCaptor.forClass(Set.class);

        verify(logisticsPointService).checkWarehouseCoordinates(warehouse(3, "ext-id-3", true));
        verify(logisticsPointService).checkWarehouseCoordinates(pointsCaptor.capture());
        verify(logisticsPointService).getActiveWarehousesByPartnerIds(Set.of(1L));

        softly.assertThat(pointsCaptor.getValue()).containsAll(Set.of(
            warehouse(3, "ext-id-3", true),
            warehouse(1, "ext-id", false),
            pickupPoint()
        ));
        checkLogbrokerEvent("data/controller/externalParams/logbrokerEvent/set_express_param_true_snapshot.json");
    }

    @Test
    @DisplayName("Успешное добавление параметра с пустым запросом")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addNoParamsSuccess() throws Exception {
        addParameters("data/controller/externalParams/set_params_empty_request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/set_params_empty_response.json"));
    }

    @Test
    @DisplayName("Добавление параметра с несуществующим типом")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addNotFoundParams() throws Exception {
        addParameters("data/controller/externalParams/set_params_not_found_request.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason("No such PartnerExternalParamType exists: [LOGO]"));
    }

    @Test
    @DisplayName("Добавление параметров с невалидными значениями")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void addParamsWithInvalidValue() throws Exception {
        addParameters("data/controller/externalParams/set_params_invalid_value_request.json")
            .andExpect(status().isBadRequest())
            .andExpect(testJson(
                "data/controller/externalParams/set_params_invalid_value_response.json",
                Option.IGNORING_ARRAY_ORDER,
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DisplayName("Адрес логистической точки проверяется")
    @DatabaseSetup({
        "/data/controller/externalParams/partner_with_1_external_param.xml",
        "/data/controller/externalParams/logistics_points_for_partner.xml"
    })
    void warehouseCoordinatesCheckCalled() throws Exception {
        tryAddParameter("data/controller/externalParams/set_express_param_true_request.json")
            .andExpect(status().isOk());

        ArgumentCaptor<Set<LogisticsPoint>> pointsCaptor = ArgumentCaptor.forClass(Set.class);

        verify(logisticsPointService).checkWarehouseCoordinates(warehouse(3, "ext-id-3", true));
        verify(logisticsPointService).checkWarehouseCoordinates(pointsCaptor.capture());
        verify(logisticsPointService).getActiveWarehousesByPartnerIds(any());

        softly.assertThat(pointsCaptor.getValue()).containsAll(Set.of(
            warehouse(3, "ext-id-3", true),
            warehouse(1, "ext-id", false),
            pickupPoint()
        ));
        checkLogbrokerEvent("data/controller/externalParams/logbrokerEvent/set_express_param_true_snapshot.json");
        checkBuildWarehouseSegmentTask(1L, 3L);
    }

    @Test
    @DisplayName("Адрес логистической точки не проверяется: параметр express устанавливается в false")
    @DatabaseSetup({
        "/data/controller/externalParams/partner_with_1_external_param.xml",
        "/data/controller/externalParams/logistics_points_for_partner.xml",
        "/data/controller/externalParams/external_param_express_true.xml"
    })
    void warehouseCoordinatesCheckDoesntCall() throws Exception {
        tryAddParameter("data/controller/externalParams/set_express_param_false_request.json")
            .andExpect(status().isOk());

        verify(logisticsPointService).getActiveWarehousesByPartnerIds(Set.of(1L));
        checkLogbrokerEvent("data/controller/externalParams/logbrokerEvent/set_express_param_false_snapshot.json");
        checkBuildWarehouseSegmentTask(1L, 3L);
    }

    @Test
    @DisplayName("Адрес логистической точки не проверяется: нет подходящих точек для проверки")
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param.xml")
    void noActiveWarehouses() throws Exception {
        tryAddParameter("data/controller/externalParams/set_express_param_true_request.json")
            .andExpect(status().isOk());

        verify(logisticsPointService).checkWarehouseCoordinates(Set.of());
        verify(logisticsPointService).getActiveWarehousesByPartnerIds(Set.of(1L));
    }

    @Test
    @DisplayName("Получение email-ов партнера")
    @DatabaseSetup("/data/service/client/partner_specific_external_params_prepare_data.xml")
    void getPartnerEmails() throws Exception {
        mockMvc.perform(get("/externalApi/partners/1/externalParams/SERVICE_EMAILS"))
            .andExpect(status().isOk())
            .andExpect(testJson(("data/controller/externalParams/partner_emails.json")));
    }

    @Test
    @DisplayName("Успешное получение дней для возврата заказов")
    @DatabaseSetup("/data/service/client/partner_specific_external_params_prepare_data.xml")
    void getPartnerDaysForReturnOrder() throws Exception {
        mockMvc.perform(get("/externalApi/partners/1/externalParams/DAYS_FOR_RETURN_ORDER"))
            .andExpect(status().isOk())
            .andExpect(testJson(("data/controller/externalParams/partner_days_for_return_order.json")));
    }

    @Test
    @DisplayName("Возвращает 404 когда партнёр не найден")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data.xml")
    void getPartnerExternalParamsById_partnerNotFound() throws Exception {
        mockMvc.perform(get("/externalApi/partners/5/externalParams/DAYS_FOR_RETURN_ORDER"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Возвращает 204 когда комбинация партнёра и типа параметра не найдена")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data.xml")
    void getPartnerExternalParamsById_partnerHasNoMatchingExternalParam() throws Exception {
        mockMvc.perform(get("/externalApi/partners/1/externalParams/ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED"))
            .andExpect(status().isNoContent())
            .andExpect(content().bytes(new byte[0]));
    }

    @Test
    @DisplayName(
        "Продьюсится таска UPDATE_DBS_PARTNER_CARGO_TYPES на изменение параметра CAN_SELL_MEDICINE у DBS партнера"
    )
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param_medicine.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param_medicine_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dbsCargoTypeUpdateQueuedSingle() throws Exception {
        tryAddParameter("data/controller/externalParams/create_param_request_medicine.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/create_param_response_medicine.json"));

        verify(updateDbsPartnerCargoTypesProducer).produceTask(1L);
    }

    @Test
    @DisplayName(
        "Продьюсится таска UPDATE_DBS_PARTNER_CARGO_TYPES "
            + "на изменение параметра CAN_SELL_MEDICINE у DBS партнера через батчевую ручку"
    )
    @DatabaseSetup("/data/controller/externalParams/partner_with_1_external_param_medicine.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/partner_with_1_external_param_medicine_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void dbsCargoTypeUpdateQueuedBatch() throws Exception {
        addParameters("data/controller/externalParams/set_params_request_medicine.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/externalParams/set_params_response_medicine.json"));

        verify(updateDbsPartnerCargoTypesProducer).produceTask(1L);
    }

    @Test
    @DisplayName("Поиск всех параметров")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data.xml")
    void getPartnerExternalParamTypeOptions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partners/externalParamsTypeOptions"))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/externalParams/external_param_type_options.json",
                Option.IGNORING_EXTRA_FIELDS
            ));
    }

    @Test
    @DisplayName("Удаление параметров")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data_for_remove.xml")
    @ExpectedDatabase(
        value = "/data/controller/externalParams/remove_params_for_partner_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteParameters() throws Exception {
        removeParameters("data/controller/externalParams/remove_parameters.json").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удаление параметров - передан пустой список")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data_for_remove.xml")
    @ExpectedDatabase(
        value = "/data/service/client/partner_external_params_prepare_data_for_remove.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteParametersWithEmptyList() throws Exception {
        removeParameters("data/controller/externalParams/remove_parameters_empty_list.json").andExpect(status().isOk());
    }

    private void checkLogbrokerEvent(String jsonPath) throws IOException {
        ArgumentCaptor<EventDto> argumentCaptor = ArgumentCaptor.forClass(EventDto.class);
        Mockito.verify(logbrokerEventTaskProducer).produceTask(argumentCaptor.capture());
        assertThatJson(argumentCaptor.getValue())
            .isEqualTo(objectMapper.readValue(pathToJson(jsonPath), EventDto.class));
    }

    @Nonnull
    private ResultActions tryAddParameter(String requestPath) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/partners/1/externalParam")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(requestPath)));
    }

    @Nonnull
    private ResultActions addParameters(String requestPath) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/partners/1/externalParams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(requestPath)));
    }

    @Nonnull
    private ResultActions removeParameters(String requestPath) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put("/externalApi/partners/2/externalParams/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(requestPath)));
    }

    @Nonnull
    private LogisticsPoint warehouse(long id, String externalId, boolean active) {
        return new LogisticsPoint().setId(id)
            .setExternalId(externalId)
            .setType(PointType.WAREHOUSE)
            .setMarketBranded(false)
            .setActive(active);
    }

    @Nonnull
    private LogisticsPoint pickupPoint() {
        return new LogisticsPoint()
            .setId(2L)
            .setExternalId("ext-id-2")
            .setType(PointType.PICKUP_POINT)
            .setMarketBranded(false)
            .setActive(true);
    }
}
