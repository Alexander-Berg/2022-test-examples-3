package ru.yandex.market.logistics.management.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("Просмотр и редактирование параметров партнера")
@DatabaseSetup("/data/controller/admin/partnerExternalParam/prepare_data.xml")
class AdminPartnerExternalParamControllerTest extends AbstractContextualTest {

    private static final MultiValueMap<String, String> PARENT_PARAMS = new LinkedMultiValueMap<>(Map.of(
        "parentId", List.of("100501"),
        "parentSlug", List.of("partner"),
        "idFieldName", List.of("id")
    ));

    @Test
    @DisplayName("Получение списка параметров (режим просмотра)")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM)
    void getGridViewModeTest() throws Exception {
        getGridTest("data/controller/admin/partnerExternalParam/response/getGridViewMode.json");
    }

    @Test
    @DisplayName("Получение списка параметров (режим реактирования)")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    void getGridEditModeTest() throws Exception {
        getGridTest("data/controller/admin/partnerExternalParam/response/getGridEditMode.json");
    }

    private void getGridTest(String responsePath) throws Exception {
        mockMvc.perform(get("/admin/lms/partner-external-param")
            .param("partnerId", "100501"))
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    @Test
    @DisplayName("Получение детальной карточки параметра (режим просмотра)")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM)
    void getDetailViewModeTest() throws Exception {
        getDetailTest("data/controller/admin/partnerExternalParam/response/getDetailViewMode.json");
    }

    @Test
    @DisplayName("Получение детальной карточки параметра (режим реактирования)")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    void getDetailEditModeTest() throws Exception {
        getDetailTest("data/controller/admin/partnerExternalParam/response/getDetailEditMode.json");
    }

    private void getDetailTest(String responsePath) throws Exception {
        mockMvc.perform(get("/admin/lms/partner-external-param/104"))
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    @DisplayName("Получение опций типов параметра")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getTypesArgs")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM)
    void getTypes(
        @SuppressWarnings("unused") String caseName,
        @Nullable Long typeId,
        @Nullable String typeTitle,
        @Nonnull String responsePath
    ) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
            .get("/admin/lms/partner-external-param/type-options");
        Optional.ofNullable(typeId).ifPresent(id -> builder.param("typeId", id.toString()));
        Optional.ofNullable(typeTitle).ifPresent(title -> builder.param("typeTitle", title));
        mockMvc.perform(builder)
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    private static Stream<Arguments> getTypesArgs() {
        return Stream.of(
            Arguments.of("Все типы", null, null,
                "data/controller/admin/partnerExternalParam/response/typesGrid/all.json"),
            Arguments.of("Один тип по id", 2L, null,
                "data/controller/admin/partnerExternalParam/response/typesGrid/byId.json"),
            Arguments.of("Поиск по паттерну описания", null, " t",
                "data/controller/admin/partnerExternalParam/response/typesGrid/byDescription.json"),
            Arguments.of("Поиск по паттерну ключа", null, "GLOB",
                "data/controller/admin/partnerExternalParam/response/typesGrid/byKey.json")
        );
    }

    @Test
    @DisplayName("Получение формы добавлнеия нового параметра")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    void getNewForm() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-external-param/new")
            .params(PARENT_PARAMS))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerExternalParam/response/new.json"));
    }

    @Test
    @DisplayName("Добавление партнеру нового параметра")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/after_new_value_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createNewParam() throws Exception {
        mockMvc.perform(post("/admin/lms/partner-external-param")
            .params(PARENT_PARAMS)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"typeId\": 3,\"value\":\"1\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Добавление партнеру нового параметра с типом TIME")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/after_new_time_value_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createNewParamWithTimeType() throws Exception {
        mockMvc.perform(post("/admin/lms/partner-external-param")
            .params(PARENT_PARAMS)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"typeId\": 6,\"value\":\"18:00\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Попытка добваить невалидный параметр")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createInvalidParam() throws Exception {
        mockMvc.perform(post("/admin/lms/partner-external-param")
            .params(PARENT_PARAMS)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"typeId\": 5,\"value\":\"This value is not boolean\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(startsWith(
                "Свойство MARKET_PICKUP_AVAILABLE может иметь только значения из списка"
            )));
    }

    @Test
    @DisplayName("Обновление значения параметра")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/after_value_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateParam() throws Exception {
        mockMvc.perform(put("/admin/lms/partner-external-param/101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":101,\"value\":\"0\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Невалидное обноавление значения параметра")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateInvalidParam() throws Exception {
        mockMvc.perform(put("/admin/lms/partner-external-param/101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":101,\"value\":\"This value is not boolean\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(startsWith("Свойство IS_GLOBAL может иметь только значения из списка")));
    }

    @Test
    @DisplayName("Удаление параметра у партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EXTERNAL_PARAM_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/after_value_deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteParam() throws Exception {
        mockMvc.perform(delete("/admin/lms/partner-external-param/101")
            .params(PARENT_PARAMS))
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("Тестирование неавторизованных запросов")
    @MethodSource("getRequests")
    void testUnauthorized(
        @SuppressWarnings("unused") String url,
        MockHttpServletRequestBuilder requestBuilder
    ) throws Exception {
        mockMvc.perform(requestBuilder)
            .andExpect(status().isUnauthorized());
    }

    private static Stream<Arguments> getRequests() {
        return Stream.of(
            Arguments.of(
                "GET /admin/lms/partner-external-param",
                get("/admin/lms/partner-external-param")
            ),
            Arguments.of(
                "GET /admin/lms/partner-external-param/12345",
                get("/admin/lms/partner-external-param/12345")
            ),
            Arguments.of(
                "PUT /admin/lms/partner-external-param/12345",
                put("/admin/lms/partner-external-param/12345")
            ),
            Arguments.of(
                "DELETE /admin/lms/partner-external-param/12345",
                delete("/admin/lms/partner-external-param/12345")
            ),
            Arguments.of(
                "GET /admin/lms/partner-external-param/type-options",
                get("/admin/lms/partner-external-param/type-options")
            ),
            Arguments.of(
                "GET /admin/lms/partner-external-param/new",
                get("/admin/lms/partner-external-param/new")
            ),
            Arguments.of(
                "POST /admin/lms/partner-external-param",
                post("/admin/lms/partner-external-param")
            )
        );
    }
}
