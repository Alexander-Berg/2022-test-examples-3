package ru.yandex.market.agency.program.cutprice;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.yt.YtUtilTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtilTest.intNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.stringNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.tableNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.treeMapNode;

/**
 * Тесты на {@link ARPCutpriceImportExecutor}
 */
@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "ARPCutpriceImportExecutor.data.csv")
class ARPCutpriceImportExecutorTest extends FunctionalTest {

    @Autowired
    protected Cypress cypress;

    @Autowired
    private Yt hahnYt;

    @Autowired
    private ARPCutpriceImportExecutor executor;

    @Value("${mbi.arp.cut_price.client.yt}")
    private String ytClientInfoFolderPath;

    @Value("${mbi.arp.cut_price.datasource.yt}")
    private String ytDatasourceInfoFolderPath;

    @Mock
    private YtTables ytTables;

    private static YTreeMapNode clientInfoRow(String date, int agencyId, int clientId,
                                              int offersCount, int offersDays) {
        return treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                .put("date", stringNode(date))
                .put("agency_id", intNode(agencyId))
                .put("client_id", intNode(clientId))
                .put("offers_count", intNode(offersCount))
                .put("offers_days", intNode(offersDays))
                .build());
    }

    private static YTreeMapNode datasourceInfoRow(String date, int clientId,
                                                  int datasourceId, String datasourceName, String datasourceDomain,
                                                  int offersCount, int offersDays) {
        return treeMapNode(ImmutableMap.<String, YTreeNode>builder()
                .put("date", stringNode(date))
                .put("client_id", intNode(clientId))
                .put("datasource_id", intNode(datasourceId))
                .put("datasource_name", stringNode(datasourceName))
                .put("datasource_domain", stringNode(datasourceDomain))
                .put("offers_count", intNode(offersCount))
                .put("offers_days", intNode(offersDays))
                .build());
    }

    private static YPath yPath(String path, String table) {
        return YPath.simple(path + "/" + table);
    }

    @BeforeEach
    void setUp() {
        when(hahnYt.cypress()).thenReturn(cypress);
        when(hahnYt.tables()).thenReturn(ytTables);
    }

    @Test
    @DisplayName("Проверка полного импорта всех таблиц")
    @DbUnitDataSet(after = "ARPCutpriceImportExecutor.full.after.csv")
    void testFullImport() {
        prepareYtTables();

        executor.doJob(null);
    }

    @Test
    @DisplayName("Проверка частичного импорта таблиц")
    @DbUnitDataSet(
            before = "ARPCutpriceImportExecutor.partial.before.csv",
            after = "ARPCutpriceImportExecutor.partial.after.csv"
    )
    @SuppressWarnings("unchecked")
    void testPartialImport() {
        prepareYtTables();

        executor.doJob(null);

        verify(ytTables, never()).read(
                eq(yPath(ytClientInfoFolderPath, "2019-12-01")), any(YTableEntryType.class), any(Consumer.class));
        verify(ytTables, never()).read(
                eq(yPath(ytDatasourceInfoFolderPath, "2019-12-01")), any(YTableEntryType.class), any(Consumer.class));
    }

    @Test
    @DisplayName("Проверка импорта таблиц в хронологическом порядке")
    @DbUnitDataSet(after = "ARPCutpriceImportExecutor.full.after.csv")
    @SuppressWarnings("unchecked")
    void testImportInAscendingOrder() {
        prepareYtTables();

        executor.doJob(null);

        InOrder order = inOrder(ytTables);
        order.verify(ytTables).read(
                eq(yPath(ytClientInfoFolderPath, "2019-12-01")), any(YTableEntryType.class), any(Consumer.class));
        order.verify(ytTables).read(
                eq(yPath(ytClientInfoFolderPath, "2019-12-02")), any(YTableEntryType.class), any(Consumer.class));
    }

    @Test
    @DisplayName("Бросать исключение, если листинги разные")
    void differentListingsTest() {
        when(cypress.list(eq(YPath.simple(ytClientInfoFolderPath)), anyCollection()))
                .thenReturn(getOneDateListing("1996-10-27"));
        when(cypress.list(eq(YPath.simple(ytDatasourceInfoFolderPath)), anyCollection()))
                .thenReturn(getOneDateListing("1996-05-28"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> executor.doJob(null));
    }

    List<YTreeStringNode> getOneDateListing(final String date) {
        return Collections.singletonList(tableNode(date));
    }

    private void prepareYtTables() {
        prepareTableRead(yPath(ytClientInfoFolderPath, "2019-12-01"), List.of(
                clientInfoRow("2019-12-01", 100, 5, 13, 8)
        ));
        prepareTableRead(yPath(ytDatasourceInfoFolderPath, "2019-12-01"), List.of(
                datasourceInfoRow("2019-12-01", 5, 1, "shop1", "shop1.ru", 6, 4),
                datasourceInfoRow("2019-12-01", 5, 2, "shop2", "shop2.ru", 5, 5)
        ));

        prepareTableRead(yPath(ytClientInfoFolderPath, "2019-12-02"), List.of(
                clientInfoRow("2019-12-02", 100, 5, 13, 9),
                clientInfoRow("2019-12-02", 100, 10, 15, 15)
        ));
        prepareTableRead(yPath(ytDatasourceInfoFolderPath, "2019-12-02"), List.of(
                datasourceInfoRow("2019-12-02", 5, 1, "shop1", "shop1.ru", 6, 4),
                datasourceInfoRow("2019-12-02", 5, 2, "shop2", "shop2.ru", 5, 5),
                datasourceInfoRow("2019-12-02", 10, 3, "shop3", null, 13, 9)
        ));

        final List<YTreeStringNode> tables = Stream.of("2019-12-02", "2019-12-01")
                .map(YtUtilTest::tableNode)
                .collect(Collectors.toList());
        when(cypress.list(eq(YPath.simple(ytClientInfoFolderPath)), anyCollection())).thenReturn(tables);
        when(cypress.list(eq(YPath.simple(ytDatasourceInfoFolderPath)), anyCollection())).thenReturn(tables);

    }

    private void prepareTableRead(YPath yPath, List<YTreeMapNode> nodes) {
        //noinspection unchecked
        doAnswer(invocation -> {
                    final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                    nodes.forEach(consumer);
                    return null;
                }
        ).when(ytTables).read(eq(yPath), any(YTableEntryType.class), any(Consumer.class));
    }
}
