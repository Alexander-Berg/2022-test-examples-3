package ru.yandex.market.logistics.management.controller.admin;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CRUD выходных дней партнера через админку")
@DatabaseSetup("/data/controller/admin/holiday/prepare_calendars.xml")
@ParametersAreNonnullByDefault
class AdminPartnerHolidayControllerTest extends AbstractAdminHolidayControllerTest {
    private static final String METHOD_URL = "/admin/lms/partner-holiday";

    @Nonnull
    @Override
    protected String getMethodUrl() {
        return METHOD_URL;
    }

    @Nonnull
    @Override
    protected String getReadOnlyDetailResponsePath() {
        return "data/controller/admin/holiday/get_partner_detail_success.json";
    }

    @Nonnull
    @Override
    protected String getReadWriteDetailResponsePath() {
        return "data/controller/admin/holiday/get_partner_detail_success_edit.json";
    }

    @Nonnull
    @Override
    protected String getNewResponsePath() {
        return "data/controller/admin/holiday/get_partner_new.json";
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/holiday/create_holiday_in_new_partner_calendar.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Override
    @DisplayName("Создать выходной в новом календаре")
    void createHolidayInNewCalendar() throws Exception {
        mockMvc.perform(create().params(parentComponentProperties(2)).content("{\"day\": \"2020-04-11\"}"))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/partner/2"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @Override
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/holiday/create_holiday_in_existing_calendar.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создать выходной в существующем календаре")
    void createHolidayInExistingCalendarTest() throws Exception {
        createHolidayInExistingCalendar();
    }

    @Test
    @Override
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
    @DisplayName("Создать существующий выходной")
    void createExistingHolidayInExistingCalendarTest() throws Exception {
        createExistingHolidayInExistingCalendar();
    }

    @Override
    @Test
    @DisplayName("Удалить выходной в родительском календаре")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/holiday/delete_holiday_in_parent_calendar.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteHolidayInParentCalendarTest() throws Exception {
        deleteHolidayInParentCalendar();
    }

    @Override
    @Test
    @DisplayName("Удалить выходной в календаре сущности")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/holiday/delete_holiday_in_own_calendar.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteHolidayInOwnCalendarTest() throws Exception {
        deleteHolidayInOwnCalendar();
    }

    @Override
    @Test
    @DisplayName("Удалить несколько выходных")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = {
            LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT,
            LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT
        }
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/holiday/delete_multiple_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteMultipleHolidayCalendarTest() throws Exception {
        deleteMultipleHolidayCalendar();
    }
}
