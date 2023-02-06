package ru.yandex.market.logistics.management.controller.admin;

import java.time.Duration;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerHandlingTime;
import ru.yandex.market.logistics.management.repository.PartnerHandlingTimeRepository;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@SuppressWarnings({"unchecked"})
@DatabaseSetup("/data/controller/admin/handlingTime/prepare_data.xml")
class LmsControllerPartnerHandlingTimeTest extends AbstractContextualTest {

    @Autowired
    private PartnerHandlingTimeRepository partnerHandlingTimeRepository;

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME})
    void getPartnerHandlingTimes() throws Exception {
        getPartnerHandlingTimes(null, null)
            .andExpect(testJson("data/controller/admin/handlingTime/handling_times_grid.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME})
    void getPartnerHandlingTimesFilter() throws Exception {
        getPartnerHandlingTimes(1, 3)
            .andExpect(testJson("data/controller/admin/handlingTime/handling_times_grid_filtrable.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME})
    void getPartnerHandlingTimesFilterNoResult() throws Exception {
        getPartnerHandlingTimes(2, 2)
            .andExpect(testJson("data/controller/admin/handlingTime/handling_times_grid_filtrable_no_result.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME})
    void getPartnerHandlingTime() throws Exception {
        getPartnerHandlingTime(1L)
            .andExpect(testJson("data/controller/admin/handlingTime/handling_time_detail.json"))
            .andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME})
    void getPartnerHandlingTimeNotFound() throws Exception {
        getPartnerHandlingTime(0)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void createPartnerHandlingTime() throws Exception {
        postPartnerHandlingTime("data/controller/admin/handlingTime/handling_time_create_request.json")
            .andExpect(status().isCreated());

        PartnerHandlingTime partnerHandlingTime = partnerHandlingTimeRepository.findByIdOrThrow(3L);

        softly.assertThat(partnerHandlingTime)
            .as("Proper fields values should be set")
            .extracting(
                pht -> pht.getPartner().getId(),
                PartnerHandlingTime::getLocationFrom,
                PartnerHandlingTime::getLocationTo,
                PartnerHandlingTime::getHandlingTime
            )
            .containsExactly(1L, 2, 4, Duration.ofSeconds(12600));
        checkBuildWarehouseSegmentTask(100000L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void createPartnerHandlingTimeWithAlreadyExistsLocations() throws Exception {
        postPartnerHandlingTime(
            "data/controller/admin/handlingTime/handling_time_create_already_exists_location_request.json")
            .andExpect(status().isConflict());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void createPartnerHandlingTimeWithPartnerNotFound() throws Exception {
        postPartnerHandlingTime(
            "data/controller/admin/handlingTime/handling_time_create_partner_not_found_request.json")
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void updatePartnerHandlingTime() throws Exception {
        putPartnerHandlingTime(1L, "data/controller/admin/handlingTime/handling_time_update_request.json")
            .andExpect(testJson("data/controller/admin/handlingTime/handling_time_update_response.json"))
            .andExpect(status().isOk());

        PartnerHandlingTime partnerHandlingTime = partnerHandlingTimeRepository.findByIdOrThrow(1L);

        softly.assertThat(partnerHandlingTime)
            .as("Proper fields values should be set")
            .extracting(
                PartnerHandlingTime::getLocationFrom,
                PartnerHandlingTime::getLocationTo,
                PartnerHandlingTime::getHandlingTime
            )
            .containsExactly(2, 4, Duration.ofSeconds(12600));
        checkBuildWarehouseSegmentTask(100000L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void updatePartnerHandlingTimeWithAlreadyExistsLocations() throws Exception {
        putPartnerHandlingTime(1L,
            "data/controller/admin/handlingTime/handling_time_update_already_exists_location_request.json")
            .andExpect(status().isConflict());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void deletePartnerHandlingTime() throws Exception {
        deletePartnerHandlingTime(1L)
            .andExpect(status().isOk());

        Optional<PartnerHandlingTime> partnerHandlingTimeOptional = partnerHandlingTimeRepository.findById(1L);

        softly.assertThat(partnerHandlingTimeOptional.isPresent())
            .as("PartnerHandlingTime should be deleted").isFalse();
        checkBuildWarehouseSegmentTask(100000L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HANDLING_TIME_EDIT})
    void deletePartnerHandlingTimeWithNotFoundId() throws Exception {
        deletePartnerHandlingTime(3L)
            .andExpect(status().isOk());

        softly.assertThat(partnerHandlingTimeRepository.count())
            .as("PartnerHandlingTimes should not be deleted").isEqualTo(2);
    }

    private ResultActions getPartnerHandlingTimes(Integer locationFrom, Integer locationTo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/admin/lms/partner-handling-time");
        if (locationFrom != null) {
            requestBuilder.param("locationFrom", String.valueOf(locationFrom));
        }
        if (locationTo != null) {
            requestBuilder.param("locationTo", String.valueOf(locationTo));
        }

        return mockMvc.perform(requestBuilder);
    }

    private ResultActions getPartnerHandlingTime(long id) throws Exception {
        return mockMvc.perform(get("/admin/lms/partner-handling-time/{id}", id));
    }

    private ResultActions postPartnerHandlingTime(String fileName) throws Exception {
        return mockMvc.perform(post("/admin/lms/partner-handling-time")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(fileName)));
    }

    private ResultActions putPartnerHandlingTime(long id, String fileName) throws Exception {
        return mockMvc.perform(put("/admin/lms/partner-handling-time/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(fileName)));
    }

    private ResultActions deletePartnerHandlingTime(long id) throws Exception {
        return mockMvc.perform(delete("/admin/lms/partner-handling-time/{id}", id));
    }
}
