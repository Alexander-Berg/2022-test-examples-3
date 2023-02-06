package ru.yandex.market.stat.dicts.web;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.scheduling.LoadCronJob;
import ru.yandex.market.stat.dicts.scheduling.LoadToYtJob;
import ru.yandex.market.stat.dicts.services.DictionaryPublishService;
import ru.yandex.market.stat.dicts.services.JugglerEventsSender;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.ReplicationService;
import ru.yandex.market.stat.dicts.services.YtClusters;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;

import java.util.List;

import static org.mockito.Mockito.when;
import static ru.yandex.market.stat.dicts.web.DictionariesReleaseRunController.HUNDRED_LOADS;

@Slf4j
@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)

public class DictionariesReleaseRunControllerTest {

    @Autowired
    protected List<DictionaryLoadersHolder> dictionaryLoadersHolders;
    @Autowired
    private MetricRegistry metricRegistry;
    @Mock
    private DefaultListableBeanFactory beanFactory;
    @Autowired
    private MetadataService metadataService;
    @Autowired
    private YtClusters ytClusters;
    @Autowired
    private ReplicationService replicationService;
    @Autowired
    private DictionaryPublishService publishService;
    @Mock
    private JugglerEventsSender jugglerEventsSender;

    private DictionariesReleaseRunController controller;

    @Test
    public void testControlles() throws Exception {
        controller = new DictionariesReleaseRunController(metadataService, dictionaryLoadersHolders);
        DictionaryLoader<?> loader = controller.getLoader(HUNDRED_LOADS);
        when(beanFactory.getBean("hundred_loads_bazinga_job", LoadCronJob.class))
                .thenReturn(new LoadCronJob(loader, metricRegistry,
                        ytClusters, new LoadToYtJob(loader, metadataService, ytClusters, jugglerEventsSender, metricRegistry),
                        null, replicationService,
                        publishService, false));
        controller.checkLoad(HUNDRED_LOADS, null, null, null, null);
    }
}
