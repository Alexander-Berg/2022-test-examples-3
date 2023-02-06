package ru.yandex.market.olap2.ytreflect;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.load.TestLoadTask;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.olap2.ytreflect.YtTestTable.TABLE_NAME;
import static ru.yandex.market.olap2.ytreflect.YtTestTable.TBL;
import static ru.yandex.market.olap2.ytreflect.YtTestTable.VIEW_NAME;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class CreateRuViewITest {
    @Autowired
    private CreateRuView createRuView;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void mustCreateView() {
        jdbcTemplate.getJdbcOperations().execute("drop view if exists \"" + VIEW_NAME +"\"");
        jdbcTemplate.getJdbcOperations().execute("drop table if exists " + TABLE_NAME + " cascade");
        jdbcTemplate.getJdbcOperations().execute("create table if not exists " + TABLE_NAME + " (" +
            Joiner.on(',').join(YtTestTable.COLUMNS.keySet()
                .stream()
                .map(s -> s + " int null")
                .collect(Collectors.toList())) + ")");
        createRuView.replaceView(new TestLoadTask("teststepeid1", TBL));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from views where table_name = '" + VIEW_NAME + "'",
                Collections.emptyMap(),
                Integer.class
            ),
            is(1));
    }

    public boolean searchStringInExceptionCause(Throwable e, String needle) {
        if(e == null) {
            return false;
        }

        if(e.getMessage().toLowerCase().contains(needle.toLowerCase())) {
            return true;
        }

        return searchStringInExceptionCause(e.getCause(), needle);
    }
}
