package ru.yandex.market.aliasmaker.cache.offers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.googlecode.protobuf.format.JsonFormat;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.cache.CategoryCache;
import ru.yandex.market.aliasmaker.models.CategoryKnowledge;
import ru.yandex.market.aliasmaker.offers.GDOfferUtils;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.OfferStorageService;
import ru.yandex.market.mbo.http.OffersStorage;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;

/**
 * @author york
 * @since 06.08.2019
 */
public class OffersShadowReloadingCacheTest {
    private Map<String, Offer> offers;
    private Map<String, OffersStorage.GenerationDataOffer> gdOffers;

    private OffersShadowReloadingCache mockCache;
    private OffersShadowReloadingCache realCache;
    private OfferStorageService offerStorageService;
    private int categoryId;
    private YtSessionReaderMock ytSessionReader;
    private CategoryKnowledge knowledge;

    @Before
    public void setUp() throws IOException {
        OffersStorage.GetOffersResponse.Builder builder = OffersStorage.GetOffersResponse.newBuilder();
        JsonFormat.merge(
                new InputStreamReader(
                        getClass().getResourceAsStream("/offers.json")
                ),
                builder
        );
        gdOffers = builder.getOffersList().stream()
                .collect(Collectors.toMap(g -> g.getClassifierMagicId(), g -> g));
        offers = new HashMap<>();
        for (OffersStorage.GenerationDataOffer offer : builder.getOffersList()) {
            offers.put(
                    offer.getClassifierMagicId(),
                    new Offer(
                            offer, new ArrayList<>(),
                            GDOfferUtils.parseOfferParams(offer.getOfferParams())
                    )
            );
            categoryId = (int) offer.getCategoryId();
        }
        SerializedSessionsService mockSessionsService = Mockito.mock(SerializedSessionsService.class);
        Mockito.when(mockSessionsService.getCurrentSessionInfo(anyInt())).thenAnswer(invocation ->
                new SerializedSessionsService.SessionInfo()
                        .setSessionId("10012011")
                        .setCategoryId(invocation.getArgument(0, Integer.class)));

        Mockito.doAnswer(invocation -> {
            Consumer<Iterator<Offer>> consumer = invocation.getArgument(1, Consumer.class);
            consumer.accept(offers.values().iterator());
            return null;
        }).when(mockSessionsService).readOffers(anyInt(), any(Consumer.class), anyBoolean());

        mockCache = new OffersShadowReloadingCache();
        mockCache.setSerializedSessionsService(mockSessionsService);
        FormalizerService formalizerService = Mockito.mock(FormalizerService.class);
        Mockito.when(formalizerService.formalize(any())).then(invocation -> {
            Formalizer.FormalizerRequest fm = invocation.getArgument(0, Formalizer.FormalizerRequest.class);
            Formalizer.FormalizerResponse.Builder resultBuilder = Formalizer.FormalizerResponse.newBuilder();
            fm.getOfferList().forEach(offer -> resultBuilder.addOffer(Formalizer.FormalizedOffer.getDefaultInstance()));
            return resultBuilder.build();
        });
        mockCache.setFormalizerService(formalizerService);
        offerStorageService = Mockito.mock(OfferStorageService.class);
        Mockito.when(offerStorageService.getOffersByIds(any())).then(invocation -> {
            OffersStorage.GetOffersRequest req = invocation.getArgument(0, OffersStorage.GetOffersRequest.class);
            return OffersStorage.GetOffersResponse.newBuilder()
                    .addAllOffers(req.getClassifierMagicIdsList().stream()
                            .map(id -> gdOffers.get(id))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .build();
        });
        mockCache.setOfferStorageService(offerStorageService);

        knowledge = Mockito.mock(CategoryKnowledge.class);
        Mockito.when(knowledge.getVendorByLocalId(anyLong())).then(
                invocation -> MboParameters.Option.newBuilder()
                        .setId(invocation.getArgument(0, Long.class))
                        .build());

        CategoryCache categoryCache = Mockito.mock(CategoryCache.class);
        Mockito.when(categoryCache.getCategory(anyInt())).thenReturn(knowledge);

        realCache = new OffersShadowReloadingCache();
        ytSessionReader = Mockito.spy(new YtSessionReaderMock());
        realCache.setYtSessionReader(ytSessionReader);
        realCache.setSerializedSessionsService(
                new SerializedSessionsService(System.getProperty("java.io.tmpdir"), 100000));
        realCache.setCategoryCache(categoryCache);
    }

    @Test
    public void testBulkLoading() {
        Assertions.assertThat(gdOffers.remove("1")).isNotNull();
        Assertions.assertThat(gdOffers.remove("2")).isNotNull();
        // loading from OfferStorage
        Assertions.assertThat(mockCache.get(categoryId, "3")).isEqualTo(offers.get("3"));
        Mockito.verify(offerStorageService, Mockito.times(1)).getOffersByIds(any());
        // second read
        Assertions.assertThat(mockCache.get(categoryId, "3")).isEqualTo(offers.get("3"));
        Mockito.verifyZeroInteractions(offerStorageService);

        Assertions.assertThat(mockCache.getOffers(categoryId, Arrays.asList("1", "2", "3"))).containsExactlyInAnyOrder(
                offers.get("1"),
                offers.get("2"),
                offers.get("3")
        );
        Mockito.verify(offerStorageService, Mockito.times(2)).getOffersByIds(any());
        //second read - all offers are in mockCache
        Assertions.assertThat(mockCache.getOffers(categoryId, Arrays.asList("1", "2", "3"))).containsExactlyInAnyOrder(
                offers.get("1"),
                offers.get("2"),
                offers.get("3")
        );
        Mockito.verifyZeroInteractions(offerStorageService);
    }

    private <T> void assertRealCache(int categoryId, Function<Offer, T> getter, T... vals) {
        List<T> cur = new ArrayList<>();
        realCache.doWithOffers(categoryId, offerIterator ->
                offerIterator.forEachRemaining(o -> cur.add(getter.apply(o))));
        Assertions.assertThat(cur).containsExactly(vals);
    }

    private OffersStorage.GenerationDataOffer offer(String offerId, long clusterId,
                                                    int vendId, long modelId, long skuId) {
        return OffersStorage.GenerationDataOffer.newBuilder()
                .setClassifierMagicId(offerId)
                .setLongClusterId(clusterId)
                .setGlobalVendorId(vendId)
                .setModelId(modelId)
                .setMarketSkuId(skuId)
                .build();
    }
}
