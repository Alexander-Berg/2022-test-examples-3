package ru.yandex.market.logistics.management.service.client;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.DeliveryIntervalSnapshotRequest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.repository.DeliveryIntervalSnapshotRequestRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class DeliveryIntervalSnapshotRequestServiceTest extends AbstractContextualTest {

    @Autowired
    private DeliveryIntervalSnapshotRequestService service;

    @Autowired
    private DeliveryIntervalSnapshotRequestRepository repository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Test
    void testRequestIsCorrectlyCreated() {
        partnerRepository.save(new Partner()
            .setId(1L)
            .setStatus(PartnerStatus.ACTIVE)
            .setPartnerType(PartnerType.DELIVERY)
            .setName("Delivery")
        );
        service.createOrSetUpdateTime(1L);
        Optional<DeliveryIntervalSnapshotRequest> request = repository.findById(1L);
        softly.assertThat(request).isPresent();
        softly.assertThat(request.get().getUpdateTime()).isNotNull();
    }
}
