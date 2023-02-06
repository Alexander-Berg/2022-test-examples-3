package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;

import static ru.yandex.inside.yt.kosher.tables.YTableEntryTypes.YSON;

public class ExportDropshipDisclosuresExecutorTest extends DeepmindBaseDbTestClass {
    private ExportDropshipDisclosuresExecutor executor;
    private TestYt testYt;
    private final YPath sourceTablePath = YPath.simple("//home/market/deepmind/tests/source");
    private final YPath targetTablePath = YPath.simple("//home/market/deepmind/tests/target");

    @Before
    public void setup() {
        testYt = new TestYt();
        executor = Mockito.spy(
            new ExportDropshipDisclosuresExecutor(UnstableInit.simple(testYt), sourceTablePath, targetTablePath));
    }

    @Test
    public void rowsAreBeingDisclosed() {
        //arrange
        testYt.tables().write(sourceTablePath, YSON, List.of(
            ytTreeMapNode(1, "1", false),
            ytTreeMapNode(2, "2", true)
        ));

        //act
        executor.execute();

        //assert
        var rows = testYt.tables().read(targetTablePath, YSON).stream().collect(Collectors.toList());
        Assertions.assertThat(rows)
            .extracting(n -> n.getBool("disabled"))
            .containsExactly(false);
    }

    @Test
    public void disclosedRowsAreNotWrote() {
        //arrange
        testYt.tables().write(sourceTablePath, YSON, List.of(
            ytTreeMapNode(1, "1", false),
            ytTreeMapNode(2, "2", true)
        ));

        //act
        executor.execute();

        //assert
        var rows = testYt.tables().read(targetTablePath, YSON).stream().collect(Collectors.toList());
        Assertions.assertThat(rows)
            .hasSize(1);
    }

    @Test
    public void timestampAreBeingUpdated() {
        //arrange
        testYt.tables().write(sourceTablePath, YSON, List.of(
            ytTreeMapNode(2, "2", true)
        ));

        //act
        var executionTimestamp = Instant.now();
        executor.execute();

        //assert
        var rows = testYt.tables().read(targetTablePath, YSON).stream().collect(Collectors.toList());
        Assertions.assertThat(rows)
            .extracting(n -> n.getLong("version_timestamp"))
            .allMatch(t -> t > ChronoUnit.MICROS.between(executionTimestamp, Instant.now()));
    }

    @Test
    public void rowsAreDividedIntoBatches() {
        //arrange
        executor.setBatchSize(2);
        testYt.tables().write(sourceTablePath, YSON, List.of(
            ytTreeMapNode(1, "1", true),
            ytTreeMapNode(2, "2", true),
            ytTreeMapNode(3, "3", true)
        ));

        //act
        executor.execute();

        //assert that statuses are divided into two batches
        Mockito.verify(executor, Mockito.times(2))
            .uploadBatch(Mockito.any());

        //does not assert batches content because TestYt create duplicate rows on multiple write calls
        /*var rows = testYt.tables().read(targetTablePath, YSON).stream().collect(Collectors.toList());
        Assertions.assertThat(rows)
            .hasSize(3)
            .usingElementComparator(ytTreeMapNodeComparator())
            .containsExactlyElementsOf(List.of(
                ytTreeMapNode(1, "1", false),
                ytTreeMapNode(2, "2", false),
                ytTreeMapNode(3, "3", false)
            ));*/
    }

    private YTreeMapNode ytTreeMapNode(int supplierId, String shopSku, boolean disabled) {
        return YTree.mapBuilder()
            .key("raw_supplier_id").value(supplierId)
            .key("raw_shop_sku").value(shopSku)
            .key("warehouse_id").value(300)
            .key("disabled").value(disabled)
            .key("version_timestamp").value(0)
            .buildMap();
    }

    private Comparator<YTreeMapNode> ytTreeMapNodeComparator() {
        return (y1, y2) -> {
            if (y1.getInt("raw_supplier_id") != y2.getInt("raw_supplier_id")) {
                return Integer.compare(y1.getInt("raw_supplier_id"), y2.getInt("raw_supplier_id"));
            } else if (!y1.getString("raw_shop_sku").equals(y2.getString("raw_shop_sku"))) {
                return y1.getString("raw_shop_sku").compareTo(y2.getString("raw_shop_sku"));
            } else if (y1.getBool("disabled") != y2.getBool("disabled")) {
                return Boolean.compare(y1.getBool("disabled"), y2.getBool("disabled"));
            } else {
                return 0;
            }
        };
    }
}
