package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@SuppressWarnings({"checkstyle:MagicNumber"})
@DatabaseSetup("/data/controller/admin/capacity/prepare_data.xml")
class LmsControllerPartnerCapacityDayOffTest extends AbstractContextualAspectValidationTest {

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testGrid() throws Exception {
        getPartnerCapacityDayOffGrid()
            .andExpect(testJson("data/controller/admin/capacity/dayoff/capacity_day_off_grid.json"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testDetail() throws Exception {
        getPartnerCapacityDayOffDetail(1L)
            .andExpect(testJson("data/controller/admin/capacity/dayoff/capacity_day_off_detail.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testDetailNotFound() throws Exception {
        getPartnerCapacityDayOffDetail(2L).andExpect(status().isNotFound());
    }

    private ResultActions getPartnerCapacityDayOffGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-capacity-day-off")
        );
    }

    private ResultActions getPartnerCapacityDayOffDetail(Long id) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-capacity-day-off/{id}", id)
        );
    }
}
