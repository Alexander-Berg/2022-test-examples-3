package ru.yandex.market.logistics.management.controller.admin.logisticsServices;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.ActionDto;
import ru.yandex.market.logistics.management.domain.dto.front.enums.AdminSegmentServiceTagKey;
import ru.yandex.market.logistics.management.domain.dto.front.logisticsServiceMetaInfo.LogisticSegmentServiceTagDetailDto;
import ru.yandex.market.logistics.management.domain.dto.front.logisticsServiceMetaInfo.LogisticSegmentServiceTagNewDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.domain.dto.front.enums.AdminSegmentServiceTagKey.DUPLICATE_SEGMENT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_LOGISTIC_SERVICE_TAGS;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@ParametersAreNonnullByDefault
@DisplayName("Тест cruda тегов сервисов логистических сегментов через админку")
@DatabaseSetup("/data/controller/admin/logisticSegments/services/tag/prepare_data.xml")
public class AdminLogisticServiceTagControllerTest extends AbstractContextualTest {
    private static final String URL = "/admin/lms/" + SLUG_LOGISTIC_SERVICE_TAGS;

    @Test
    @DisplayName("Получить теги для сервиса")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG})
    void getGridForService() throws Exception {
        mockMvc.perform(get(URL).param("serviceId", "302"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticSegments/services/tag/response/grid.json"));
    }

    @Test
    @DisplayName("У сервиса нет тегов")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG)
    void getEmptyGrid() throws Exception {
        mockMvc.perform(get(URL).param("serviceId", "8888"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticSegments/services/tag/response/empty_grid.json"));
    }

    @Test
    @DisplayName("Получение детальной карточки")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG)
    void getDetail() throws Exception {
        mockMvc.perform(get(URL + "/" + 100))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticSegments/services/tag/response/detail_dto.json"));
    }

    @Test
    @DisplayName("Получение детальной карточки для редактирования")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG)
    void getDetailEdit() throws Exception {
        mockMvc.perform(get(URL + "/" + 300))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticSegments/services/tag/response/detail_dto_edit.json"));
    }

    @Test
    @DisplayName("Ошибка получения детальной карточки - нет такого тега")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG)
    void getDetailFail() throws Exception {
        mockMvc.perform(get(URL + "/" + 99900)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение карточки создания")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void getNewDetail() throws Exception {
        mockMvc.perform(getCreateDetail())
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticSegments/services/tag/response/create_dto.json"));
    }

    @Test
    @DisplayName("Удаление")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/services/tag/after/delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void delete() throws Exception {
        mockMvc.perform(deleteTag(objectMapper.writeValueAsString(new ActionDto().setIds(Set.of(200L, 300L)))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/services/tag/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void create() throws Exception {
        mockMvc.perform(createTag(objectMapper.writeValueAsString(newDto())))
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/logistic-service-tags/1"));
    }

    @Test
    @DisplayName("Ошибка создания - тег не существует")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/services/tag/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void createKeyError() throws Exception {
        mockMvc.perform(createTag(objectMapper.writeValueAsString(newDto().setKey(DUPLICATE_SEGMENT))))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Cannot find segment meta info key DUPLICATE_SEGMENT"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка создания")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/services/tag/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void invalidCreate(
        @SuppressWarnings("unused") String name,
        LogisticSegmentServiceTagNewDto newDto,
        String responsePath
    )
        throws Exception {
        mockMvc.perform(createTag(objectMapper.writeValueAsString(newDto)))
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(responsePath, IGNORING_EXTRA_FIELDS));
    }

    @Nonnull
    private static Stream<Arguments> invalidCreate() {
        return Stream.of(
            Arguments.of(
                "Не указан идентификатор сервиса",
                newDto().setServiceId(null),
                "data/controller/admin/logisticSegments/services/tag/response/create_without_service_id.json"
            ),
            Arguments.of(
                "Не указан ключ",
                newDto().setKey(null),
                "data/controller/admin/logisticSegments/services/tag/response/create_without_key.json"
            ),
            Arguments.of(
                "Не указано значение",
                newDto().setValue(null),
                "data/controller/admin/logisticSegments/services/tag/response/create_with_null_value.json"
            ),
            Arguments.of(
                "Пустое значение",
                newDto().setValue(""),
                "data/controller/admin/logisticSegments/services/tag/response/create_with_empty_value.json"
            )
        );
    }

    @Test
    @DisplayName("Обновление")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticSegments/services/tag/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void update() throws Exception {
        mockMvc.perform(updateTag(objectMapper.writeValueAsString(updateDto())))
            .andExpect(status().isOk())
            .andExpect(jsonContent("data/controller/admin/logisticSegments/services/tag/response/after_update.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка обновления")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG_EDIT)
    void invalidUpdate(
        @SuppressWarnings("unused") String name,
        LogisticSegmentServiceTagDetailDto updateDto,
        String responsePath
    )
        throws Exception {
        mockMvc.perform(updateTag(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(responsePath, IGNORING_EXTRA_FIELDS));
    }

    @Nonnull
    private static Stream<Arguments> invalidUpdate() {
        return Stream.of(
            Arguments.of(
                "Не указано значение",
                updateDto().setValue(null),
                "data/controller/admin/logisticSegments/services/tag/response/update_with_empty_value.json"
            ),
            Arguments.of(
                "Пустое значение",
                updateDto().setValue(""),
                "data/controller/admin/logisticSegments/services/tag/response/update_with_null_value.json"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Недостаточно прав для операции")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_TAG)
    void invalidRole(
        @SuppressWarnings("unused") String name,
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }

    @Nonnull
    private static Stream<Arguments> invalidRole() {
        return Stream.of(
            Arguments.of("Получение карточки для создания", getCreateDetail()),
            Arguments.of(
                "Создание",
                createTag(pathToJson("data/controller/admin/logisticSegments/services/tag/request/create.json"))
            ),
            Arguments.of(
                "Обновление",
                updateTag(pathToJson("data/controller/admin/logisticSegments/services/tag/request/update.json"))
            ),
            Arguments.of(
                "Удаление",
                deleteTag(pathToJson("data/controller/admin/logisticSegments/services/tag/request/delete.json"))
            )
        );
    }

    @Nonnull
    private static LogisticSegmentServiceTagNewDto newDto() {
        return new LogisticSegmentServiceTagNewDto()
            .setKey(AdminSegmentServiceTagKey.RETURN_SORTING_CENTER_ID)
            .setValue("100")
            .setServiceId(303L);
    }

    @Nonnull
    private static LogisticSegmentServiceTagDetailDto updateDto() {
        return new LogisticSegmentServiceTagDetailDto()
            .setId(100L)
            .setValue("newValue")
            .setKey(AdminSegmentServiceTagKey.RETURN_SORTING_CENTER_ID)
            .setDescription("Возвратный СЦ для дропоффа");
    }

    @Nonnull
    private static MockHttpServletRequestBuilder updateTag(String updateDto) {
        return request(HttpMethod.PUT, URL + "/100")
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateDto);
    }

    @Nonnull
    private static MockHttpServletRequestBuilder deleteTag(String action) {
        return request(HttpMethod.POST, URL + "/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(action);
    }

    @Nonnull
    private static MockHttpServletRequestBuilder createTag(String newDto) {
        return request(HttpMethod.POST, URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(newDto);
    }

    @Nonnull
    private static MockHttpServletRequestBuilder getCreateDetail() {
        return get(URL + "/new").param("parentId", "10003");
    }
}
