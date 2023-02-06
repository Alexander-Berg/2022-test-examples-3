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


@DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/prepare_delivery_intervals.xml")
class AdminDeliveryIntervalScheduleDayFileControllerTest extends AbstractContextualTest {

    public static final String REQUEST_SLUG = "/admin/lms/" +
        LMSPlugin.SLUG_DELIVERY_INTERVAL_SCHEDULE_DAY + "/upload/add-snapshot";

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SCHEDULE_DAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/after/add_schedule_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAddNewSchedule() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart(REQUEST_SLUG)
                .file(Helper.multipartFile(
                    TestUtil.pathToJson(
                        "data/controller/admin/deliveryIntervalSnapshots/request/add_schedule_new.csv"
                    ))))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SCHEDULE_DAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/prepare_delivery_intervals.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInvalidSeveralPartnersInOneSnapshot() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart(REQUEST_SLUG)
                .file(Helper.multipartFile(
                    TestUtil.pathToJson(
                        "data/controller/admin/deliveryIntervalSnapshots" +
                            "/request/add_schedule_several_partners.csv"
                    ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SCHEDULE_DAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/after/add_schedule_replace.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testReplaceSchedule() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart(REQUEST_SLUG)
                .file(Helper.multipartFile(
                    TestUtil.pathToJson(
                        "data/controller/admin/deliveryIntervalSnapshots/request/add_schedule_replace.csv"
                    ))))
            .andExpect(status().isOk());
    }
}
