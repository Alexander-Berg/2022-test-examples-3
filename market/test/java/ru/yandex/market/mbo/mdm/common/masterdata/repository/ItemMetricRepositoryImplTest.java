package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.SourceItemKey;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class ItemMetricRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    private ItemMetricRepository repository;
    private EnhancedRandom random = TestDataUtils.defaultRandom(128);

    @Test
    public void testSimpleInsert() {
        var metric = nextMetric();
        repository.insert(metric);
        assertThat(repository.findById(metric.getKey())).isEqualTo(metric);
    }

    @Test
    public void testMultipleInsert() {
        var metrics = List.of(nextMetric(), nextMetric(), nextMetric());
        repository.insertBatch(metrics);
        assertThat(repository.findByIds(keysOf(metrics))).containsExactlyInAnyOrderElementsOf(metrics);
    }

    @Test
    public void testSimpleInsertOrUpdate() {
        var metric = nextMetric();
        metric.setProcessed(false);
        metric.setSourceTs(0);
        repository.insertOrUpdate(metric);
        assertThat(repository.findById(metric.getKey())).isEqualTo(metric);

        metric.setSourceTs(100);
        metric.setProcessed(true);
        assertThat(repository.findById(metric.getKey())).isNotEqualTo(metric);

        repository.insertOrUpdate(metric);
        assertThat(repository.findById(metric.getKey())).isEqualTo(metric);
    }

    @Test
    public void testMultipleInsertOrUpdate() {
        var metrics = List.of(nextMetric(), nextMetric(), nextMetric())
            .stream()
            .map(metric -> metric.setProcessed(false).setSourceTs(0))
            .collect(Collectors.toList());
        repository.insertOrUpdateAll(metrics);
        assertThat(repository.findByIds(keysOf(metrics))).containsExactlyInAnyOrderElementsOf(metrics);

        metrics.forEach(metric -> metric.setSourceTs(100).setProcessed(true));
        repository.insertOrUpdateAll(metrics);
        assertThat(repository.findByIds(keysOf(metrics))).containsExactlyInAnyOrderElementsOf(metrics);
    }

    @Test
    public void testFindUnprocessedByKeys() {
        var unprocessed1 = new ItemMetric().setSupplierId(666).setShopSku("???????????? ????????")
            .setProcessed(false).setSourceType(MasterDataSource.WAREHOUSE).setSourceId("500");
        var unprocessed2 = new ItemMetric().setSupplierId(123).setShopSku("?????????????? ????????????????????")
            .setProcessed(false).setSourceType(MasterDataSource.SUPPLIER).setSourceId("499");
        var processed1 = new ItemMetric().setSupplierId(666).setShopSku("???????????? ????????")
            .setProcessed(true).setSourceType(MasterDataSource.WAREHOUSE).setSourceId("200");
        var processed2 = new ItemMetric().setSupplierId(777).setShopSku("???????????? ?????????????????????? ????????????")
            .setProcessed(true).setSourceType(MasterDataSource.MDM).setSourceId("403");
        var unprocessedExcluded = new ItemMetric().setSupplierId(333).setShopSku("?????????? ???????????? ????????")
            .setProcessed(false).setSourceType(MasterDataSource.WAREHOUSE).setSourceId("404");
        repository.insertBatch(unprocessed1, unprocessed2, processed1, processed2, unprocessedExcluded);
        assertThat(repository.findAll())
            .containsExactlyInAnyOrder(unprocessed1, unprocessed2, processed1, processed2, unprocessedExcluded);

        var found = repository.findUnprocessedByShopSkuKeys(List.of(
            new ShopSkuKey(666, "???????????? ????????"),
            new ShopSkuKey(123, "?????????????? ????????????????????")
        ));
        assertThat(found).hasSize(2);
        assertThat(found.get(new ShopSkuKey(666, "???????????? ????????"))).containsExactly(unprocessed1);
        assertThat(found.get(new ShopSkuKey(123, "?????????????? ????????????????????"))).containsExactly(unprocessed2);
    }

    private ItemMetric nextMetric() {
        return random.nextObject(ItemMetric.class);
    }

    private List<SourceItemKey> keysOf(List<ItemMetric> metrics) {
        return metrics.stream().map(ItemMetric::getKey).collect(Collectors.toList());
    }
}
