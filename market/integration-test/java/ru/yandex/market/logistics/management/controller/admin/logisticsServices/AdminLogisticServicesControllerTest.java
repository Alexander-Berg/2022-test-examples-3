package ru.yandex.market.logistics.management.controller.admin.logisticsServices;

import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ScheduleDto;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.service.DeliveryTypeFrontDto;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.service.LogisticServiceStatusFrontDto;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.service.LogisticServiceUpdateDto;
import ru.yandex.market.logistics.management.domain.entity.KorobyteRestriction;
import ru.yandex.market.logistics.management.domain.entity.ServiceCode;
import ru.yandex.market.logistics.management.domain.entity.combinator.LogisticSegment;
import ru.yandex.market.logistics.management.domain.entity.combinator.LogisticSegmentService;
import ru.yandex.market.logistics.management.queue.producer.LogisticSegmentValidationProducer;
import ru.yandex.market.logistics.management.repository.CalendarDayRepository;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentServiceRepository;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Контроллер логистических сегментов")
@DatabaseSetup({
    "/data/service/combinator/db/before/logistic_segments_services_meta_keys.xml",
    "/data/controller/admin/logisticServices/before/prepare_data.xml",
})
class AdminLogisticServicesControllerTest extends AbstractContextualTest {

    private static final int SEGMENT_ID = 10001;

    @Autowired
    LogisticSegmentServiceRepository logisticSegmentServiceRepository;

