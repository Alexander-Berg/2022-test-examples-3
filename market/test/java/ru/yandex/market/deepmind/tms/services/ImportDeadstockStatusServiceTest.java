package ru.yandex.market.deepmind.tms.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.stubs.YtTablesStub;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.DeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.DeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.DeepmindConstants;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;
import ru.yandex.market.deepmind.tms.executors.AutoDeadstockRobot;
import ru.yandex.market.mboc.common.config.YtAndYqlJdbcAutoCluster;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.services.lifecycle.LifecycleGroupAndRunReducer.PACKING_MATERIALS_CATEGORY_ID;

/**
 * @author kravchenko-aa
 * @date 22.07.2020
 */
public class ImportDeadstockStatusServiceTest extends DeepmindBaseDbTestClass {
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_TEMPLATE)
    private JdbcOperations yqlJdbcTemplate;
    @Resource
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private MskuInfoRepository mskuInfoRepository;
    @Resource
    private DeadstockStatusRepository deadstockStatusRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private ImportDeadstockStatusService statusService;
    private AutoDeadstockRobot autoDeadstockRobot;

    private YPath deadStockTable;
    private YPath mbocOffers;
    private YPath tmpTable;

    @Before
    public void setUp() {
        YqlOverPgUtils.setTransformYqlToSql(true);
        deadStockTable = YPath.simple("//tmp/deadstock/20200101_0101");
        mbocOffers = YPath.simple("//tmp/extended_offer/20200101_0101");

        yqlJdbcTemplate.execute("" +
            "create table " + deadStockTable + " (\n" +
            "  supplier_id          int,\n" +
            "  shop_sku             text,\n" +
            "  deadstock_since      text,\n" +
            "  warehouse_id         bigint " +
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

        statusService = new ImportDeadstockStatusService(
            namedParameterJdbcTemplate,
            YtAndYqlJdbcAutoCluster.createMock(ytMock, yqlMock),
            TransactionHelper.MOCK,
            deadStockTable,
            mbocOffers,
            "unit-test"
        );
        var format = "//tmp/deepmind/importdeadstockstatusservice/test";
        statusService.setTmpTableFormat(format);
        tmpTable = YPath.simple(format);

        yqlJdbcTemplate.execute("" +
            "create table " + tmpTable + " (\n" +
            "  shop_sku      text,\n" +
            "  supplier_id      bigint,\n" +
            "  warehouse_id        bigint,\n" +
            "  deadstock_since    text" +
            ");");

        var ytTablesStub = Mockito.mock(YtTablesStub.class);
        Mockito.when(ytMock.tables()).thenReturn(ytTablesStub);
        Mockito.doAnswer(invocation -> {
            var consumer = (Consumer<YTreeMapNode>) invocation.getArgument(2);
            yqlJdbcTemplate.query(
                "SELECT shop_sku,\n" +
                    "     supplier_id,\n" +
                    "     warehouse_id,\n" +
                    "     deadstock_since\n" +
                    "FROM " + tmpTable,
                rs -> {
                    var node = YTree.mapBuilder()
                        .key("shop_sku").value(rs.getString("shop_sku"))
                        .key("supplier_id").value(rs.getInt("supplier_id"))
                        .key("warehouse_id").value(rs.getLong("warehouse_id"))
                        .key("deadstock_since").value(rs.getString("deadstock_since"))
                        .buildMap();
                    consumer.accept(node);
                }
            );
            return null;
        }).when(ytTablesStub).read(Mockito.eq(tmpTable), Mockito.any(), Mockito.any(Consumer.class));

        autoDeadstockRobot = new AutoDeadstockRobot(jdbcTemplate, sskuAvailabilityMatrixRepository);

        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            mskuOffer(1, "sku1"),
            mskuOffer(1, "sku2"),
            mskuOffer(2, "sku3"),
            mskuOffer(77, "sku1"),
            mskuOffer(77, "sku2"),
            mskuOffer(77, "sku3")
        );
    }

    @After
    public void tearDown() {
        YqlOverPgUtils.setTransformYqlToSql(false);
    }

    @Test
    public void importShouldCreateUpdateDeleteStatuses() {
        DeadstockStatus status1 = status("sku1", 1, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status2 = status("sku2", 1, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status3 = status("sku3", 2, SOFINO_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        status1 = deadstockStatusRepository.save(status1);
        status3 = deadstockStatusRepository.save(status3);

        status1.setDeadstockSince(LocalDate.now().minus(1, ChronoUnit.DAYS));
        insertDeadstockToYt(status1, status3);

        // status1 - update since date; status2 - remove from pg; status3 - add to pg
        statusService.importDeadstockStatus();

        Map<Long, DeadstockStatus> statuses = deadstockStatusRepository.findAllMap();
        Assertions.assertThat(statuses.values())
            .usingElementComparatorIgnoringFields("id", "importTs")
            .containsExactlyInAnyOrder(status1, status3);

        // отдельно проверяем, что import_ts тот, который нам нужен
        Assertions.assertThat(statuses.get(status1.getId()).getImportTs())
            .isAfter(status1.getImportTs());
        Assertions.assertThat(statuses.get(status3.getId()).getImportTs())
            .isEqualTo(status3.getImportTs());
    }

    @Test
    public void importShouldDeleteStatusesWithModifiedLoginNullOrMskuInfoRobot() {
        sskuAvailabilityMatrixRepository.save(
            new SskuAvailabilityMatrix()
                .setSupplierId(77)
                .setWarehouseId(ROSTOV_ID)
                .setShopSku("sku1")
                .setAvailable(false)
                .setCreatedLogin(DeepmindConstants.AUTO_DEADSTOCK_ROBOT),
            new SskuAvailabilityMatrix()
                .setSupplierId(77)
                .setWarehouseId(ROSTOV_ID)
                .setShopSku("sku2")
                .setAvailable(false)
                .setCreatedLogin(DeepmindConstants.AUTO_DEADSTOCK_ROBOT)
                .setModifiedLogin("auto-mskuinfo-robot")
        );

        DeadstockStatus status1 = status("sku1", 77, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status2 = status("sku2", 77, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        deadstockStatusRepository.save(status1, status2);

        statusService.importDeadstockStatus();
        Assertions.assertThat(deadstockStatusRepository.findAll()).isEmpty();

        // run robot
        autoDeadstockRobot.execute();
        Assertions.assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId", "available")
            .containsExactlyInAnyOrder(
                sskuMatrix(77, ROSTOV_ID, "sku1", null),
                sskuMatrix(77, ROSTOV_ID, "sku2", null)
            );
    }

    @Test
    public void importShouldContains1PSupplierStatuses() {
        DeadstockStatus status1 = status("sku1", 1, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status2 = status("sku2", 77, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status3 = status("sku3", 77, SOFINO_ID, Instant.now().minus(2, ChronoUnit.DAYS));

        insertDeadstockToYt(status1, status2, status3);

        statusService.importDeadstockStatus();

        List<DeadstockStatus> statuses = deadstockStatusRepository.findAll();
        Assertions.assertThat(statuses)
            .usingElementComparatorIgnoringFields("id", "importTs")
            .contains(status1, status2, status3);
    }

    @Test
    public void importShouldUpdateAvailabilities() {
        DeadstockStatus status1 = status("sku1", 77, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status2 = status("sku2", 77, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status3 = status("sku3", 2, SOFINO_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        insertDeadstockToYt(status1, status2, status3);

        statusService.importDeadstockStatus();
        Assertions.assertThat(deadstockStatusRepository.findAll()).isNotEmpty();

        autoDeadstockRobot.execute();

        List<SskuAvailabilityMatrix> matrices = sskuAvailabilityMatrixRepository.findAll();
        AssertionsForInterfaceTypes.assertThat(matrices)
            .usingElementComparatorOnFields("shopSku", "supplierId", "warehouseId", "available", "blockReasonKey")
            .containsExactlyInAnyOrder(
                new SskuAvailabilityMatrix()
                    .setSupplierId(77)
                    .setWarehouseId(ROSTOV_ID)
                    .setShopSku("sku1")
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK),
                new SskuAvailabilityMatrix()
                    .setSupplierId(77)
                    .setWarehouseId(ROSTOV_ID)
                    .setShopSku("sku2")
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK),
                new SskuAvailabilityMatrix()
                    .setSupplierId(2)
                    .setWarehouseId(SOFINO_ID)
                    .setShopSku("sku3")
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_DEADSTOCK)
            );
    }

    @Test
    public void importShouldIgnoreBusinessSuppliers() {
        DeadstockStatus status1 = status("sku1", 1, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus status2 = status("sku2", 1, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        DeadstockStatus statusOnBusinessSupplier =
            status("sku3", 200, ROSTOV_ID, Instant.now().minus(2, ChronoUnit.DAYS));

        insertDeadstockToYt(status1, status2);
        insertDeadstockToYt(statusOnBusinessSupplier);

        statusService.importDeadstockStatus();

        List<DeadstockStatus> statuses = deadstockStatusRepository.findAll();
        Assertions.assertThat(statuses)
            .usingElementComparatorIgnoringFields("id", "importTs")
            .containsExactlyInAnyOrder(status1, status2);
    }

    @Test
    public void importShouldIgnorePackingMaterialsCategoryId() {
        DeadstockStatus status1 = status("sku11", 77, SOFINO_ID, Instant.now().minus(2, ChronoUnit.DAYS));
        insertDeadstockToYt(status1);
        serviceOfferReplicaRepository.save(mskuOffer(77, "sku11").setCategoryId(PACKING_MATERIALS_CATEGORY_ID));
        mskuInfoRepository.save(new MskuInfo().setMarketSkuId(1L).setInTargetAssortment(false));
        statusService.importDeadstockStatus();

        autoDeadstockRobot.execute();

        List<DeadstockStatus> statuses = deadstockStatusRepository.findAll();
        Assertions.assertThat(statuses)
            .usingElementComparatorIgnoringFields("id", "importTs")
            .containsExactlyInAnyOrder(status1);

        List<SskuAvailabilityMatrix> matrices = sskuAvailabilityMatrixRepository.findAll();
        AssertionsForInterfaceTypes.assertThat(matrices).isEmpty();
    }

    private ServiceOfferReplica mskuOffer(
        int supplierId, String ssku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title")
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(SupplierType.REAL_SUPPLIER)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private DeadstockStatus status(String shopSku, int supplierId, long warehouse, Instant time) {
        return new DeadstockStatus()
            .setShopSku(shopSku)
            .setSupplierId(supplierId)
            .setWarehouseId(warehouse)
            .setDeadstockSince(time.atZone(ZoneId.systemDefault()).toLocalDate())
            .setImportTs(time);
    }

    private SskuAvailabilityMatrix sskuMatrix(int supplierId, long rostovId, String sku1, @Nullable Boolean available) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(supplierId)
            .setWarehouseId(rostovId)
            .setShopSku(sku1)
            .setAvailable(available);
    }

    private void insertDeadstockToYt(DeadstockStatus... statuses) {
        for (DeadstockStatus status : statuses) {
            ytInsertDeadstockStatus(status.getSupplierId(), status.getSupplierId(), status.getShopSku(),
                status.getShopSku(), status.getDeadstockSince(), status.getWarehouseId());
        }
    }

    private void ytInsertDeadstockStatus(int supplierId,
                                         int rawSupplierId,
                                         String shopSku,
                                         String rawShopSku,
                                         LocalDate deadstockSince,
                                         long warehouseId) {
        var supplier = deepmindSupplierRepository.findById(supplierId).orElseThrow();
        if (supplier.getSupplierType() == SupplierType.REAL_SUPPLIER) {
            yqlJdbcTemplate.update(
                "insert into `" + deadStockTable + "` " +
                    " (supplier_id, shop_sku, deadstock_since, warehouse_id) " +
                    " values (?, ?, ?, ?) ",
                supplierId, shopSku, deadstockSince.toString(), warehouseId);
            yqlJdbcTemplate.update(
                "insert into `" + mbocOffers + "` " +
                    " (supplier_id, raw_supplier_id, shop_sku, raw_shop_sku, supplier_type) " +
                    " values (?, ?, ?, ?, ?::mbo_category.supplier_type) ",
                supplierId, rawSupplierId, shopSku, rawShopSku, supplier.getSupplierType().name());
        } else {
            yqlJdbcTemplate.update(
                "insert into `" + deadStockTable + "` " +
                    " (supplier_id, shop_sku, deadstock_since, warehouse_id) " +
                    " values (?, ?, ?, ?) ",
                465852, supplier.getRealSupplierId() + "." + shopSku, deadstockSince.toString(),
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
