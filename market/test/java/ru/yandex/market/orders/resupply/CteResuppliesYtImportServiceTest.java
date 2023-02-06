package ru.yandex.market.orders.resupply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtUtilTest;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link CteResuppliesYtImportService}
 */
class CteResuppliesYtImportServiceTest extends FunctionalTest {

    private static final String HAHN = "hahn.yt.yandex.net";

    @Autowired
    private YtHttpFactory ytHttpFactory;

    private YtCluster ytCluster;

    @Autowired
    private CteResuppliesYtImportService cteResuppliesYtImportService;

    private static YTreeMapNode buildCteResupplyMapNode(
            long id,
            String manufacturersku,
            String orderId,
            String createdAt,
            long requestId,
            List<String> attributes,
            String stock,
            long supplierId,
            String supplyType
    ) {
        Map<String, YTreeNode> params = new HashMap<>();
        params.put("id", YtUtilTest.longNode(id));
        params.put("manufacturersku", YtUtilTest.stringNode(manufacturersku));
        params.put("order_id", YtUtilTest.stringNode(orderId));
        params.put("created_at", YtUtilTest.stringNode(createdAt));
        params.put("ff_request_id", YtUtilTest.longNode(requestId));
        params.put("attributes", YtUtilTest.treeListNode(attributes.stream()
                .map(YtUtilTest::stringNode)
                .collect(Collectors.toList())));
        params.put("stock", YtUtilTest.stringNode(stock));
        params.put("supplier_id", YtUtilTest.longNode(supplierId));
        params.put("supply_type", YtUtilTest.stringNode(supplyType));
        return YtUtilTest.treeMapNode(params);
    }

    void setUp(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        ytCluster = new YtCluster(HAHN, hahn);
    }

    @Test
    @DbUnitDataSet(
            before = "CteResuppliesYtImportServiceTest.before.csv",
            after = "CteResuppliesYtImportServiceTest.after.csv"
    )
    void testImport() {
        setUp(List.of(
                buildCteResupplyMapNode(
                        1,
                        "sku1",
                        "91",
                        "2020-07-09 16:45:46.047172",
                        12345,
                        List.of("PACKAGE_HOLES",
                                "PACKAGE_JAMS"),
                        "OK",
                        1,
                        "REFUND"
                ),
                buildCteResupplyMapNode(
                        2,
                        "sku1",
                        "92",
                        "2020-07-09 16:45:46.047172",
                        12345,
                        List.of("PACKAGE_HOLES",
                                "PACKAGE_JAMS",
                                "PACKAGE_SCRATCHES"),
                        "DAMAGE",
                        1,
                        null
                ),
                buildCteResupplyMapNode(
                        2,
                        "sku1",
                        "123123",
                        "2020-07-09 16:45:46.047172",
                        12345,
                        List.of("DEFORMED",
                                "PACKAGE_HOLES",
                                "PACKAGE_JAMS"),
                        "DAMAGE",
                        1,
                        "UNPAID"
                )
        ));

        cteResuppliesYtImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "CteResuppliesYtImportServiceTest.duplicateSku.before.csv",
            after = "CteResuppliesYtImportServiceTest.duplicateSku.after.csv"
    )
    void testImportDuplicateInSupplierOrder() {
        setUp(List.of(
                buildCteResupplyMapNode(
                        2,
                        "sku1",
                        "91",
                        "2020-07-09 16:45:47.047172",
                        12345,
                        List.of("PACKAGE_HOLES",
                                "PACKAGE_JAMS",
                                "PACKAGE_SCRATCHES"),
                        "DAMAGE",
                        1,
                        "REFUND"
                ),
                buildCteResupplyMapNode(
                        3,
                        "sku1",
                        "91",
                        "2020-07-09 16:45:48.047172",
                        12345,
                        List.of("PACKAGE_HOLES",
                                "PACKAGE_JAMS",
                                "PACKAGE_SCRATCHES"),
                        "DAMAGE",
                        1,
                        "REFUND"
                )
        ));

        cteResuppliesYtImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "CteResuppliesYtImportServiceTest.incorrectData.before.csv",
            after = "CteResuppliesYtImportServiceTest.incorrectData.after.csv"
    )
    void testImportIncorrectData() {
        setUp(List.of(
                buildCteResupplyMapNode(
                        2,
                        "sku1",
                        null,
                        "2020-07-09 16:45:47.047172",
                        12345,
                        List.of("PACKAGE_HOLES",
                                "PACKAGE_JAMS",
                                "PACKAGE_SCRATCHES"),
                        "DAMAGE",
                        1,
                        "REFUND"
                ),
                buildCteResupplyMapNode(
                        3,
                        "sku1",
                        "91",
                        "2020-07-09 16:45:48.047172",
                        12345,
                        List.of("PACKAGE_HOLES",
                                "PACKAGE_JAMS",
                                "PACKAGE_SCRATCHES"),
                        "DAMAGE",
                        1,
                        "REFUND"
                )
        ));

        cteResuppliesYtImportService.runAtCluster(ytCluster);
    }
}
