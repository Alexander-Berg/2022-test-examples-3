package ru.yandex.market.stat.dicts.web;

import java.util.ArrayList;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.dicts.services.JugglerEventsSender;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.YtClusters;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DictionariesManualRunControllerTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    private List<DictionaryLoadersHolder> dictionaryLoadersHolders = new ArrayList<>();
    private List<DictionaryLoader> loaders = new ArrayList<>();
    private DictionariesManualRunController manualRunController;

    @Mock
    private JugglerEventsSender jugglerEventsSender;
    @Mock
    private MetricRegistry metricsService;

    @Before
    public void initServer() throws Exception {
        Dictionary dictionay = mock(Dictionary.class);
        when(dictionay.nameForLoader()).thenReturn("dct");
        when(dictionay.getName()).thenReturn("dct");

        DictionaryLoader loader = mock(DictionaryLoader.class);
        when(loader.getDictionary()).thenReturn(dictionay);
        when(loader.load(eq(DEFAULT_CLUSTER), any())).thenReturn(42L);
        when(loader.sourceExistsForDay(eq(DEFAULT_CLUSTER), any())).thenReturn(true);
        loaders.add(loader);

        MetadataService metadataService = mock(MetadataService.class);
        YtClusters ytClusters = mock(YtClusters.class);

        manualRunController = new DictionariesManualRunController(
                dictionaryLoadersHolders, loaders, metadataService, ytClusters, jugglerEventsSender, metricsService);
    }

    @Test
    public void testLoad() {
        DictionariesManualRunController.LoadResponse resp = manualRunController
                .manualLoad(DEFAULT_CLUSTER, "dct", "2019-06-06T15:50:42");
        assertEquals("success", resp.getStatus());
    }

    @Test
    public void testLoadNullDate() {
        DictionariesManualRunController.LoadResponse resp = manualRunController
                .manualLoad(DEFAULT_CLUSTER, "dct", null);
        assertEquals("success", resp.getStatus());
    }

    @Test
    public void testFailFuture() {
        DictionariesManualRunController.LoadResponse resp = manualRunController
                .manualLoad(DEFAULT_CLUSTER, "dct", "2319-06-06T15:50:42");
        assertEquals("error", resp.getStatus());
    }

    @Test
    public void testFailNoSuchDict() {
        DictionariesManualRunController.LoadResponse resp = manualRunController
                .manualLoad(DEFAULT_CLUSTER, "some_dict_not_in_list", null);
        assertEquals("error", resp.getStatus());
    }

}
