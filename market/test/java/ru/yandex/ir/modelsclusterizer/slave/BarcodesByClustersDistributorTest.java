package ru.yandex.ir.modelsclusterizer.slave;

import com.google.common.collect.Multimaps;
import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.CategoryInfo;
import ru.yandex.ir.modelsclusterizer.be.Cluster;
import ru.yandex.ir.modelsclusterizer.be.DirtyBarcode;
import ru.yandex.ir.modelsclusterizer.be.FormalizedOffer;
import ru.yandex.ir.modelsclusterizer.be.RepresentativeOfferSet;
import ru.yandex.ir.modelsclusterizer.utils.MapToSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * @author Aydar Gilmullin, <a href="mailto:aydar-gil@yandex-team.ru"/>
 */
public class BarcodesByClustersDistributorTest {

    private Cluster buildCluster(CategoryInfo categoryInfo, long vendorId, String name, List<String> articleAliases) {
        return new Cluster.Builder()
            .setRuntimeContext(
                new Cluster.ClusterRuntimeContext.Builder()
                    .setCategoryInfo(categoryInfo)
                    .setSessionId("0")
                    .setFormalizedOffers(Collections.<FormalizedOffer>emptyList())
                    .setDuplicateRate(0.)
                    .build()
            )
            .setContent(
                new Cluster.ClusterContent.Builder()
                    .setVendor(vendorId)
                    .setName(name)
                    .setArticleAliases(articleAliases)
                    .setGoodIdToMagicIdOffersMap(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new))
                    .setRepresentativeOfferSet(RepresentativeOfferSet.REPRESENTATIVE_OFFER_DISABLED_ENTRY)
                    .build()
            )
            .build();
    }

    @Test
    public void distributeBarcodesByClustersTest() {
        List<Cluster> clusters = new ArrayList<>();
        CategoryInfo categoryInfoMock = new CategoryInfo.Builder().setCategoryId(1).build();
        Cluster atala = buildCluster(categoryInfoMock, 123, "atala", asList("atala"));
        clusters.add(atala);
        Cluster puky = buildCluster(categoryInfoMock, 321, "puky", asList("puky"));
        clusters.add(puky);
        Cluster atalaExt = buildCluster(categoryInfoMock, 222, "atalaExt", asList("atala", "toys"));
        clusters.add(atalaExt);
        Cluster imc = buildCluster(categoryInfoMock, 222, "imc", asList("IMC"));
        clusters.add(imc);
        Cluster dummy = buildCluster(categoryInfoMock, 0, "dummy", asList("dummy"));
        clusters.add(dummy);

        List<DirtyBarcode> dirtyBarcodes = createBarcodes();

        MapToSet<Cluster, Long> map = new BarcodesByClustersDistributor().distributeBarcodesByClusters(
            clusters, dirtyBarcodes
        );
        // баркод пытаемся прицепить по алиасу/алиасам максимальной длины, который нашли в строке баркода
        // если алиас/алиасы относятся к разным кластерам, то отметаем такой баркод
        // сделал шурк в мае 2015, BarcodesByClustersDistributor#findMaxAliasLength
        assertFalse(map.containsKey(atala));
        assertFalse(map.containsKey(atalaExt));
        assertFalse(map.containsKey(imc));

        assertTrue(map.containsKey(puky));
        //dummy не будет в мапе, так как его алиасам не соответствует ни один токен из описания баркодов
        assertEquals(1, map.size());

        //Баркод будет сопоставлен двум кластерам, а поэтому выкинут. Больше никто не соответствует atala
        assertNull(map.get(atala));

        assertEquals(6, map.get(puky).size());
        assertTrue(map.get(puky).containsAll(
            asList(2000803500002L, 2001218700308L,
                4015731040153L, 4015731040160L,
                4015731040191L, 4015731040399L)
            )
        );

        //Один баркод будет равен баркоду для кластера atala, другой, будет равен баркоду для кластера imc
        assertNull(map.get(atalaExt));
    }

    private List<DirtyBarcode> createBarcodes() {
        List<DirtyBarcode> codes = new ArrayList<>();

        codes.add(new DirtyBarcode(4250136782059L, "ATALA 24 Jugend-Rennrad Speedy Racing", null));
        codes.add(new DirtyBarcode(8421134250116L, "IMC Toys Cars Pinball-Flipper", null));
        codes.add(new DirtyBarcode(2140000456273L, "Велосипед трехколесный Abeya ET 103B", null));
        codes.add(new DirtyBarcode(2140000456297L, "Велосипед трехколесный Abeya ET 83B", null));
        codes.add(new DirtyBarcode(2140000803633L, "Велосипед детский трехколесный 146-311", null));
        codes.add(new DirtyBarcode(2000803500002L, "Puky Lenker-Korb-Laufrad LKL silber", null));
        codes.add(new DirtyBarcode(2001218700308L, "Puky-Laufrad*** Puky - Laufrad LR 1 ( PINK)", null));
        codes.add(new DirtyBarcode(4015731040153L, "Puky Laufrad PUKYlino My first PUKY pink", null));
        codes.add(new DirtyBarcode(4015731040160L, "Puky Laufrad PUKYlino My first PUKY ozean blau", null));
        codes.add(new DirtyBarcode(4015731040191L, "Puky Laufrad PUKYlino My first PUKY rot", null));
        codes.add(new DirtyBarcode(4015731040399L, "Laufrad Prinzessin Lillifee von Puky (4039)", null));
        codes.add(new DirtyBarcode(6905150800484L, "KWESTкруж.Колесн.250 под.", null));

        return codes;
    }


}
