package ru.yandex.market.pricelabs.tms.services.market_indexer;

import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests;
import ru.yandex.market.pricelabs.tms.services.market_indexer.model.IndexerGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexerServiceTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private IndexerService indexer;

    @Autowired
    @Qualifier("mockWebServerMarketIndexer")
    private ConfigurationForTests.MockWebServerControls mockWebServerMarketIndexer;

    @BeforeEach
    public void init() {
        mockWebServerMarketIndexer.cleanup();
    }

    @Test
    void testCorrectlyGettingGenerations() {
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
        mockWebServerMarketIndexer.enqueue(new MockResponse().setBody(json));
        var generations = indexer.getGenerations();
        var expected = List.of(new IndexerGeneration("mi01ht",
                95003,
                "20210706_1044",
                "Tue, 06 Jul 2021 09:48:03 GMT",
                true,
                "Tue, 06 Jul 2021 07:44:34 GMT"));
        assertEquals(expected, generations);
    }

}
