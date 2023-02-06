package ru.yandex.market.deepmind.tms.executors;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.db.monitoring.DbMonitoringUnit;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.BlockReason;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.ModifyRowsRequestMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindBlockReasonRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.OfferAvailabilityYtService;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.tms.config.DeepmindYtConfig;
import ru.yandex.market.deepmind.tms.utils.AvailabilityProtoConverter;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.http.MbocMatrixAvailability;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtColumnSchema;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.market.yt.util.table.model.YtTableSchema;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability.Reason;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.utils.TestUtils.createOffer;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.BOOLEAN;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.STRING;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.UINT64;

/**
 * @author kravchenko-aa
 * @date 18.09.2020
 */
public class UploadOfferAvailabilityToYtExecutorTest extends DeepmindBaseDbTestClass {
    public static final int BERU_ID = 465852;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    protected DeepmindBlockReasonRepository deepmindBlockReasonRepository;
    @Resource(name = "serviceOfferReplicaRepository")
    private ServiceOfferReplicaRepository serviceOfferRepository;
    @Resource
    protected ChangedSskuRepository changedSskuRepository;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private UploadOfferAvailabilityToYtExecutor executor;
    private ShopSkuMatrixAvailabilityServiceMock shopSkuMatrixAvailabilityService;
    private ModifyRowsRequestMock modifyRowsRequestMock;
    private AvailableWarehouseService availableWarehouseService;
    private UnversionedRowset rowsetMock = Mockito.mock(UnversionedRowset.class);
    private JdbcTemplate yqlMock = Mockito.mock(JdbcTemplate.class);
    private DbMonitoring dbMonitoring = Mockito.mock(DbMonitoring.class);
    private DbMonitoringUnit dbMonitoringUnit = Mockito.mock(DbMonitoringUnit.class);
    private OfferAvailabilityYtService offerAvailabilityYtServiceMock = Mockito.mock(OfferAvailabilityYtService.class);

