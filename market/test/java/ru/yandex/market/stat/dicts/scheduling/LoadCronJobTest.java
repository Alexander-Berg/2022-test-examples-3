package ru.yandex.market.stat.dicts.scheduling;

import com.codahale.metrics.MetricRegistry;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.commune.bazinga.scheduler.TaskQueueName;
import ru.yandex.market.stat.dicts.bazinga.BazingaHelper;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;
import ru.yandex.market.stat.dicts.services.DictionaryPublishService;
import ru.yandex.market.stat.dicts.services.ReplicationService;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;
import ru.yandex.market.stat.dicts.services.YtClusters;
import ru.yandex.market.stat.yt.YtClusterProvider;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoadCronJobTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    @Mock
    private DictionaryLoader loader;
    private LoadCronJob job;

    @Mock
    private LoadToYtJob loadFromSourceJob;

    @Mock
    private YtDictionaryStorage storage;
    @Mock
    private YtClusterProvider ytClusterProvider;

    @Mock
    private YtClusters ytClusters;

    @Mock
    private MetricRegistry metricRegistry;

    @Mock
    private SourceYtClusterDetector sourceYtClusterDetector;

    @Mock
    private ReplicationService replicationService;

    @Mock
    private DictionaryPublishService publishService;

    private Dictionary dictSla = Dictionary.from("path", SchemelessDictionaryRecord.class, LoaderScale.DEFAULT, -1L, true);
    private Dictionary dictNonSla = Dictionary.from("path", SchemelessDictionaryRecord.class, LoaderScale.DEFAULT, -1L, false);

    @Test
    @SneakyThrows
    public void testCpuIntensiveJob() {
        when(ytClusters.getPreferredCluster()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusterProvider.getClusterName()).thenReturn(DEFAULT_CLUSTER);
        when(sourceYtClusterDetector.canLoadFromSource()).thenReturn(true);


        givenLoaderIsSla(false);
        job = getJobForLoader(loader, true);
        assertThat(job.queueName(), is(TaskQueueName.CPU_INTENSIVE));
        job.run();
    }

    @Test
    @SneakyThrows
    public void testSlaJob() {
        when(ytClusters.getPreferredCluster()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusterProvider.getClusterName()).thenReturn(DEFAULT_CLUSTER);
        when(sourceYtClusterDetector.canLoadFromSource()).thenReturn(true);

        givenLoaderIsSla(true);
        job = getJobForLoader(loader, false);
        assertThat(job.queueName(), is(TaskQueueName.REGULAR));
        job.run();
    }

    @Test
    @SneakyThrows
    public void testCronJob() {
        when(ytClusterProvider.getClusterName()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusters.getNativeCluster()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusters.getPreferredCluster()).thenReturn(DEFAULT_CLUSTER);

        givenLoaderIsSla(false);
        job = getJobForLoader(loader, false);
        assertThat(job.queueName(), is(TaskQueueName.CRON));
        job.run();
    }

    private void givenLoaderIsSla(boolean sla) {
        if (sla) {
            when(loader.getDictionary()).thenReturn(dictSla);
        } else {
            when(loader.getDictionary()).thenReturn(dictNonSla);
        }
        when(loader.getCron()).thenReturn(BazingaHelper.cronHourly());
        when(loader.getMaxDurationInMinutes()).thenReturn(10L);
    }

    private LoadCronJob getJobForLoader(DictionaryLoader loader, boolean isHeavy) {
        return new LoadCronJob(loader, metricRegistry, ytClusters, loadFromSourceJob, ytClusterProvider,
                replicationService, publishService, isHeavy);
    }
}
