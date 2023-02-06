package ru.yandex.market.health;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.clickhouse.ddl.DdlQuery;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;
import ru.yandex.market.monitoring.ComplicatedMonitoring;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DockerConfiguration.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IntegrationTests {

    @Test
    public void test1Logshatter() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(LogshatterTestConfig.class);
        context.refresh();

        LogShatterMonitoring monitoring = context.getBean(LogShatterMonitoring.class);
        ComplicatedMonitoring.Result result = monitoring.getOverallResult();
        Assert.assertFalse(result.getMessage(), result.getStatus() == MonitoringStatus.CRITICAL);

        // Проверяем что повторное применение DDL не накатывает ничего по второму разу.
        List<DdlQuery> queries = context.getBean(UpdateDDLService.class)
            .updateDDL(context.getBean(ru.yandex.market.logshatter.config.ConfigurationService.class).getConfigs())
            .stream()
            .flatMap(ddl -> Stream.concat(ddl.getUpdates().stream(), ddl.getManualUpdates().stream()))
            .collect(Collectors.toList());
        if (!queries.isEmpty()) {
            Assert.fail(String.format("Some queries were applied twice.\n" +
                    "\n" +
                    "This test launched %s twice. During the second launch we expect no mutating queries to run\n" +
                    "because the first launch should have ran them all already.\n" +
                    "\n" +
                    "This is most likely a query normalization issue. Logshatter runs SHOW CREATE TABLE and\n" +
                    "compares the result with the query that it would run if the table didn't exist.\n" +
                    "SHOW CREATE TABLE returns a normalized query with parentheses and capital letters in all the\n" +
                    "right places. Logshatter doesn't know anything about the normalization rules, so sometimes\n" +
                    "it's necessary to manually edit configs and TableDefinition's to make sure that that\n" +
                    "Logshatter generates normalized queries.\n" +
                    "\n" +
                    "This command will show what queries Logshatter decided to run and why:\n" +
                    "  ./gradlew :health-integration-tests:integrationTest 2>&1 | grep 'Planned DDL'\n" +
                    "\n" +
                    "Queries that were applied twice:\n%s" +
                    "\n",
                UpdateDDLService.class.getSimpleName(),
                queries.stream()
                    .map(DdlQuery::getQueryString)
                    .collect(Collectors.joining("\n  ", "  ", ""))
            ));
        }
    }

    @Test
    public void test2Clickphite() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ClickphiteTestConfig.class);
        context.refresh();

        ConfigurationService configurationService = context.getBean(ConfigurationService.class);
        configurationService.getConfiguration();

        ComplicatedMonitoring monitoring = context.getBean(ComplicatedMonitoring.class);
        ComplicatedMonitoring.Result result = monitoring.getResult();
        Assert.assertEquals(result.getMessage(), MonitoringStatus.OK, result.getStatus());
    }
}
