package ru.yandex.market.mbo.gurulight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.mbi.ShopsProvider;
import ru.yandex.market.mbo.common.model.Shop;
import ru.yandex.market.mbo.core.saas.SaasActiveServiceRouter;
import ru.yandex.market.mbo.core.saas.SaasClient;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.utils.ReflectionUtils;
import ru.yandex.market.saas.search.SaasSearchService;
import ru.yandex.market.saas.search.response.SaasKeyValuePair;
import ru.yandex.market.saas.search.response.SaasResponseGroup;
import ru.yandex.market.saas.search.response.SaasResponseGrouping;
import ru.yandex.market.saas.search.response.SaasSearchDocument;
import ru.yandex.market.saas.search.response.SaasSearchResponse;

@SuppressWarnings("checkstyle:MagicNumber")
public class MbologsServiceHidSplitTest {
    private static final int SIMPLE_SIZE = 10;
    private static final int SPLIT_SIZE = 400;

    private MbologsService mbologsService;
    private TovarCategoryNode node;
    private SaasSearchService searchService;

    @Before
    public void setUp() throws Exception {
        // Heavy deep mocking :)
        SaasClient saasClient = Mockito.mock(SaasClient.class);
        SaasActiveServiceRouter saasMock = Mockito.mock(SaasActiveServiceRouter.class);
        searchService = Mockito.mock(SaasSearchService.class);
        Mockito.when(saasMock.getActiveClient()).thenReturn(saasClient);
        Mockito.when(saasClient.searchService()).thenReturn(searchService);

        TovarTreeForVisualService treeService = Mockito.mock(TovarTreeForVisualService.class);
        TovarTree tovarTree = Mockito.mock(TovarTree.class);
        node = Mockito.mock(TovarCategoryNode.class);
        Mockito.when(treeService.loadSchemeWholeTree()).thenReturn(tovarTree);
        Mockito.when(tovarTree.byHid(Mockito.anyLong())).thenReturn(node);

        ShopsProvider shopsProvider = Mockito.mock(ShopsProvider.class);
        Mockito.when(shopsProvider.getShop(Mockito.anyLong())).thenReturn(new Shop());

        mbologsService = new MbologsService(treeService, null, null,
            shopsProvider, saasMock, null, null, null, ThreadLocalRandom::current);
    }

    @Test
    public void testSimpleCaseCount() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SIMPLE_SIZE));

        SaasSearchResponse searchResponse = new SaasSearchResponse();
        // ReflectionUtils.set is required as SaasSearchResponse & company don't contain setters.
        ReflectionUtils.set(searchResponse, "total", new int[]{100});
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        int offersCount = mbologsService.getOffersCount(new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offersCount).isEqualTo(100);

        Mockito.verify(searchService, Mockito.only()).search(Mockito.any());
    }

    @Test
    public void testSplitCount() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SPLIT_SIZE));

        SaasSearchResponse searchResponse = new SaasSearchResponse();
        ReflectionUtils.set(searchResponse, "total", new int[]{100});
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        int offersCount = mbologsService.getOffersCount(new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offersCount).isEqualTo(200);
        Mockito.verify(searchService, Mockito.times(2)).search(Mockito.any());
    }

    @Test
    public void testSimpleOffers() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SIMPLE_SIZE));

        SaasSearchResponse searchResponse = generateResults(SIMPLE_SIZE);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        List<OfferData> offers = mbologsService.getOfferDatas(0, new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offers).hasSize(SIMPLE_SIZE);
        Mockito.verify(searchService, Mockito.times(1)).search(Mockito.any());
    }

    @Test
    public void testSimplePageOffers() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SIMPLE_SIZE));

        SaasSearchResponse searchResponse = generateResults(10000);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        List<OfferData> offers = mbologsService.getOfferDatas(0, new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offers).hasSize(Settings.OFFERS_ON_PAGE);

        Mockito.verify(searchService, Mockito.times(1)).search(Mockito.any());
    }

    @Test
    public void testSplitOffers() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SPLIT_SIZE));

        SaasSearchResponse searchResponse = generateResults(SIMPLE_SIZE);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        List<OfferData> offers = mbologsService.getOfferDatas(0, new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offers).hasSize(20);
        Mockito.verify(searchService, Mockito.times(2)).search(Mockito.any());
    }

    @Test
    public void testSplitPageSizeOffers() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SPLIT_SIZE));

        SaasSearchResponse searchResponse = generateResults(10000);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        List<OfferData> offers = mbologsService.getOfferDatas(0, new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offers).hasSize(Settings.OFFERS_ON_PAGE);

        // Should be sorted and thus shouldn't be 00099
        Assertions.assertThat(offers.get(offers.size() - 1).offerId).isEqualTo("00049");
        Mockito.verify(searchService, Mockito.times(2)).search(Mockito.any());
    }

    @Test
    public void testSimplePageOk() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SIMPLE_SIZE));

        SaasSearchResponse searchResponse = generateResults(10000);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        List<OfferData> offers = mbologsService.getOfferDatas(1, new MbologsSearchFilter().setHid(1L));
        Assertions.assertThat(offers).hasSize(Settings.OFFERS_ON_PAGE);

        Mockito.verify(searchService, Mockito.times(1)).search(Mockito.any());
    }

    @Test
    public void testSplitPageException() throws Exception {
        Mockito.when(node.subtreeHids()).thenReturn(range(SPLIT_SIZE));

        SaasSearchResponse searchResponse = generateResults(100);
        Mockito.when(searchService.search(Mockito.any())).thenReturn(searchResponse);

        Assertions.assertThatThrownBy(() ->
            mbologsService.getOfferDatas(1, new MbologsSearchFilter().setHid(1L))
        ).isInstanceOf(UnsupportedOperationException.class);
    }

    private SaasSearchResponse generateResults(int count) throws Exception {
        List<SaasResponseGroup> groups = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SaasSearchDocument document = new SaasSearchDocument();
            String id = String.format("%05d", i);
            ReflectionUtils.set(document, "archiveInfo.id", id);
            ReflectionUtils.set(document, "archiveInfo.attributes", ImmutableMap.of(
                OfferDataSaasFields.OFFER_ID.getName(), id,
                OfferDataSaasFields.SHOP_ID.getName(), "1",
                OfferDataSaasFields.CATEGORY_ID.getName(), "1"
            ).entrySet().stream().map(it -> {
                SaasKeyValuePair saasKeyValuePair = new SaasKeyValuePair();
                try {
                    ReflectionUtils.set(saasKeyValuePair, "key", it.getKey());
                    ReflectionUtils.set(saasKeyValuePair, "value", it.getValue());
                } catch (Exception e) {
                    throw new RuntimeException("Error during mocking", e);
                }
                return saasKeyValuePair;
            }).collect(Collectors.toList()));

            SaasResponseGroup group = new SaasResponseGroup();
            ReflectionUtils.set(group, "documents", Collections.singletonList(document));
            groups.add(group);
        }
        SaasSearchResponse saasSearchResponse = new SaasSearchResponse();
        SaasResponseGrouping saasResponseGrouping = new SaasResponseGrouping();
        ReflectionUtils.set(saasResponseGrouping, "groups", groups);
        ReflectionUtils.set(saasSearchResponse, "groupings", Arrays.asList(saasResponseGrouping));
        return saasSearchResponse;
    }

    private Set<Long> range(int size) {
        HashSet<Long> longs = new HashSet<>();
        for (long i = 0; i < size; i++) {
            longs.add(i);
        }
        return longs;
    }
}
