package ru.yandex.market.stat.dicts.integration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableSet;
import com.zaxxer.hikari.HikariDataSource;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.stat.dicts.bazinga.BazingaHelper;
import ru.yandex.market.stat.dicts.config.ExtraJdbcLoadersConfig;
import ru.yandex.market.stat.dicts.config.JdbcConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.jdbc.JdbcTaskDefinition;
import ru.yandex.market.stat.dicts.loaders.jdbc.TypelessJdbcLoader;
import ru.yandex.market.stat.dicts.monitoring.MissingConfiguration;
import ru.yandex.market.stat.dicts.services.DictionaryStorage;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.stat.dicts.bazinga.BazingaHelper.HALF_HOURLY_REGEX;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JdbcConfigSpringTest.Configuration.class)
@ActiveProfiles("integration-tests")
public class JdbcConfigSpringTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Autowired(required = false)
    private JdbcTemplate mbiJdbcTemplate;

    @Autowired(required = false)
    private JdbcTemplate test2JdbcTemplate;

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    @Autowired
    private MissingConfiguration missingConfiguration;

    @Test
    public void autowiredJdbcTemplateShouldNotBeNull() {
        assertThat(test2JdbcTemplate, notNullValue());
        assertThat(mbiJdbcTemplate, notNullValue());
    }

    @Test
    public void beanDefinitionsShouldPresentInContext() {
        assertThat(Arrays.asList(beanFactory.getBeanDefinitionNames()), hasItems(
                "test2JdbcTemplate", "test2DataSource",
                "test3JdbcTemplate",
                "mbiJdbcTemplate"
        ));
    }

    @Test
    public void noAnyBeanNoTables() {
        assertThat(beanFactory.containsBean("testDataSource"), is(false));
        assertThat(beanFactory.containsBean("testJdbcTemplate"), is(false));
    }

    @Test
    public void noAnyBeanNoDataSourceConfig() {
        assertThat(beanFactory.containsBean("test1DataSource"), is(true));
        assertThat(beanFactory.containsBean("test1JdbcTemplate"), is(false));
        assertThat(beanFactory.containsBean("missingConfiguration"), is(true));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().keySet(), hasItem("test1"));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().get("test1").getTap(), is("test1"));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().get("test1").getMissingProperties(),
                Matchers.equalTo(ImmutableSet.of("driver", "url", "username", "password")));
        assertThat(beanFactory.containsBean("jdbc_loader_test_table1"), is(false));
    }

    @Test
    public void noAnyBeanWithDataSourceConfig() {
        assertThat(beanFactory.containsBean("test2DataSource"), is(true));
        assertThat(beanFactory.containsBean("test2JdbcTemplate"), is(true));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().keySet(), not(hasItem("test2")));
        JdbcTemplate test2JdbcTemplate = beanFactory.getBean("test2JdbcTemplate", JdbcTemplate.class);
        DataSource test2DataSource = beanFactory.getBean("test2DataSource", HikariDataSource.class);
        assertThat(test2JdbcTemplate.getDataSource(), is(test2DataSource));
        assertThat(((HikariDataSource) test2JdbcTemplate.getDataSource()).getSchema(), is("my_schema_path"));
        TypelessJdbcLoader loader = testLoaderParams("test_table2", "test_table2", LoaderScale.DEFAULT, null, -1, null);

        assertThat(loader.getTask().getSchema(), is("my_schema_path"));
        assertThat(loader.getTask().getSql(), is("SELECT * FROM my_schema_path.test_table2"));
    }

    @Test
    public void testReadyTimeCome() throws IOException {
        TypelessJdbcLoader loader = testLoaderParams("test_ready_time", "test_ready_time", LoaderScale.DEFAULT, null, -1, null, LocalTime.of(0, 0));
        assertTrue(loader.sourceExistsForDay(DEFAULT_CLUSTER, LocalDateTime.now()));
        LocalDateTime lastLoadTime = LocalDateTime.of(2020, Month.DECEMBER, 1, 0, 0, 0);
        assertTrue(loader.needLoad(DEFAULT_CLUSTER, lastLoadTime, LocalDateTime.now()));
    }

    @Test
    public void testReadyTimeNotCome() throws IOException {
        TypelessJdbcLoader loader = testLoaderParams("test_ready_time_2", "test_ready_time_2-1d", LoaderScale.DAYLY, null, -1, null, LocalTime.of(23, 59, 59));
        assertFalse(loader.sourceExistsForDay(DEFAULT_CLUSTER, LocalDateTime.now()));
        LocalDateTime lastLoadTime = LocalDateTime.of(2020, Month.DECEMBER, 1, 0, 0, 0);
        assertFalse(loader.needLoad(DEFAULT_CLUSTER, lastLoadTime, LocalDateTime.now()));

        loader = testLoaderParams("test_ready_time_2", "test_ready_time_2-1h", LoaderScale.HOURLY, null, 3, null, LocalTime.of(23, 59, 59));
        assertFalse(loader.sourceExistsForDay(DEFAULT_CLUSTER, LocalDateTime.now()));

        assertFalse(loader.needLoad(DEFAULT_CLUSTER, lastLoadTime, LocalDateTime.now()));
    }

    @Test
    public void dataSourceSpringBean() {
        assertThat(beanFactory.containsBean("test3DataSource"), is(true));
        assertThat(beanFactory.containsBean("test3JdbcTemplate"), is(true));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().keySet(), not(hasItem("test3")));
        HikariDataSource test3DataSource = beanFactory.getBean("test3DataSource", HikariDataSource.class);
        assertThat(test3DataSource.getDriverClassName(), is("oracle.jdbc.driver.OracleDriver"));
        JdbcTemplate test3JdbcTemplate = beanFactory.getBean("test3JdbcTemplate", JdbcTemplate.class);
        assertThat(test3JdbcTemplate.getQueryTimeout(), is(42));
        assertThat(test3JdbcTemplate.getDataSource(), is(test3DataSource));

        testLoaderParams("test_renamed_table3", "test_renamed_table3-1d", LoaderScale.DAYLY, null, 42, 24);
    }

    @Test
    public void dataSourceAndTemplateSpringBean() {
        assertThat(beanFactory.containsBean("test4DataSource"), is(true));
        assertThat(beanFactory.containsBean("test4JdbcTemplate"), is(true));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().keySet(), not(hasItem("test4")));
        HikariDataSource test4DataSource = beanFactory.getBean("test4DataSource", HikariDataSource.class);
        assertThat(test4DataSource.getDriverClassName(), is("oracle.jdbc.driver.OracleDriver"));
        JdbcTemplate test4JdbcTemplate = beanFactory.getBean("test4JdbcTemplate", JdbcTemplate.class);
        assertThat(test4JdbcTemplate.getQueryTimeout(), is(43));

        testLoaderParams("test_table4", "test_table4-1h", LoaderScale.HOURLY, BazingaHelper.HALF_HOURLY, 3, 1, LocalTime.of(6, 0));
        testLoaderParams("test_table4", "test_table4-1d", LoaderScale.DAYLY, "0 * * * *", 55, 12);
        testLoaderParams("test_table4", "test_table4", LoaderScale.DEFAULT, BazingaHelper.HALF_HOURLY, 55, 12);


    }

    private TypelessJdbcLoader testLoaderParams(String dictioaryName, String dictNameForLoader, LoaderScale scale, String cron, long ttlDays, Integer loadPeriodHour) {
        return testLoaderParams(dictioaryName, dictNameForLoader, scale, cron, ttlDays, loadPeriodHour, null);
    }

    private TypelessJdbcLoader testLoaderParams(String dictioaryName, String dictNameForLoader, LoaderScale scale, String cron, long ttlDays, Integer loadPeriodHours, LocalTime time) {
        assertThat(getLoadTables(), hasItem(dictNameForLoader));
        DictionaryLoader<?> loader = getTableLoader(dictioaryName, scale);
        assertNotNull(loader);
        assertThat(loader.getScale(), is(scale));
        assertThat(loader.getDictionary().nameForLoader(), is(dictNameForLoader));
        assertThat(((TypelessJdbcLoader) loader).getTask().getTtlDays(), is(ttlDays));

        if (cron == null) {
            assertNull(((TypelessJdbcLoader) loader).getTask().getCron());
            assertNotNull(loader.getCron().toPrettyString());
        } else {
            assertThat(((TypelessJdbcLoader) loader).getTask().getCron(), is(cron));
            if (cron.equals(BazingaHelper.HALF_HOURLY)) {
                assertTrue(loader.getCron().toPrettyString().matches(HALF_HOURLY_REGEX));
            } else {
                assertThat(loader.getCron().toPrettyString(), is(BazingaHelper.cron(cron).toPrettyString()));
            }
        }

        if (loadPeriodHours == null) {
            assertNull(((TypelessJdbcLoader) loader).getTask().getLoadPeriodHours());
        } else {
            assertThat(((TypelessJdbcLoader) loader).getTask().getLoadPeriodHours(), is(loadPeriodHours));
        }

        JdbcTaskDefinition task = ((TypelessJdbcLoader) loader).getTask();
        if (time == null) {
            assertNull(task.getReadyTime());
        } else {
            assertThat(task.getReadyTime(), CoreMatchers.is(time));
        }
        return (TypelessJdbcLoader) loader;
    }

    @Test
    public void jdbcTemplateSpringBean() {
        assertThat(beanFactory.containsBean("test5DataSource"), is(false));
        assertThat(beanFactory.containsBean("test5JdbcTemplate"), is(true));
        assertThat(missingConfiguration.getMissingTapConfigurationMap().keySet(), not(hasItem("test5")));
        JdbcTemplate test5JdbcTemplate = beanFactory.getBean("test5JdbcTemplate", JdbcTemplate.class);
        assertThat(test5JdbcTemplate.getQueryTimeout(), is(44));
        assertThat(getLoadTables(), hasItem("test_table5"));
        DictionaryLoader<?> loader = getTableLoader("test_table5", LoaderScale.DEFAULT);
        assertNotNull(loader);
        assertThat(loader.getDictionary().nameForLoader(), is("test_table5"));

        JdbcTemplate jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(ReflectionTestUtils.getField(loader, "jdbcLoader"), "jdbcTemplate");
        assertThat(jdbcTemplate.getQueryTimeout(), is(44));
        assertThat(test5JdbcTemplate, is(jdbcTemplate));
    }

    private Set<String> getLoadTables() {
        return beanFactory.getBeansOfType(DictionaryLoadersHolder.class).values().stream()
                .flatMap(h -> h.getLoaders().stream())
                .map(d -> d.getDictionary().nameForLoader())
                .collect(Collectors.toSet());
    }

    private DictionaryLoader<?> getTableLoader(String table, LoaderScale scale) {
        return beanFactory.getBeansOfType(DictionaryLoadersHolder.class).values().stream()
                .flatMap(h -> h.getLoaders().stream())
                .filter(d -> d.getDictionary().getName().equals(table) && d.getDictionary().getScale() == scale)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Loader for " + table + " " + scale + " not found"));
    }

    @Profile({"integration-tests"})
    @PropertySource("classpath:jdbc-config-spring-test.properties")
    @Import({ExtraJdbcLoadersConfig.class, JdbcConfig.class})
    public static class Configuration {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public DictionaryStorage ytDictionaryStorage() {
            return new YtDictionaryStorage(null, null, null);
        }

        @Bean
        public DataSource test3DataSource() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
            return dataSource;
        }

        @Bean
        public DataSource test4DataSource() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
            return dataSource;
        }

        @Bean
        public JdbcTemplate test4JdbcTemplate() {
            JdbcTemplate test4JdbcTemplate = new JdbcTemplate(test4DataSource());
            test4JdbcTemplate.setQueryTimeout(43);
            return test4JdbcTemplate;
        }

        @Bean
        public JdbcTemplate test5JdbcTemplate() {
            JdbcTemplate test4JdbcTemplate = new JdbcTemplate(new HikariDataSource());
            test4JdbcTemplate.setQueryTimeout(44);
            return test4JdbcTemplate;
        }

        @Bean
        public JdbcTemplate testReadyTimeJdbcTemplate() {
            JdbcTemplate testReadyTimeJdbcTemplate = new JdbcTemplate(new HikariDataSource());
            return testReadyTimeJdbcTemplate;
        }
    }
}
