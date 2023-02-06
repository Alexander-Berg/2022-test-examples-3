package ru.yandex.market.logistics.management.controller.admin.deliveryInterval;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_DELIVERY_INTERVAL_CALENDAR_DAY;


@DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/prepare_delivery_intervals.xml")
class AdminDeliveryIntervalCalendarControllerTest extends AbstractContextualTest {

    public static final String REQUEST_SLUG = "/admin/lms/" +
        SLUG_DELIVERY_INTERVAL_CALENDAR_DAY + "/upload/add-snapshot";

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_CALENDAR_DAY_EDIT,
        LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_CALENDAR_DAY
    })
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/prepare_delivery_intervals.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAddCalendarForNotExistingInterval() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart(REQUEST_SLUG)
                .file(Helper.multipartFile(
                    TestUtil.pathToJson(
                        "data/controller/admin/deliveryIntervalSnapshots" +
                            "/request/add_calendar_for_not_existing_interval.csv"
                    ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_CALENDAR_DAY_EDIT,
        LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_CALENDAR_DAY
    })
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/after/add_calendar_replace.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testReplaceOldCalendars() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart(REQUEST_SLUG)
                .file(Helper.multipartFile(
                    TestUtil.pathToJson(
                        "data/controller/admin/deliveryIntervalSnapshots/request/add_calendar_replace.csv"
                    ))))
            .andExpect(status().isOk());
    }
}