    public static UnversionedRow createYtRow(long supplierId, String shopSku, long warehouseId) {
        return new UnversionedRow(
            List.of(
                new UnversionedValue(1, UINT64, false, supplierId),
                new UnversionedValue(2, STRING, false, shopSku.getBytes()),
                new UnversionedValue(3, UINT64, false, warehouseId),
                new UnversionedValue(4, UINT64, false, 0L),
                new UnversionedValue(5, STRING, false, "".getBytes()),
                new UnversionedValue(6, STRING, false, "".getBytes()),
                new UnversionedValue(7, UINT64, false, 0L),
                new UnversionedValue(8, BOOLEAN, false, false),
                new UnversionedValue(9, STRING, false, "".getBytes()),
                new UnversionedValue(10, STRING, false, "".getBytes()),
                new UnversionedValue(11, STRING, false, Instant.now().toString().getBytes())
            )
        );
    }

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        var offersConverter = new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository);
        shopSkuMatrixAvailabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        availableWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT);
        executor = new UploadOfferAvailabilityToYtExecutor(
            null,
            Mockito.mock(YtTableModel.class),
            null,
            deepmindStorageKeyValueService,
            shopSkuMatrixAvailabilityService,
            changedSskuRepository,
            deepmindBlockReasonRepository,
            deepmindWarehouseRepository,
            availableWarehouseService,
            offersConverter,
            offerAvailabilityYtServiceMock,
            Mockito.mock(DeepmindSolomonPushService.class),
            YPath.simple("//tmp"),
            yqlMock,
            "pool",
            dbMonitoring,
            TransactionHelper.MOCK
        );
        Mockito.when(dbMonitoringUnit.getStatus()).thenReturn(MonitoringStatus.OK);
        Mockito.when(dbMonitoring.getOrCreateUnit(anyString())).thenReturn(dbMonitoringUnit);

        modifyRowsRequestMock = new ModifyRowsRequestMock();
        YtTableRpcApi rpcApi = Mockito.mock(YtTableRpcApi.class);
        YtTableSchema availabilitySchema = getAvailabilitySchema();
        Mockito.when(rpcApi.getValue(any(UnversionedRow.class), anyString())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            YtColumnSchema column = availabilitySchema.getColumnSchema((String) args[1]);
            UnversionedRow row = (UnversionedRow) args[0];
            if (column == null) {
                return null;
            }

            return row.getValues().get(column.getIndex());
        });
        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        CompletableFuture futureMock = Mockito.mock(CompletableFuture.class);
        Mockito.when(rpcApi.createModifyRowRequest()).thenReturn(modifyRowsRequestMock);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(clientMock.selectRows(Mockito.any(SelectRowsRequest.class))).thenReturn(futureMock);
        Mockito.when(futureMock.join()).thenReturn(rowsetMock);
        Mockito.when(rowsetMock.getRows()).thenReturn(Collections.emptyList());
        executor.setRpcApi(rpcApi);
        String upperLimits = "{\"145\":9223372036854775807,\"147\":9223372036854775807,\"171\":9223372036854775807," +
            "\"172\":9223372036854775807,\"300\":9223372036854775807,\"301\":9223372036854775807," +
            "\"302\":9223372036854775807}";
        String lowerLimits = "{\"145\":0,\"147\":0,\"171\":0,\"172\":0,\"300\":0,\"301\":0,\"302\":0}";
        deepmindStorageKeyValueService.putValue("ssku_count_by_warehouse_upper_limits", upperLimits);
        deepmindStorageKeyValueService.putValue("ssku_count_by_warehouse_lower_limits", lowerLimits);
        deepmindStorageKeyValueService.putValue("ssku_count_by_warehouse", "{}");
    }

    @Test
    public void dryRun() {
        executor.execute();
        modifyRowsRequestMock.isEmpty();
        List<ChangedSsku> all = changedSskuRepository.findAll();
        Assertions.assertThat(all).isEmpty();
    }

    @Test
    public void simpleRun() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(1, "sku-1"), new ServiceOfferKey(2, "sku-2")),
            createStats()
        );

        Msku msku = new Msku().setId(1L).setTitle("msku " + 1L).setCategoryId(-1L).setVendorId(-1L);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1, "sku-1"), CROSSDOCK_SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(2, "sku-2"), TOMILINO_ID,
            MatrixAvailabilityUtils.offerDelisted(2, "sku-2"));

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            entry("raw_shop_sku", "sku-2"),
            entry("raw_supplier_id", 2),
            entry("warehouse_id", TOMILINO_ID)
        });
        assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
        assertThat(modifyRowsRequestMock.getUpdation()).isEmpty();
    }

    @Test
    public void sskuCountSavingTest() throws IOException {
        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(BERU_ID, "000042.sku-1", SOFINO_ID)
        ));

        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4);
        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(77, "sku-1"), new ServiceOfferKey(1002, "sku-2"),
                new ServiceOfferKey(1002, "sku-3"), new ServiceOfferKey(1002, "sku-4")),
            createStats()
        );

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        Msku msku = new Msku().setId(1L).setTitle("msku " + 1L).setCategoryId(-1L).setVendorId(-1L);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);

        assertThat(readSskuCountByWarehouse().get(TOMILINO_ID)).isNull();
        Mockito.when(yqlMock.query(anyString(), any(RowMapper.class))).thenReturn(new ArrayList<Pair<Long, Long>>());
        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        List<Map<String, ?>> updation = modifyRowsRequestMock.getUpdation();
        assertThat(updation).hasSize(1);
        assertThat(insertion).hasSize(2);
        var sskuCount = readSskuCountByWarehouse();
        assertThat(sskuCount.get(TOMILINO_ID)).isEqualTo(2);
        assertThat(sskuCount.get(SOFINO_ID)).isNull();

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(BERU_ID, "001234.sku-2", TOMILINO_ID)
        ));
        changedSskuRepository.updateVersionTs(List.of(new ServiceOfferKey(1002, "sku-2")), createStats());
        executor.execute();
        List<Map<String, ?>> deletion = modifyRowsRequestMock.getDeletion();
        assertThat(deletion).hasSize(1);
        assertThat(readSskuCountByWarehouse().get(TOMILINO_ID)).isEqualTo(1);
    }

    @Test
    public void sskuCountLimitsTest() throws IOException {
        deepmindStorageKeyValueService
            .putValue("ssku_count_by_warehouse_upper_limits", "{\"172\":9223372036,\"171\":1}");
        deepmindStorageKeyValueService.putValue("ssku_count_by_warehouse_lower_limits", "{\"172\":0,\"171\":0}");
        Mockito.doThrow(new RuntimeException("To main")).when(offerAvailabilityYtServiceMock)
            .changeOfferAvailabilityBackupYtLinkWithMonitoring(null, false);
        Mockito.doThrow(new RuntimeException("To Backup"))
            .when(offerAvailabilityYtServiceMock).changeOfferAvailabilityBackupYtLinkWithMonitoring(
            LocalDate.now().minus(1, ChronoUnit.DAYS).toString(), true);

        // upper limit check
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        serviceOfferReplicaRepository.save(offer3, offer4);
        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(1002, "sku-3"), new ServiceOfferKey(1002, "sku-4")), createStats()
        );

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);

        assertThat(readSskuCountByWarehouse().get(TOMILINO_ID)).isNull();
        Mockito.when(yqlMock.query(anyString(), any(RowMapper.class))).thenReturn(new ArrayList<Pair<Long, Long>>());
        assertThatThrownBy(() -> executor.execute())
            .hasMessage("To Backup");

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(2);
        var sskuCount = readSskuCountByWarehouse();
        assertThat(sskuCount.get(TOMILINO_ID)).isEqualTo(2);

        // auto return to main check
        Mockito.when(dbMonitoringUnit.getStatus()).thenReturn(MonitoringStatus.WARNING);
        // setting null to last calculation to make executor recalculation of current ssku count values
        deepmindStorageKeyValueService.putValue("last_ssku_count_by_warehouse_calculation", null);
        deepmindStorageKeyValueService
            .putValue("ssku_count_by_warehouse_upper_limits", "{\"172\":9223372036,\"171\":2}");
        assertThatThrownBy(() -> executor.execute())
            .hasMessage("To main");
        Mockito.when(dbMonitoringUnit.getStatus()).thenReturn(MonitoringStatus.OK);

        // lower limit check
        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(BERU_ID, "000042.sku-1", SOFINO_ID),
            createYtRow(BERU_ID, "001234.sku-2", SOFINO_ID)
        ));
        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(77, "sku-1"), new ServiceOfferKey(1002, "sku-2")), createStats());
        assertThatThrownBy(() -> executor.execute())
            .hasMessage("To Backup");

        List<Map<String, ?>> deletion = modifyRowsRequestMock.getDeletion();
        assertThat(deletion).hasSize(2);
        assertThat(readSskuCountByWarehouse().get(SOFINO_ID)).isEqualTo(-2);
    }

    private Map<Long, Long> readSskuCountByWarehouse() throws IOException {
        return OBJECT_MAPPER.readValue(
            deepmindStorageKeyValueService.getValue("ssku_count_by_warehouse", String.class),
            new TypeReference<Map<Long, Long>>() {
            });
    }

    @Test
    public void testDoubleUpload() {
        // first update and upload
        var offer1 = createOffer(1, "sku-1", 100L);
        var shopSkuKey = new ServiceOfferKey(1, "sku-1");
        serviceOfferReplicaRepository.save(offer1);
        changedSskuRepository.updateVersionTs(List.of(shopSkuKey), createStats());

        ChangedSsku ssku1 = changedSskuRepository.findByShopSkuKeys(shopSkuKey).get(0);
        executor.execute();

        ChangedSsku updated = changedSskuRepository.findByShopSkuKey(ssku1.getSupplierId(), ssku1.getShopSku());
        Assertions.assertThat(List.of(updated))
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "availabilityUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(shopSkuKey, ssku1.getVersionTs(), ssku1.getVersionTs())
            );

        // second update and upload
        changedSskuRepository.updateVersionTs(List.of(shopSkuKey), createStats());
        ssku1 = changedSskuRepository.findByShopSkuKeys(shopSkuKey).get(0);
        executor.execute();

        ChangedSsku updated2 = changedSskuRepository.findByShopSkuKey(ssku1.getSupplierId(), ssku1.getShopSku());
        Assertions.assertThat(List.of(updated2))
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "availabilityUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(shopSkuKey, ssku1.getVersionTs(), ssku1.getVersionTs())
            );

        // check, that timestamps are changed
        Assertions.assertThat(updated.getVersionTs()).isNotEqualTo(updated2.getVersionTs());
        Assertions.assertThat(updated.getAvailabilityUploadedVersionTs())
            .isNotEqualTo(updated2.getAvailabilityUploadedVersionTs());
    }

    @Test
    public void testInsertDeleteUpdate() {
        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(BERU_ID, "000042.sku-1", SOFINO_ID),
            createYtRow(BERU_ID, "000042.sku-1", TOMILINO_ID),
            createYtRow(BERU_ID, "001234.sku-2", TOMILINO_ID)
        ));

        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4);
        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(77, "sku-1"), new ServiceOfferKey(1002, "sku-2"),
                new ServiceOfferKey(1002, "sku-3"), new ServiceOfferKey(1002, "sku-4")),
            createStats()
        );
        Instant versionTs = changedSskuRepository.findAll().get(0).getVersionTs();

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        List<BlockReason> blockReasonList = deepmindBlockReasonRepository.findAll();
        MbocMatrixAvailability.WarehouseAvailabilityWithoutInterval delistedProto =
            AvailabilityProtoConverter.convert(TOMILINO_ID, blockReasonList, delisted);
        Msku msku = new Msku().setId(1L).setTitle("msku " + 1L).setCategoryId(-1L).setVendorId(-1L);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        List<Map<String, ?>> updation = modifyRowsRequestMock.getUpdation();
        List<Map<String, ?>> deletion = modifyRowsRequestMock.getDeletion();

        assertThat(updation).hasSize(1);
        assertThat(updation.get(0)).contains(new Map.Entry[]{
            entry("raw_shop_sku", "sku-1"),
            entry("raw_supplier_id", 77),
            entry("shop_sku", "000042.sku-1"),
            entry("supplier_id", BERU_ID),
            entry("warehouse_id", SOFINO_ID)
        });

        assertThat(deletion).containsExactlyInAnyOrder(
            Map.of(
                "shop_sku", "000042.sku-1",
                "supplier_id", BERU_ID,
                "warehouse_id", TOMILINO_ID),
            Map.of(
                "shop_sku", "001234.sku-2",
                "supplier_id", BERU_ID,
                "warehouse_id", TOMILINO_ID)
        );

        insertion.forEach(ins -> {
            if (ins.get("raw_shop_sku").equals("sku-4")) {
                assertThat(ins).contains(new Map.Entry[]{
                    entry("supplier_id", BERU_ID),
                    entry("shop_sku", "001234.sku-4"),
                    entry("warehouse_id", 171L),
                    entry("raw_supplier_id", 1002),
                    entry("raw_shop_sku", "sku-4"),
                    entry("erp_real_supplier_id", "001234"),
                    entry("market_sku_id", 400L),
                    entry("available", false),
                    entry("reasons", delistedProto.toByteArray()),
                    entry("reasons_str", "1. Оффер выведен из оборота;"),
                    entry("version_ts", versionTs.toString())
                });
            } else if (ins.get("raw_shop_sku").equals("sku-3")) {
                assertThat(ins).contains(new Map.Entry[]{
                    entry("available", false),
                    entry("market_sku_id", 300L),
                    entry("raw_shop_sku", "sku-3"),
                    entry("raw_supplier_id", 1002),
                    entry("reasons", delistedProto.toByteArray()),
                    entry("reasons_str", "1. Оффер выведен из оборота;"),
                    entry("shop_sku", "001234.sku-3"),
                    entry("supplier_id", BERU_ID),
                    entry("warehouse_id", 171L),
                    entry("erp_real_supplier_id", "001234"),
                    entry("version_ts", versionTs.toString())
                });
            } else {
                Assertions.fail("There are unexpected insertion " + ins);
            }
        });
    }

    @Test
    public void testReasonInfoFilling() {
        List<BlockReason> blockReasonList = deepmindBlockReasonRepository.findAll();

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        MbocMatrixAvailability.WarehouseAvailabilityWithoutInterval delistedProto =
            AvailabilityProtoConverter.convert(TOMILINO_ID, blockReasonList, delisted);
        assertThat(delistedProto.getAvailabilityReasonsList().get(0).getReasonInfo()).isNotNull();
        assertThat(delistedProto.getAvailabilityReasonsList().get(0).getReasonInfo().getName())
            .isEqualTo(blockReasonList.stream()
                .filter(reason -> reason.getReasonKey() == delisted.getBlockReasonKey())
                .findFirst().get().getReason());

        MatrixAvailability available = MatrixAvailabilityUtils.available(2, "ssku", 1L);
        MbocMatrixAvailability.WarehouseAvailabilityWithoutInterval availableProto =
            AvailabilityProtoConverter.convert(TOMILINO_ID, blockReasonList, available);
        assertThat(availableProto.getAvailabilityReasonsList().get(0).getReasonInfo())
            .isEqualTo(MbocMatrixAvailability.ReasonInfo.getDefaultInstance());
        assertThat(availableProto.getAvailabilityReasonsList().get(0).getReasonInfo().getName()).isEmpty();
    }

    @Test
    public void testSkipNotFulfilmentAndNotCrossdockSuppliers() {
        // set fulfillment & crossdock to false
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(60).orElseThrow()
            .setFulfillment(false).setCrossdock(false));
        // set only crossdock to true, other false
        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(1002).orElseThrow()
            .setCrossdock(true).setFulfillment(false).setDropship(false));

        var offer1 = createOffer(60, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(60, "sku-1", TOMILINO_ID),
            createYtRow(BERU_ID, "001234.sku-2", TOMILINO_ID)
        ));

        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(60, "sku-1"), new ServiceOfferKey(1002, "sku-2")), createStats());

        executor.execute();

        List<Map<String, ?>> deletion = modifyRowsRequestMock.getDeletion();

        assertThat(deletion).hasSize(2);
        assertThat(deletion)
            .containsExactlyInAnyOrder(
                Map.of(
                    "supplier_id", 60,
                    "shop_sku", "sku-1",
                    "warehouse_id", TOMILINO_ID
                ),
                Map.of(
                    "supplier_id", BERU_ID,
                    "shop_sku", "001234.sku-2",
                    "warehouse_id", TOMILINO_ID
                )
            );

        assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
        assertThat(modifyRowsRequestMock.getUpdation()).isEmpty();
    }

    @Test
    public void useForFulfillmentTest() {
        var offer1 = createOffer(60, "sku-1", 100L);
        var offer2 = createOffer(BERU_ID, "123123.sku-3", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(60, "sku-1", TOMILINO_ID),
            createYtRow(BERU_ID, "123123.sku-3", TOMILINO_ID)
        ));

        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(60).orElseThrow()
            .setFulfillment(true).setDropship(false).setCrossdock(false));
        changedSskuRepository.updateVersionTsBySupplier(60, createStats());

        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(BERU_ID).orElseThrow()
            .setFulfillment(false).setDropship(false).setCrossdock(false));
        changedSskuRepository.updateVersionTsBySupplier(BERU_ID, createStats());

        deepmindSupplierRepository.save(deepmindSupplierRepository.findById(1002).orElseThrow()
            .setRealSupplierId("123123").setFulfillment(false)
            .setDropship(false).setCrossdock(false).setSupplierType(SupplierType.REAL_SUPPLIER));
        changedSskuRepository.updateVersionTsBySupplier(1002, createStats());

        Msku msku = new Msku().setId(1L).setTitle("msku " + 1L).setCategoryId(-1L).setVendorId(-1L);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(60, "sku-1"), SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(BERU_ID, "123123.sku-3"), SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), SOFINO_ID, mskuAvailability);

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();

        assertThat(insertion).hasSize(2);
    }

    @Test
    public void rowsWithDisabledWarehouseAreBeingDeleted() {
        //arrange
        var disabledWarehouse = getOrCreateDisabledWarehouse();

        var offer = createOffer(1, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer);

        // disabled warehouse availability
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1, "sku-1"),
            disabledWarehouse.getId(),
            mskuInWarehouseDisabledAvailability(disabledWarehouse)
        );

        // active warehouse availability
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1, "sku-1"),
            TOMILINO_ID,
            mskuInWarehouseDisabledAvailability(TOMILINO)
        );

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of(
            createYtRow(1, "sku-1", disabledWarehouse.getId()),
            createYtRow(1, "sku-1", TOMILINO_ID)
        ));

        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(1, "sku-1")),
            createStats()
        );

        //act
        executor.execute();

        //assert that row with disabled warehouse is deleted
        var deletion = modifyRowsRequestMock.getDeletion();
        Assertions.assertThat(deletion).hasSize(1);
        assertThat(deletion.get(0)).contains(new Map.Entry[]{
            entry("supplier_id", 1),
            entry("shop_sku", "sku-1"),
            entry("warehouse_id", disabledWarehouse.getId())
        });

        var updation = modifyRowsRequestMock.getUpdation();
        Assertions.assertThat(updation).hasSize(1);
        assertThat(updation.get(0)).contains(new Map.Entry[]{
            entry("supplier_id", 1),
            entry("shop_sku", "sku-1"),
            entry("warehouse_id", TOMILINO_ID)
        });

        //assert that no rows was inserted
        assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
    }

    @Test
    public void explicitPermitsAreNotBeingSavedToYt() {
        //arrange
        var offer = createOffer(1, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer);

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1, "sku-1"),
            TOMILINO_ID,
            mskuInWarehouseAvailability(TOMILINO, true)
        );

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of());

        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(1, "sku-1")),
            createStats()
        );

        //act
        executor.execute();

        //assert that explicit availability are not being saved
        assertThat(modifyRowsRequestMock.getInsertion()).isEmpty();
    }

    @Test
    public void executorIsBeingDisabledByKey() {
        //arrange
        var offer = createOffer(1, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer);

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1, "sku-1"),
            TOMILINO_ID,
            mskuInWarehouseDisabledAvailability(TOMILINO)
        );

        changedSskuRepository.updateVersionTs(
            List.of(new ServiceOfferKey(1, "sku-1")),
            createStats()
        );

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of());

        deepmindStorageKeyValueService.putValue(executor.getDisableKey(), true);

        //act
        deepmindStorageKeyValueService.invalidateCache();
        executor.execute();

        //assert that explicit availability are not being saved
        assertThat(modifyRowsRequestMock.isEmpty())
            .isEqualTo(true);
    }

    private MatrixAvailability mskuInWarehouseDisabledAvailability(Warehouse warehouse) {
        return mskuInWarehouseAvailability(warehouse, false);
    }

    private MatrixAvailability mskuInWarehouseAvailability(Warehouse warehouse, boolean available) {
        return MatrixAvailabilityUtils.mskuInWarehouse(
            available,
            TestUtils.newMsku(1L, -1L),
            warehouse,
            null,
            LocalDate.parse("2020-02-26"),
            null,
            null
        );
    }

    private Warehouse getOrCreateDisabledWarehouse() {
        var warehouse = deepmindWarehouseRepository.findById(-1250L).orElse(null);
        if (warehouse == null) {
            warehouse = new Warehouse(
                -1250L,
                "Disabled warehouse",
                WarehouseType.FULFILLMENT,
                new Long[]{},
                Instant.now(),
                null,
                null
            );
            deepmindWarehouseRepository.save(warehouse);
            return warehouse;
        }
        return warehouse;
    }

    private ChangedSsku changedSsku(ServiceOfferKey key, Instant versionTs, Instant availabilityUploadedVersionTs) {
        return new ChangedSsku()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .setVersionTs(versionTs)
            .setAvailabilityUploadedVersionTs(availabilityUploadedVersionTs);
    }

    private ChangedSskuRepository.UpdateVersionTsStats createStats() {
        return ChangedSskuRepository.UpdateVersionTsStats.builder()
            .availabilityTs(java.time.Instant.now())
            .queueTs(java.time.Instant.now())
            .reason(Reason.MSKU)
            .build();
    }

    private YtTableSchema getAvailabilitySchema() {
        //  хак, чтобы не тянуть yt конфигурацию в unit тесты
        return new DeepmindYtConfig(null, null).availabilityTableModel().getSchema();
    }
}
