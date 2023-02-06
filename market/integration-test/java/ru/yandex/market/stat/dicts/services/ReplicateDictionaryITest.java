package ru.yandex.market.stat.dicts.services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.BaseLoadTest;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.scheduling.LoadToYtJob;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
public class ReplicateDictionaryITest extends BaseLoadTest {
    private static final String DEFAULT_CLUSTER = "hahn";
    private static final String ANOTHER_CLUSTER = "arnold";
    private static String DICT_FOR_TEST = "shop_datasources_in_testing";

    @Autowired
    private ReplicationService service;
    @Autowired
    private MetadataService metadataService;
    @Autowired
    private DictionaryYtService ytService;
    @Autowired
    private YtClusters ytClusters;
    @Autowired
    private NamedParameterJdbcTemplate metadataTemplate;
    @Autowired
    private DictionaryYtPublishService publisher;
    @Mock
    private MetricRegistry metricRegistry;
    @Mock
    private JugglerEventsSender jugglerEventsSender;

    @Test
    @Ignore("For local run only")
    public void testReplicate() {
        Assume.assumeFalse("No dictionary to check, this is ok for automatic run", DICT_FOR_TEST.isEmpty());

        DictionaryLoader loader = getLoader(DICT_FOR_TEST);
        Dictionary dict = loader.getDictionary();
        LoadToYtJob job = new LoadToYtJob(loader, metadataService, ytClusters, jugglerEventsSender, metricRegistry);
        job.load(DEFAULT_CLUSTER, LocalDate.now().minusDays(1).atStartOfDay());

        boolean replicated = service.replicate(DEFAULT_CLUSTER, ANOTHER_CLUSTER, dict, false);
        assertTrue("Dictionary was not replicated to another cluster", replicated);
        publisher.publish(ANOTHER_CLUSTER, dict);
    }

    @Test
    @Ignore("For local run only")
    public void testReplicateWhenOldProcessingDict() {
        Assume.assumeFalse("No dictionary to check, this is ok for automatic run", DICT_FOR_TEST.isEmpty());

        DictionaryLoader loader = getLoader(DICT_FOR_TEST);
        Dictionary dict = loader.getDictionary();
        LoadToYtJob job = new LoadToYtJob(loader, metadataService, ytClusters, jugglerEventsSender, metricRegistry);
        job.load(DEFAULT_CLUSTER, LocalDate.now().minusDays(1).atStartOfDay());

        boolean replicated = service.replicate(ANOTHER_CLUSTER, DEFAULT_CLUSTER, dict, false);
        assertFalse("Dictionary should not be replicated from cluster arnold  - nothing to replicate", replicated);
    }


    public boolean markDictionaryAsProcessing(Dictionary dictionary, LocalDateTime time) {
        KeyHolder holder = new GeneratedKeyHolder();
        metadataTemplate.update("" +
                        "INSERT INTO processing_dicts\n" +
                        "(dictionary,scale, process_end) \n" +
                        "VALUES  (:dictionary, :scale, :end)\n",

                new MapSqlParameterSource()
                        .addValue("dictionary", dictionary.getName())
                        .addValue("scale", dictionary.getScale().getName())
                        .addValue("end", Timestamp.valueOf(time)),
                holder,
                new String[]{"processing_id"}
        );

        return !holder.getKeyList().isEmpty();
    }
}
