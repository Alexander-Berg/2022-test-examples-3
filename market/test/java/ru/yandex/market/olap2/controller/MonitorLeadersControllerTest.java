package ru.yandex.market.olap2.controller;

import com.opentable.db.postgres.embedded.LiquibasePreparer;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.PreparedDbRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.olap2.dao.LoggingJdbcTemplate;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.sla.ImportantCubesPaths;

public class MonitorLeadersControllerTest {
    @Rule
    public PreparedDbRule db = EmbeddedPostgresRules.preparedDatabase(
            LiquibasePreparer.forClasspathLocation("liquibase/changelog.xml")
    );

    @Test
    public void mustBeCrit() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table leaders");
        l.exec("insert into leaders (created_at, hostname) values (now() - interval '1 hour', 'h1')");
        l.exec("insert into leaders (created_at, hostname) values (now() - interval '2 hour', 'h1')");
        l.exec("insert into leaders (created_at, hostname) values (now() - interval '3 hour', 'h1')");
        MonitorLeadersController c = new MonitorLeadersController(
                new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class)));
        String mon = c.tooManyElections().getBody().toString();
        Assert.assertTrue(mon, mon.startsWith(JugglerConstants.CRIT));
    }

    @Test
    public void mustBeOkNoElections() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table leaders");
        MonitorLeadersController c = new MonitorLeadersController(
                new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class)));
        String mon = c.tooManyElections().getBody().toString();
        Assert.assertTrue(mon, mon.startsWith(JugglerConstants.OK));
    }

    @Test
    public void mustBeOkFewElections() {
        LoggingJdbcTemplate l = new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(db.getTestDatabase()),
                "localhost");
        l.exec("truncate table leaders");
        for (int i = 0; i < 30; i++) {
            l.exec("insert into leaders (created_at, hostname) values (now() - interval '1 hour', 'h1')");
        }
        MonitorLeadersController c = new MonitorLeadersController(
                new MetadataDao(l, Mockito.mock(ImportantCubesPaths.class)));
        String mon = c.tooManyElections().getBody().toString();
        Assert.assertTrue(mon, mon.startsWith(JugglerConstants.OK));
    }
}
