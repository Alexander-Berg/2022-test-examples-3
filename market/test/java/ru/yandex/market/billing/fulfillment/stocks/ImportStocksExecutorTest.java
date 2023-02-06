package ru.yandex.market.billing.fulfillment.stocks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.StockDao;
import ru.yandex.market.core.fulfillment.matchers.StockMatchers;
import ru.yandex.market.core.fulfillment.model.Stock;
import ru.yandex.market.core.order.SupplierShopSkuKey;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtil.stringNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.intNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.yPathHasPath;

/**
 * Функциональный тест на {@link SyncStocksExecutor}
 *
 * @author fbokovikov
 */
@ExtendWith(MockitoExtension.class)
class ImportStocksExecutorTest extends FunctionalTest {
    private static final String INACTIVE_TABLE = "shops_web.imported_stocks_alt";

    @Autowired
    @Qualifier("syncStocksPgExecutor")
    private Executor syncStocksExecutor;

    @Autowired
    private StockDao stockDao;

    @Autowired
    @Qualifier("stocksImportYtCluster")
    private YtCluster stocksImportYtCluster;

    @Mock
    private Yt yt;
    @Mock
    private Cypress cypress;
    @Mock
    private YtTables ytTables;
    @Value("${mbi.fulfillment.utilized.stocks.daily.yt.path}")
    private String dailyStocksYtPathUtilized;

    @Captor
    private ArgumentCaptor<List<Stock<SupplierShopSkuKey>>> mergeSkuStockInfoListCapture;

    private static YTreeMapNodeImpl buildSkuNode(int supplierId, String shopSku) {
        YTreeMapNodeImpl yTreeMapNode = new YTreeMapNodeImpl(new OpenHashMap<>());
        yTreeMapNode.put("supplier_id", intNode(supplierId));
        yTreeMapNode.put("warehouse_id", intNode(1));
        yTreeMapNode.put("shop_sku", stringNode(shopSku));
        yTreeMapNode.put("fit", intNode(1));
        yTreeMapNode.put("quarantine", intNode(1));
        yTreeMapNode.put("utilization", intNode(1));
        yTreeMapNode.put("defect", intNode(1));
        yTreeMapNode.put("freezed", intNode(1));
        yTreeMapNode.put("expired", intNode(1));
        yTreeMapNode.put("lifetime", intNode(1));
        yTreeMapNode.put("refilled_date", stringNode("2019-04-10"));
        return yTreeMapNode;
    }


    @DisplayName("Импорт стоков с переключением таблиц")
    @Test
    @DbUnitDataSet(
            before = "ImportStocksExecutorTest.syncStocksImport.before.csv",
            after = "ImportStocksExecutorTest.syncStocksImport.after.csv")
    void testSyncStocksImport_tableSwitch() {
        initYt("2017-01-05", dailyStocksYtPathUtilized, List.of(
                buildSkuNode(1, "sku1"),
                buildSkuNode(2, "sku2"), // Не должна быть проигнорирована, потому что supplierId = 2
                buildSkuNode(1, "sku2"), // Должна быть проигнорирована, потому что есть в env
                buildSkuNode(1, "sku3"), // Должна быть проигнорирована, потому что есть в env
                buildSkuNode(3, "sku3"), // Все shop_sku поставщика supplierId = 3 должны быть проигнорированы
                buildSkuNode(3, "sku4"), // Все shop_sku поставщика supplierId = 3 должны быть проигнорированы
                buildSkuNode(4, "sku5")
        ), null);

        syncStocksExecutor.doJob(null);

        // проверяем обращения к yt
        verify(cypress).list(
                argThat(ytPath -> yPathHasPath(ytPath, dailyStocksYtPathUtilized)),
                any(SetF.class)
        );
        verify(ytTables).read(
                argThat(ytPath -> yPathHasPath(ytPath, dailyStocksYtPathUtilized + "/2017-01-05")),
                any(YTableEntryType.class),
                any(Consumer.class)
        );

        verifyNoMoreInteractions(cypress);
        verifyNoMoreInteractions(ytTables);

        // проверяем обращения к dao
        verify(stockDao).clearTable(eq(INACTIVE_TABLE));
        verify(stockDao).insertImportedStocks(mergeSkuStockInfoListCapture.capture(), eq(INACTIVE_TABLE));

        assertThat(
                mergeSkuStockInfoListCapture.getValue().stream()
                        .sorted(Comparator.comparingLong(Stock::getSupplierId))
                        .collect(Collectors.toList()),
                contains(
                        allOf(
                                //один проверяем полностью
                                StockMatchers.hasSupplierId(1L),
                                StockMatchers.hasShopSku("sku1"),
                                StockMatchers.hasAvailable(1),
                                StockMatchers.hasWarehouseId(1L),
                                StockMatchers.hasQuarantine(1),
                                StockMatchers.hasUtilization(1),
                                StockMatchers.hasFreeze(1),
                                StockMatchers.hasLifetime(1),
                                StockMatchers.hasDefect(1),
                                StockMatchers.hasExpired(1),
                                StockMatchers.hasDateTime(LocalDateTime.of(2017, 1, 5, 0, 0, 0))
                        ),   allOf(
                                StockMatchers.hasSupplierId(2L),
                                StockMatchers.hasShopSku("sku2")
                        )
                )
        );
        verifyNoMoreInteractions(stockDao);
    }

    private void initYt(String nodeValue, String path, List<YTreeMapNodeImpl> ytReturnValue, String filter) {
        String tableFullPath = (filter == null ? "" : filter) + path + "/" + nodeValue;
        when(cypress.list(
                argThat(ytPath -> yPathHasPath(ytPath, path)),
                any(SetF.class)
        )).thenReturn(
                Cf.arrayList(new YTreeStringNodeImpl(
                        nodeValue,
                        Cf.map("type", stringNode("table"))
                ))
        );

        doAnswer(invocation -> {
            Consumer<YTreeMapNode> consumer = invocation.getArgument(2);

            List<YTreeMapNode> nodes = new ArrayList<>(ytReturnValue);
            nodes.forEach(consumer);

            return null;
        }).when(ytTables)
                .read(
                        argThat(ytPath -> yPathHasPath(ytPath, tableFullPath)),
                        any(YTableEntryType.class),
                        any(Consumer.class)
                );

        when(yt.cypress()).thenReturn(cypress);
        when(yt.tables()).thenReturn(ytTables);
        when(stocksImportYtCluster.getYt()).thenReturn(yt);
    }
}
