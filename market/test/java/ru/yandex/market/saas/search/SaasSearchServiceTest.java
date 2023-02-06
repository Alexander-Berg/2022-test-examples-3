package ru.yandex.market.saas.search;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.Any;
import org.mockito.internal.matchers.Not;

import ru.yandex.market.saas.search.keys.SaasGroupingAttribute;
import ru.yandex.market.saas.search.keys.SaasSearchAttribute;
import ru.yandex.market.saas.search.response.SaasResponseGroup;
import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;
import ru.yandex.market.saas.search.term.AndTerm;
import ru.yandex.market.saas.search.term.SimpleTerm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.01.2019
 */
public class SaasSearchServiceTest {

    private HttpClient client;
    private SaasSearchService service;
    private final ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
    private static final String T_SERVICE_TICKET = "mockServiceTicket";

    @Before
    public void init() throws SaasSearchException {
        client = mock(HttpClient.class);
        service = new SaasSearchService("host", 80, "some_service", client, new TvmTicketProviderStub());
        SaasMockUtils.mockSaasResponse(client, r -> new ByteArrayInputStream(new byte[0]), Any.ANY);
    }

    @Test
    public void testSimpleFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_simple_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10"),
                        SaasMockUtils.withHeader(SaasSearchService.TVM_TICKET_HEADER, T_SERVICE_TICKET)
                ));

        SaasSearchRequest request = SaasSearchRequest.searchBy(TestSearchAttribute.TEST_ATTR, "value")
                .pageNumber(1)
                .withNumberOfResults(10);

        SaasSearchResponse response = service.search(request);

        assertEquals(5, response.getTotal());

        Map<String, SaasSearchDocument> docMap = response.getDocuments().stream()
                .collect(Collectors.toMap(
                        x -> x.getProperty("s_id"),
                        x -> x
                ));

        String testDocId = "1413834";
        assertTrue(docMap.containsKey(testDocId));

        SaasSearchDocument testDoc = docMap.get(testDocId);
        assertEquals("0", testDoc.getProperty("i_type"));
        assertEquals("1527003633000", testDoc.getProperty("create_dt"));
        assertEquals("1714248882", testDoc.getProperty("s_model_id"));

        assertEquals("", response.getDocumentGroups().get(0).getCategoryName());
        assertEquals(1, response.getDocumentGroups().get(0).getCount());
    }

    @Test
    public void testSimplePostFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_simple_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withTextBody("+%28+create_dt%3A1527003633000+%26%26+s_model_id%3A1714248882+%29+"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10"),
                        SaasMockUtils.withHeader(SaasSearchService.TVM_TICKET_HEADER, T_SERVICE_TICKET)
                ));

        AndTerm rootTerm = new AndTerm();
        rootTerm.addTerm(new SimpleTerm("create_dt", "1527003633000", ""));
        rootTerm.addTerm(new SimpleTerm("s_model_id", "1714248882", ""));
        SaasSearchRequest request = SaasSearchRequest.searchBy(rootTerm)
                .usePost()
                .pageNumber(1)
                .withNumberOfResults(10);

        SaasSearchResponse response = service.search(request);

        assertEquals(5, response.getTotal());

        Map<String, SaasSearchDocument> docMap = response.getDocuments().stream()
                .collect(Collectors.toMap(
                        x -> x.getProperty("s_id"),
                        x -> x
                ));

        String testDocId = "1413834";
        assertTrue(docMap.containsKey(testDocId));

        SaasSearchDocument testDoc = docMap.get(testDocId);
        assertEquals("0", testDoc.getProperty("i_type"));
        assertEquals("1527003633000", testDoc.getProperty("create_dt"));
        assertEquals("1714248882", testDoc.getProperty("s_model_id"));

        assertEquals("", response.getDocumentGroups().get(0).getCategoryName());
        assertEquals(1, response.getDocumentGroups().get(0).getCount());
    }

    @Test
    public void testGroupFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_group_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_other", "value", "otherValue"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10"),
                        SaasMockUtils.withQueryParam("g=2.s_other.3.2.....rlv.0.count")
                ));

        SaasSearchRequest request = SaasSearchRequest.searchBy(TestSearchAttribute.TEST_OTHER, "value", "otherValue")
                .pageNumber(1)
                .withNumberOfResults(10)
                .withGroupFilter(
                        new SaasGroupFilter()
                                .mode(SaasGroupMode.WIDE)
                                .attribute(TestGroupAttribute.TEST_OTHER)
                                .groupsCount(3)
                                .docsCount(2)
                                .docOrderAsc(false)
                                .sort(SaasGroupSort.COUNT)
                );

        SaasSearchResponse response = service.search(request);

        assertEquals(6, response.getTotal());

        List<SaasResponseGroup> groups = response.getDocumentGroups();

        SaasResponseGroup group1 = groups.get(0);
        SaasResponseGroup group2 = groups.get(1);

        assertEquals("1294172", group1.getCategoryName());
        assertEquals(4, group1.getCount());
        assertEquals(2, group1.getDocuments().size());

        assertEquals("1292948", group2.getCategoryName());
        assertEquals(2, group2.getCount());
        assertEquals(2, group2.getDocuments().size());
    }

    @Test
    public void testSimpleFilterTimeout() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_simple_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10")
                ));

        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_simple_filter.json",
                HttpStatus.SC_SERVICE_UNAVAILABLE,
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("timeout=1000000")
                ));

        // regular request
        SaasSearchRequest request = SaasSearchRequest.searchBy(TestSearchAttribute.TEST_ATTR, "value")
                .pageNumber(1)
                .withNumberOfResults(10);

        SaasSearchResponse response = service.search(request);

        assertEquals(5, response.getTotal());

        // with timeout
        request = SaasSearchRequest.searchBy(TestSearchAttribute.TEST_ATTR, "value")
                .pageNumber(1)
                .withNumberOfResults(10)
                .withTimeout(1, TimeUnit.SECONDS);

        try {
            response = service.search(request);

            // should not get here
            throw new AssertionError();
        } catch (SaasSearchTimeoutException e) {
            // ok
        }
    }

    @Test
    public void testExcludeFilterAndPartialText() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_simple_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10")
                ));

        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_excluded_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withPartialText("partialTextQuery"),
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withSearchExcludeAttribute("s_other", "other_value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=15")
                ));

        SaasSearchRequest request = SaasSearchRequest
                .searchBy("partialTextQuery")
                .filterBy(TestSearchAttribute.TEST_ATTR, "value")
                .filterExcludeBy(TestSearchAttribute.TEST_OTHER, "other_value")
                .pageNumber(1)
                .withNumberOfResults(15);

        SaasSearchResponse response = service.search(request);

        assertEquals(2, response.getTotal());

        Map<String, SaasSearchDocument> docMap = response.getDocuments().stream()
                .collect(Collectors.toMap(
                        x -> x.getProperty("s_id"),
                        x -> x
                ));

        String testDocId = "1307345";
        assertTrue(docMap.containsKey(testDocId));
    }

    @Test
    public void testGtaFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(client,
                "/saas/saas_gta_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10"),
                        SaasMockUtils.withQueryParam("gta=s_author_id"),
                        new Not(SaasMockUtils.withQueryParam("haha=da"))
                ));

        SaasSearchRequest request = SaasSearchRequest
                .searchBy(TestSearchAttribute.TEST_ATTR, "value")
                .filterExcludeBy(TestSearchAttribute.TEST_OTHER, "other_value")
                .onlyProperties(TestSearchAttribute.TEST_AUTHOR_ID)
                .pageNumber(1)
                .withNumberOfResults(10);

        SaasSearchResponse response = service.search(request);

        assertEquals(5, response.getTotal());

        List<String> gtaAttributes = new ArrayList<>();
        response.getDocuments()
                .forEach(it -> gtaAttributes.addAll(it.getAttribute(TestSearchAttribute.TEST_AUTHOR_ID)));

        assertEquals(2, gtaAttributes.size());
    }

    private static enum TestSearchAttribute implements SaasSearchAttribute {
        TEST_ATTR("s_name"),
        TEST_OTHER("s_other"),
        TEST_AUTHOR_ID("s_author_id"),
        ;

        private final String code;

        private TestSearchAttribute(String code) {
            this.code = code;
        }

        @Override
        public String getName() {
            return code;
        }
    }

    private static enum TestGroupAttribute implements SaasGroupingAttribute {
        TEST_OTHER(TestSearchAttribute.TEST_OTHER.getName()),
        ;

        private final String code;

        private TestGroupAttribute(String code) {
            this.code = code;
        }

        @Override
        public String getName() {
            return code;
        }
    }

    private static class TvmTicketProviderStub implements Supplier<Optional<String>> {

        @Override
        public Optional<String> get() {
            return Optional.of(T_SERVICE_TICKET);
        }
    }

    @Test
    public void testPronFilter() throws Exception {
        SaasMockUtils.mockSaasResponseWithFile(
                client,
                "/saas/saas_simple_filter.json",
                SaasMockUtils.and(
                        SaasMockUtils.withSearchAttribute("s_name", "value"),
                        SaasMockUtils.withQueryParam("p=1"),
                        SaasMockUtils.withQueryParam("numdoc=10"),
                        SaasMockUtils.withQueryParam("pron=noprune"),
                        SaasMockUtils.withQueryParam("pron=nofreqban")
                ));

        List<String> pronList = new ArrayList<>();
        pronList.add("noprune");
        pronList.add("nofreqban");

        SaasSearchRequest request = SaasSearchRequest.searchBy(TestSearchAttribute.TEST_ATTR, "value")
                .pageNumber(1)
                .withNumberOfResults(10)
                .withPron(pronList);

        SaasSearchResponse response = service.search(request);

        assertEquals(5, response.getTotal());
    }
}
