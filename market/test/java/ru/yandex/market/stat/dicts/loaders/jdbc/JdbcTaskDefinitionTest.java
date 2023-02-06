package ru.yandex.market.stat.dicts.loaders.jdbc;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.time.LocalTime;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.stat.dicts.common.ConversionStrategy;
import ru.yandex.market.stat.dicts.config.JdbcConfig;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.jdbc.db.TransactionIsolation;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.stat.dicts.bazinga.BazingaHelper.HALF_HOURLY;


public class JdbcTaskDefinitionTest {
    private Map<String, List<JdbcLoadConfigFromFile>> tasks;
    private List<JdbcTaskDefinition> loads;

    @Before
    public void setUp() throws Exception {
        JdbcConfig jdbcConfig = new JdbcConfig();
        FieldUtils.writeField(jdbcConfig, "jdbcDictionariesConfigFile", "/jdbc-test-dictionaries.yaml", true);
        tasks = ReflectionTestUtils.invokeMethod(jdbcConfig, "readTasks");
        loads = Objects.requireNonNull(tasks).entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(JdbcLoadConfigFromFile::flattenLoads)
                        .flatMap(Collection::stream)
                        .map(task -> task.setSystemSourceAndReturn(e.getKey()))
                )
                .collect(Collectors.toList());
    }

    @Test
    public void testConversionStrategy() {
        assertThat(tasks, notNullValue());
        assertThat(tasks.keySet(), not(empty()));
        assertThat(tasks.get("test5").get(0).getConversionStrategy(), is(ConversionStrategy.LEGACY));

        List<ConversionStrategy> list = tasks.entrySet().stream()
                .filter(tap -> !"test5".equals(tap.getKey())).flatMap(tap -> tap.getValue().stream())
                .map(JdbcLoadConfigFromFile::getConversionStrategy)
                .collect(Collectors.toList());
        assertTrue(list.stream()
                .allMatch(strategy -> strategy == ConversionStrategy.STANDARD));
    }

    @Test
    public void testLoadersMoreThanTasks() {
        assertThat(tasks, notNullValue());
        assertThat(tasks.keySet(), hasSize(11));
        assertThat(loads, notNullValue());
        assertThat(loads, hasSize(16));
    }

    @Test
    public void testDefaultScaleInfo() {
        testLoaderParams("test_table2", LoaderScale.DEFAULT, null, -1, null, TransactionIsolation.DEFAULT, true,
                false, false, false);
    }

    @Test
    public void testNewDestination() {
        System.out.println(loads);
        testLoaderParams("stuff/test_renamed_table3", LoaderScale.DAYLY, null, 42, 24, TransactionIsolation.DEFAULT,
                false, false, true, false);
        testLoaderParams("stuff/test_renamed_table3", LoaderScale.HOURLY, null, 3, null, TransactionIsolation.DEFAULT
                , true, false, true, false);
    }

    @Test
    public void testMultipleScales() {
        testLoaderParams("test_table4", LoaderScale.HOURLY, HALF_HOURLY, 3, 1, TransactionIsolation.DEFAULT, false, false, true, true, LocalTime.of(6, 0));
        testLoaderParams("test_table4", LoaderScale.DAYLY, "0 * * * *", 55, 12, TransactionIsolation.DEFAULT, true,
                false, true, true);
        testLoaderParams("test_table4", LoaderScale.DEFAULT, HALF_HOURLY, 55, 12, TransactionIsolation.DEFAULT, false
                , true, true, true);
    }

    @Test
    public void testTransactionIsolation() {
        testLoaderParams("test_table_with_tx6", LoaderScale.DEFAULT, null, -1, null,
                TransactionIsolation.READ_UNCOMMITTED);
        testLoaderParams("test_table_without6", LoaderScale.DEFAULT, null, -1, null, TransactionIsolation.DEFAULT,
                false, true, false, false);
    }

    @Test
    public void testLoadTimeout() {
        final String taskName = "testLoadTimeout";

        final JdbcLoadConfigFromFile testLoadTimeout = tasks.get(taskName)
                .get(0);
        final Long expected = 111L;
        final Long actual = testLoadTimeout.getLoadTimeoutMinutes();
        assertEquals(expected, actual);
    }

    @Test
    public void testReadyTime() {
        final String taskName = "testReadyTime";

        final JdbcLoadConfigFromFile testReadyTime = tasks.get(taskName)
                .get(0);
        final LocalTime expected = LocalTime.of(0, 0, 0, 0);
        final LocalTime actual = testReadyTime.getReadyTime();
        assertEquals(expected, actual);
    }

    @Test
    public void testDefaultLoadTimeout() {
        final String taskName = "testLoadTimeout";

        final Long expected = 720L;
        final Predicate<JdbcTaskDefinition> defaultLoadTimeoutOnly = task -> !task.getSourceTable().equals(taskName);
        loads.stream()
                .filter(defaultLoadTimeoutOnly)
                .forEach(task ->
                        assertEquals(String.format("Wrong default load timeout for %s", task.getSourceTable()),
                                expected, task.getLoadTimeoutMinutes()
                        )
                );
    }

    @Test
    public void testSortBy() {
        final String taskName = "testSortBy";

        final JdbcLoadConfigFromFile jdbcConfig = tasks.get(taskName)
                .get(0);
        final List<String> expected = asList("first", "second");
        final List<String> actual = jdbcConfig.getSortBy();
        assertEquals(expected, actual);
    }

    @Test
    public void testDefaultSortBy() {
        final String taskName = "testSortBy";

        final Predicate<JdbcTaskDefinition> defaultSortByOnly = task -> !task.getSourceTable().equals(taskName);
        final List<Object> expected = emptyList();
        loads.stream()
                .filter(defaultSortByOnly)
                .forEach(task -> assertEquals(String.format("Wrong default sortBy for %s", task.getSourceTable()),
                        expected, task.getSortBy()
                        )

                );
    }

    @Test
    public void testGetSystemSource() {
        assertThat(loads.get(0).getSystemSource(), is("test1"));
        assertThat(loads.get(1).getSystemSource(), is("test2"));
        assertThat(loads.get(2).getSystemSource(), is("test3"));
        assertThat(loads.get(3).getSystemSource(), is("test3"));

        // Different Scales - one systemSource
        assertThat(loads.get(4).getSystemSource(), is("test4"));
        assertThat(loads.get(5).getSystemSource(), is("test4"));
        assertThat(loads.get(6).getSystemSource(), is("test4"));

        // Users systemSource
        assertThat(loads.get(7).getSystemSource(), is("oroboros"));


        assertThat(loads.get(8).getSystemSource(), is("test6"));
        assertThat(loads.get(9).getSystemSource(), is("test6"));
        assertThat(loads.get(10).getSystemSource(), is("mbi"));
        assertThat(loads.get(11).getSystemSource(), is("testLoadTimeout"));
        assertThat(loads.get(12).getSystemSource(), is("testSortBy"));
    }

    private void testLoaderParams(String destinationTable, LoaderScale scale, String cron, long lltDays,
                                  Integer loadPeriodHours, TransactionIsolation transactionIsolation) {
        testLoaderParams(destinationTable, scale, cron, lltDays, loadPeriodHours, transactionIsolation, false, false,
                false, false);
    }

    private void testLoaderParams(String destinationTable, LoaderScale scale, String cron, long lltDays,
                                  Integer loadPeriodHours, TransactionIsolation transactionIsolation, Boolean isSla,
                                  Boolean isHeavy,
                                  Boolean allowEmpty, Boolean skipEmpty) {
        testLoaderParams( destinationTable,  scale,  cron,  lltDays,loadPeriodHours,  transactionIsolation,  isSla,isHeavy,
                 allowEmpty,  skipEmpty, null);
    }

    private void testLoaderParams(String destinationTable, LoaderScale scale, String cron, long lltDays,
                                  Integer loadPeriodHours, TransactionIsolation transactionIsolation, Boolean isSla,
                                  Boolean isHeavy,
                                  Boolean allowEmpty, Boolean skipEmpty, LocalTime time) {
        List<JdbcTaskDefinition> loadTasks =
                loads.stream().filter(l -> l.getRelativePath().equals(destinationTable) && l.getScale() == scale).collect(Collectors.toList());
        assertThat(loadTasks.size(), is(1));
        JdbcTaskDefinition task = loadTasks.get(0);
        assertNotNull(task);
        assertThat(task.getScale(), CoreMatchers.is(scale));
        assertThat(task.getTtlDays(), CoreMatchers.is(lltDays));

        assertThat(task.isSla(), is(isSla));
        assertThat(task.isAllowEmpty(), is(allowEmpty));
        assertThat(task.isSkipEmpty(), is(skipEmpty));
        assertThat(task.isHeavy(), is(isHeavy));
        if (cron == null) {
            assertNull(task.getCron());
        } else {
            assertThat(task.getCron(), CoreMatchers.is(cron));
        }

        if (loadPeriodHours == null) {
            assertNull(task.getLoadPeriodHours());
        } else {
            assertThat(task.getLoadPeriodHours(), CoreMatchers.is(loadPeriodHours));
        }

        assertThat(task.getTransactionIsolation(), CoreMatchers.is(transactionIsolation));

        if (time == null) {
            assertNull(task.getReadyTime());
        } else {
            assertThat(task.getReadyTime(), CoreMatchers.is(time));
        }
    }
}
