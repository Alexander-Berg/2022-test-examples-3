package ru.yandex.market.logistics.management.controller.admin.partnerTransport;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.partnerTransport.Helper.READ_WRITE;

@DatabaseSetup("/data/controller/admin/partnerTransport/before/setup.xml")
public class AdminPartnerTransportControllerTest extends AbstractContextualTest {

    @Test
    void logisticsPointGridUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport"))
            .andExpect(status().isUnauthorized());
    }


    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGridForbidden() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY})
    void getReadonlyAllTransports() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/results_readonly.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getAllTransports() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/results.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getFilteredTransports() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport").param("logisticsPointFrom", "1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/results.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getFilteredBySearchStringIdTransports() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport").param("searchString", "1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/results.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getFilteredBySearchStringNameTransports() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport").param("searchString", "Point"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/results.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerTransport/after/manual_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTransport() throws Exception {
        mockMvc.perform(post("/admin/lms/partner-transport")
            .content("{\n"
                + "  \"logisticsPointFrom\": 1,\n"
                + "  \"logisticsPointTo\": 2,\n"
                + "  \"partner\": 11,\n"
                + "  \"duration\": 23,\n"
                + "  \"price\": 100,\n"
                + "  \"palletCount\": 33\n"
                + "}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {READ_ONLY, READ_WRITE})
    void getNoTransports() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-transport")
            .param("logisticsPointFrom", "100")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/partnerTransport/after/empty.json"));
    }
}
