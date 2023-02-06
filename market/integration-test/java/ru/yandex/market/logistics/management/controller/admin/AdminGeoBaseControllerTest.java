package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/admin/geoBase/prepare_data.xml")
public class AdminGeoBaseControllerTest extends AbstractContextualTest {
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void testGetRegions() throws Exception {
        mockMvc.perform(get("/admin/lms/geobase/regions"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/geoBase/response.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void testGetRegion() throws Exception {
        mockMvc.perform(get("/admin/lms/geobase/regions/162"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/geoBase/response_detail.json"));
    }
}
