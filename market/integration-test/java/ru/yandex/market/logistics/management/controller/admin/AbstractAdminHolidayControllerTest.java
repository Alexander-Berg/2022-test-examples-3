package ru.yandex.market.logistics.management.controller.admin;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@ParametersAreNonnullByDefault
abstract class AbstractAdminHolidayControllerTest extends AbstractContextualTest {
    protected static final Map<String, String> PARENT_COMPONENT_PROPERTIES = Map.of(
        "idFieldName", "id",
        "parentSlug", "partner",
        "parentId", "1"
    );

    @Nonnull
    protected abstract String getMethodUrl();

    @Nonnull
    protected abstract String getReadOnlyDetailResponsePath();

    @Nonnull
    protected abstract String getReadWriteDetailResponsePath();

    @Nonnull
    protected abstract String getNewResponsePath();

    @Test
    @DisplayName("Найти выходные дни - Неавторизованный пользователь")
    void getGridUnauthorized() throws Exception {
        mockMvc.perform(getGrid()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Найти выходные дни - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getGridForbidden() throws Exception {
        mockMvc.perform(getGrid().param("entityId", "1")).andExpect(status().isForbidden());
    }

    @DisplayName("Найти выходные дни - ReadOnly mode - Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void getGridSuccessReadOnly(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        String responsePathReadOnlyMode,
        @SuppressWarnings("unused") String responsePathReadWriteMode
    ) throws Exception {
        mockMvc.perform(getGrid().param("entityId", "1").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadOnlyMode));
    }

    @DisplayName("Найти выходные дни - ReadWrite mode - Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY,
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT,
        }
    )
    void getGridSuccessReadWrite(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        @SuppressWarnings("unused") String responsePathReadOnlyMode,
        String responsePathReadWriteMode
    ) throws Exception {
        mockMvc.perform(getGrid().param("entityId", "1").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadWriteMode));
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/holiday/search_no_filter.json",
                "data/controller/admin/holiday/search_no_filter_edit.json"
            ),
            Arguments.of(
                "По дате",
                Map.of("day", "2020-01-01"),
                "data/controller/admin/holiday/search_by_day.json",
                "data/controller/admin/holiday/search_by_day_edit.json"
            ),
            Arguments.of(
                "С размером страницы",
                Map.of("size", "2"),
                "data/controller/admin/holiday/search_with_size.json",
                "data/controller/admin/holiday/search_with_size_edit.json"
            ),
            Arguments.of(
                "С номером страницы",
                Map.of("size", "2", "page", "1"),
                "data/controller/admin/holiday/search_with_page.json",
                "data/controller/admin/holiday/search_with_page_edit.json"
            ),
            Arguments.of(
                "С сортировкой",
                Map.of("sort", "day,asc"),
                "data/controller/admin/holiday/search_with_sort.json",
                "data/controller/admin/holiday/search_with_sort_edit.json"
            )
        );
    }

    @Test
    @DisplayName("Получить детальную карточку выходного - Неавторизованный пользователь")
    void getDetailUnauthorized() throws Exception {
        mockMvc.perform(getDetail(1)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить детальную карточку выходного - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getDetailForbidden() throws Exception {
        mockMvc.perform(getDetail(1).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получить детальную карточку выходного - Без указания родительской сущности")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void getDetailWithoutPartnerId() throws Exception {
        mockMvc.perform(getDetail(1)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получить детальную карточку выходного - ReadOnly mode - Успешно")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void getDetailSuccessReadOnly() throws Exception {
        mockMvc.perform(getDetail(1).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(getReadOnlyDetailResponsePath()));
    }

    @Test
    @DisplayName("Получить детальную карточку выходного - ReadWrite mode - Успешно")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY,
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT,
        }
    )
    void getDetailSuccessReadWrite() throws Exception {
        mockMvc.perform(getDetail(1).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(getReadWriteDetailResponsePath()));
    }

    @Test
    @DisplayName("Получить DTO для создания новой сущности - Неавторизованный пользователь")
    void getNewUnauthorized() throws Exception {
        mockMvc.perform(getNew()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить DTO для создания новой сущности - Недостаточно прав")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void getNewForbidden() throws Exception {
        mockMvc.perform(getNew().params(toParams(PARENT_COMPONENT_PROPERTIES))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получить DTO для создания новой сущности - Без указания родительской сущности")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void getNewWithoutPartnerId() throws Exception {
        mockMvc.perform(getNew()).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получить DTO для создания новой сущности - Успешно")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void getNewSuccess() throws Exception {
        mockMvc.perform(getNew().params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(getNewResponsePath()));
    }

    @Test
    @DisplayName("Создать выходной - Неавторизованный пользователь")
    void createUnauthorized() throws Exception {
        mockMvc.perform(create()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Создать выходной - Недостаточно прав")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void createForbidden() throws Exception {
        mockMvc.perform(create().params(toParams(PARENT_COMPONENT_PROPERTIES)).content("{\"day\": \"2020-04-11\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Создать выходной - Без указания родительской сущности")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void createWithoutPartnerId() throws Exception {
        mockMvc.perform(create()).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создать выходной - Партнер или лог.точка не найдены")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void createErrorPartnerNotFound() throws Exception {
        mockMvc.perform(create().params(parentComponentProperties(3)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создать выходной - null")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void createNullHoliday() throws Exception {
        mockMvc.perform(create().params(parentComponentProperties(2)).content("{\"day\": null}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создать выходной в новом календаре")
    abstract void createHolidayInNewCalendar() throws Exception;

    @Test
    @DisplayName("Создать выходной в существующем календаре")
    abstract void createHolidayInExistingCalendarTest() throws Exception;

    void createHolidayInExistingCalendar() throws Exception {
        mockMvc.perform(create().params(toParams(PARENT_COMPONENT_PROPERTIES)).content("{\"day\": \"2020-04-11\"}"))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner/1"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Создать существующий выходной")
    abstract void createExistingHolidayInExistingCalendarTest() throws Exception;

    void createExistingHolidayInExistingCalendar() throws Exception {
        mockMvc.perform(create().params(toParams(PARENT_COMPONENT_PROPERTIES)).content("{\"day\": \"2020-04-10\"}"))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner/1"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Удалить выходной - Неавторизованный пользователь")
    void deleteUnauthorized() throws Exception {
        mockMvc.perform(delete(15)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Удалить выходной - Недостаточно прав")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void deleteForbidden() throws Exception {
        mockMvc.perform(delete(15).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Удалить выходной - Без указания родительской сущности")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void deleteWithoutPartnerId() throws Exception {
        mockMvc.perform(delete(15)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Удалить выходной - Партнер или лог.точка не найдены")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void deleteErrorPartnerNotFound() throws Exception {
        mockMvc.perform(delete(15).params(parentComponentProperties(3)))
            .andExpect(status().isNotFound())
            .andExpect(TestUtil.hasResolvedExceptionContainingMessage("with id=3"));
    }

    @Test
    @DisplayName("Удалить выходной в родительском календаре")
    abstract void deleteHolidayInParentCalendarTest() throws Exception;

    void deleteHolidayInParentCalendar() throws Exception {
        mockMvc.perform(delete(1).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isOk())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner/1"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Удалить выходной в календаре сущности")
    abstract void deleteHolidayInOwnCalendarTest() throws Exception;

    void deleteHolidayInOwnCalendar() throws Exception {
        mockMvc.perform(delete(15).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isOk())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner/1"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Удалить несколько выходных - Неавторизованный пользователь")
    void deleteMultipleUnauthorized() throws Exception {
        mockMvc.perform(deleteMultiple()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Удалить несколько выходных - Недостаточно прав")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY, LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY}
    )
    void deleteMultipleForbidden() throws Exception {
        mockMvc.perform(deleteMultiple().params(toParams(PARENT_COMPONENT_PROPERTIES)).content("{\"ids\": [14, 15]}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Удалить несколько выходных - Без указания родительской сущности")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void deleteMultipleWithoutPartnerId() throws Exception {
        mockMvc.perform(deleteMultiple().content("{\"ids\": [14, 15]}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Удалить несколько выходных - Партнер или лог.точка не найдены")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    void deleteMultipleErrorPartnerNotFound() throws Exception {
        mockMvc.perform(deleteMultiple().params(parentComponentProperties(3)).content("{\"ids\": [14, 15]}"))
            .andExpect(status().isNotFound())
            .andExpect(TestUtil.hasResolvedExceptionContainingMessage("with id=3"));
    }

    @Test
    @DisplayName("Удалить несколько выходных - Ошибка при удалении одного не удаляет остальные")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/holiday/prepare_calendars.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteMultipleHolidayError() throws Exception {
        mockMvc.perform(
                deleteMultiple()
                    .params(toParams(PARENT_COMPONENT_PROPERTIES))
                    .content("{\"ids\": [1, 3, 17, 16]}")
            )
            .andExpect(status().isNotFound())
            .andExpect(TestUtil.hasResolvedExceptionContainingMessage(
                "Entities CalendarDay with ids [16, 17] not found"
            ));
    }

    @Test
    @DisplayName("Удалить несколько выходных")
    abstract void deleteMultipleHolidayCalendarTest() throws Exception;

    void deleteMultipleHolidayCalendar() throws Exception {
        mockMvc.perform(
                deleteMultiple()
                    .params(toParams(PARENT_COMPONENT_PROPERTIES))
                    .content("{\"ids\": [1, 3, 15]}")
            )
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @NotNull
    protected MockHttpServletRequestBuilder getGrid() {
        return get(getMethodUrl());
    }

    @NotNull
    protected MockHttpServletRequestBuilder getDetail(long calendarDayId) {
        return get(getMethodUrl() + "/{calendarDayId}", calendarDayId);
    }

    @NotNull
    protected MockHttpServletRequestBuilder getNew() {
        return get(getMethodUrl() + "/new");
    }

    @NotNull
    protected MockHttpServletRequestBuilder create() {
        return post(getMethodUrl()).contentType(MediaType.APPLICATION_JSON);
    }

    @Nonnull
    protected MockHttpServletRequestBuilder delete(long calendarDayId) {
        return MockMvcRequestBuilders.delete(getMethodUrl() + "/{calendarDayId}", calendarDayId);
    }

    @Nonnull
    protected MockHttpServletRequestBuilder deleteMultiple() {
        return MockMvcRequestBuilders.post(getMethodUrl() + "/delete").contentType(MediaType.APPLICATION_JSON);
    }

    @Nonnull
    protected MultiValueMap<String, String> parentComponentProperties(long parentId) {
        return toParams(Map.of(
            "idFieldName", "id",
            "parentSlug", "partner",
            "parentId", String.valueOf(parentId)
        ));
    }
}
