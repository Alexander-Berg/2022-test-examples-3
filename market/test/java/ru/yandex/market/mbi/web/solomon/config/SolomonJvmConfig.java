package ru.yandex.market.mbi.web.solomon.config;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.application.MarketApplicationCommonConfig;
import ru.yandex.market.mbi.web.solomon.common.MetricsProvider;
import ru.yandex.market.mbi.web.solomon.metrics.JvmGcMetricsProvider;
import ru.yandex.market.mbi.web.solomon.metrics.JvmMemoryMetricsProvider;
import ru.yandex.market.mbi.web.solomon.metrics.JvmRuntimeMetricsProvider;
import ru.yandex.market.mbi.web.solomon.metrics.JvmThreadMetricsProvider;
import ru.yandex.market.mbi.web.solomon.pull.SolomonJvmController;
import ru.yandex.market.mbi.web.solomon.pull.jvm.JvmMetricsCollector;

public class SolomonJvmConfig extends MarketApplicationCommonConfig {

    public SolomonJvmConfig() {
        super(null, false);
    }

    @Bean
    public JvmMetricsCollector jvmMetricsCollector() {
        List<MetricsProvider> metrics = ImmutableList.of(
                new JvmGcMetricsProvider(),
                new JvmMemoryMetricsProvider(),
                new JvmRuntimeMetricsProvider(),
                new JvmThreadMetricsProvider());
        return new JvmMetricsCollector(metrics);
    }

    @Bean
    public SolomonJvmController solomonJvmController(JvmMetricsCollector jvmMetricsCollector) {
        return new SolomonJvmController(jvmMetricsCollector);
    }
}
