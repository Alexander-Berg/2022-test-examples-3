package ru.yandex.market.stat.dicts.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.stat.dicts.config.DictionariesMetricsConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.integration.conf.PropertiesDictionariesUTestConfig;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.TmpDirectoryConfig;
import ru.yandex.market.stat.dicts.services.YtClusters;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nettoyeur
 * @since 10.01.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PropertiesDictionariesUTestConfig.class, DictionariesMetricsTest.Cfg.class})
public class DictionariesMetricsTest {

    @Autowired
    private MetricRegistry metricRegistry;

    @Test
    public void testMetricsNotNull() {
        Map<String, Gauge> gauges = metricRegistry.getGauges();
        assertThat(gauges.get("system.load_average"), notNullValue());
        assertThat(gauges.get("jvm.thread.count"), notNullValue());
        assertThat(gauges.get("disk.logs.used"), notNullValue());
        assertThat(gauges.get("disk.pdata.used"), notNullValue());
        assertThat(gauges.get("disk.tmp.used"), notNullValue());
    }

    @Test
    public void testMetricsValue() {
        Integer value = (Integer) metricRegistry.getGauges().get("jvm.thread.count").getValue();
        assertThat(value, notNullValue());
        assertThat(value, greaterThan(0));
    }

    @Configuration
    @Import({DictionariesMetricsConfig.class})
    public static class Cfg {
        @Bean
        public MetricRegistry metricRegistry() {
            return new MetricRegistry();
        }

        @Bean
        public GraphiteReporter graphiteReporter() {
            return mock(GraphiteReporter.class);
        }

        @Bean
        public MetadataService metadataService() {
            return mock(MetadataService.class);
        }

        @Bean
        public List<DictionaryLoadersHolder> dictionaryLoadersHolders() {
            return new ArrayList<>();
        }

        @Bean
        public DictionaryYtService dictionaryYtService() {
            return mock(DictionaryYtService.class);
        }

        @Bean
        public YtClusters ytClusters() {
            return mock(YtClusters.class);
        }

        @Bean
        public SpringLiquibase liquibase() {
            return mock(SpringLiquibase.class);
        }

        @Bean
        public TmpDirectoryConfig tmpDirectoryConfig() throws IOException {
            TmpDirectoryConfig tmpDirectoryConfig = mock(TmpDirectoryConfig.class);
            when(tmpDirectoryConfig.getTmpBufferDir()).thenReturn("/dictionaries-yt-tmp/");
            return tmpDirectoryConfig;
        }
    }
}
