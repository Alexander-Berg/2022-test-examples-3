package ru.yandex.market.mbo.db.modelstorage.index.saas;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.saas.search.SaasSearchException;
import ru.yandex.market.saas.search.SaasSearchRequest;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

/**
 * @author apluhin
 * @created 11/13/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SaasIndexReaderTest {

    SaasIndexReader reader;
    SaasSearchService searchService;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        searchService = Mockito.mock(SaasSearchService.class);
        reader = new SaasIndexReader(searchService, 1);
    }

    @Test
    public void successSimpleQuery() throws SaasSearchException {
        MboIndexesFilter filter = new MboIndexesFilter();
        ReadStats stats = Mockito.spy(new ReadStats());

        Map<String, Object> props = new HashMap<>();
        props.put(GenericField.MODEL_ID.saasField(), "1");
        props.put(GenericField.CATEGORY_ID.saasField(), "2");
        props.put(GenericField.PARENT_ID.saasField(), "3");

        SaasSearchResponse response = Mockito.mock(SaasSearchResponse.class);
        Mockito.when(response.getDocuments()).thenReturn(
            Arrays.asList(
                buildDoc(props)
            )
        );
        Mockito.when(searchService.search(Mockito.any())).thenReturn(response);
        List<SaasIndexModel> result = reader.query(filter, stats);
        Assertions.assertThat(new SaasIndexModel(1, 2, (long) 3)).isEqualTo(result.get(0));
        Assertions.assertThat(stats.getFind().getCount()).isEqualTo(result.size());
        Assertions.assertThat(stats.getFind().getAttempts()).isEqualTo(1);
    }

    @Test(expected = RuntimeException.class)
    public void notEnoughFieldItDoc() throws SaasSearchException {
        MboIndexesFilter filter = new MboIndexesFilter();
        ReadStats stats = Mockito.spy(new ReadStats());

        Map<String, Object> props = new HashMap<>();
        props.put(GenericField.MODEL_ID.saasField(), "1");
        props.put(GenericField.PARENT_ID.saasField(), "3");

        SaasSearchResponse response = Mockito.mock(SaasSearchResponse.class);
        Mockito.when(response.getDocuments()).thenReturn(
            Arrays.asList(
                buildDoc(props)
            )
        );
        Mockito.when(searchService.search(Mockito.any())).thenReturn(response);
        reader.query(filter, stats);
    }

    @Test
    public void nullableParentId() throws SaasSearchException {
        MboIndexesFilter filter = new MboIndexesFilter();
        ReadStats stats = Mockito.spy(new ReadStats());

        Map<String, Object> props = new HashMap<>();
        props.put(GenericField.MODEL_ID.saasField(), "1");
        props.put(GenericField.CATEGORY_ID.saasField(), "2");

        SaasSearchResponse response = Mockito.mock(SaasSearchResponse.class);
        Mockito.when(response.getDocuments()).thenReturn(
            Arrays.asList(
                buildDoc(props)
            )
        );
        ArgumentCaptor<SaasSearchRequest> captor = ArgumentCaptor.forClass(SaasSearchRequest.class);
        Mockito.when(searchService.search(captor.capture())).thenReturn(response);
        List<SaasIndexModel> result = reader.query(filter, stats);
        Assertions.assertThat(new SaasIndexModel(1, 2, null)).isEqualTo(result.get(0));
    }

    @Test
    public void testSimpleCount() throws SaasSearchException {
        MboIndexesFilter filter = new MboIndexesFilter();
        ReadStats stats = Mockito.spy(new ReadStats());

        SaasSearchResponse response = Mockito.mock(SaasSearchResponse.class);
        Mockito.when(response.getTotal()).thenReturn(3);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(response);
        long count = reader.count(filter, stats);
        Assertions.assertThat(3).isEqualTo(count);
    }

    @Test
    public void testBaseParamsWithDisableRanking() {
        SaasSearchRequest request = Mockito.mock(SaasSearchRequest.class);
        reader.appendBaseParams(request, MboIndexesFilter.newFilter());
        Mockito.verify(request, Mockito.times(1)).withPrefix(Mockito.eq(1));
        Mockito.verify(request, Mockito.times(1)).sortBy(
            Mockito.eq(new SaasGroupingAttributeImpl("docid")));
    }

    @Test
    public void testBaseParamsWithSort() {
        SaasSearchRequest request = Mockito.mock(SaasSearchRequest.class);
        reader.appendBaseParams(request, MboIndexesFilter.newFilter()
            .addOrderBy(GenericField.VENDOR_ID, Sort.Direction.ASC));
        Mockito.verify(request, Mockito.times(1)).withPrefix(Mockito.eq(1));
        Mockito.verify(request, Mockito.times(0)).sortBy(Mockito.any(SaasGroupingAttributeImpl.class));
    }

    @Test
    public void testRandomOrder() {
        SaasSearchRequest request = Mockito.mock(SaasSearchRequest.class);
        ArgumentCaptor<SaasGroupingAttributeImpl> captor = ArgumentCaptor.forClass(SaasGroupingAttributeImpl.class);

        MboIndexesFilter filter = MboIndexesFilter.newFilter().setRandom(true);
        new SaasIndexQuery(filter).buildQuery(request);
        reader.appendBaseParams(request, filter);

        Mockito.verify(request, Mockito.times(1)).sortBy(captor.capture());
        Assertions.assertThat(captor.getValue()).isEqualTo(new SaasGroupingAttributeImpl("shuffle"));
    }

    @Test
    public void testNoRandomOrder() {
        SaasSearchRequest request = Mockito.mock(SaasSearchRequest.class);
        ArgumentCaptor<SaasGroupingAttributeImpl> captor = ArgumentCaptor.forClass(SaasGroupingAttributeImpl.class);

        MboIndexesFilter filter = MboIndexesFilter.newFilter().setRandom(false);
        new SaasIndexQuery(filter).buildQuery(request);
        reader.appendBaseParams(request, filter);

        Mockito.verify(request, Mockito.times(1)).sortBy(captor.capture());
        Assertions.assertThat(captor.getValue()).isEqualTo(new SaasGroupingAttributeImpl("docid"));
    }

    @Test
    public void extractFieldValues() throws SaasSearchException {
        MboIndexesFilter filter = new MboIndexesFilter();
        ReadStats stats = Mockito.spy(new ReadStats());

        SaasSearchResponse response = Mockito.mock(SaasSearchResponse.class);

        Map<String, Integer> count = new HashMap<>();
        count.put("1", 2);
        count.put("2", 3);
        count.put("3", 4);
        Mockito.when(
            response.getFacets(Mockito.eq(new SaasGroupingAttributeImpl(
                GenericField.PARENT_ID.saasField().replaceFirst("s_", "i_")))))
            .thenReturn(count);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(response);
        Set<Long> values = reader.fieldValues(filter, GenericField.PARENT_ID, Long::valueOf, stats);
        Assertions.assertThat(values).containsSequence(1L, 2L, 3L);
        Assertions.assertThat(stats.getFind().getCount()).isEqualTo(values.size());
        Assertions.assertThat(stats.getFind().getAttempts()).isEqualTo(1);
    }

    SaasSearchDocument buildDoc(Map<String, Object> props) {
        HashMap<Object, Object> doc = new HashMap<>();
        List<Map> value = props.entrySet().stream().map(it -> {
            HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("Key", it.getKey());
            objectObjectHashMap.put("Value", it.getValue());
            return objectObjectHashMap;
        }).collect(Collectors.toList());
        doc.put("ArchiveInfo", Collections.singletonMap("GtaRelatedAttribute", value));
        doc.put("Relevance", null);
        return mapper.convertValue(doc, SaasSearchDocument.class);
    }
}
