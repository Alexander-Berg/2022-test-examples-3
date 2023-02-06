package ru.yandex.market.stat.dicts.web;

import java.util.List;

import com.codahale.metrics.MetricRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.market.stat.dicts.config.DictionariesITestConfig;
import ru.yandex.market.stat.dicts.config.loaders.DictionaryLoadersHolder;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.services.DictionaryYtService;
import ru.yandex.market.stat.dicts.services.JugglerEventsSender;
import ru.yandex.market.stat.dicts.services.MetadataService;
import ru.yandex.market.stat.dicts.services.YtClusters;
import ru.yandex.market.stats.test.config.LocalPostgresInitializer;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@Slf4j
@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
@ContextConfiguration(classes = DictionariesITestConfig.class, initializers = LocalPostgresInitializer.class)

public class DictionariesManualRunIntegrationTest {
    private static final String DEFAULT_CLUSTER = "hahn";

    private DictionariesManualRunController manualRunController;

    @Autowired
    private List<DictionaryLoadersHolder> dictionaryLoadersHolders;
    @Autowired
    private List<DictionaryLoader> loaders;
    @Mock
    private MetadataService metadataService;
    @Mock
    private YtClusters ytClusters;
    @Mock
    private MetricRegistry metricRegistry;
    private MockMvc mockMvc;

    @Mock
    private JugglerEventsSender jugglerEventsSender;

    @Before
    public void initServer() {
        MockitoAnnotations.initMocks(this);
        when(ytClusters.getPreferredCluster()).thenReturn(DEFAULT_CLUSTER);
        manualRunController = new DictionariesManualRunController(dictionaryLoadersHolders, loaders, metadataService,
                ytClusters, jugglerEventsSender, metricRegistry);
        this.mockMvc = standaloneSetup(manualRunController).build();
    }

    @Test
    public void testLoadExisting() throws InterruptedException {
        assertThat(getRequest("/manual/load?name=axapta_suppliers&date=2019-10-30"), is("{\"status\":\"success\"," +
                "\"info\":\"Load of axapta_suppliers for date 2019-10-30T00:00 was started\"}"));
        assertThat(DictionariesManualRunController.getResult().get(), not(nullValue()));
    }

    @Test
    public void testLoadUnexisting() {
        assertThat(getRequest("/manual/load?name=blabla&date=2019-10-30T00:00:00"),
                startsWith("{\"status\":\"error\",\"info\":\"no such dictionary: blabla; dicts avaliable"));
        assertThat(DictionariesManualRunController.getResult().get(), nullValue());
    }

    @SneakyThrows
    protected String getRequest(String path) {
        return getRequest(path, HttpStatus.SC_OK);
    }

    @SneakyThrows
    protected String getRequest(String path, int expected) {
        MvcResult result = mockMvc.perform(get(path).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        log.info("Request was " + result.getRequest().getRequestURL().toString());
        MockHttpServletResponse response = result.getResponse();
        assertThat("Bad http status " + response.getStatus(), response.getStatus(), is(expected));
        return response.getContentAsString();
    }

}
