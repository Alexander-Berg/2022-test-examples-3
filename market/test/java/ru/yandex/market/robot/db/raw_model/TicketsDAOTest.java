package ru.yandex.market.robot.db.raw_model;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
public class TicketsDAOTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    RawModelStorage rawModelStorage;

    @Before
    public void init() {
        List<String> emptyPipelineTypes =
            jdbcTemplate.queryForList("SELECT pipeline_type FROM ticket ORDER BY id", String.class);
        assertThat(emptyPipelineTypes).isEmpty();

        jdbcTemplate.update(
            "INSERT INTO ticket(id, version, vendor_model_id, pipeline_type, type, source_id, category_id) VALUES " +
                "(1, 1, 1, ?::ticket_pipeline_type, 'CREATE', 100, 11)," +
                "(2, 1, 2, ?::ticket_pipeline_type, 'CREATE', 100, 12)",
            AutoGenerationApi.TicketPipelineType.MBO_OPERATOR.name(),
            AutoGenerationApi.TicketPipelineType.AUTOGENERATION_TMS.name()
        );
    }

    @Test
    public void whenGettingTicketCountsByMarketCategoriesReturnValuesMatchingDatabase() {
        Int2IntMap ticketCountsByMarketCategories = rawModelStorage.getTicketCountsByMarketCategories(100);
        assertThat(ticketCountsByMarketCategories.keySet())
            .as("Ожидаемые категории не совпадают с полученными из базы")
            .containsExactly(12);
    }

    @Test
    public void whenSelectingPipelineTypeShouldReturnValuesMatchingDatabase() {
        final List<AutoGenerationApi.TicketPipelineType> result = new ArrayList<>();
        jdbcTemplate.query("SELECT pipeline_type FROM ticket ORDER BY id",
            (ResultSet rs) -> {
                result.add(AutoGenerationApi.TicketPipelineType.valueOf(rs.getString("pipeline_type")));
            }
        );
        assertThat(result)
            .as("Ожидаемые значения pipeline_type не совпадают с полученными из базы")
            .containsExactly(AutoGenerationApi.TicketPipelineType.values());
    }
}
