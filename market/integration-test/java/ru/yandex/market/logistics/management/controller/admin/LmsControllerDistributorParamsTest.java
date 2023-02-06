package ru.yandex.market.logistics.management.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@SuppressWarnings({"checkstyle:MagicNumber"})
@CleanDatabase
@Sql("/data/controller/admin/distributor/prepare_data.sql")
class LmsControllerDistributorParamsTest extends AbstractContextualTest {

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testGrid() throws Exception {
        getDistributorParamsGrid()
            .andExpect(testJson("data/controller/admin/distributor/distributor_param_grid.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testDetail() throws Exception {
        getDistributorParamsDetail(1L)
            .andExpect(testJson("data/controller/admin/distributor/distributor_param_detail.json", false))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testDetailNew() throws Exception {
        getDistributorParamsDetailNew()
            .andExpect(testJson("data/controller/admin/distributor/distributor_param_detail_new.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testCreate() throws Exception {
        createDistributorParamsGrid("distributor_param_create")
            .andExpect(status().isCreated());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testUpdate() throws Exception {
        updateDistributorParamsDetail(1L, "distributor_param_create")
            .andExpect(testJson("data/controller/admin/distributor/distributor_param_update.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testDelete() throws Exception {
        deleteDistributorParamsDetail(1L)
            .andExpect(status().isOk());
    }

    @Test
    void testGridUnauthorized() throws Exception {
        getDistributorParamsGrid()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS})
    void testGridReadOnlyRole() throws Exception {
        getDistributorParamsGrid()
            .andExpect(testJson("data/controller/admin/distributor/distributor_param_grid_read_only.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS})
    void testDetailReadOnlyRole() throws Exception {
        getDistributorParamsDetail(1L)
            .andExpect(testJson("data/controller/admin/distributor/distributor_param_detail_read_only.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS})
    void testDetailNewReadOnlyRole() throws Exception {
        getDistributorParamsDetailNew()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS})
    void testCreateReadOnlyRole() throws Exception {
        createDistributorParamsGrid("distributor_param_create")
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS})
    void testUpdateReadOnlyRole() throws Exception {
        updateDistributorParamsDetail(1L, "distributor_param_create")
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS})
    void testDeleteReadOnlyRole() throws Exception {
        deleteDistributorParamsDetail(1L)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testDetailNotFound() throws Exception {
        getDistributorParamsDetail(2L)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testUpdateNotFound() throws Exception {
        updateDistributorParamsDetail(2L, "distributor_param_create")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_DISTRIBUTOR_PARAMS_EDIT})
    void testDeleteNotFound() throws Exception {
        deleteDistributorParamsDetail(2L)
            .andExpect(status().isNotFound());
    }

    private ResultActions getDistributorParamsGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/distributor-params")
        );
    }

    private ResultActions getDistributorParamsDetail(Long id) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/distributor-params/{id}", id)
        );
    }

    private ResultActions getDistributorParamsDetailNew() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/distributor-params/new")
        );
    }

    private ResultActions createDistributorParamsGrid(String fileName) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/distributor-params")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/distributor/" + fileName + ".json"))
        );
    }

    private ResultActions updateDistributorParamsDetail(Long id, String fileName) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/distributor-params/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/distributor/" + fileName + ".json"))
        );
    }

    private ResultActions deleteDistributorParamsDetail(Long id) throws Exception {
        return mockMvc.perform(
            delete("/admin/lms/distributor-params/{id}", id)
        );
    }

}
