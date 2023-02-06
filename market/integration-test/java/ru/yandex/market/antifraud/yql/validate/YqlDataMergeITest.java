package ru.yandex.market.antifraud.yql.validate;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.yt.YtTablesHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlDataMergeITest {

    @Autowired
    private LoggingJdbcTemplate yqlJdbcTemplate;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Autowired
    private YqlDataMerge yqlDataMerge;

    @Autowired
    private YtTablesHelper ytTablesHelper;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YqlValidatorHelper yqlValidatorHelper;

    private YqlSession dataReadySession;

    @Before
    public void prepareTestData() {
        testDataGenerator.initOnce();
        dataReadySession = yqlValidatorHelper.createSession(new UnvalidatedDay(
                testDataGenerator.getArchiveDay(), 0, UnvalidatedDay.Scale.ARCHIVE));
        yqlValidatorHelper.sessionFiltersExecuted(dataReadySession);
        yqlValidatorHelper.sessionDataReady(dataReadySession);

        if(ytTablesHelper.exists(dataReadySession.getRollbacksDayTable())) {
            ytTablesHelper.remove(dataReadySession.getRollbacksDayTable());
        }
    }

    @Test
    public void mustMergeWithoutDaytable() throws InterruptedException {
        yqlJdbcTemplate.exec("insert into `" + dataReadySession.getDataReadyFile() + "` (c1, c2) " +
                "values ('a1', 'a2'), ('b1', 'b2');");

        yqlDataMerge.dataMerge(dataReadySession);

        commonResultAsserts();
    }

    @Test
    public void mustMergeWithDaytable() throws InterruptedException {
        yqlJdbcTemplate.exec("insert into `" + dataReadySession.getRollbacksDayTable() + "` (c1, c2) " +
                "values ('a1', 'a2');");
        yqlJdbcTemplate.exec("insert into `" + dataReadySession.getDataReadyFile() + "` (c1, c2) " +
                "values ('b1', 'b2');");

        yqlDataMerge.dataMerge(dataReadySession);

        commonResultAsserts();
    }

    private void commonResultAsserts() {
        assertThat(jdbcTemplate.query("select status from sessions where session_id = :session_id",
                "session_id", dataReadySession.getId(),
                String.class), is(SessionStatusEnum.SUCCESSFUL.toString()));

        List<List<String>> result = new ArrayList<>();
        yqlJdbcTemplate.query("select * from CONCAT('" + dataReadySession.getRollbacksDayTable() + "') order by c1",
                (rs) -> {
                    result.add(
                            Arrays.asList(
                                    rs.getString("c1"),
                                    rs.getString("c2")
                            )
                    );
                });
        assertThat(result.size(), is(2));
        assertThat(result.get(0), is(Arrays.asList("a1", "a2")));
        assertThat(result.get(1), is(Arrays.asList("b1", "b2")));
    }
}
