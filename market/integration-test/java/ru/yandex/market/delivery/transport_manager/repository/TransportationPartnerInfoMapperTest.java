package ru.yandex.market.delivery.transport_manager.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.PartnerKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerExtendedInfoRelated;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerInfoMapper;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.hamcrest.MatcherAssert.assertThat;

public class TransportationPartnerInfoMapperTest extends AbstractContextualTest {

    private static final long TRANSPORTATION_ID = 1L;

    @Autowired
    private TransportationPartnerInfoMapper transportationPartnerInfoMapper;

    @Test
    @DatabaseSetup({"/repository/metadata/transportation.xml",
        "/repository/metadata/transportation_partner_info_metadata.xml"})
    void getTest() {
        TransportationPartnerInfo transportationPartnerInfo =
            transportationPartnerInfoMapper.get(1L, 2L);
        TransportationPartnerInfo expectedInfo =
            new TransportationPartnerInfo()
                .setTransportationId(TRANSPORTATION_ID)
                .setPartnerId(2L)
                .setPartnerName("abc")
                .setPartnerType(PartnerType.DELIVERY);

        assertThatModelEquals(expectedInfo, transportationPartnerInfo);
    }

    @Test
    @DatabaseSetup("/repository/transportation/multiple_transportation_partner_info.xml")
    void getMultiple() {
        PartnerKey key1 = new PartnerKey(4L, 4L);
        PartnerKey key2 = new PartnerKey(6L, 666L);

        Map<PartnerKey, TransportationPartnerInfo> actual =
            transportationPartnerInfoMapper.get(Set.of(key1, key2));

        softly.assertThat(actual.keySet().size()).isEqualTo(2);

        assertThat(actual, IsMapContaining.hasEntry(
            key1,
            new TransportationPartnerInfo()
                .setPartnerId(4L)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setPartnerName("CDT")
                .setTransportationId(4L)
        ));
        assertThat(actual, IsMapContaining.hasEntry(
            key2,
            new TransportationPartnerInfo()
                .setPartnerId(666L)
                .setPartnerName("Партнер без типа")
                .setTransportationId(6L)
        ));
    }

    @Test
    @DatabaseSetup({
        "/repository/metadata/transportation_different_partners.xml",
        "/repository/metadata/multiple_partner_info.xml"
    })
    void getRelated() {
        List<TransportationPartnerExtendedInfoRelated> related = transportationPartnerInfoMapper.getRelated(Set.of(1L));
        softly.assertThat(related).containsExactlyInAnyOrder(
            getSingleRelatedInfo(123, "DELIVERY 1", "Партнер 123", "12345", true, false, false),
            getSingleRelatedInfo(2L, "DELIVERY 2", "Партнер 2", "23456", false, true, false),
            getSingleRelatedInfo(456L, "DELIVERY 3", "Партнер 456", "34567", false, false, true)
        );
    }

    @Test
    @DatabaseSetup({"/repository/metadata/transportation.xml", "/repository/metadata/same_partner_info.xml"})
    void getRelatedSamePartner() {
        List<TransportationPartnerExtendedInfoRelated> related = transportationPartnerInfoMapper.getRelated(Set.of(1L));
        softly.assertThat(related).containsExactlyInAnyOrder(
            getSingleRelatedInfo(1, "DELIVERY 1", "Партнер 1", "12345", true, false, false),
            getSingleRelatedInfo(2, "DELIVERY 2", "Партнер 2", "23456", false, true, true)
        );
    }

    @Test
    @DatabaseSetup({"/repository/metadata/transportation.xml", "/repository/metadata/same_partner_info.xml"})
    void getRelated0() {
        List<TransportationPartnerExtendedInfoRelated> related = transportationPartnerInfoMapper.getRelated(Set.of());
        softly.assertThat(related).isEmpty();
    }

    @Test
    @DatabaseSetup({"/repository/metadata/transportation.xml", "/repository/metadata/same_partner_info.xml"})
    void getRelatedNull() {
        List<TransportationPartnerExtendedInfoRelated> related = transportationPartnerInfoMapper.getRelated(null);
        softly.assertThat(related).isEmpty();
    }

    @Test
    @DatabaseSetup({"/repository/metadata/transportation.xml", "/repository/metadata/same_partner_info.xml"})
    void getRelatedMissing() {
        List<TransportationPartnerExtendedInfoRelated> related =
            transportationPartnerInfoMapper.getRelated(List.of(1000000L));
        softly.assertThat(related).isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/metadata/transportation_different_partners.xml",
        "/repository/metadata/multiple_partner_info.xml"
    })
    void getByTransportations() {
        softly
            .assertThat(transportationPartnerInfoMapper.getByTransportations(List.of(1L)))
            .containsExactlyInAnyOrder(

                new TransportationPartnerInfo()
                    .setTransportationId(1L)
                    .setPartnerId(123L)
                    .setPartnerName("DELIVERY 1")
                    .setPartnerType(PartnerType.DELIVERY),
                new TransportationPartnerInfo()
                    .setTransportationId(1L)
                    .setPartnerId(2L)
                    .setPartnerName("DELIVERY 2")
                    .setPartnerType(PartnerType.DELIVERY),

                new TransportationPartnerInfo()
                    .setTransportationId(1L)
                    .setPartnerId(456L)
                    .setPartnerName("DELIVERY 3")
                    .setPartnerType(PartnerType.DELIVERY)
            );
    }

    @Test
    @DatabaseSetup({
        "/repository/metadata/transportation_different_partners.xml",
        "/repository/metadata/multiple_partner_info.xml"
    })
    void getByTransportations0() {
        softly
            .assertThat(transportationPartnerInfoMapper.getByTransportations(List.of()))
            .isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/metadata/transportation_different_partners.xml",
        "/repository/metadata/multiple_partner_info.xml"
    })
    void getByTransportationsNull() {
        softly
            .assertThat(transportationPartnerInfoMapper.getByTransportations(null))
            .isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/repository/metadata/transportation_different_partners.xml",
        "/repository/metadata/multiple_partner_info.xml"
    })
    void getByTransportationsMissing() {
        softly
            .assertThat(transportationPartnerInfoMapper.getByTransportations(List.of(100L, 10000L)))
            .isEmpty();
    }

    @Nonnull
    private TransportationPartnerExtendedInfoRelated getSingleRelatedInfo(
        long partnerId,
        String name,
        String legalName,
        String inn,
        boolean isOutb,
        boolean isInb,
        boolean isMov
    ) {
        return new TransportationPartnerExtendedInfoRelated()
            .setOutbound(isOutb)
            .setMovement(isMov)
            .setInbound(isInb)
            .setMarketId(partnerId)
            .setInn(inn)
            .setOgrn(inn)
            .setLegalName(legalName)
            .setLegalType("ООО")
            .setLegalAddress("Адрес")
            .setPartnerInfo(
                new TransportationPartnerInfo(
                    partnerId,
                    TRANSPORTATION_ID,
                    name,
                    PartnerType.DELIVERY
                )
            );
    }

    private TransportationUnit getUnit(Long id, Long partnerId, TransportationUnitType type) {
        return new TransportationUnit()
            .setId(id)
            .setPartnerId(partnerId)
            .setType(type);
    }
}
