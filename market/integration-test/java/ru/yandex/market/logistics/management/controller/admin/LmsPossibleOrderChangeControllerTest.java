package ru.yandex.market.logistics.management.controller.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PossibleOrderChange;
import ru.yandex.market.logistics.management.repository.PossibleOrderChangeRepository;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/admin/possibleOrderChange/prepare_data.xml")
class LmsPossibleOrderChangeControllerTest extends AbstractContextualTest {
    private static final Long FIRST_DELIVERY_SERVICE_ID = 1001L;
    private static final Long SECOND_DELIVERY_SERVICE_ID = 1002L;
    private static final Instant FIXED_TIME = Instant.parse("2021-01-01T00:00:00Z");

    @Autowired
    private PossibleOrderChangeRepository possibleOrderChangeRepository;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE})
    void getFilteredByPartnerPossibleOrderChangeGrid() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/admin/lms/possible-order-change")
            .param("partnerId", String.valueOf(FIRST_DELIVERY_SERVICE_ID));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/possibleOrderChange/possible_order_change_grid_filtered_by_partner_id.json"
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE})
    void getPossibleOrderChangeDetail() throws Exception {
        mockMvc.perform(get("/admin/lms/possible-order-change/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/possibleOrderChange/possible_order_change_detail.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    void getPossibleOrderChangeCreateForm() throws Exception {
        mockMvc.perform(
            get("/admin/lms/possible-order-change/new")
                .param("parentId", String.valueOf(FIRST_DELIVERY_SERVICE_ID)))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/possibleOrderChange/possible_order_change_create_form.json"));
    }

    @Test
    @Transactional
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/possibleOrderChange/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createPossibleOrderChange() throws Exception {
        mockMvc.perform(post("/admin/lms/possible-order-change")
            .param("parentId", String.valueOf(SECOND_DELIVERY_SERVICE_ID))
            .param("parentSlug", LMSPlugin.SLUG_PARTNER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson("data/controller/admin/possibleOrderChange/possible_order_change_create_request.json")))
            .andExpect(status().isCreated());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/possibleOrderChange/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updatePossibleOrderChange() throws Exception {
        mockMvc.perform(put("/admin/lms/possible-order-change/{id}", 3L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson("data/controller/admin/possibleOrderChange/possible_order_change_update_request.json")))
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/possibleOrderChange/possible_order_change_update_response.json"
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/possibleOrderChange/delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deletePossibleOrderChange() throws Exception {
        mockMvc.perform(delete("/admin/lms/possible-order-change/{id}", 1L)
            .param("parentId", String.valueOf(FIRST_DELIVERY_SERVICE_ID))
            .param("parentSlug", LMSPlugin.SLUG_PARTNER))
            .andExpect(status().isOk());

        Optional<PossibleOrderChange> possibleOrderChange = possibleOrderChangeRepository.findById(1L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/possibleOrderChange/enable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bulkEnable() throws Exception {
        mockMvc.perform(
            post("/admin/lms/possible-order-change/enable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(
                    "data/controller/admin/possibleOrderChange/possible_order_change_bulk_enable_request.json"
                ))
        )
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/possibleOrderChange/enable_already_enabled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bulkEnableAlreadyEnabled() throws Exception {
        Instant savedEnabledAt = Instant.parse("2020-12-01T00:00:00Z");
        mockMvc.perform(
            post("/admin/lms/possible-order-change/enable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(
                    "data/controller/admin/possibleOrderChange/" +
                        "possible_order_change_bulk_enable_already_enabled_request.json"
                ))
        )
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_POSSIBLE_ORDER_CHANGE_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/possibleOrderChange/disable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void bulkDisable() throws Exception {
        mockMvc.perform(
            post("/admin/lms/possible-order-change/disable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(
                    "data/controller/admin/possibleOrderChange/possible_order_change_bulk_disable_request.json"
                ))
        )
            .andExpect(status().isOk());
    }
}
