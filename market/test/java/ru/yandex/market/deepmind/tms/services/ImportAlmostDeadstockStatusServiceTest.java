package ru.yandex.market.deepmind.tms.services;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.stubs.YtTablesStub;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AlmostDeadstockStatus;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;
import ru.yandex.market.mboc.common.config.YtAndYqlJdbcAutoCluster;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;

/**
 * @author gornvx
 * @date 19.10.2021
 */
public class ImportAlmostDeadstockStatusServiceTest extends DeepmindBaseDbTestClass {
    @Resource(name = "deepmindDsl")
    private DSLContext dslContext;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_TEMPLATE)
    private JdbcOperations yqlJdbcTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;

    private YPath almostDeadStockTable;
    private YPath mbocOffers;
    private ImportAlmostDeadstockStatusService statusService;
    private YPath tmpTable;

    @Before
    public void setUp() {
        YqlOverPgUtils.setTransformYqlToSql(true);
        almostDeadStockTable = YPath.simple("//tmp/almostdeadstock/20200101_0101");
        mbocOffers = YPath.simple("//tmp/extended_offer/20200101_0101");

        yqlJdbcTemplate.execute("" +
            "create table " + almostDeadStockTable + " (\n" +
            "  supplier_id                 int,\n" +
            "  shop_sku                    text,\n" +
            "  almost_deadstock_since      text,\n" +
            "  warehouse_id                bigint " +
            ");");

        yqlJdbcTemplate.execute(
            "create table " + mbocOffers + " (\n" +
                "  supplier_id          int,\n" +
                "  shop_sku             text,\n" +
                "  raw_supplier_id      int,\n" +
                "  raw_shop_sku         text,\n " +
                "  supplier_type        mbo_category.supplier_type " +
                ");");

        //mock ytAndJdbcCluster
        Yt ytMock = Mockito.mock(Yt.class);
        var yqlMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(yqlMock.getJdbcOperations()).thenReturn(yqlJdbcTemplate);

        statusService = new ImportAlmostDeadstockStatusService(
            namedParameterJdbcTemplate,
            YtAndYqlJdbcAutoCluster.createMock(ytMock, yqlMock),
            TransactionHelper.MOCK,
            almostDeadStockTable,
            mbocOffers,
            "unit-test"
        );
        var format = "//tmp/deepmind/importalmostdeadstockstatusservice/test";
        statusService.setTmpTableFormat(format);
        tmpTable = YPath.simple(format);

        yqlJdbcTemplate.execute("" +
            "create table " + tmpTable + " (\n" +
            "  shop_sku      text,\n" +
            "  supplier_id      bigint,\n" +
            "  warehouse_id        bigint,\n" +
            "  almost_deadstock_since    text" +
            ");");

        var ytTablesStub = Mockito.mock(YtTablesStub.class);
        Mockito.when(ytMock.tables()).thenReturn(ytTablesStub);
        Mockito.doAnswer(invocation -> {
            var consumer = (Consumer<YTreeMapNode>) invocation.getArgument(2);
            yqlJdbcTemplate.query(
                "SELECT shop_sku,\n" +
                    "     supplier_id,\n" +
                    "     warehouse_id,\n" +
                    "     almost_deadstock_since " +
                    "FROM " + tmpTable,
                rs -> {
                    var node = YTree.mapBuilder()
                        .key("shop_sku").value(rs.getString("shop_sku"))
                        .key("supplier_id").value(rs.getInt("supplier_id"))
                        .key("warehouse_id").value(rs.getLong("warehouse_id"))
                        .key("almost_deadstock_since").value(rs.getString("almost_deadstock_since"))
                        .buildMap();
                    consumer.accept(node);
                }
            );
            return null;
        }).when(ytTablesStub).read(Mockito.eq(tmpTable), Mockito.any(), Mockito.any(Consumer.class));

        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
    }

    @After
    public void tearDown() {
        YqlOverPgUtils.setTransformYqlToSql(false);
    }

    @Test
    public void importShouldCreateUpdateDeleteStatuses() {
        AlmostDeadstockStatus status1 = status("sku_1", 1, ROSTOV_ID, LocalDate.now().minusDays(2));
        AlmostDeadstockStatus status2 = status("sku_2", 1, ROSTOV_ID, LocalDate.now().minusDays(2));
        AlmostDeadstockStatus status3 = status("sku_3", 2, SOFINO_ID, LocalDate.now().minusDays(2));
        dslContext.newRecord(Tables.ALMOST_DEADSTOCK_STATUS, status1).insert();
        dslContext.newRecord(Tables.ALMOST_DEADSTOCK_STATUS, status2).insert();

        status1.setAlmostDeadstockSince(LocalDate.now().minusDays(1));
        insertAlmostDeadStockStatus(status1, status3);

        // status1 - update since date; status2 - remove from pg; status3 - add to pg
        statusService.importAlmostDeadstockStatus();

        List<AlmostDeadstockStatus> statuses =
            dslContext.selectFrom(Tables.ALMOST_DEADSTOCK_STATUS).fetchInto(AlmostDeadstockStatus.class);
        assertThat(statuses)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(status1, status3);
    }

    @Test
    public void importShouldContains1PSupplierStatuses() {
        AlmostDeadstockStatus status1 = status("sku_1", 1, ROSTOV_ID, LocalDate.now().minusDays(2));
        AlmostDeadstockStatus status2 = status("sku_2", 77, ROSTOV_ID, LocalDate.now().minusDays(2));
        AlmostDeadstockStatus status3 = status("sku_3", 77, SOFINO_ID, LocalDate.now().minusDays(2));

        insertAlmostDeadStockStatus(status1, status2, status3);

        statusService.importAlmostDeadstockStatus();

        List<AlmostDeadstockStatus> statuses =
            dslContext.selectFrom(Tables.ALMOST_DEADSTOCK_STATUS).fetchInto(AlmostDeadstockStatus.class);
        assertThat(statuses)
            .usingElementComparatorIgnoringFields("id")
            .contains(status1, status2, status3);
    }

    private void insertAlmostDeadStockStatus(AlmostDeadstockStatus... statuses) {
        for (AlmostDeadstockStatus status : statuses) {
            insertAlmostDeadstockStatus(status.getSupplierId(), status.getSupplierId(), status.getShopSku(),
                status.getShopSku(), status.getAlmostDeadstockSince(), status.getWarehouseId());
        }
    }

    private AlmostDeadstockStatus status(String shopSku, int supplierId, long warehouse, LocalDate date) {
        return new AlmostDeadstockStatus()
            .setShopSku(shopSku)
            .setSupplierId(supplierId)
            .setWarehouseId(warehouse)
            .setAlmostDeadstockSince(date);
    }

    private void insertAlmostDeadstockStatus(int supplierId,
                                             int rawSupplierId,
                                             String shopSku,
                                             String rawShopSku,
                                             LocalDate almostDeadstockSince,
                                             long warehouseId) {
        var supplier = deepmindSupplierRepository.findById(supplierId).orElseThrow();
        if (supplier.getSupplierType() == SupplierType.REAL_SUPPLIER) {
            yqlJdbcTemplate.update(
                "insert into `" + almostDeadStockTable + "` " +
                    " (supplier_id, shop_sku, almost_deadstock_since, warehouse_id) " +
                    " values (?, ?, ?, ?) ",
                supplierId, shopSku, almostDeadstockSince.toString(), warehouseId);
            yqlJdbcTemplate.update(
                "insert into `" + mbocOffers + "` " +
                    " (supplier_id, raw_supplier_id, shop_sku, raw_shop_sku, supplier_type) " +
                    " values (?, ?, ?, ?, ?::mbo_category.supplier_type) ",
                supplierId, rawSupplierId, shopSku, rawShopSku, supplier.getSupplierType().name());
        } else {
            yqlJdbcTemplate.update(
                "insert into `" + almostDeadStockTable + "` " +
                    " (supplier_id, shop_sku, almost_deadstock_since, warehouse_id) " +
                    " values (?, ?, ?, ?) ",
                465852, supplier.getRealSupplierId() + "." + shopSku, almostDeadstockSince.toString(),
                warehouseId);
            yqlJdbcTemplate.update(
                "insert into `" + mbocOffers + "` " +
                    " (supplier_id, raw_supplier_id, shop_sku, raw_shop_sku, supplier_type) " +
                    " values (?, ?, ?, ?, ?::mbo_category.supplier_type) ",
                465852, rawSupplierId, supplier.getRealSupplierId() + "." + shopSku, rawShopSku,
                supplier.getSupplierType().name());
        }
    }
}
