package ru.yandex.market.core.fulfillment.billing.storage.dao;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.billing.storage.model.ReportStorageBillingBilledAmount;
import ru.yandex.market.core.fulfillment.model.FulfillmentOperationType;
import ru.yandex.market.core.yt.YtTablesMockUtils;
import ru.yandex.market.mbi.yt.YtUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasDaysOfPaidStorage;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasDaysOnStock;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasDaysToPay;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasOperationType;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasShopSku;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasStartTimestamp;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasSupplierId;
import static ru.yandex.market.core.fulfillment.billing.storage.dao.matchers.ReportStorageBillingBilledAmountMatchers.hasSupplyId;

/**
 * {@link StorageBillingBilledAmountYtDao}
 */
public class StorageBillingBilledAmountYtDaoTest extends FunctionalTest {

    private static final String PATH = "//home/some/random/path";

    private static final LocalDate REPORT_DATE = LocalDate.of(2020, 1, 16);
    private static final long SUPPLIER_ID = 465209L;

    private Yt ytMock;
    private YtTables ytTablesMock;
    private Cypress cypressMock;

    private StorageBillingBilledAmountYtDao storageBillingBilledAmountYtDao;

    @BeforeEach
    void setUp() {
        ytMock = mock(Yt.class);
        cypressMock = mock(Cypress.class);
        ytTablesMock = mock(YtTables.class);
        when(ytMock.tables()).thenReturn(ytTablesMock);
        when(ytMock.cypress()).thenReturn(cypressMock);

        storageBillingBilledAmountYtDao = new StorageBillingBilledAmountYtDao(
                () -> ytMock,
                PATH
        );
    }

    @Test
    void testGetReportDataNoTable() {
        when(cypressMock.exists(any(YPath.class))).thenReturn(false);
        List<ReportStorageBillingBilledAmount> actual =
                storageBillingBilledAmountYtDao.getReportWithAmount(SUPPLIER_ID, REPORT_DATE);
        MatcherAssert.assertThat(actual, Matchers.empty());
    }

    @Test
    void testGetReportData() throws IOException {
        List<YTreeMapNode> yTreeNodes = parseYtJsonData("yt_json/reports.stocks_by_supply.json");
        mockReadTable(REPORT_DATE, yTreeNodes);

        when(cypressMock.exists(any(YPath.class))).thenReturn(true);

        List<ReportStorageBillingBilledAmount> actual =
                storageBillingBilledAmountYtDao.getReportWithAmount(SUPPLIER_ID, REPORT_DATE);

        MatcherAssert.assertThat(actual, Matchers.contains(
                Matchers.allOf(
                        hasSupplierId(SUPPLIER_ID),
                        hasOperationType(FulfillmentOperationType.INVENTORY),
                        hasSupplyId(73928),
                        hasShopSku("136618"),
                        hasStartTimestamp(LocalDate.of(2019, 7, 1))
                ),
                Matchers.allOf(
                        hasSupplierId(SUPPLIER_ID),
                        hasOperationType(null),
                        hasSupplyId(2839),
                        hasShopSku("171380"),
                        hasStartTimestamp(LocalDate.of(2018, 11, 1))
                )
        ));
    }

    @Test
    void testGetNewReportData() throws IOException {
        final LocalDate oldReportDate = LocalDate.of(2022, 5, 31);
        final LocalDate newReportDate = LocalDate.of(2022, 6, 1);
        List<YTreeMapNode> yTreeNodes = parseYtJsonData("yt_json/reports.stocks_by_supply.json");
        mockReadTable(newReportDate, yTreeNodes);
        mockReadTable(oldReportDate, yTreeNodes);

        when(cypressMock.exists(any(YPath.class))).thenReturn(true);

        List<ReportStorageBillingBilledAmount> actual =
                storageBillingBilledAmountYtDao.getReportWithAmount(SUPPLIER_ID, newReportDate);
        MatcherAssert.assertThat(actual, Matchers.contains(
                Matchers.allOf(
                        hasSupplierId(SUPPLIER_ID),
                        hasOperationType(null),
                        hasDaysToPay(0),
                        hasDaysOfPaidStorage(17),
                        hasDaysOnStock(456)
                )
        ));
    }

    private void mockReadTable(LocalDate reportDate, List<YTreeMapNode> yTreeNodes) {
        Mockito.doAnswer(invocation -> {
                    YPath tablePath = invocation.getArgument(0);

                    assertThat(tablePath.toString())
                            .contains(Long.toString(SUPPLIER_ID));

                    Consumer<YTreeMapNode> consumer = invocation.getArgument(2);

                    yTreeNodes.stream()
                            .filter(node -> node.getString("billing_timestamp").equals(reportDate.toString()))
                            .filter(node -> node.getLong("supplier_id") == SUPPLIER_ID)
                            .forEach(consumer);

                    return null;
                }).when(ytTablesMock)
                .read(
                        argThat(arg -> arg.toString().endsWith(PATH + "/" + reportDate)),
                        eq(YTableEntryTypes.YSON),
                        any(Consumer.class)
                );
    }

    @Test
    void testGetAllAvailableStocksBySupplyData() {
        Map<String, YTreeNode> files = Cf.hashMap();
        files.put("2021-08-31", YtTablesMockUtils.buildYtReportTableAttributes(true, 23));
        files.put("2021-06-18", YtTablesMockUtils.buildYtReportTableAttributes(true, 23));
        files.put("incorrect table", YtTablesMockUtils.buildYtReportTableAttributes(true, 100));
        files.put("2021-09-10", YtTablesMockUtils.buildYtReportTableAttributes(false, 1000));
        files.put("2021-10-10", YtTablesMockUtils.buildYtReportTableAttributes(true, null));
        files.put("2021-10-04", YtTablesMockUtils.buildYtReportTableAttributes(null, null));
        files.put("2021-07-10", YtTablesMockUtils.buildYtReportTableAttributes(null, 500));
        files.put("2021-07-15", YtTablesMockUtils.buildYtReportTableAttributes(true, 0));

        YTreeMapNode entries = YtUtil.treeMapNode(files);
        when(cypressMock.get(any(), any())).thenReturn(entries);

        List<LocalDate> result = storageBillingBilledAmountYtDao.findAllAvailableReportDates();
        Assertions.assertThat(result)
                .isSorted()
                .containsExactly(
                        LocalDate.parse("2021-06-18"),
                        LocalDate.parse("2021-08-31")
                );
    }

    private List<YTreeMapNode> parseYtJsonData(String filePath) throws IOException {
        return Arrays.stream(
                new String(
                        getClass()
                                .getResourceAsStream(filePath)
                                .readAllBytes()
                ).split("\n")
        ).map(json -> {
            YTreeBuilder yTreeBuilder = YTree.mapBuilder();

            JSONObject jsonObject = new JSONObject(json);
            jsonObject.keySet()
                    .forEach(key -> yTreeBuilder
                            .key(key)
                            .value(jsonObject.get(key))
                    );

            return yTreeBuilder.buildMap();
        }).collect(Collectors.toList());
    }
}
