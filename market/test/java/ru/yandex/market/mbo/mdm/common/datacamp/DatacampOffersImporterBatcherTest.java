package ru.yandex.market.mbo.mdm.common.datacamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.FromDatacampMapping;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.FromDatacampOfferConvertResult;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class DatacampOffersImporterBatcherTest {

    @Test
    public void testNothing() {
        DatacampOffersImporterImpl.batchize(mappings(0, 0), sskus(0, 0), 10, (m, s) -> {
            Assertions.fail("Should not reach this point");
        });
    }

    @Test
    public void testSomeMappingsNoSskus() {
        List<FromDatacampOfferConvertResult> traversedMappings = new ArrayList<>();
        List<SilverCommonSsku> traversedSskus = new ArrayList<>();
        DatacampOffersImporterImpl.batchize(mappings(0, 27), sskus(0, 0), 10, (m, s) -> {
            traversedMappings.addAll(m);
            traversedSskus.addAll(s);
        });

        Assertions.assertThat(traversedMappings).containsExactlyInAnyOrderElementsOf(mappings(0, 27));
        Assertions.assertThat(traversedSskus).isEmpty();
    }

    @Test
    public void testNoMappingsSomeSskus() {
        List<FromDatacampOfferConvertResult> traversedMappings = new ArrayList<>();
        List<SilverCommonSsku> traversedSskus = new ArrayList<>();
        DatacampOffersImporterImpl.batchize(mappings(0, 0), sskus(0, 24), 10, (m, s) -> {
            traversedMappings.addAll(m);
            traversedSskus.addAll(s);
        });

        Assertions.assertThat(traversedMappings).isEmpty();
        Assertions.assertThat(traversedSskus).containsExactlyInAnyOrderElementsOf(sskus(0, 24));
    }

    @Test
    public void testSharedMappingsAndSskus1() {
        List<FromDatacampOfferConvertResult> traversedMappings = new ArrayList<>();
        List<SilverCommonSsku> traversedSskus = new ArrayList<>();
        DatacampOffersImporterImpl.batchize(mappings(0, 37), sskus(0, 24), 10, (m, s) -> {
            traversedMappings.addAll(m);
            traversedSskus.addAll(s);
        });

        Assertions.assertThat(traversedMappings).containsExactlyInAnyOrderElementsOf(mappings(0, 37));
        Assertions.assertThat(traversedSskus).containsExactlyInAnyOrderElementsOf(sskus(0, 24));
    }

    @Test
    public void testSharedMappingsAndSskus2() {
        List<FromDatacampOfferConvertResult> traversedMappings = new ArrayList<>();
        List<SilverCommonSsku> traversedSskus = new ArrayList<>();
        DatacampOffersImporterImpl.batchize(mappings(0, 17), sskus(0, 24), 10, (m, s) -> {
            traversedMappings.addAll(m);
            traversedSskus.addAll(s);
        });

        Assertions.assertThat(traversedMappings).containsExactlyInAnyOrderElementsOf(mappings(0, 17));
        Assertions.assertThat(traversedSskus).containsExactlyInAnyOrderElementsOf(sskus(0, 24));
    }

    @Test
    public void testDifferentMappingsAndSskus() {
        List<FromDatacampOfferConvertResult> traversedMappings = new ArrayList<>();
        List<SilverCommonSsku> traversedSskus = new ArrayList<>();
        DatacampOffersImporterImpl.batchize(mappings(0, 17), sskus(23, 41), 10, (m, s) -> {
            traversedMappings.addAll(m);
            traversedSskus.addAll(s);
        });

        Assertions.assertThat(traversedMappings).containsExactlyInAnyOrderElementsOf(mappings(0, 17));
        Assertions.assertThat(traversedSskus).containsExactlyInAnyOrderElementsOf(sskus(23, 41));
    }

    @Test
    public void testIntersectedMappingsAndSskus() {
        List<FromDatacampOfferConvertResult> traversedMappings = new ArrayList<>();
        List<SilverCommonSsku> traversedSskus = new ArrayList<>();
        DatacampOffersImporterImpl.batchize(mappings(0, 17), sskus(13, 38), 10, (m, s) -> {
            traversedMappings.addAll(m);
            traversedSskus.addAll(s);
        });

        Assertions.assertThat(traversedMappings).containsExactlyInAnyOrderElementsOf(mappings(0, 17));
        Assertions.assertThat(traversedSskus).containsExactlyInAnyOrderElementsOf(sskus(13, 38));
    }

    private static Collection<FromDatacampOfferConvertResult> mappings(int from, int to) {
        return IntStream.range(from, to).boxed().map(i -> new FromDatacampOfferConvertResult(
            new CommonSsku(new ShopSkuKey(i, ".")),
            new FromDatacampMapping(new ShopSkuKey(i, "."), 0, 0, 0, true, false)
        )).collect(Collectors.toList());
    }

    private static Collection<SilverCommonSsku> sskus(int from, int to) {
        return IntStream.range(from, to)
            .boxed()
            .map(i -> new SilverCommonSsku(new SilverSskuKey(new ShopSkuKey(i, "."), MasterDataSource.DEFAULT_SOURCE)))
            .collect(Collectors.toList());
    }
}
