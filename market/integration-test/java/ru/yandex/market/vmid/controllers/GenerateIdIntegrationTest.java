package ru.yandex.market.vmid.controllers;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.vmid.AbstractIntegrationTest;
import ru.yandex.market.vmid.controllers.dto.Vmid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GenerateIdIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void generateIds_negative() throws Exception {
        String body = "[{\"feedId\": 123}]";
        mvc.perform(post("/vmids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().
                        is4xxClientError());
    }

    @Test
    public void generateIds() throws Exception {
        ArrayList<Vmid> vmids = new ArrayList<>(Arrays.asList(
                new Vmid().setOfferId("1id").setFeedId(1L),
                new Vmid().setOfferId("2id").setFeedId(2L)
        ));

        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(vmids);
        String response = mvc.perform(post("/vmids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Vmid> actual = mapper.readValue(response, TypeFactory.defaultInstance().constructCollectionType(List.class,
                Vmid.class));
        List<Vmid> expected = Arrays.asList(
                new Vmid().setOfferId("1id").setFeedId(1L).setVmid(2000000000000L),
                new Vmid().setOfferId("2id").setFeedId(2L).setVmid(2000000000001L)
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void generateId() throws Exception {
        getId(10)
                .andExpect(status().isOk())
                .andExpect(content().string("{\"vmid\":2000000000000}"));
    }

    @Test
    public void generateId_LT() {
        int limit = 100_000;
        Random random = new Random();

        IntStream.generate(() -> random.nextInt(100))
                .parallel()
                .filter(i -> i > 0)
                .limit(limit)
                .forEach(this::getId);
    }

    @Test
    public void generateIds_bigBatchWithDeleted() throws Exception {
        final LocalDate yesterday = LocalDate.now().minusDays(1);
        jdbcTemplate.batchUpdate("insert into ids (feed_id, offer_id, last_request) values (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setInt(1, i);
                        ps.setString(2, String.valueOf(i));
                        ps.setDate(3, Date.valueOf(yesterday));
                    }

                    @Override
                    public int getBatchSize() {
                        return 2500;
                    }
                });


        final List<Vmid> vmids = LongStream.range(0, 5000)
                .mapToObj(i -> new Vmid().setOfferId(i + ".id")
                        .setFeedId(i))
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(vmids);

        mvc.perform(post("/vmids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    private ResultActions getId(int ids) {
        try {
            return mvc.perform(get("/vmid")
                    .param("offerId", ids + "id")
                    .param("feedId", String.valueOf(ids)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
