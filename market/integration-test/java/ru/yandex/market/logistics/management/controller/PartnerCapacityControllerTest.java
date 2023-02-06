package ru.yandex.market.logistics.management.controller;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.repository.PartnerCapacityDayOffRepository;
import ru.yandex.market.logistics.management.repository.PartnerCapacityRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@DatabaseSetup("/data/controller/partnerCapacity/prepare_data.xml")
class PartnerCapacityControllerTest extends AbstractContextualTest {

    private static final String EXISTED_DAY_OFF_DAY = "2019-05-01";
    private static final String NEW_DAY_OFF_DAY = "2019-05-04";

    @Autowired
    private PartnerCapacityDayOffRepository partnerCapacityDayOffRepository;

    @Autowired
    private PartnerCapacityRepository partnerCapacityRepository;

    @Test
    void getCapacities() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/partner-capacities"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partnerCapacity/capacities_response.json"));
    }

    @Test
    void createCapacityDayOffWithNew() throws Exception {
        createCapacityDayOffAndExpect(NEW_DAY_OFF_DAY,
            "data/controller/newDayOff_response.json");
    }

    @Test
    void createCapacityDayOffWithExisted() throws Exception {
        createCapacityDayOffAndExpect(EXISTED_DAY_OFF_DAY,
            "data/controller/existedDayOff_response.json");
    }

    @Test
    void createCapacityDayOffWithNonExistentCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities/11/days-off")
                .param("day", EXISTED_DAY_OFF_DAY)
        )
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistedCapacityDayOff() throws Exception {
        deleteCapacityDayOffAndExpect(EXISTED_DAY_OFF_DAY);

        softly.assertThat(partnerCapacityDayOffRepository.findById(1L).orElse(null))
            .as("DayOff should be deleted")
            .isNull();
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partnerCapacity/duplicate_days_off.xml",
        type = DatabaseOperation.INSERT
    )
    void deleteMultipleExistingCapacityDaysOff() throws Exception {
        deleteCapacityDayOffAndExpect(EXISTED_DAY_OFF_DAY);

        softly.assertThat(partnerCapacityDayOffRepository.findById(1L).orElse(null))
            .as("DayOff should be deleted")
            .isNull();
    }

    @Test
    void deleteNonExistedCapacityDayOff() throws Exception {
        long expectedDayOffCount = partnerCapacityDayOffRepository.count();

        deleteCapacityDayOffAndExpect(NEW_DAY_OFF_DAY);

        softly.assertThat(partnerCapacityDayOffRepository.count())
            .as("Initial amount of daysOff should remain")
            .isEqualTo(expectedDayOffCount);
    }

    @Test
    void deleteCapacityDayOffWithNonExistentCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/externalApi/partner-capacities/11/days-off")
                .param("day", EXISTED_DAY_OFF_DAY)
        )
            .andExpect(status().isNotFound());
    }

    @Test
    void createCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/capacity_request.json"))
        )
            .andExpect(status().isCreated());

        softly.assertThat(partnerCapacityRepository.count()).isEqualTo(6);
    }

    @Test
    void createCapacityWithoutValue() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/capacity_request_without_value.json"))
        )
            .andExpect(status().isBadRequest());

        softly.assertThat(partnerCapacityRepository.count()).isEqualTo(5);
    }

    @Test
    void createInvalidCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/capacity_request_invalid_partner.json"))
        )
            .andExpect(status().isNotFound());

        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/capacity_request_invalid_platform_client.json"))
        )
            .andExpect(status().isNotFound());

        softly.assertThat(partnerCapacityRepository.count()).isEqualTo(5);
    }

    @Test
    void createCapacityWithCountingType() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/capacity_request_with_counting_type.json"))
        )
            .andExpect(status().isCreated());

        softly.assertThat(partnerCapacityRepository.count())
            .as("Asserting that the total partner capacities count is valid")
            .isEqualTo(6);
        List<PartnerCapacity> partnerCapacities = partnerCapacityRepository.findAllByPartnerId(2L);
        softly.assertThat(partnerCapacities.size())
            .as("Asserting that the found partner capacities count is valid")
            .isEqualTo(1);
        PartnerCapacity partnerCapacity = partnerCapacities.get(0);
        softly.assertThat(partnerCapacity.getCountingType())
            .as("Asserting that the partner capacity's counting type is valid")
            .isEqualTo(CountingType.ITEM);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partnerCapacity/service_capacity_value_update.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerCapacity/after/service_capacity_value_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner-capacities/1/value")
                .param("value", "1000")
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partnerCapacity/capacity_response.json"));
    }

    @Test
    void updateNonExistedCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner-capacities/10/value")
                .param("value", "1000")
        )
            .andExpect(status().isNotFound());
    }

    @Test
    void searchCapacity() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner-capacities/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/search_all.json"))
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partnerCapacity/capacities_response.json"));

        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner-capacities/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/search_with_null.json"))
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partnerCapacity/search_with_null_response.json"));

        mockMvc.perform(
            MockMvcRequestBuilders
                .put("/externalApi/partner-capacities/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson(
                    "data/controller/partnerCapacity/search_combine.json"))
        )
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/partnerCapacity/search_combine_response.json"));
    }


    private void deleteCapacityDayOffAndExpect(String day) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .delete("/externalApi/partner-capacities/1/days-off")
                .param("day", day)
        )
            .andExpect(status().isNoContent());
    }

    private void createCapacityDayOffAndExpect(String day, String expectedResponseFileName) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/externalApi/partner-capacities/1/days-off")
                .param("day", day)
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(expectedResponseFileName));
    }
}
