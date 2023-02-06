package ru.yandex.market.stat.dicts.scheduling;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.BaseLoadTest;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.yt.YtClusterProvider;

@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@Slf4j
public class FindStaleDictionariesOnYtJobTest extends BaseLoadTest {

    public static final String PRODUCTION_MSTAT_DICTIONARIES = "//home/market/production/mstat/dictionaries";

    @Autowired
    private DictionaryYtService ytService;
    @Autowired
    private YtClusterProvider provider;

    private FindStaleDictionariesOnYtJob job;

    @Test
    @Ignore("Manual run only")
    public void checkDataForProduction() {
        job = new FindStaleDictionariesOnYtJob(provider, Mockito.mock(MetricRegistry.class),
                null, ytService, dictionaryLoadersHolders);
        job.runForRootFolder(PRODUCTION_MSTAT_DICTIONARIES);
    }
}
