package ru.yandex.market.saas.search;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Any;

import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.12.2019
 */
public class SaasKvSearchServiceTest {
    private HttpClient client;
    private SaasKvSearchService service;

    @Before
    public void init() {
        client = mock(HttpClient.class);
        service = new SaasKvSearchService("http://host:80", "some_service", client);
        SaasMockUtils.mockSaasResponse(client, r -> new ByteArrayInputStream(new byte[0]), Any.ANY);
    }

    @Test
    public void testSimpleFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
            "/saas/saas_kv_simple_response.json",
            SaasMockUtils.and(
                SaasMockUtils.withQueryParam("text=mykey"),
                SaasMockUtils.withQueryParam("sp_meta_search=multi_proxy"),
                SaasMockUtils.withQueryParam("hr=json"),
                SaasMockUtils.withQueryParam("ms=proto")
            ));

        SaasKvSearchRequest request = SaasKvSearchRequest.simpleKey("mykey");
        SaasSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());

        Map<String, SaasSearchDocument> docMap = response.getDocuments().stream()
            .collect(Collectors.toMap(
                x -> x.getTitle(),
                x -> x
            ));

        String testDocId = "37482";
        assertTrue(docMap.containsKey(testDocId));

        SaasSearchDocument testDoc = docMap.get(testDocId);
        assertEquals("0", testDoc.getProperty("user_type"));
        assertEquals("42", testDoc.getProperty("delta_count"));
        assertEquals("37482", testDoc.getProperty("user_id"));
        assertEquals("2", testDoc.getProperty("known_count"));
    }

    @Test
    public void testFilterWithField() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
            "/saas/saas_kv_simple_response.json",
            SaasMockUtils.and(
                SaasMockUtils.withQueryParam("text=mykey"),
                SaasMockUtils.withQueryParam("sp_meta_search=multi_proxy"),
                SaasMockUtils.withQueryParam("hr=json"),
                SaasMockUtils.withQueryParam("ms=proto"),
                SaasMockUtils.withQueryParam("gta=field")
            ));

        SaasKvSearchRequest request = SaasKvSearchRequest.simpleKey("mykey").withFields(Arrays.asList("field"));
        SaasSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());
    }

    @Test
    public void testFilterWithManyFields() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
            "/saas/saas_kv_simple_response.json",
            SaasMockUtils.and(
                SaasMockUtils.withQueryParam("text=mykey"),
                SaasMockUtils.withQueryParam("sp_meta_search=multi_proxy"),
                SaasMockUtils.withQueryParam("hr=json"),
                SaasMockUtils.withQueryParam("ms=proto"),
                SaasMockUtils.withQueryParam("gta=field"),
                SaasMockUtils.withQueryParam("gta=other")
            ));

        SaasKvSearchRequest request = SaasKvSearchRequest.simpleKey("mykey").withFields(Arrays.asList("field", "other"));
        SaasSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());
    }

    @Test
    public void testMultiKeyFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
            "/saas/saas_kv_simple_response.json",
            SaasMockUtils.and(
                SaasMockUtils.withQueryParam("text=mykey"),
                SaasMockUtils.withQueryParam("text=otherkey"),
                SaasMockUtils.withQueryParam("sp_meta_search=multi_proxy"),
                SaasMockUtils.withQueryParam("hr=json"),
                SaasMockUtils.withQueryParam("ms=proto")
            ));

        SaasKvSearchRequest request = SaasKvSearchRequest.simpleKeys(Arrays.asList("mykey", "otherkey"));
        SaasSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());

        Map<String, SaasSearchDocument> docMap = response.getDocuments().stream()
            .collect(Collectors.toMap(
                x -> x.getTitle(),
                x -> x
            ));

        String testDocId = "37482";
        assertTrue(docMap.containsKey(testDocId));

        SaasSearchDocument testDoc = docMap.get(testDocId);
        assertEquals("0", testDoc.getProperty("user_type"));
        assertEquals("42", testDoc.getProperty("delta_count"));
        assertEquals("37482", testDoc.getProperty("user_id"));
        assertEquals("2", testDoc.getProperty("known_count"));
    }

    @Test
    public void testCustomKeyFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
            "/saas/saas_kv_simple_response.json",
            SaasMockUtils.and(
                SaasMockUtils.withQueryParam("text=mykey"),
                SaasMockUtils.withQueryParam("key_name=customKeyName"),
                SaasMockUtils.withQueryParam("sp_meta_search=multi_proxy"),
                SaasMockUtils.withQueryParam("hr=json"),
                SaasMockUtils.withQueryParam("ms=proto")
            ));

        SaasKvSearchRequest request = SaasKvSearchRequest.customKey("mykey", "customKeyName");
        SaasSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());

        Map<String, SaasSearchDocument> docMap = response.getDocuments().stream()
            .collect(Collectors.toMap(
                x -> x.getTitle(),
                x -> x
            ));

        String testDocId = "37482";
        assertTrue(docMap.containsKey(testDocId));
    }

    @Test
    public void testTimeouts() throws Exception {
        doTestDefaultTimeout(1123L, 4314L, 4314000L);
        doTestDefaultTimeout(1123L, 500L, 500000L);
        doTestDefaultTimeout(1123L, null, 1123000L);
        doTestDefaultTimeout(1123L, 0L, 1123000L);
        doTestDefaultTimeout(null, 4314L, 4314000L);
        doTestDefaultTimeout(0L, 4314L, 4314000L);
        doTestDefaultTimeout(null, null, null);
        doTestDefaultTimeout(0L, 0L, null);
    }

    public void doTestDefaultTimeout(Long defaultTimeout, Long requestTimeout, Long resultTimeout) throws Exception {
        Matcher<HttpUriRequest> timeoutMatcher = resultTimeout != null
            ? SaasMockUtils.withQueryParam("timeout=" + resultTimeout)
            : SaasMockUtils.not(SaasMockUtils.withQueryParam("timeout=[0-9]*")); // timeout parameter is not used

        SaasMockUtils.mockSaasResponseWithFile(client,
            "/saas/saas_kv_simple_response.json",
            SaasMockUtils.and(
                SaasMockUtils.withQueryParam("text=mykey"),
                SaasMockUtils.withQueryParam("sp_meta_search=multi_proxy"),
                SaasMockUtils.withQueryParam("hr=json"),
                SaasMockUtils.withQueryParam("ms=proto"),
                SaasMockUtils.withQueryParam("ms=proto"),
                timeoutMatcher
            ));

        SaasKvSearchService serviceWithTimeout = new SaasKvSearchService("http://host:80", "some_service", client);
        serviceWithTimeout.setDefaultTimeout(defaultTimeout, TimeUnit.MILLISECONDS);

        SaasKvSearchRequest request = SaasKvSearchRequest.simpleKey("mykey");
        request.withTimeout(requestTimeout, TimeUnit.MILLISECONDS);

        SaasSearchResponse response = serviceWithTimeout.search(request);

        assertEquals(1, response.getTotal());
    }
}
