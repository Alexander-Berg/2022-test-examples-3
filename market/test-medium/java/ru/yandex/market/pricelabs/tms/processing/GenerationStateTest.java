package ru.yandex.market.pricelabs.tms.processing;

import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests;
import ru.yandex.market.pricelabs.tms.services.market_indexer.model.IndexerGeneration;

public class GenerationStateTest extends AbstractTmsSpringConfiguration {

    @Autowired
    GenerationState generationState;

    @Autowired
    @Qualifier("mockWebServerMarketIndexer")
    private ConfigurationForTests.MockWebServerControls mockWebServerMarketIndexer;

    @Autowired
    @Qualifier("mockWebServerMarketReport")
    private ConfigurationForTests.MockWebServerControls mockWebServerMarketReport;

    String json = "[\n" +
            "  {\n" +
            "    \"hostname\": \"mi01ht\",\n" +
            "    \"id\": 95003,\n" +
            "    \"name\": \"20210706_1044\",\n" +
            "    \"release_date\": \"Tue, 06 Jul 2021 09:48:03 GMT\",\n" +
            "    \"released\": 1,\n" +
            "    \"start_date\": \"Tue, 06 Jul 2021 07:44:34 GMT\"\n" +
            "  }\n" +
            "]";

    @BeforeEach
    void init() {
        mockWebServerMarketIndexer.cleanup();
        mockWebServerMarketReport.cleanup();
    }

    @Test
    void testCorrectlyGettingBothHandle() {
        mockWebServerMarketIndexer.enqueue(new MockResponse().setBody(json));

        mockWebServerMarketReport.enqueue(new MockResponse().setBody(Utils.
                readResource("tms/services/market_report/admin-action-example.xml")));

        List<IndexerGeneration> generations = generationState.getActualGenerations();
        Assertions.assertNotNull(generations);
        Assertions.assertEquals(2, generations.size());
    }

    @Test
    void testGettingOnlyReportGeneration() {
        mockWebServerMarketReport.enqueue(new MockResponse().setBody(Utils.
                readResource("tms/services/market_report/admin-action-example.xml")));
        List<IndexerGeneration> generations = generationState.getActualGenerations();
        Assertions.assertNotNull(generations);
        Assertions.assertEquals(1, generations.size());
        Assertions.assertEquals("sas2-4935-1f5-sas-market-test--b33-17050.gencfg-c.yandex.net",
                generations.get(0).getHostname());
        Assertions.assertEquals("20210708_0700", generations.get(0).getName());

    }

    @Test
    void testGettingOnlyIndexerGenerations() {
        mockWebServerMarketIndexer.enqueue(new MockResponse().setBody(json));
        List<IndexerGeneration> generations = generationState.getActualGenerations();
        Assertions.assertNotNull(generations);
        Assertions.assertEquals(2, generations.size());
        Assertions.assertEquals(new IndexerGeneration(null, null, null, null, true, null), generations.get((0)));
        Assertions.assertEquals("mi01ht", generations.get(1).getHostname());
        Assertions.assertEquals("20210706_1044", generations.get(1).getName());

    }

    @Test
    void testBothServicesDropDown() {
        List<IndexerGeneration> generations = generationState.getActualGenerations();
        Assertions.assertNotNull(generations);
        Assertions.assertEquals(1, generations.size());
        Assertions.assertEquals(new IndexerGeneration(null, null, null, null, true, null), generations.get((0)));
    }

}
