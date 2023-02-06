package ru.yandex.market.delivery.transport_manager.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

class TransportationPartnerInfoServiceTest extends AbstractContextualTest {
    @Autowired
    private TransportationPartnerInfoService service;

    @Test
    @DatabaseSetup({
        "/repository/metadata/transportation_different_partners.xml",
        "/repository/metadata/multiple_partner_info.xml"
    })
    void get() {
        softly.assertThat(service.get(List.of(1L), 2L))
            .containsExactlyInAnyOrder(
                new TransportationPartnerInfo()
                    .setPartnerId(2L)
                    .setPartnerType(PartnerType.DELIVERY)
                    .setPartnerName("DELIVERY 2")
                    .setTransportationId(1L)
            );
    }
}
