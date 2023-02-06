package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Resource;

import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.db.monitoring.DbMonitoringUnit;
import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.task_queue.events.ShopSkuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.ShopSkuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.tms.config.DeepmindYtConfig;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtColumnSchema;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.market.yt.util.table.model.YtTableSchema;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.CHANGED_SSKU;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.INCONSISTENT_AVAILABILITY_SHOP_SKU;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_AVAILABILITIES_LAST_PROCESSED_SSKU_KEY;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_AVAILABILITIES_LAST_START_TIME;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_AVAILABILITIES_SLEEP_SEC;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_AVAILABILITIES_VALID_INCONSISTENT_ITEMS_LIMIT;
import static ru.yandex.market.deepmind.common.utils.TestUtils.createOffer;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;
import static ru.yandex.market.deepmind.tms.executors.UploadOfferAvailabilityToYtExecutorTest.createYtRow;
import static ru.yandex.market.deepmind.tms.executors.UploadOfferHidingsToLogbrokerExecutor.BATCH_SIZE;

/**
 * @author kravchenko-aa
 * @date 12.10.2020
 */
public class UploadOfferAvailabilityToYtCheckExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    public static final int BERU_ID = 465852;

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource(name = "serviceOfferReplicaRepository")
    private ServiceOfferReplicaRepository serviceOfferRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private StorageKeyValueServiceImpl storageKeyValueService;
    @Resource
    private ShopSkuAvailabilityChangedHandler handler;
    @Resource
    private DbMonitoring monitoring;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private UploadOfferAvailabilityToYtCheckExecutor executor;
    private ShopSkuMatrixAvailabilityServiceMock shopSkuMatrixAvailabilityService;
    private UnversionedRowset rowsetMock = Mockito.mock(UnversionedRowset.class);
    private AvailableWarehouseService availableWarehouseService;

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        var offersConverter = new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository);
        shopSkuMatrixAvailabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        availableWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT);
        executor = new UploadOfferAvailabilityToYtCheckExecutor(
            serviceOfferRepository,
            shopSkuMatrixAvailabilityService,
            storageKeyValueService,
                availableWarehouseService,
            offersConverter,
            null,
            Mockito.mock(YtTableModel.class),
            null,
            monitoring,
            handler,
            changedSskuRepository
        );

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
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(clientMock.selectRows(any(SelectRowsRequest.class))).thenReturn(futureMock);
        Mockito.when(futureMock.join()).thenReturn(rowsetMock);
        Mockito.when(rowsetMock.getRows()).thenReturn(Collections.emptyList());
        executor.setRpcApi(rpcApi);

        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_VALID_INCONSISTENT_ITEMS_LIMIT, 1);
        storageKeyValueService.invalidateCache();
    }

    @After
    public void tearDown() {
        executor.setBatchSize(BATCH_SIZE);
        // MBO-27956
        DbMonitoringUnit unit = monitoring.getOrCreateUnit(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        unit.ok();

        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_VALID_INCONSISTENT_ITEMS_LIMIT, null);
        storageKeyValueService.invalidateCache();
    }

    private YtTableSchema getAvailabilitySchema() {
        //  хак, чтобы не тянуть yt конфигурацию в unit тесты
        return new DeepmindYtConfig(null, null).availabilityTableModel().getSchema();
    }

    @Test
    public void testFixWrongAvailabilities() {
        Mockito.when(rowsetMock.getRows())
            .thenReturn(List.of(
                // Considering it as full table
                createYtRow(77, "sku-1", SOFINO_ID),
                // 001234.sku-2 -> supplier_id = 1002, shop_sku = 'sku-2'
                createYtRow(BERU_ID, "001234.sku-2", TOMILINO_ID),
                createYtRow(BERU_ID, "001234.sku-2", ROSTOV_ID)
            ))
            .thenReturn(List.of(
                // Returning only sku-2 and sku-3 rows on recheck
                //  because sku-1 should be considered as consistent
                createYtRow(BERU_ID, "001234.sku-2", TOMILINO_ID),
                createYtRow(BERU_ID, "001234.sku-2", ROSTOV_ID)
            ));

        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);
        clearQueue();

        var msku = TestUtils.newMsku(1L, -1L);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null, null);

        // sku-1 present in both sources
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, mskuAvailability
        );
        // sku-2 blocks only TOMILINO in DB and there will be ROSTOV inconsistent block in YT
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), TOMILINO_ID,
            mskuAvailability
        );
        // sku-3 not present in YT
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID,
            MatrixAvailabilityUtils.offerDelisted(1002, "sku-3")
        );

        executor.execute();

        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        AssertionsForInterfaceTypes.assertThat(tasks)
            .usingElementComparatorOnFields("shopSkuKeys")
            .containsExactlyInAnyOrder(
                new ShopSkuAvailabilityChangedTask().setShopSkuKeys(Set.of(
                    new ServiceOfferKey(1002, "sku-2"),
                    new ServiceOfferKey(1002, "sku-3")
                ))
            );

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("INCONSISTENT_AVAILABILITY_SHOP_SKU: " +
            "Some availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(1002,'sku-2',147),(1002,'sku-3',171)");
    }

    @Test
    public void testStartAfterFailFromTheSamePlace() {
        // пусть в yt ничего нет, в базе лежит ключ (1002, sku-3)
        // Джоба должна запуститься с этого оффера
        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4);
        clearQueue();
        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);

        Instant start = Instant.now();
        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_LAST_PROCESSED_SSKU_KEY,
            new ServiceOfferKey(1002, "sku-3"));
        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_LAST_START_TIME, start);

        executor.execute();

        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        AssertionsForInterfaceTypes.assertThat(tasks)
            .usingElementComparatorOnFields("shopSkuKeys")
            .containsExactlyInAnyOrder(
                new ShopSkuAvailabilityChangedTask().setShopSkuKeys(Set.of(
                    new ServiceOfferKey(1002, "sku-4")
                ))
            );

        assertThat(storageKeyValueService.getValue(UPLOAD_AVAILABILITIES_LAST_PROCESSED_SSKU_KEY,
            ServiceOfferKey.class)).isNull();
        assertThat(storageKeyValueService.getInstant(UPLOAD_AVAILABILITIES_LAST_START_TIME, null)).isEqualTo(start);
    }

    @Test
    public void testUpdateMonitoringBetweenBatches() {
        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        var offer5 = createOffer(1002, "sku-5", 400L);
        var offer6 = createOffer(1002, "sku-6", 400L);
        var offer7 = createOffer(1002, "sku-7", 400L);
        var offer8 = createOffer(1002, "sku-8", 400L);
        var offer9 = createOffer(1002, "sku-9", 400L);
        var offer10 = createOffer(1002, "sku-10", 400L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4, offer5,
            offer6, offer7, offer8, offer9, offer10);
        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-5"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-6"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-7"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-8"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-9"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-10"), SOFINO_ID, delisted);

        executor.setBatchSize(2);
        executor.execute();

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("INCONSISTENT_AVAILABILITY_SHOP_SKU: " +
            "Some availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(77,'sku-1',172),(1002,'sku-10',172)...+8");
    }

    @Test
    public void testIgnoreRecentlyUpdatedVersionTs() {
        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);
        MatrixAvailability block = MatrixAvailabilityUtils.offerDelisted(1, "");
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, block);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), SOFINO_ID, block);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, block);

        // обновляем version_ts через sql, так как через репозиторий не получится
        dsl.newRecord(CHANGED_SSKU, new ChangedSsku(1L, 1002, "sku-2", Instant.now().minusSeconds(10),
            null, null, null, null, null, null, false)
        ).insert();
        dsl.newRecord(CHANGED_SSKU, new ChangedSsku(2L, 1002, "sku-3", Instant.now().minusSeconds(100),
            null, null, null, null, null, null, false)
        ).insert();

        executor.execute();

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("INCONSISTENT_AVAILABILITY_SHOP_SKU: " +
            "Some availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(77,'sku-1',172),(1002,'sku-3',171)");
    }

    @Test
    public void testWaitUntilConcurrentWritesHasFinished() {
        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);
        MatrixAvailability block = MatrixAvailabilityUtils.offerDelisted(1, "");
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, block);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), SOFINO_ID, block);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, block);

        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_SLEEP_SEC, 1);

        Mockito.when(rowsetMock.getRows())
            .thenReturn(List.of())// в первый раз возвращаем пустое множество
            .thenReturn(List.of(// во второй раз возвращаем запись, как будто данные появились в YT
                // 465852 -- beruId
                // 001234 (real_supplier_id) -> 1002 (1P supplier)
                createYtRow(BERU_ID, "001234.sku-2", SOFINO_ID)
            ));

        executor.execute();

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("INCONSISTENT_AVAILABILITY_SHOP_SKU: " +
            "Some availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(77,'sku-1',172),(1002,'sku-3',171)");
    }

    @Test
    public void testClearOldMonitoringBeforeStart() {
        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), SOFINO_ID, delisted);

        executor.execute();
        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);

        // Мониторинг не изменится так как джоба не запуститься сразу
        executor.execute();
        result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);

        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_LAST_START_TIME, Instant.now().minus(8, HOURS));
        executor.setBatchSize(0); // хак чтобы новый репроцессинг не запустился и опять не насчитал горящий мониторинг
        executor.execute();
        result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testOkLessThanLimit() {
        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_VALID_INCONSISTENT_ITEMS_LIMIT, 1000);

        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4);

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        var msku =  TestUtils.newMsku(1L, -1L);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null, null);

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), TOMILINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);

        executor.execute();

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);

        var unit = monitoring.getOrCreateUnit(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(unit.getMessage()).isEqualTo("Less than 1000" +
            " availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(77,'sku-1',172),(1002,'sku-2',171)...+2");
    }

    @Test
    public void testWarningMoreThanLimitWithBatches() {
        storageKeyValueService.putValue(UPLOAD_AVAILABILITIES_VALID_INCONSISTENT_ITEMS_LIMIT, 3);
        executor.setBatchSize(2);

        var offer1 = createOffer(77, "sku-1", 100L);
        var offer2 = createOffer(1002, "sku-2", 200L);
        var offer3 = createOffer(1002, "sku-3", 300L);
        var offer4 = createOffer(1002, "sku-4", 400L);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4);

        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        var msku =  TestUtils.newMsku(1, -1);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, msku, TOMILINO,
            null, LocalDate.parse("2020-02-26"), null, null);

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"), SOFINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-2"), TOMILINO_ID, mskuAvailability);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-3"), TOMILINO_ID, delisted);
        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1002, "sku-4"), TOMILINO_ID, delisted);

        executor.execute();

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("INCONSISTENT_AVAILABILITY_SHOP_SKU: " +
            "Some availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(77,'sku-1',172),(1002,'sku-2',171)...+2");
    }

    @Test
    public void disablingWarehouseLeadsToConsideringYtRowsAsInconsistent() {
        //arrange
        var disabledWarehouse = new Warehouse(
            -125L,
            "Disabled warehouse",
            WarehouseType.FULFILLMENT,
            new Long[]{},
            Instant.now(),
            null,
            null
        );
        deepmindWarehouseRepository.save(disabledWarehouse);

        Mockito.when(rowsetMock.getRows())
            .thenReturn(List.of(
                createYtRow(77, "sku-1", disabledWarehouse.getId()),
                createYtRow(1002, "sku-2", disabledWarehouse.getId())
            ));

        var offer = createOffer(77, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer);
        clearQueue();

        var availability = MatrixAvailabilityUtils.mskuInWarehouse(
            false,
            TestUtils.newMsku(1L, -1L),
            disabledWarehouse,
            null,
            LocalDate.parse("2020-02-26"),
            null,
            null
        );

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"),
            disabledWarehouse.getId(),
            availability
        );

        //act
        executor.execute();

        //assert
        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        AssertionsForInterfaceTypes.assertThat(tasks)
            .usingElementComparatorOnFields("shopSkuKeys")
            .containsExactlyInAnyOrder(
                new ShopSkuAvailabilityChangedTask().setShopSkuKeys(Set.of(
                    new ServiceOfferKey(77, "sku-1"),
                    new ServiceOfferKey(1002, "sku-2")
                ))
            );

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo("INCONSISTENT_AVAILABILITY_SHOP_SKU: " +
            "Some availabilities are in inconsistent state in yt (supplier_id, shop_sku, warehouse_id): " +
            "(77,'sku-1',-125),(1002,'sku-2',-125)");
    }

    @Test
    public void explicitPermitsDoesNotConsideredAsInconsistent() {
        //arrange
        var offer = createOffer(77, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer);
        clearQueue();

        var availability = MatrixAvailabilityUtils.mskuInWarehouse(
            true,
            TestUtils.newMsku(1L, -1L),
            TOMILINO,
            null,
            LocalDate.parse("2020-02-26"),
            null,
            null
        );

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(77, "sku-1"),
            TOMILINO_ID,
            availability
        );

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of());

        //act
        executor.execute();

        //assert
        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        AssertionsForInterfaceTypes.assertThat(tasks).isEmpty();

        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void executorIsBeingDisabledByKey() {
        //arrange
        var offer = createOffer(1, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer);
        clearQueue();

        var availability = MatrixAvailabilityUtils.mskuInWarehouse(
            true,
            TestUtils.newMsku(1L, -1L),
            TOMILINO,
            null,
            LocalDate.parse("2020-02-26"),
            null,
            null
        );

        shopSkuMatrixAvailabilityService.addAvailability(
            serviceOfferRepository.findOfferByKey(1, "sku-1"),
            TOMILINO_ID,
            availability
        );

        Mockito.when(rowsetMock.getRows()).thenReturn(List.of());

        storageKeyValueService.putValue(executor.getDisableKey(), true);

        //act
        executor.execute();

        //assert that no sskus in being added
        List<ShopSkuAvailabilityChangedTask> tasks = getQueueTasksOfType(ShopSkuAvailabilityChangedTask.class);
        AssertionsForInterfaceTypes.assertThat(tasks).isEmpty();

        //assert that monitoring is fine
        ComplexMonitoring.Result result = monitoring.getOrFetchResult(INCONSISTENT_AVAILABILITY_SHOP_SKU);
        assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }
}
