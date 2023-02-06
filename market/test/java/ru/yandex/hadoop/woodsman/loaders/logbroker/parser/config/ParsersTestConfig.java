package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.config;

import java.net.UnknownHostException;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.MockContextUtil;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.IpRegionLookupService;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.pp.reader.PpReader;
import ru.yandex.market.stats.test.config.PropertiesITestConfig;

/**
 * @author Aleksandr Kormushin <kormushin@yandex-team.ru>
 */
@Configuration
@Import({ParsersConfig.class, PropertiesITestConfig.class})
public class ParsersTestConfig {

    @Bean
    public IpRegionLookupService ipRegionLookupService() throws UnknownHostException {
        return MockContextUtil.getIpRegionLookupService();
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public HealthCheckRegistry healthCheckRegistry() {
        return new HealthCheckRegistry();
    }

    @Bean
    public PpReader ppReader() {
        return () -> {
            Set<Integer> allPp = ImmutableSet.of(1002, 27, 7, 143, 200, 403, 1001, 251);
            HashMultimap<String, Integer> valid = HashMultimap.create();
            valid.putAll("clicks", allPp);
            valid.putAll("cpa_clicks", allPp);
            valid.putAll("vendor_clicks", allPp);
            valid.putAll("beru_clicks", allPp);
            return new PpReader.PpSetup(ImmutableMap.of(PpReader.PpKind.VALID, valid));
        };
    }

}
