package ru.yandex.market.logistics.management.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerDeliveryIntervalSnapshot;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerDeliveryIntervalSnapshotStatus;
import ru.yandex.market.logistics.management.domain.entity.type.SnapshotCreationMethod;

@DatabaseSetup("/data/repository/partner_delivery_interval_snapshots.xml")
class PartnerDeliveryIntervalSnapshotRepositoryTest extends AbstractContextualTest {

    @Autowired
    private PartnerDeliveryIntervalSnapshotRepository partnerDeliveryIntervalSnapshotRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Test
    void testSnapshotCreation() {
        Partner partner = partnerRepository.findById(1L).get();
        PartnerDeliveryIntervalSnapshot snapshot = new PartnerDeliveryIntervalSnapshot()
            .setId(1L)
            .setPartner(partner)
            .setStatus(PartnerDeliveryIntervalSnapshotStatus.NEW)
            .setCreationMethod(SnapshotCreationMethod.MANUAL);

        partnerDeliveryIntervalSnapshotRepository.save(snapshot);

        softly.assertThat(partnerDeliveryIntervalSnapshotRepository.findAll().size()).isEqualTo(2);
    }

    @Transactional
    @Test
    void testSnapshotRead() {
        PartnerDeliveryIntervalSnapshot snapshot =
            partnerDeliveryIntervalSnapshotRepository.getOne(123L);

        softly.assertThat(snapshot).isNotNull();
        softly.assertThat(snapshot.getPartner().getId()).isEqualTo(2);
    }
}
