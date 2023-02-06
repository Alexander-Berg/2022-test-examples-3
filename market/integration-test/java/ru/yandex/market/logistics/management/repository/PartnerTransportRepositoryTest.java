package ru.yandex.market.logistics.management.repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.filter.InternalPartnerTransportFilter;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerTransport;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.specification.PartnerTransportSpecification;

public class PartnerTransportRepositoryTest extends AbstractContextualTest {

    @Autowired
    private PartnerTransportRepository partnerTransportRepository;

    @Autowired
    private PartnerTransportSpecification specification;

    @Test
    @DatabaseSetup("/data/repository/partner/before/partner_transport.xml")
    @ExpectedDatabase(
        value = "/data/repository/partner/after/partner_transport.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void saveEntity() {
        var tenPalletsFiveHours = createTransport();
        partnerTransportRepository.save(tenPalletsFiveHours);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/after/partner_transport.xml")
    void updateEntityTs() {
        var tenPalletsFiveHours = createTransport();
        tenPalletsFiveHours.setPalletCount(5);
        partnerTransportRepository.save(tenPalletsFiveHours);
        var tenPalletsFiveHoursUpdated = partnerTransportRepository.findById(1L).orElse(new PartnerTransport());
        softly.assertThat(tenPalletsFiveHoursUpdated.getUpdated()).isNotEqualTo(tenPalletsFiveHours.getUpdated());
        softly.assertThat(tenPalletsFiveHoursUpdated.getPalletCount()).isEqualTo(5);
    }

    @Test
    @DatabaseSetup("/data/repository/partner/after/partner_transport.xml")
    void getEntity() {
        var tenPalletsFiveHours = partnerTransportRepository.findById(1L).orElse(new PartnerTransport());
        softly.assertThat(tenPalletsFiveHours).isEqualTo(createTransport());
    }

    @Test
    @DatabaseSetup("/data/repository/partner/after/partner_transport.xml")
    void getByFilter() {
        var tenPalletsFiveHours = partnerTransportRepository.findAll(specification.fromFilter(
            InternalPartnerTransportFilter.builder()
                .logisticPointFrom(1L)
                .logisticPointTo(2L)
                .build()
        ));
        softly.assertThat(tenPalletsFiveHours).isEqualTo(List.of(createTransport()));
    }

    @Test
    @DatabaseSetup("/data/repository/partner/after/partner_transport.xml")
    void getByFilterWithIds() {
        var tenPalletsFiveHours = partnerTransportRepository.findAll(specification.fromFilter(
            InternalPartnerTransportFilter.builder()
                .ids(Set.of(1L))
                .build()
        ));
        softly.assertThat(tenPalletsFiveHours).isEqualTo(List.of(createTransport()));
    }

    @Test
    @DatabaseSetup("/data/repository/partner/after/partner_transport.xml")
    void getByIrrelevantFilter() {
        var nodata = partnerTransportRepository.findAll(specification.fromFilter(
            InternalPartnerTransportFilter.builder()
                .logisticPointFrom(3L)
                .logisticPointTo(2L)
                .build()
        ));
        softly.assertThat(nodata).isEqualTo(List.of());
    }

    private PartnerTransport createTransport() {
        return new PartnerTransport()
            .setId(1L)
            .setPartner(new Partner().setId(1L).setPartnerType(PartnerType.DELIVERY).setStatus(PartnerStatus.ACTIVE))
            .setLogisticsPointFrom(new LogisticsPoint().setId(1L)
                .setExternalId("warehouse1")
                .setAddress(new Address().setAddressString("some address")))
            .setLogisticsPointTo(new LogisticsPoint().setId(2L)
                .setExternalId("warehouse2")
                .setAddress(new Address().setAddressString("some address")))
            .setDuration(Duration.ofHours(5))
            .setPrice(100000L)
            .setPalletCount(10);
    }
}
