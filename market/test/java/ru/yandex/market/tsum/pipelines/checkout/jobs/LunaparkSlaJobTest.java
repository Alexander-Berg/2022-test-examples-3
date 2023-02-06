package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.clients.lunapark.LunaparkApi;
import ru.yandex.market.tsum.clients.lunapark.models.PercentileResponse;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEvent;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LunaparkSlaJobTest.Config.class)
public class LunaparkSlaJobTest {
    @Autowired
    private LunaparkApi api;
    @Autowired
    private JobTester jobTester;

    @Test
    public void execute() {
        Mockito.when(api.checkResultReady(12345L))
            .thenReturn(true);

        Mockito.when(api.getPercentile(12345L, "case"))
            .thenReturn(Collections.singletonList(
                new PercentileResponse("0.95", 10000)
            ));

        LunaparkSlaJob lunaparkSlaJob = jobTester.jobInstanceBuilder(LunaparkSlaJob.class)
            .withBeanIfNotPresent(api)
            .withResource(LunaparkSlaJobConfig.builder()
                .withSla("case", 1000, "0.95")
                .build())
            .withResource(new LunaparkIdResource(12345L))
            .create();

        TestJobContext context = new TestJobContext();
        Exception expected = null;
        try {
            lunaparkSlaJob.execute(context);
        } catch (Exception ex) {
            expected = ex;
        }

        Assert.assertThat(expected, Matchers.instanceOf(JobManualFailException.class));
        Assert.assertThat(expected.getMessage(), is("Failed slas: case"));

        ArgumentCaptor<NotificationEvent> notificationEvent = ArgumentCaptor.forClass(NotificationEvent.class);
        ArgumentCaptor<Map<String, Object>> arguments = ArgumentCaptor.forClass(Map.class);

        Mockito.verify(context.notifications())
            .notifyAboutEvent(notificationEvent.capture(), arguments.capture());

        List<Sla> failedSlas = (List<Sla>) arguments.getValue().get("failedSlas");
        Assert.assertThat(failedSlas, CoreMatchers.notNullValue());
        Assert.assertThat(failedSlas, Matchers.hasSize(1));
        Assert.assertThat(failedSlas.get(0).getCaseName(), is("case"));
        Assert.assertThat(failedSlas.get(0).getTimingMs(), is(1000));
        Assert.assertThat(failedSlas.get(0).getPercentile(), is("0.95"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void executeAndFailOnUnknownSla() throws Exception {
        Mockito.when(api.checkResultReady(12345L))
            .thenReturn(true);

        Mockito.when(api.getPercentile(12345L, "case"))
            .thenReturn(Collections.emptyList());

        TestJobContext context = new TestJobContext();

        LunaparkSlaJob lunaparkSlaJob = jobTester.jobInstanceBuilder(LunaparkSlaJob.class)
            .withBeanIfNotPresent(api)
            .withResource(LunaparkSlaJobConfig.builder()
                .withSla("case", 1000, "0.95")
                .withFailIfSlaForNameNotFound(true)
                .build())
            .withResource(new LunaparkIdResource(12345L))
            .create();

        lunaparkSlaJob.execute(context);
    }

    @Test
    public void executeAndNotFailOnUnknownSla() throws Exception {
        Mockito.when(api.checkResultReady(12345L))
            .thenReturn(true);

        Mockito.when(api.getPercentile(12345L, "case"))
            .thenReturn(Collections.emptyList());

        TestJobContext context = new TestJobContext();

        LunaparkSlaJob lunaparkSlaJob = jobTester.jobInstanceBuilder(LunaparkSlaJob.class)
            .withBeanIfNotPresent(api)
            .withResource(LunaparkSlaJobConfig.builder()
                .withSla("case", 1000, "0.95")
                .withFailIfSlaForNameNotFound(false)
                .build())
            .withResource(new LunaparkIdResource(12345L))
            .create();

        lunaparkSlaJob.execute(context);

        Mockito.verify(context.notifications(), Mockito.never()).notifyAboutEvent(any(), any());
    }

    @Configuration
    @Import(JobTesterConfig.class)
    public static class Config {
        @Bean
        public LunaparkApi lunaparkApi() {
            return Mockito.mock(LunaparkApi.class);
        }

        @Bean
        public Notificator notificator() {
            return Mockito.mock(Notificator.class);
        }

    }
}
