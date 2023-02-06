package ru.yandex.market.logistics.management.controller.admin;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.segment.LogisticSegmentCalendarDayUpdateDto;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.segment.LogisticSegmentNewDto;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.segment.LogisticSegmentUpdateDto;
import ru.yandex.market.logistics.management.entity.type.EdgesFrozen;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.queue.producer.LogisticSegmentValidationProducer;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Контроллер логистических сегментов")
@DatabaseSetup({
    "/data/controller/admin/logisticSegments/prepare_data.xml",
    "/data/controller/admin/logisticSegments/prepare_services.xml"
})
@ParametersAreNonnullByDefault
class AdminLogisticSegmentsControllerTest extends AbstractContextualTest {

    private static final String LOGISTIC_SEGMENTS_URL =
        "/admin/" + LMSPlugin.SLUG + "/" + LMSPlugin.SLUG_LOGISTIC_SEGMENTS;
    private static final String LOGISTIC_SEGMENTS_ID_URL_TEMPLATE = LOGISTIC_SEGMENTS_URL + "/{id}";
    private static final String DUPLICATE_WAREHOUSE_URL_TEMPLATE =
        LOGISTIC_SEGMENTS_URL + "/{warehouseSegmentId}/" + LMSPlugin.SLUG_DUPLICATE_WAREHOUSE;
    private static final String VALIDATE_SEGMENT_URL_TEMPLATE =
        LOGISTIC_SEGMENTS_URL + "/{segmentId}/" + LMSPlugin.SLUG_VALIDATE_SEGMENT;

    private static final long WAREHOUSE_SEGMENT_ID = 10001L;
    private static final long MOVEMENT_SEGMENT_ID = 10002L;
    private static final long LINEHAUL_SEGMENT_ID = 10005L;
    private static final long PICKUP_SEGMENT_ID = 10006L;
    private static final long HANDING_SEGMENT_ID = 10007L;

