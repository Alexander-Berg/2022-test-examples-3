package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FFIntervalOperationsTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before.xml")
    void getFFIntervals() throws Exception {
        String response = mockMvc.perform(get("/sync-job-interval")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        softly
                .assertThat(response)
                .is(jsonMatching(extractFileContent("response/ff_interval/find_all.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before.xml")
    void getFFIntervalById() throws Exception {
        String response = mockMvc.perform(get("/sync-job-interval/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        softly
                .assertThat(response)
                .is(jsonMatching(extractFileContent("response/ff_interval/find_by_id.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before.xml")
    public void getFFIntervalByJobNameAndWarehouseIdWhenExists() throws Exception {
        String response = mockMvc.perform(get("/sync-job-interval/job1/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        softly
                .assertThat(response)
                .is(jsonMatching(extractFileContent("response/ff_interval/find_by_job_name_and_warehouse_id.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before.xml")
    public void getFFIntervalByJobNameAndWarehouseIdWhenNotExists() throws Exception {
        mockMvc.perform(get("/sync-job-interval/job234/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before.xml")
    @ExpectedDatabase(value = "classpath:database/states/ff_interval/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateFFInterval() throws Exception {
        mockMvc.perform(post("/sync-job-interval/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/ff_interval/updated_ff_interval.json")))
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * Тестирует на ошибку 400 при попытке апдейта записи с warehouse_id=-1
     */
    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before_wrong_warehouse_id.xml")
    void updateFFIntervalWrongWarehouseID() throws Exception {
        mockMvc.perform(post("/sync-job-interval/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/ff_interval/updated_ff_interval.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before2.xml")
    @ExpectedDatabase(value = "classpath:database/states/ff_interval/after2.xml", assertionMode = NON_STRICT_UNORDERED)
    void createFFInterval() throws Exception {
        mockMvc.perform(post("/sync-job-interval")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/ff_interval/create_ff_interval.json")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/after2.xml")
    @ExpectedDatabase(value = "classpath:database/states/ff_interval/after2.xml", assertionMode = NON_STRICT_UNORDERED)
    void createAlreadyExistingFFInterval() throws Exception {
        mockMvc.perform(post("/sync-job-interval")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/ff_interval/create_ff_interval.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before3.xml")
    @ExpectedDatabase(value = "classpath:database/states/ff_interval/after3.xml", assertionMode = NON_STRICT_UNORDERED)
    public void deleteFFInterval() throws Exception {
        mockMvc.perform(delete("/sync-job-interval/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is2xxSuccessful());
    }

    /**
     * Тестирует на ошибку 400 при попытке удаления записи с warehouse_id=-1
     */
    @Test
    @DatabaseSetup("classpath:database/states/ff_interval/before_wrong_warehouse_id.xml")
    void deleteFFIntervalWrongWarehouseID() throws Exception {
        mockMvc.perform(delete("/sync-job-interval/1")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }
}
