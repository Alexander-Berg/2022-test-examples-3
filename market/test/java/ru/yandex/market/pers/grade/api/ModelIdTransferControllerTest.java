package ru.yandex.market.pers.grade.api;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.grade.MockedPersGradeTest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author vvolokh
 * 08.08.2019
 */
public class ModelIdTransferControllerTest extends MockedPersGradeTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCsvLoading() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/data/model_id_transfer.csv"),
            StandardCharsets.UTF_8
        );

        doImport(csv);

        assertEquals(Long.valueOf(2), pgJdbcTemplate.queryForObject("select count(*) from model_id_transfer", Long.class));
        assertEquals(Long.valueOf(2), pgJdbcTemplate.queryForObject("select new_id from model_id_transfer where old_id = 1", Long.class));
        assertEquals(Long.valueOf(1234567890), pgJdbcTemplate.queryForObject("select new_id from model_id_transfer where old_id = 123456789", Long.class));
    }

    @Test
    public void testSingleRequestLoading() throws Exception {
        doImport(123L, 124L);

        assertEquals(Long.valueOf(1), pgJdbcTemplate.queryForObject("select count(*) from model_id_transfer", Long.class));
        assertEquals(Long.valueOf(124), pgJdbcTemplate.queryForObject("select new_id from model_id_transfer where old_id = 123", Long.class));
    }

    private void doImport(String csv) throws Exception {
        mockMvc.perform(
            post("/api/transfer/model/csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .content(csv)
        )
        .andDo(print())
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    }

    private void doImport(Long oldId, Long newId) throws Exception {
        mockMvc.perform(
            post("/api/transfer/model/single")
                .param("oldId", String.valueOf(oldId))
                .param("newId", String.valueOf(newId))
        )
        .andDo(print())
        .andExpect(status().is2xxSuccessful())
        .andReturn();
    }
}