    @Autowired
    private LogisticSegmentValidationProducer logisticSegmentValidationProducer;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(logisticSegmentValidationProducer).produceTask(anyLong());
    }

    @DisplayName("Фильтрация логистических сегментов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    void getLogisticSegmentsGrid(
        @SuppressWarnings("unused") String displayName,
        Map<String, String> queryParams,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-segments").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> getLogisticSegmentsGrid() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/logisticSegments/response/search_no_filter.json"
            ),
            Arguments.of(
                "По идентификатору партнера",
                Map.of("partner", "1"),
                "data/controller/admin/logisticSegments/response/search_by_partner.json"
            ),
            Arguments.of(
                "По типу сегмента",
                Map.of("type", "MOVEMENT"),
                "data/controller/admin/logisticSegments/response/search_by_type.json"
            ),
            Arguments.of(
                "По идентификатору логистической точки",
                Map.of("logisticPoint", "102"),
                "data/controller/admin/logisticSegments/response/search_by_logistic_point.json"
            ),
            Arguments.of(
                "По идентификатору региона",
                Map.of("locationId", "1003"),
                "data/controller/admin/logisticSegments/response/search_by_region_id.json"
            ),
            Arguments.of(
                "Поиск последующих к данному сегменту",
                Map.of("fromSegmentId", "10005"),
                "data/controller/admin/logisticSegments/response/search_by_from_segment_id.json"
            ),
            Arguments.of(
                "Поиск предыдущих к данному сегменту",
                Map.of("toSegmentId", "10005"),
                "data/controller/admin/logisticSegments/response/search_by_to_segment_id.json"
            ),
            Arguments.of(
                "По признаку активности",
                Map.of("active", "0"),
                "data/controller/admin/logisticSegments/response/search_by_inactive_segment.json"
            )
        );
    }

    @DisplayName("Проверка ограничений доступов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    void logisticSegmentRestriction(
        @SuppressWarnings("unused") String displayName,
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden());
    }

    @Nonnull
    private static Stream<Arguments> logisticSegmentRestriction() {
        return Stream.of(
            Arguments.of(
                "К форме создания сегмента",
                get("/admin/lms/logistic-segments/new")
            ),
            Arguments.of(
                "К ручке создания сегмента",
                post("/admin/lms/logistic-segments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pathToJson("data/controller/admin/logisticSegments/request/create_full.json"))
            ),
            Arguments.of(
                "К ручке удаления сегмента",
                delete("/admin/lms/logistic-segments/10001")
            ),
            Arguments.of(
                "К ручке деактивации сегмента",
                post("/admin/lms/logistic-segments/10001/deactivate")
            )
        );
    }

    @Test
    @DisplayName("Создание логистического сегмента со связью с существующим сегментом")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_segment_with_edge.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentWithEdge() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_full.json", params(10001L))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Создание логистического сегмента со связью с несуществующим сегментом")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentWithWrongConnection() {
        softly.assertThatThrownBy(() ->
                performCreate("data/controller/admin/logisticSegments/request/create_full.json", params(404L))
            )
            .isInstanceOf(NestedServletException.class);

        verify(logisticSegmentValidationProducer, never()).produceTask(1);
    }

    @Test
    @DisplayName("Создание сегмента Склад с существующей логистической точкой")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createExistingWarehouse() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_existing_warehouse.json", null)
            .andExpect(status().isConflict())
            .andExpect(status().reason(
                "Для данной логистической точки уже существует сегмент с типом \"Склад\": 10001"
            ));

        verify(logisticSegmentValidationProducer, never()).produceTask(1);
    }

    @Test
    @DisplayName("Создание сегмента Склад с существующей логистической точкой")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_with_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createExistingWarehouseWithParams() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_existing_warehouse.json", params(10004L))
            .andExpect(status().isConflict())
            .andExpect(status().reason(
                "Сегмент \"Склад\" с логистической точкой 101 уже существует (10001). Была создана связь с ней"
            ));

        verify(logisticSegmentValidationProducer, never()).produceTask(1);
    }

    @DisplayName("Валидация запроса на создание сегмента")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void createLogisticSegmentWithWrongBody(
        @SuppressWarnings("unused") String displayName,
        LogisticSegmentNewDto.Builder dto
    ) throws Exception {
        performCreate(dto.build()).andExpect(status().isBadRequest());
        verify(logisticSegmentValidationProducer, never()).produceTask(1);
    }

    @Nonnull
    private static Stream<Arguments> createLogisticSegmentWithWrongBody() {
        return Stream.of(
            Arguments.of("Без всех необходимых полей", LogisticSegmentNewDto.builder()),
            Arguments.of("Без идентификатора партнера", validLogisticSegmentNewDto().partnerId(null)),
            Arguments.of("Без идентификатора локации", validLogisticSegmentNewDto().locationId(null)),
            Arguments.of("Без типа", validLogisticSegmentNewDto().type(null))
        );
    }

    @Test
    @DisplayName("Создание логистического сегмента c полным списком полей")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentWithAllFields() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_full.json", null)
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Создание логистического сегмента со списком необходимых полей для магистралей")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_required_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentRequired() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_minimal.json", null)
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Создание логистического сегмента со списком необходимых полей для складов")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_required_fields_warehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentRequiredForWarehouse() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_minimal_warehouse.json", null)
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Создание логистического сегмента со списком необходимых полей для пвз")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_required_fields_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentRequiredForPickup() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_minimal_pickup.json", null)
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Создание логистического сегмента со списком необходимых полей и логистической точкой")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/create_required_fields_and_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createLogisticSegmentRequiredWithPoint() throws Exception {
        performCreate("data/controller/admin/logisticSegments/request/create_with_logistic_point.json", null)
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @Test
    @DisplayName("Обновление данных сегмента")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticSegment() throws Exception {
        performAndCheckUpdate(logisticSegmentUpdateDto(101, 11, null));

        verify(logisticSegmentValidationProducer).produceTask(10001);
    }

    @Test
    @DatabaseSetup("/data/controller/admin/logisticSegments/prepare_meta_info.xml")
    @DisplayName("Обновление возвратного склада сегмента: настройка для возвратных сегментов выключена")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/update_return_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticSegmentReturnSortingCenterId() throws Exception {
        performAndCheckUpdate(logisticSegmentUpdateDto(1, 101, 2L));

        verify(logisticSegmentValidationProducer).produceTask(10001);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = {
            "/data/controller/admin/logisticSegments/prepare_bmv_data.xml",
            "/data/controller/admin/logisticSegments/prepare_add_return_sc_id_data.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Обновление сегмента: добавление настройки RETURN_SORTING_CENTER_ID")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/update_segment_add_return_sc_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentAddReturnSortingCenterId() throws Exception {
        performAndCheckUpdate(logisticSegmentUpdateDto(4L));

        verify(logisticSegmentValidationProducer).produceTask(10003);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = {
            "/data/controller/admin/logisticSegments/prepare_bmv_data.xml",
            "/data/controller/admin/logisticSegments/prepare_delete_return_sc_id_data.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Обновление сегмента: удаление настройки RETURN_SORTING_CENTER_ID")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/update_segment_delete_return_sc_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentDeleteReturnSortingCenterId() throws Exception {
        performAndCheckUpdate(logisticSegmentUpdateDto(null));

        verify(logisticSegmentValidationProducer).produceTask(10003);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/admin/logisticSegments/prepare_meta_info.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = {
            "/data/controller/admin/logisticSegments/prepare_bmv_data.xml",
            "/data/controller/admin/logisticSegments/prepare_modify_return_sc_id_data.xml",
        },
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Обновление сегмента: изменение настройки RETURN_SORTING_CENTER_ID")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/update_segment_modify_return_sc_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSegmentModifyReturnSortingCenterId() throws Exception {
        performAndCheckUpdate(logisticSegmentUpdateDto(5L));

        verify(logisticSegmentValidationProducer).produceTask(10003);
    }

    @Test
    @DisplayName("Удаление сегмента с существующими связями")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/delete_segment_with_edge.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteLogisticSegment() throws Exception {
        mockMvc.perform(delete("/admin/lms/logistic-segments/10001")).andExpect(status().isOk());

        verify(logisticSegmentValidationProducer, never()).produceTask(anyLong());
    }

    @Test
    @DisplayName("Деактивация сегмента")
    @DatabaseSetup("/data/controller/admin/logisticSegments/before/prepare_active.xml")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/prepare_deactivated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void deactivateLogisticSegment() throws Exception {
        mockMvc.perform(post("/admin/lms/logistic-segments/10002/deactivate"))
            .andExpect(status().isOk());

        verify(logisticSegmentValidationProducer, never()).produceTask(10002);
    }

    @Test
    @DisplayName("Получить детальную информацию о логистическом сегменте - неавторизованный пользователь")
    void getLogisticSegmentDetail_Unauthorized() throws Exception {
        performGetLogisticSegmentDetail()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить детальную информацию о логистическом сегменте - недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getLogisticSegmentDetail_Forbidden() throws Exception {
        performGetLogisticSegmentDetail()
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получить детальную информацию о логистическом сегменте")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS,
        LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void getLogisticSegmentDetail(
        @SuppressWarnings("unused") String displayName,
        long segmentId,
        String jsonPath
    ) throws Exception {
        performGetLogisticSegmentDetail(segmentId)
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> getLogisticSegmentDetail() {
        return Stream.of(
            Arguments.of(
                "Склад",
                WAREHOUSE_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_detail_warehouse.json"
            ),
            Arguments.of(
                "Перемещение",
                MOVEMENT_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_detail_movement.json"
            ),
            Arguments.of(
                "Магистраль",
                LINEHAUL_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_detail_linehaul.json"
            ),
            Arguments.of(
                "ПВЗ",
                PICKUP_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_detail_pickup.json"
            ),
            Arguments.of(
                "Вручение",
                HANDING_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_detail_handing.json"
            )
        );
    }

    @Test
    @DisplayName("Дублировать сегмент склада - неавторизованный пользователь")
    void duplicateWarehouseSegment_Unauthorized() throws Exception {
        performDuplicateWarehouseSegment()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Дублировать сегмент склада - недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void duplicateWarehouseSegment_Forbidden() throws Exception {
        performDuplicateWarehouseSegment()
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Дублировать сегмент склада - идентификатор не склада")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void duplicateWarehouseSegment_BadRequest() throws Exception {
        performDuplicateWarehouseSegment(MOVEMENT_SEGMENT_ID)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "Дублировать можно только логистический сегмент типа Склад. Сегмент 10002 имеет тип MOVEMENT"
            ));
    }

    @DisplayName("Дублировать сегмент склада")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    @DatabaseSetup(
        value = "/data/controller/admin/logisticSegments/before/duplicate_warehouse_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/after/duplicate_warehouse_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void duplicateWarehouseSegment_Created() throws Exception {
        performDuplicateWarehouseSegment()
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl());

        verify(logisticSegmentValidationProducer).produceTask(1);
    }

    @DisplayName("Валидация сегмента - ok")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void validateSegment_ok() throws Exception {
        performValidateSegment(10002)
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(10002);
    }

    @DisplayName("Валидация сегмента - сегмента не существует")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void validateSegment_notFound() throws Exception {
        performValidateSegment(5000)
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получить результаты валидации логистического сегмента")
    @DatabaseSetup(
        value = "/data/controller/admin/logisticSegments/before/segment_validation.xml",
        type = DatabaseOperation.INSERT
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS,
        LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void getValidationSegmentResult(
        @SuppressWarnings("unused") String displayName,
        long segmentId,
        String jsonPath
    ) throws Exception {
        mockMvc.perform(get(String.format("/admin/lms/logistic-segments/%d/validate", segmentId)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> getValidationSegmentResult() {
        return Stream.of(
            Arguments.of(
                "Получение списка результатов валидаций для сегмента без результата",
                WAREHOUSE_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_validation_no_fails.json"
            ),
            Arguments.of(
                "Получение списка результатов валидаций для сегмента - 1 ошибка",
                MOVEMENT_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_validation_1_fail.json"
            ),
            Arguments.of(
                "Получение списка результатов валидаций для сегмента - много ошибок",
                PICKUP_SEGMENT_ID,
                "data/controller/admin/logisticSegments/response/get_segment_validation_multiple_fails.json"
            )
        );
    }

    @DisplayName("Получение расписаний сервисов одного сегмента")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getScheduleArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS})
    @DatabaseSetup("/data/controller/admin/logisticSegments/before/prepare_schedule.xml")
    void getGridSuccessReadOnly(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        String responsePathReadOnlyMode
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/logistic-segments/{id}/schedule", 10001L).params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadOnlyMode));
    }

    @DisplayName("Обновление данных сервиса с тем же расписанием")
    @Test
    @DatabaseSetup("/data/controller/admin/logisticSegments/before/prepare_schedule.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticSegments/after/update_schedule.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS_EDIT})
    void updateLogisticSegmentScheduleTime() throws Exception {
        performUpdateLogisticSegmentCalendar(updateCalendarDto().build());
    }

    private void performUpdateLogisticSegmentCalendar(
        LogisticSegmentCalendarDayUpdateDto logisticSegmentCalendarDayUpdateDto
    ) throws Exception {
        mockMvc
            .perform(
                put("/admin/lms/logistic-segments/10001/schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logisticSegmentCalendarDayUpdateDto))
            )
            .andExpect(status().isOk());
    }

    @Nonnull
    private LogisticSegmentCalendarDayUpdateDto.Builder updateCalendarDto() {
        return LogisticSegmentCalendarDayUpdateDto
            .builder()
            .id(4071L)
            .calendarKey("11330")
            .start("2022-01-31T09:00:00+03:00")
            .end("2022-01-31T18:00:00+03:00");
    }

    @Nonnull
    private static Stream<Arguments> getScheduleArguments() {
        return Stream.of(
            Arguments.of(
                "Неделя",
                Map.of(
                    "fromDate", "2022-01-31",
                    "toDate", "2022-02-06"
                ),
                "data/controller/admin/logisticSegments/response/get_segment_schedules_week.json"
            ),
            Arguments.of(
                "3 дня",
                Map.of(
                    "fromDate", "2022-01-31",
                    "toDate", "2022-02-02"
                ),
                "data/controller/admin/logisticSegments/response/get_segment_schedules_3_days.json"
            )
        );
    }

    @Nonnull
    private ResultActions performCreate(String path, @Nullable Map<String, String> params) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/logistic-segments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(path))
                .params(toParams(params))
        );
    }

    @Nonnull
    private ResultActions performCreate(LogisticSegmentNewDto dto) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/logistic-segments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
        );
    }

    private void performAndCheckUpdate(LogisticSegmentUpdateDto logisticSegmentUpdateDto) throws Exception {
        mockMvc.perform(
                put("/admin/lms/logistic-segments/10001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(logisticSegmentUpdateDto))
            )
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions performGetLogisticSegmentDetail() throws Exception {
        return performGetLogisticSegmentDetail(WAREHOUSE_SEGMENT_ID);
    }

    @Nonnull
    private ResultActions performGetLogisticSegmentDetail(long segmentId) throws Exception {
        return mockMvc.perform(
            get(LOGISTIC_SEGMENTS_ID_URL_TEMPLATE, segmentId)
        );
    }

    @Nonnull
    private ResultActions performDuplicateWarehouseSegment() throws Exception {
        return performDuplicateWarehouseSegment(WAREHOUSE_SEGMENT_ID);
    }

    @Nonnull
    private ResultActions performDuplicateWarehouseSegment(long warehouseSegemntId) throws Exception {
        return mockMvc.perform(
            post(DUPLICATE_WAREHOUSE_URL_TEMPLATE, warehouseSegemntId)
        );
    }

    private ResultActions performValidateSegment(long segmentId) throws Exception {
        return mockMvc.perform(post(VALIDATE_SEGMENT_URL_TEMPLATE, segmentId));
    }

    @Nonnull
    private Map<String, String> params(Long partnerId) {
        return Map.of(
            "parentSlug", "logistic-segments",
            "parentColumn", "toSegmentId",
            "parentId", partnerId.toString()
        );
    }

    @Nonnull
    private static LogisticSegmentNewDto.Builder validLogisticSegmentNewDto() {
        return LogisticSegmentNewDto.builder()
            .partnerId(1L)
            .type(LogisticSegmentType.WAREHOUSE)
            .locationId(1);
    }

    @Nonnull
    private LogisticSegmentUpdateDto logisticSegmentUpdateDto(@Nullable Long returnScId) {
        return LogisticSegmentUpdateDto.builder()
            .id(10003L)
            .title(null)
            .name("Test")
            .partner(new ReferenceObject().setId("2"))
            .type(null)
            .logisticPointId(102L)
            .locationId(1002)
            .returnSortingCenterId(returnScId)
            .edgesFrozen(EdgesFrozen.AUTO_TO)
            .build();
    }

    @Nonnull
    private LogisticSegmentUpdateDto logisticSegmentUpdateDto(long pointId, int locationId, @Nullable Long scId) {
        return LogisticSegmentUpdateDto.builder()
            .id(10001L)
            .title(null)
            .name("Test")
            .partner(new ReferenceObject().setId("1"))
            .type(LogisticSegmentType.LINEHAUL)
            .logisticPointId(pointId)
            .locationId(locationId)
            .returnSortingCenterId(scId)
            .edgesFrozen(EdgesFrozen.MANUALLY)
            .build();
    }

    @Nonnull
    private ResultMatcher redirectedUrl() {
        return MockMvcResultMatchers.redirectedUrl("http://localhost/admin/lms/logistic-segments/1");
    }
}
