package ru.yandex.market.saas_java_client.http.indexer.response.docfetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.yandex.market.saas_java_client.http.indexer.SaasDmService;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("checkstyle:MagicNumber")
public class DocFetcherResponseTest {
    @Test
    public void testJsonIsParsed() throws IOException {
        ObjectMapper mapper = SaasDmService.createMapper();
        URL responseBody = getClass().getResource("/requests/docfetcher-response.json");

        assertNotNull(responseBody);

        DocFetcherResponse response = mapper.readValue(responseBody, DocFetcherResponse.class);
        assertEquals(2, response.getSlots().getInfo().get("market-backoffice").size());
        assertEquals(1516892825, response.getSlots().getInfo()
                .get("market-backoffice").values().iterator().next()
                .getStatus().getSearchableTimestamp());
        assertEquals(1516892825, response.getStats().getSearchableTimestamp().getMin());
        assertEquals(1516892825, response.getStats().getSearchableTimestamp().getMax());
        assertEquals(1517259806, response.getStats().getErrors().get(0).getDate().getTime());
    }
}
