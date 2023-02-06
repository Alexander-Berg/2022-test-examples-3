package ru.yandex.market.logistics.management.controller.admin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.combinator.ServiceCapacityValue;
import ru.yandex.market.logistics.management.repository.combinator.ServiceCapacityValueRepository;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@SuppressWarnings({"checkstyle:MagicNumber"})
@DatabaseSetup("/data/controller/admin/capacity/prepare_data.xml")
class LmsControllerPartnerCapacityTest extends AbstractContextualAspectValidationTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    private ServiceCapacityValueRepository capacityValueRepository;

    @BeforeEach
    void setup() {
        LocalDate today = LocalDate.of(2018, 10, 29);
        clock.setFixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testGrid() throws Exception {
        getPartnerCapacityGrid()
            .andExpect(testJson("data/controller/admin/capacity/capacity_grid.json", false));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testCsDetail() throws Exception {
        getPartnerCapacityDetail(1L)
            .andExpect(testJson("data/controller/admin/capacity/capacity_cs_detail.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testLmsDetail() throws Exception {
        getPartnerCapacityDetail(2L)
            .andExpect(testJson("data/controller/admin/capacity/capacity_lms_detail.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testDetailNew() throws Exception {
        getPartnerCapacityNew()
            .andExpect(testJson("data/controller/admin/capacity/capacity_new.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testCreate() throws Exception {
        createPartnerCapacity("capacity_create")
            .andExpect(status().isCreated());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testCreateWithBadPartnerId() throws Exception {
        createPartnerCapacity("capacity_create_partner_not_found")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testCreateWithBadPlatformId() throws Exception {
        createPartnerCapacity("capacity_create_platform_not_found")
            .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/admin/capacity/service_capacity_value_update.xml",
        type = DatabaseOperation.REFRESH
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testCsUpdate() throws Exception {
        updatePartnerCapacity(1L, "capacity_update")
            .andExpect(testJson("data/controller/admin/capacity/capacity_cs_update_result.json"))
            .andExpect(status().isOk());
        Optional<ServiceCapacityValue> capacityValueOptional = capacityValueRepository.findById(11L);
        softly.assertThat(capacityValueOptional.isPresent())
            .isTrue();
        softly.assertThat(capacityValueOptional.get().getValue())
            .isEqualTo(100);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/admin/capacity/service_capacity_value_update.xml",
        type = DatabaseOperation.REFRESH
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testLmsUpdate() throws Exception {
        updatePartnerCapacity(1L, "capacity_update")
            .andExpect(testJson("data/controller/admin/capacity/capacity_cs_update_result.json"))
            .andExpect(status().isOk());
        Optional<ServiceCapacityValue> capacityValueOptional = capacityValueRepository.findById(11L);
        softly.assertThat(capacityValueOptional.isPresent())
            .isTrue();
        softly.assertThat(capacityValueOptional.get().getValue())
            .isEqualTo(100);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testDelete() throws Exception {
        deletePartnerCapacity(1L)
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testDetailNotFound() throws Exception {
        getPartnerCapacityDetail(-1L)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testUpdateNotFound() throws Exception {
        updatePartnerCapacity(-1L, "capacity_create")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_CAPACITY_EDIT})
    void testDeleteNotFound() throws Exception {
        deletePartnerCapacity(-1L)
            .andExpect(status().isNotFound());
    }

    private ResultActions getPartnerCapacityGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-capacity").param("partner", "1", "2")
        );
    }

    private ResultActions getPartnerCapacityDetail(Long id) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-capacity/{id}", id)
        );
    }

    private ResultActions getPartnerCapacityNew() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-capacity/new")
        );
    }

    private ResultActions createPartnerCapacity(String fileName) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/partner-capacity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/capacity/" + fileName + ".json"))
        );
    }

    private ResultActions updatePartnerCapacity(Long id, String fileName) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/partner-capacity/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/capacity/" + fileName + ".json"))
        );
    }

    private ResultActions deletePartnerCapacity(Long id) throws Exception {
        return mockMvc.perform(
            delete("/admin/lms/partner-capacity/{id}", id)
        );
    }
}
