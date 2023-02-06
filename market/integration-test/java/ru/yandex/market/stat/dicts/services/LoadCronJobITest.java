package ru.yandex.market.stat.dicts.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.commune.bazinga.scheduler.TaskQueueName;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.records.TestDictionary;
import ru.yandex.market.stat.dicts.records.TestDictionaryLoader;
import ru.yandex.market.stat.dicts.scheduling.LoadCronJob;
import ru.yandex.market.stat.dicts.scheduling.LoadToYtJob;
import ru.yandex.market.stat.dicts.scheduling.SourceYtClusterDetector;
import ru.yandex.market.stat.yt.YtClusterProvider;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)
public class LoadCronJobITest {
    private static final String DEFAULT_CLUSTER = "hahn";
    private static final String ANOTHER_CLUSTER = "arnold";

    private LoadCronJob job;

    @Autowired
    private MetadataService metadataService;
    @Autowired
    private DictionaryYtService yt;
    @Autowired
    private YtDictionaryStorage storage;
    @Autowired
    private YtClusterProvider ytClusterProvider;
    @Autowired
    private NamedParameterJdbcTemplate metadataTemplate;
    @Mock
    private JugglerEventsSender jugglerEventsSender;
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private SourceYtClusterDetector sourceYtClusterDetector;

    @Mock
    private ReplicationService replicationService;
    @Mock
    private DictionaryPublishService publishService;
    @Mock
    private YtClusters ytClusters;

    @Test
    @SneakyThrows
    //@Ignore("No postgres in arcadia, run integr tests only locally")
    public void testCpuIntensiveJob() {
        when(ytClusters.getPreferredCluster()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusters.getYtService(DEFAULT_CLUSTER)).thenReturn(yt);

        job = getJobForLoader(true, false);
        assertThat(job.queueName(), is(TaskQueueName.CPU_INTENSIVE));
        job.run();
    }

    @Test
    //@Ignore("No postgres in arcadia, run integr tests only locally")
    public void testSlaJob() {
        job = getJobForLoader(false, true);
        assertThat(job.queueName(), is(TaskQueueName.REGULAR));
    }


    @Test
    //@Ignore("No postgres in arcadia, run integr tests only locally")
    public void testCronJob() {
        job = getJobForLoader(false, false);
        assertThat(job.queueName(), is(TaskQueueName.CRON));
    }


    @Test
    @SneakyThrows
    //@Ignore("No postgres in arcadia, run integr tests only locally")
    public void testMetric() {
        when(ytClusters.getAllClusters()).thenReturn(Arrays.asList(DEFAULT_CLUSTER, ANOTHER_CLUSTER));
        when(ytClusters.getLiveYtClusters()).thenReturn(ImmutableSet.of(DEFAULT_CLUSTER, ANOTHER_CLUSTER));
        when(ytClusters.getNativeCluster()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusters.getPreferredCluster()).thenReturn(DEFAULT_CLUSTER);
        when(ytClusters.getYtService(DEFAULT_CLUSTER)).thenReturn(yt);

        givenJobInDb();
        job = getJobForLoader(false, false);
        job.run();

        Dictionary<TestDictionary> dict = getLoader(false, false).getDictionary();
        verify(metricRegistry).meter(anyString());
        assertNotNull(metadataService.getCurrentJobStartTime(dict));
    }

    private void givenJobInDb() {
        metadataTemplate.update("insert into cron_job (task, start_time, create_time, status) values ('test_dictionary', now() - interval '1 minute', now() - interval '1 minute', 'running')", Collections.emptyMap());
    }

    private LoadCronJob getJobForLoader(boolean isHeavy, boolean isSla) {
        TestDictionaryLoader loader = getLoader(isHeavy, isSla);

        LoadToYtJob loadFromSourceJob = new LoadToYtJob(loader, metadataService, ytClusters, jugglerEventsSender, metricRegistry);
        return new LoadCronJob(loader, metricRegistry, ytClusters, loadFromSourceJob, ytClusterProvider,
                replicationService, publishService, isHeavy);
    }

    @NotNull
    private TestDictionaryLoader getLoader(boolean isHeavy, boolean isSla) {
        List<String> ids = Arrays.asList("id1", "id2", "id3", "id4", "id5", "id6", "id7");
        return new TestDictionaryLoader(storage, ids, LoaderScale.DEFAULT, isHeavy, isSla);
    }
}
