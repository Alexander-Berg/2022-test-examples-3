package ru.yandex.market.deepmind.common.db;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.github.mfvanek.pg.model.index.IndexWithNulls;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests of health of postgres db. Don't add new exceptions without exceptional reason.
 * <p>
 * See https://ivvakhrushev.at.yandex-team.ru/1
 * Or https://st.yandex-team.ru/MBO-24340
 */
public class DeepmindPgIndexHealthTest extends BasePgIndexHealthTest {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    protected List<String> getSchemasToAnalyze() {
        var schemas = jdbcTemplate.queryForList("select schema_name from information_schema.schemata;", String.class);
        return schemas.stream()
            .filter(schema -> !schema.equals("public") // ignore public schema
                && !schema.equals("information_schema") // ignore system schema
                && !schema.equals("pg_catalog") // ignore system schema
                && !schema.equals("default")) // ignore system schema
            .collect(Collectors.toList());
    }

    @Test
    public void getIndexesWithNullValuesShouldReturnNothing() {
        List<IndexWithNulls> indexesWithNulls = getIndexesWithNullValues();

        Assertions.assertThat(indexesWithNulls)
            .containsExactlyInAnyOrder(
                // hidings_search_indx используется для поиска по comment & user_name
                // переделать без использования null и при этом не потерять использование индекса у меня не получилось
                IndexWithNulls.of("msku.hiding", "msku.hidings_search_indx", 0, "comment")
            );
    }
}
