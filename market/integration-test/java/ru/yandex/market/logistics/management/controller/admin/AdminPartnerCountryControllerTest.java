package ru.yandex.market.logistics.management.controller.admin;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Работа со странами партнёров в админке")
@DatabaseSetup("/data/controller/admin/partnerCountry/prepare_data.xml")
class AdminPartnerCountryControllerTest extends AbstractContextualAspectValidationTest {

    private static final String SLUG_URL = "/admin/lms/partner-country";

    @Test
    @DisplayName("Найти страны партнёра - неавторизованный пользователь")
    void testGetGridUnauthorized() throws Exception {
        mockMvc.perform(get(SLUG_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Найти страны партнёра - недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void testGetGridForbidden() throws Exception {
        mockMvc.perform(get(SLUG_URL).param("partnerId", "1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Найти страны партнёра - не передан id партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY})
    void testGetGridPartnerIdRequired() throws Exception {
        mockMvc.perform(get(SLUG_URL))
            .andExpect(status().isBadRequest())
            .andExpect(
                result -> assertThat(result.getResponse().getErrorMessage())
                    .isEqualTo("Required Long parameter 'partnerId' is not present")
            );
    }

    @DisplayName("Найти страны партнёра")
    @ParameterizedTest(name = "{0}")
    @MethodSource
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY})
    void testGetGridSuccess(
        @SuppressWarnings("unused") String description,
        Map<String, String> params,
        String responsePath
    )
        throws Exception {
        mockMvc.perform(get(SLUG_URL)
                .param("partnerId", "100")
                .params(toParams(params)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Test
    @DisplayName("Получить DTO для создания новой страны партнёра")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY_EDIT})
    void testGetNewDetailDto() throws Exception {
        mockMvc.perform(
            get(SLUG_URL + "/new")
                .param("parentSlug", "partner")
                .param("idFieldName", "id")
                .param("parentId", "100")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerCountry/new_detail_dto.json"));
    }

    @Test
    @DisplayName("Создать новую страну партнёра")
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCountry/created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY_EDIT})
    void testCreate() throws Exception {
        mockMvc.perform(
            post(SLUG_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerCountry/create_request.json"))
        )
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/lms/partner/100"));
    }

    @Test
    @DisplayName("Получить страну партнёра по id страны и id партнёра (без прав администратора)")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY})
    void testGetDetailDtoAsViewer() throws Exception {
        mockMvc.perform(
                get(SLUG_URL + "/106")
                    .param("parentSlug", "partner")
                    .param("idFieldName", "id")
                    .param("parentId", "101")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerCountry/detail_dto_for_viewer.json"));
    }

    @Test
    @DisplayName("Получить страну партнёра по id страны и id партнёра (с правами администратора)")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY_EDIT})
    void testGetDetailDtoAsAdmin() throws Exception {
        mockMvc.perform(
                get(SLUG_URL + "/106")
                    .param("parentSlug", "partner")
                    .param("idFieldName", "id")
                    .param("parentId", "101")
            )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerCountry/detail_dto_for_admin.json"));
    }

    @Test
    @DisplayName("Удалить страну партнёра")
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerCountry/deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_COUNTRY_EDIT})
    void testDelete() throws Exception {
        mockMvc.perform(
                delete(SLUG_URL + "/106")
                    .param("parentSlug", "partner")
                    .param("idFieldName", "id")
                    .param("parentId", "101")
        )
            .andExpect(status().isOk())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner/101"));
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> testGetGridSuccess() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации и сортировки",
                Map.of(),
                "data/controller/admin/partnerCountry/search_no_filter.json"
            ),
            Arguments.of(
                "Фильтрация по частичному вхождению названия страны",
                Map.of("countryName", "стан"),
                "data/controller/admin/partnerCountry/search_name_substring_filter.json"
            ),
            Arguments.of(
                "Фильтрация по частичному полному совпадению названия страны",
                Map.of("countryName", "Казахстан"),
                "data/controller/admin/partnerCountry/search_name_filter.json"
            ),
            Arguments.of(
                "Фильтрация по id",
                Map.of("countryId", "104"),
                "data/controller/admin/partnerCountry/search_id_filter.json"
            ),
            Arguments.of(
                "Номер страницы и размер",
                Map.of("size", "2", "page", "1"),
                "data/controller/admin/partnerCountry/search_page_number_and_size.json"
            ),
            Arguments.of(
                "Сортировка по имени",
                Map.of("sort", "countryName,asc"),
                "data/controller/admin/partnerCountry/search_with_sort.json"
            )
        );
    }
}
