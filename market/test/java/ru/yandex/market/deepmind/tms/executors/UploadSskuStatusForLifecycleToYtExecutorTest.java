package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.mocks.BeruIdMock;
import ru.yandex.market.deepmind.common.mocks.ModifyRowsRequestMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusDeletedRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverterImpl;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.SecurityUtil;
import ru.yandex.market.yt.util.table.YtTableRpcApi;

import static org.assertj.core.api.Assertions.assertThat;

public class UploadSskuStatusForLifecycleToYtExecutorTest extends DeepmindBaseDbTestClass {

    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private SskuStatusDeletedRepository sskuStatusDeletedRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private DbMonitoring deepmindDbMonitoring;
    @Resource
    private JdbcTemplate jdbcTemplate;

    private ModifyRowsRequestMock modifyRowsRequestMock;

    private UploadSskuStatusForLifecycleToYtExecutor executor;
    private UploadSskuStatusForLifecycleToYtExecutor.MonitoringExecutor monitoringExecutor;

    @Before
    public void setUp() throws Exception {
        executor = new UploadSskuStatusForLifecycleToYtExecutor(
            sskuStatusRepository,
            sskuStatusDeletedRepository,
            new OffersConverterImpl(jdbcTemplate, new BeruIdMock(), deepmindSupplierRepository),
            jdbcTemplate,
            null,
            null,
            null,
            new StorageKeyValueServiceMock()
        );

        monitoringExecutor = new UploadSskuStatusForLifecycleToYtExecutor.MonitoringExecutor(
            deepmindDbMonitoring.getOrCreateUnit("unit"),
            sskuStatusRepository,
            sskuStatusDeletedRepository
        );

        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));

        modifyRowsRequestMock = new ModifyRowsRequestMock();
        var rpcApi = Mockito.mock(YtTableRpcApi.class);
        Mockito.when(rpcApi.createModifyRowRequest()).thenReturn(modifyRowsRequestMock);
        executor.setRpcApi(rpcApi);
    }

    @Test
    public void testUploadOne() {
        sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.ACTIVE));

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 42),
            Map.entry("shop_sku", "sku1"),
            Map.entry("availability", "ACTIVE")
        });

        assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
    }

    @Test
    public void testUpload1PStatus() {
        sskuStatusRepository.save(sskuStatus(77, "sku5", OfferAvailability.DELISTED));

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 465852),
            Map.entry("shop_sku", "000042.sku5"),
            Map.entry("availability", "DELISTED")
        });

        assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
    }

    @Test
    public void testUploadWhiteStatus() {
        sskuStatusRepository.save(sskuStatus(202, "sku200", OfferAvailability.ACTIVE));
        DeepmindAssertions.assertThat(serviceOfferReplicaRepository.findOfferByKey(202, "sku200"))
            .isNull();

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 202),
            Map.entry("shop_sku", "sku200"),
            Map.entry("availability", "ACTIVE")
        });

        assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
    }

    @Test
    public void testSecondUploadWontHappen() {
        sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.ACTIVE));

        executor.execute();

        var insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 42),
            Map.entry("shop_sku", "sku1"),
            Map.entry("availability", "ACTIVE")
        });

        // second call
        modifyRowsRequestMock.clear();
        executor.execute();

        insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).isEmpty();

        assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
    }

    @Test
    public void testSecondCallWillUploadIfDataHasChanged() {
        var ssku1 = sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));
        var ssku5 = sskuStatusRepository.save(sskuStatus(77, "sku5", OfferAvailability.DELISTED));

        executor.execute();

        var insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(2);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 465852),
            Map.entry("shop_sku", "000042.sku5"),
            Map.entry("availability", "DELISTED")
        });
        assertThat(insertion.get(1)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 42),
            Map.entry("shop_sku", "sku1"),
            Map.entry("availability", "INACTIVE")
        });

        // second call
        SecurityUtil.wrapWithLogin("user", () -> {
            sskuStatusRepository.save(ssku1.setAvailability(OfferAvailability.ACTIVE).setComment("Test comment"));
        });

        modifyRowsRequestMock.clear();

        executor.execute();

        insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("supplier_id", 42),
            Map.entry("shop_sku", "sku1"),
            Map.entry("availability", "ACTIVE")
        });
        assertThat((Map<String, Object>) insertion.get(0).get("params"))
            .contains(Map.entry("modified_login", "user"));

        assertThat(modifyRowsRequestMock.getDeletion()).isEmpty();
    }

    @Test
    public void testRunNow() {
        sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));

        executeWithNowTime(Instant.now());
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testRunAfter1Min() {
        sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));

        executeWithNowTime(Instant.now().plus(1, ChronoUnit.MINUTES));
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testRunAfter50Min() {
        sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));
        sskuStatusRepository.save(sskuStatus(77, "sku5", OfferAvailability.DELISTED));

        executeWithNowTime(Instant.now().plus(50, ChronoUnit.MINUTES));

        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getMessage())
            .contains("ssku_status not uploaded to yt in 45 mins (supplier_id, ssku): (42,'sku1'),(77,'sku5')");
    }

    @Test
    public void deletedStatusAreBeingDeleted() {
        //arrange
        var shopSkuKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(shopSkuKey);

        //act
        executor.execute();

        //assert that insert request is empty
        Assertions.assertThat(modifyRowsRequestMock.getInsertion())
            .isEmpty();

        //assert that delete request is correct
        Assertions.assertThat(modifyRowsRequestMock.getDeletion()).hasSize(1);
        var deleteRequest = (Map<String, Object>) modifyRowsRequestMock.getDeletion().get(0);
        Assertions.assertThat(deleteRequest)
            .contains(Map.entry("supplier_id", 1))
            .contains(Map.entry("shop_sku", "1"));
    }

    @Test
    public void deletingStatusesDoesNotLeadToUselessRequests() {
        //arrange
        var shopSkuKey = new ServiceOfferKey(1, "1");
        sskuStatusDeletedRepository.addByKeys(shopSkuKey);
        executor.execute();
        modifyRowsRequestMock.clear();

        //act
        executor.execute();

        //assert that requests is empty
        Assertions.assertThat(modifyRowsRequestMock.getInsertion())
            .isEmpty();
        Assertions.assertThat(modifyRowsRequestMock.getDeletion())
            .isEmpty();
    }

    @Test
    public void restoredStatusAreBeingReupload() {
        //arrange
        var status = sskuStatus(1, "1");
        sskuStatusDeletedRepository.add(status);
        sskuStatusRepository.save(status);

        //act
        executor.execute();

        //assert that insert request is correct
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).hasSize(1);
        var insertRequest = (Map<String, Object>) modifyRowsRequestMock.getInsertion().get(0);
        Assertions.assertThat(insertRequest)
            .contains(Map.entry("supplier_id", 1))
            .contains(Map.entry("shop_sku", "1"));

        //assert that delete request is empty
        Assertions.assertThat(modifyRowsRequestMock.getDeletion())
            .isEmpty();
    }

    @Test
    public void insertRequestIsNotLost() {
        //arrange
        var status = sskuStatus(1, "1");
        sskuStatusRepository.save(status);
        sskuStatusRepository.delete(status);
        sskuStatusRepository.save(status);

        //act
        executor.execute();

        //assert that all insert requests is present
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).hasSize(1);

        //assert that insert request is correct
        var insertRequest = (Map<String, Object>) modifyRowsRequestMock.getInsertion().get(0);
        Assertions.assertThat(insertRequest)
            .contains(Map.entry("supplier_id", 1))
            .contains(Map.entry("shop_sku", "1"));

        //assert that delete request is empty
        Assertions.assertThat(modifyRowsRequestMock.getDeletion())
            .isEmpty();
    }

    @Test
    public void insertRequestIsNotLostWithExecutionBetweenStatusModifications() {
        //arrange
        var status = sskuStatus(1, "1");
        sskuStatusRepository.save(status);

        executor.execute();

        sskuStatusRepository.delete(status);

        executor.execute();

        sskuStatusRepository.save(status);

        //act
        executor.execute();

        //assert that all insert requests is present
        Assertions.assertThat(modifyRowsRequestMock.getInsertion()).hasSize(2);

        //assert that first insert request is correct
        var insertRequest = (Map<String, Object>) modifyRowsRequestMock.getInsertion().get(0);
        Assertions.assertThat(insertRequest)
            .contains(Map.entry("supplier_id", 1))
            .contains(Map.entry("shop_sku", "1"));

        //assert that delete request is correct
        var deleteRequest = (Map<String, Object>) modifyRowsRequestMock.getDeletion().get(0);
        Assertions.assertThat(insertRequest)
            .contains(Map.entry("supplier_id", 1))
            .contains(Map.entry("shop_sku", "1"));

        //assert that second insert request is correct
        insertRequest = (Map<String, Object>) modifyRowsRequestMock.getInsertion().get(1);
        Assertions.assertThat(insertRequest)
            .contains(Map.entry("supplier_id", 1))
            .contains(Map.entry("shop_sku", "1"));
    }

    @Test
    public void notDeletedMonitoringDoesNotWarnAheadOfTime() {
        //arrange
        sskuStatusDeletedRepository.add(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));

        //act
        executeWithNowTime(Instant.now().plus(1, ChronoUnit.MINUTES));

        //assert
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void notDeletedMonitoringWarnInTime() {
        //arrange
        sskuStatusDeletedRepository.add(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));
        sskuStatusDeletedRepository.add(sskuStatus(77, "sku5", OfferAvailability.DELISTED));

        //act
        executeWithNowTime(Instant.now().plus(50, ChronoUnit.MINUTES));

        //assert
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getMessage())
            .contains("ssku_status not deleted from yt in 45 mins (supplier_id, ssku): (42,'sku1'),(77,'sku5')");
    }

    @Test
    public void twoWarningsAreDisplayedCorrectly() {
        //assert
        sskuStatusRepository.save(sskuStatus(42, "sku1", OfferAvailability.INACTIVE));
        sskuStatusRepository.save(sskuStatus(424, "sku1", OfferAvailability.INACTIVE));
        sskuStatusDeletedRepository.add(sskuStatus(77, "sku5", OfferAvailability.DELISTED));

        //act
        executeWithNowTime(Instant.now().plus(50, ChronoUnit.MINUTES));

        //assert
        assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.WARNING);
        assertThat(deepmindDbMonitoring.fetchTotalResult().getMessage())
            .contains(
                "ssku_status not synced to yt in 45 mins (supplier_id, ssku): (42,'sku1'),(424,'sku1'),(77,'sku5')"
            );
    }

    private void executeWithNowTime(Instant checkTime) {
        try {
            monitoringExecutor.setClock(Clock.fixed(checkTime, ZoneOffset.UTC));
            monitoringExecutor.execute();
        } finally {
            monitoringExecutor.setClock(Clock.systemDefaultZone());
        }
    }

    private SskuStatus sskuStatus(int supplierId, String shopSku) {
        return sskuStatus(supplierId, shopSku, OfferAvailability.ACTIVE);
    }

    private static SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability offerAvailability) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(offerAvailability);
    }
}
