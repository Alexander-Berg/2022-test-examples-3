package ru.yandex.market.checkout.pushapi.service;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by oroboros on 13.11.14.
 */
public class GraphiteReporterService implements InitializingBean {
    private static final Pattern SANITIZE = Pattern.compile("[\\.\\s]+");
    private static final String HOSTNAME;
    private static final String METRIC_NAME;

    static {
        String hostname = System.getProperty("host.name");
        if(hostname == null) {
            throw new RuntimeException("No -Dhost.name property provided. Check your run script.");
        }

        HOSTNAME = sanitize(hostname);
        METRIC_NAME = "five_min." + HOSTNAME + ".pushapi";

    }

    public static GraphiteReporterService bean;

    private MetricRegistry metricRegistry = new MetricRegistry();
    Histogram histogram = metricRegistry.histogram(METRIC_NAME + ".timings");

    private String graphiteHost;
    private int graphitePort;

    @Override
    public void afterPropertiesSet() throws Exception {
        Graphite graphite = new Graphite(new InetSocketAddress(graphiteHost, graphitePort));
        GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
        reporter.start(2, TimeUnit.MINUTES);

        final MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        metricRegistry.register(METRIC_NAME + ".heap",
            new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return mbean.getHeapMemoryUsage().getUsed();
                }
            }
        );

        bean = this;
    }

    public static GraphiteReporterService bean() {
        return bean;
    }

    public Histogram getHistogram() {
        return histogram;
    }

    @Required
    public void setGraphiteHost(String host) {
        this.graphiteHost = host;
    }

    @Required
    public void setGraphitePort(int port) {
        this.graphitePort = port;
    }

    private static String sanitize(String s) {
        return SANITIZE.matcher(s).replaceAll("_");
    }

}
