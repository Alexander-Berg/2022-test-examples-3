package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.tms.executors.CountSskuMskuStatusesExecutor.MskuStatusStat;
import ru.yandex.market.deepmind.tms.executors.CountSskuMskuStatusesExecutor.SskuStatusStat;

public class CountSskuMskuStatusesExecutorTest extends DeepmindBaseDbTestClass {

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    protected EnhancedRandom random;

    private CountSskuMskuStatusesExecutor countSskuMskuStatusesExecutor;


    @Before
    public void setUp() {
        countSskuMskuStatusesExecutor = new CountSskuMskuStatusesExecutor(
            jdbcTemplate,
            Mockito.mock(DeepmindSolomonPushService.class)
        );
        random = new EnhancedRandomBuilder().seed(1).build();
    }

    @Test
    public void testSskuStatusCount() {
        insertMskuStatus(1111, MskuStatusValue.REGULAR);
        insertOffer(1, "sku1", SupplierType.FIRST_PARTY);
        insertOffer(2, "sku2", SupplierType.FIRST_PARTY);
        insertOffer(3, "sku3", SupplierType.FIRST_PARTY);
        insertOffer(4, "sku4", SupplierType.FIRST_PARTY);
        insertOffer(5, "sku5", SupplierType.FIRST_PARTY);
        insertOffer(6, "sku6", SupplierType.FIRST_PARTY);
        insertOffer(7, "sku7", SupplierType.THIRD_PARTY);
        insertOffer(8, "sku8", SupplierType.THIRD_PARTY);
        insertOffer(9, "sku9", SupplierType.THIRD_PARTY);
        insertOffer(10, "sku10", SupplierType.REAL_SUPPLIER);
        insertOffer(11, "sku11", SupplierType.REAL_SUPPLIER);
        insertOffer(12, "sku12", SupplierType.REAL_SUPPLIER);

        sskuStatusRepository.save(
            sskuStatus(1, "sku1", OfferAvailability.ACTIVE),
            sskuStatus(2, "sku2", OfferAvailability.ACTIVE),
            sskuStatus(3, "sku3", OfferAvailability.ACTIVE),
            sskuStatus(4, "sku4", OfferAvailability.INACTIVE),
            sskuStatus(5, "sku5", OfferAvailability.INACTIVE),
            sskuStatus(6, "sku6", OfferAvailability.DELISTED),
            sskuStatus(7, "sku7", OfferAvailability.ACTIVE),
            sskuStatus(8, "sku8", OfferAvailability.ACTIVE),
            sskuStatus(9, "sku9", OfferAvailability.ACTIVE),
            sskuStatus(10, "sku10", OfferAvailability.ACTIVE),
            sskuStatus(11, "sku11", OfferAvailability.ACTIVE),
            sskuStatus(12, "sku12", OfferAvailability.ACTIVE)
        );

        var result = countSskuMskuStatusesExecutor.countSskuStatuses();

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            new SskuStatusStat().setStatus("ACTIVE").setSupplierType("FIRST_PARTY").setCount(3),
            new SskuStatusStat().setStatus("INACTIVE").setSupplierType("FIRST_PARTY").setCount(2),
            new SskuStatusStat().setStatus("DELISTED").setSupplierType("FIRST_PARTY").setCount(1),
            new SskuStatusStat().setStatus("ACTIVE").setSupplierType("THIRD_PARTY").setCount(3),
            new SskuStatusStat().setStatus("ACTIVE").setSupplierType("REAL_SUPPLIER").setCount(3)
        );
    }

    @Test
    public void testMskuStatusCount() {
        insertMskuStatus(1, MskuStatusValue.NPD);
        insertMskuStatus(2, MskuStatusValue.REGULAR);
        insertMskuStatus(3, MskuStatusValue.REGULAR);
        insertMskuStatus(4, MskuStatusValue.IN_OUT);
        insertMskuStatus(5, MskuStatusValue.IN_OUT);
        insertMskuStatus(6, MskuStatusValue.IN_OUT);
        insertMskuStatus(7, MskuStatusValue.SEASONAL);
        insertMskuStatus(8, MskuStatusValue.SEASONAL);
        insertMskuStatus(9, MskuStatusValue.SEASONAL);
        insertMskuStatus(10, MskuStatusValue.SEASONAL);
        insertMskuStatus(11, MskuStatusValue.END_OF_LIFE);
        insertMskuStatus(12, MskuStatusValue.END_OF_LIFE);
        insertMskuStatus(13, MskuStatusValue.END_OF_LIFE);
        insertMskuStatus(14, MskuStatusValue.END_OF_LIFE);
        insertMskuStatus(15, MskuStatusValue.END_OF_LIFE);
        insertMskuStatus(16, MskuStatusValue.ARCHIVE);
        insertMskuStatus(17, MskuStatusValue.ARCHIVE);
        insertMskuStatus(18, MskuStatusValue.ARCHIVE);
        insertMskuStatus(19, MskuStatusValue.ARCHIVE);
        insertMskuStatus(20, MskuStatusValue.ARCHIVE);
        insertMskuStatus(21, MskuStatusValue.ARCHIVE);

        var result = countSskuMskuStatusesExecutor.countMskuStatuses();

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            new MskuStatusStat().setStatus("NPD").setCount(1),
            new MskuStatusStat().setStatus("REGULAR").setCount(2),
            new MskuStatusStat().setStatus("IN_OUT").setCount(3),
            new MskuStatusStat().setStatus("SEASONAL").setCount(4),
            new MskuStatusStat().setStatus("END_OF_LIFE").setCount(5),
            new MskuStatusStat().setStatus("ARCHIVE").setCount(6)
        );
    }

    private static SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability offerAvailability) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(offerAvailability);
    }

    protected void insertMskuStatus(long mskuId, MskuStatusValue status) {
        var msku = TestUtils.newMsku(mskuId);
        var mskuStatus = TestUtils.randomMskuStatus(random)
            .setMarketSkuId(mskuId)
            .setMskuStatus(status)
            .setSeasonId(null);
        if (status == MskuStatusValue.SEASONAL) {
            var season = seasonRepository.save(new Season().setName("test" + mskuId));
            mskuStatus.setSeasonId(season.getId());
        }
        deepmindMskuRepository.save(msku);
        mskuStatusRepository.save(mskuStatus);
    }

    private void insertOffer(int supplierId, String shopSku, SupplierType supplierType) {
        var supplier = new Supplier().setId(supplierId)
            .setName("test_supplier_" + supplierId)
            .setSupplierType(supplierType);
        if (supplierType == SupplierType.REAL_SUPPLIER) {
            supplier.setRealSupplierId("00004" + supplierId);
        }
        var offer = new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(1111L)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        if (suppliers.isEmpty()) {
            deepmindSupplierRepository.save(supplier);
        }
        serviceOfferReplicaRepository.save(offer);
    }

}
