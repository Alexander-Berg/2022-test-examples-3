package ru.yandex.market.reporting.generator.service;

import org.junit.Test;
import ru.yandex.market.reporting.generator.domain.AuditReportParameters;
import ru.yandex.market.reporting.generator.domain.MarketReportParameters;
import ru.yandex.market.reporting.generator.domain.ReportComponents;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JobPriorityTest {

    @Test
    public void nonForecastJobsShouldBeScheduledEarlier() {
        MarketReportParameters marketReportParameters = new MarketReportParameters();
        marketReportParameters.getComponents().setAssortment(new ReportComponents.Assortment());

        AuditReportParameters auditReportParameters = new AuditReportParameters();

        MarketReportParameters forecastReportParameters = new MarketReportParameters();
        forecastReportParameters.getComponents().setForecaster(new ReportComponents.Forecaster());

        JobExecutor executor = new JobExecutor(null, Collections.emptyList(), null, null);

        Queue<Runnable> queue = new PriorityBlockingQueue<>(10, JobExecutor.JobPriorityComparator.INSTANCE);

        Runnable anyJob = () -> {
        };
        Runnable forecastJob = executor.wrapRunnable("1", forecastReportParameters);
        Runnable auditJob = executor.wrapRunnable("2", auditReportParameters);
        Runnable marketJob = executor.wrapRunnable("3", marketReportParameters);

        queue.add(forecastJob);
        queue.add(auditJob);
        queue.add(anyJob);
        queue.add(marketJob);

        assertThat(queue.poll(), anyOf(is(auditJob), is(marketJob)));
        assertThat(queue.poll(), anyOf(is(auditJob), is(marketJob)));
        assertThat(queue.poll(), is(forecastJob));
        assertThat(queue.poll(), is(anyJob));
    }
}