    @Autowired
    CalendarDayRepository calendarDayRepository;
    @Autowired
    private LogisticSegmentValidationProducer logisticSegmentValidationProducer;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(logisticSegmentValidationProducer).produceTask(anyLong());
    }

    @Nonnull
    private static Stream<Arguments> filterArgs() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/logisticServices/response/no_filter.json"
            ),
            Arguments.of(
                "По идентификатору сегмента",
                Map.of("segment_id", "10001"),
                "data/controller/admin/logisticServices/response/filter_by_segment_id.json"
            ),
            Arguments.of(
                "По коду сервиса",
                Map.of("code", "CUTOFF"),
                "data/controller/admin/logisticServices/response/filter_by_service_code.json"
            ),
            Arguments.of(
                "По типу сервиса",
                Map.of("type", "INBOUND"),
                "data/controller/admin/logisticServices/response/filter_by_service_type.json"
            ),
            Arguments.of(
                "По типу доставки",
                Map.of("deliveryType", "COURIER"),
                "data/controller/admin/logisticServices/response/filter_by_delivery_type.json"
            ),
            Arguments.of(
                "По признаку замороженности",
                Map.of("frozen", "false"),
                "data/controller/admin/logisticServices/response/filter_by_frozen.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> createRestrictedArguments() {
        return Stream.of(
            Arguments.of(
                "К форме создания сервиса",
                get("/admin/lms/logistic-services/new")
            ),
            Arguments.of(
                "К ручке создания сервиса",
                post("/admin/lms/logistic-services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pathToJson("data/controller/admin/logisticServices/request/create_full.json"))
            ),
            Arguments.of(
                "К ручке удаления сервиса",
                delete("/admin/lms/logistic-services/10")
            ),
            Arguments.of(
                "К ручке активации сервиса",
                post("/admin/lms/logistic-services/10/activate")
            ),
            Arguments.of(
                "К ручке деактивации сервиса",
                post("/admin/lms/logistic-services/10/deactivate")
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> createLackingArguments() {
        return Stream.of(
            Arguments.of(
                "С пустым телом запроса",
                Map.of()
            ),
            Arguments.of(
                "С некоторыми необходимыми полями",
                Map.of(
                    "segment_id", "10001",
                    "delievryType", "PICKUP"
                )
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> createArguments() {
        return Stream.of(
            Arguments.of(
                "С полным списком полей",
                Map.of(
                    "code", "RETURN_SORT",
                    "status", "ACTIVE",
                    "segmentId", "10001",
                    "duration", "312",
                    "price", "21",
                    "deliveryType", "COURIER",
                    "frozen", "false",
                    "korobyteRestrictionId", "1"
                )
            ),
            Arguments.of(
                "Со списком необходимых полей",
                Map.of(
                    "code", "RETURN_SORT",
                    "segmentId", "10001"
                )
            ),
            Arguments.of(
                "SHIPMENT сервис",
                Map.of(
                    "code", "SHIPMENT",
                    "segmentId", "10002",
                    "meta", "[SegmentMetainfoValue{id=1, service=1, paramType=3, value=1}]"
                )
            )
        );
    }

    @DisplayName("Фильтрация логистических сервисов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("filterArgs")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    void getLogisticServiceGrid(
        String displayName,
        Map<String, String> queryParams,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-services").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @DisplayName("Проверка ограничений доступов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("createRestrictedArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    void logisticServiceRestriction(
        String displayName,
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden());
    }

    @DisplayName("Создание логистического сервиса с отсутствующими необходимыми полями")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("createLackingArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void createLogisticServiceWithWrongBody(
        String displayName,
        Map<String, String> body
    ) throws Exception {
        mockMvc
            .perform(
                post("/admin/lms/logistic-services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new JSONObject(body).toString())
            )
            .andExpect(status().isBadRequest());
    }

    @DisplayName("Создание логистического сервиса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("createArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void createLogisticService(
        String displayName,
        Map<String, String> params
    ) throws Exception {
        mockMvc
            .perform(
                post("/admin/lms/logistic-services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new JSONObject(params).toString())
            )
            .andExpect(status().isCreated())
            .andExpect(
                header().string("location", "http://localhost/admin/lms/logistic-services/1")
            );

        Optional<LogisticSegmentService> logisticSegmentService = logisticSegmentServiceRepository.findById(1L);

        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getCode)
                    .map(ServiceCode::getCode)
                    .map(Objects::toString)
                    .orElseThrow()
            )
            .isEqualTo(params.get("code"));
        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getSegment)
                    .map(LogisticSegment::getId)
                    .map(Objects::toString)
                    .orElseThrow()
            )
            .isEqualTo(params.get("segmentId"));
        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getStatus)
                    .map(Objects::toString)
                    .orElseThrow()
            )
            .isEqualTo(Optional.ofNullable(params.get("status")).orElse("ACTIVE"));
        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getDuration)
                    .orElseThrow()
            )
            .isEqualTo(
                Optional
                    .ofNullable(params.get("duration"))
                    .map(Integer::parseInt)
                    .map(duration -> duration * 60)
                    .orElse(0)
            );
        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getPrice)
                    .map(Objects::toString)
                    .orElseThrow()
            )
            .isEqualTo(Optional.ofNullable(params.get("price")).orElse("0"));
        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getMetainfo)
                    .map(Objects::toString)
                    .orElseThrow()
            )
            .isEqualTo(Optional.ofNullable(params.get("meta")).orElse("[]"));
        softly
            .assertThat(
                logisticSegmentService
                    .map(LogisticSegmentService::getKorobyteRestriction)
                    .map(KorobyteRestriction::getId)
                    .map(Objects::toString)
                    .orElse(null)
            )
            .isEqualTo(params.get("korobyteRestrictionId"));

        verify(logisticSegmentValidationProducer).produceTask(Long.parseLong(params.get("segmentId")));
    }

    @Test
    @DisplayName("Обновление данных сервиса с тем же расписанием")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_update.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/update_old_schedule.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateLogisticServiceWithOldSchedule() throws Exception {
        performUpdateLogisticSegmentService(updateDto().build());

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление данных сервиса с новым расписания")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_update.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/update_new_schedule.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateLogisticServiceWithNewSchedule() throws Exception {
        var logisticServiceUpdateDto = updateDto()
            .schedule(new ScheduleDto().setTuesdayFrom(LocalTime.of(12, 34, 56)).setTuesdayTo(LocalTime.of(23, 45)))
            .build();
        performUpdateLogisticSegmentService(logisticServiceUpdateDto);

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление сервиса. Добавление нового расписания второй волны. Расписания склеиваются в одно")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_update.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/add_second_wave_schedule.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateLogisticServiceWithNewSecondWaveSchedule() throws Exception {
        LocalTime from = LocalTime.of(12, 34, 56);
        LocalTime to = LocalTime.of(23, 45);
        var logisticServiceUpdateDto = updateDto()
            .schedule(new ScheduleDto().setTuesdayFrom(from).setTuesdayTo(to))
            .secondWaveSchedule(new ScheduleDto().setMondayFrom(from).setMondayTo(to))
            .build();
        performUpdateLogisticSegmentService(logisticServiceUpdateDto);

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление сервиса. Добавление расписания второй волны. Корректно создается во вторую волну")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_update.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/update_second_wave_schedule.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateSecondWaveSchedule() throws Exception {
        LocalTime from = LocalTime.of(12, 34, 56);
        LocalTime to = LocalTime.of(23, 45);
        var logisticServiceUpdateDto = updateDto()
            .schedule(new ScheduleDto().setTuesdayFrom(from).setTuesdayTo(to))
            .secondWaveSchedule(new ScheduleDto().setTuesdayFrom(from).setTuesdayTo(to.plusMinutes(5)))
            .build();
        performUpdateLogisticSegmentService(logisticServiceUpdateDto);

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление данных сервиса с удалением расписания")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_update.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/update_remove_schedule.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateLogisticServiceAndRemoveSchedule() throws Exception {
        var logisticServiceUpdateDto = updateDto()
            .schedule(null)
            .secondWaveSchedule(null)
            .build();
        performUpdateLogisticSegmentService(logisticServiceUpdateDto);

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Обновление сервиса. Изменение ограничения ВГХ")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_update.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/update_add_korobyte_restriction.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateKorobyteRestriction() throws Exception {
        var logisticServiceUpdateDto = updateDto()
            .korobyteRestrictionId(1L)
            .build();
        performUpdateLogisticSegmentService(logisticServiceUpdateDto);

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Удаление сервиса")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void deleteLogisticService() throws Exception {
        mockMvc.perform(delete("/admin/lms/logistic-services/10"))
            .andExpect(status().isOk());

        softly.assertThat(logisticSegmentServiceRepository.findById(10L).isPresent()).isEqualTo(false);

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Активация сервиса")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_inactive.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/prepare_activated.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void activateLogisticService() throws Exception {
        mockMvc.perform(post("/admin/lms/logistic-services/10/activate"))
            .andExpect(status().isOk());

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Деактивация сервиса")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_active.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/prepare_deactivated.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void deactivateLogisticService() throws Exception {
        mockMvc.perform(post("/admin/lms/logistic-services/10/deactivate"))
            .andExpect(status().isOk());

        verify(logisticSegmentValidationProducer).produceTask(SEGMENT_ID);
    }

    @Test
    @DisplayName("Создание дня/дней в календаре")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_calendar_day.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/prepare_create_calendar_day.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void postLogisticServiceHoliday() throws Exception {
        mockMvc.perform(post("/admin/lms/logistic-services/139372/day-off/new?idFieldName=id&parentSlug=logistic" +
                "-services&parentColumn=logisticServiceId&parentId=139372")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticServices/request/create_calendar_day.json"))
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание обновление дня/дней в календаре")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_calendar_day.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticServices/after/prepare_create_calendar_day.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void putLogisticServiceHoliday() throws Exception {
        mockMvc.perform(put("/admin/lms/logistic-services/139372/day-off/?idFieldName=id&parentSlug=logistic-services" +
                "&parentColumn=logisticServiceId&parentId=139372")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticServices/request/create_calendar_day.json"))
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удаление(изменение на противоположный) дня в календаре")
    @DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_calendar_day.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void deleteLogisticServiceHoliday() throws Exception {
        mockMvc.perform(delete("/admin/lms/logistic-services/139372/day-off/4?idFieldName=id&parentSlug=logistic" +
                "-services&parentColumn=logisticServiceId&parentId=139372"))
            .andExpect(status().isOk());

        softly.assertThat(calendarDayRepository.findById(4L).orElseThrow().getIsHoliday()).isEqualTo(false);
    }

    @Test
    @DisplayName("Получение детальной карточки сервиса")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void getDetailLogisticsService() throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-services/10"))
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson("data/controller/admin/logisticServices/response/details.json")));
    }

    @Test
    @DisplayName("Получение карготипов сервиса")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_CARGO_TYPE})
    void getServiceCargoTypes() throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-services/10/" + LMSPlugin.SLUG_CARGO_TYPE))
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson("data/controller/admin/logisticServices/response/cargo_types.json")));
    }

    private void performUpdateLogisticSegmentService(LogisticServiceUpdateDto logisticServiceUpdateDto)
        throws Exception {
        mockMvc
            .perform(
                put("/admin/lms/logistic-services/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logisticServiceUpdateDto))
            )
            .andExpect(status().isOk());
    }

    @Nonnull
    private LogisticServiceUpdateDto.LogisticServiceUpdateDtoBuilder updateDto() {
        return LogisticServiceUpdateDto.builder()
            .id(10)
            .code("CUTOFF")
            .segment(new ReferenceObject().setId("10001"))
            .duration(312)
            .price(21)
            .deliveryType(DeliveryTypeFrontDto.COURIER)
            .status(LogisticServiceStatusFrontDto.INACTIVE)
            .schedule(new ScheduleDto().setMondayFrom(LocalTime.of(1, 23, 45)).setMondayTo(LocalTime.of(2, 34, 56)))
            .frozen(false);
    }
}
