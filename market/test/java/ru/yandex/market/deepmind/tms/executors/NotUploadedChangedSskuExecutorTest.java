package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository.UpdateVersionTsStats;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

@SuppressWarnings("checkstyle:MagicNumber")
public class NotUploadedChangedSskuExecutorTest extends DeepmindBaseDbTestClass {
    public static final int MINUTES_TO_WAIT = 3;

    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Autowired
    private ChangedSskuRepository changedSskuRepository;
    @Autowired
    private DbMonitoring deepmindDbMonitoring;
    @Autowired
    private PartnerRelationRepository partnerRelationRepository;

    private NotUploadedChangedSskuExecutor executor;

    @Before
    public void setUp() {
        List<Supplier> suppliers = YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml");
        deepmindSupplierRepository.save(suppliers);
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));

        partnerRelationRepository.save(
            suppliers.stream()
                .map(s -> new PartnerRelation()
                    .setSupplierId(s.getId())
                    .setRelationType(s.getId() % 2 == 0 ? PartnerRelationType.CROSSDOCK : PartnerRelationType.DROPSHIP)
                    .setFromWarehouseIds(100L))
                .collect(Collectors.toList())
        );

        executor = new NotUploadedChangedSskuExecutor(
            changedSskuRepository,
            deepmindDbMonitoring.getOrCreateUnit("unit"),
            MINUTES_TO_WAIT
        );
    }

    @Test
    public void testRunNow() {
        changedSskuRepository.updateVersionTs(List.of(new ServiceOfferKey(60, "sku4")),
            UpdateVersionTsStats.builder().build());

        run(Instant.now());
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testRunAfter1Min() {
        changedSskuRepository.updateVersionTs(List.of(new ServiceOfferKey(60, "sku4")),
            UpdateVersionTsStats.builder().build());

        run(Instant.now().plus(1, ChronoUnit.MINUTES));
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    public void run(Instant checkTime) {
        try {
            executor.setClock(Clock.fixed(checkTime, ZoneOffset.UTC));
            executor.notUploadedAvailabilities();
        } finally {
            executor.setClock(Clock.systemDefaultZone());
        }
    }
}
