package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.dto.GenericPartnerId;
import ru.yandex.market.delivery.transport_manager.domain.dto.PartnerMarketKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.enums.PartnerIdType;

import static org.hamcrest.MatcherAssert.assertThat;

@DatabaseSetup({
    "/repository/transportation/all_kinds_of_transportation.xml",
    "/repository/transportation_legal_info/transportation_legal_info.xml",
})
public class TransportationLegalInfoMapperTest extends AbstractContextualTest {
    static final TransportationLegalInfo PARTNER_ID_LEGAL_INFO = new TransportationLegalInfo()
        .setId(1L)
        .setTransportationId(1L)
        .setPartnerId(5L)
        .setMarketId(7L)
        .setInn("55")
        .setOgrn("555")
        .setLegalName("5555")
        .setLegalType("OOO")
        .setUrl("http://url555.ru")
        .setLegalAddress("address");
    static final TransportationLegalInfo MARKET_ID_LEGAL_INFO = new TransportationLegalInfo()
        .setId(2L)
        .setTransportationId(1L)
        .setMarketId(7L)
        .setInn("77")
        .setOgrn("777")
        .setLegalName("7777")
        .setLegalType("OOO")
        .setUrl("http://url777.ru")
        .setLegalAddress("address");

    static final TransportationLegalInfo ANOTHER_LEGAL_INGO = new TransportationLegalInfo()
        .setId(3L)
        .setTransportationId(2L)
        .setPartnerId(10L)
        .setMarketId(7L)
        .setInn("66")
        .setOgrn("666")
        .setLegalName("6666")
        .setLegalType("OOO")
        .setUrl("http://url666.ru")
        .setLegalAddress("address");

    @Autowired
    private TransportationLegalInfoMapper mapper;

    @Test
    void testGetByTransportationAndPartnerId() {
        softly
            .assertThat(mapper.getByTransportationAndPartnerId(1L, 5L))
            .isEqualTo(PARTNER_ID_LEGAL_INFO);
    }

    @Test
    void testGetByTransportationAndMarketId() {
        softly
            .assertThat(mapper.getByTransportationAndMarketId(1L, 7L))
            .isEqualTo(MARKET_ID_LEGAL_INFO);
    }

    @Test
    void getNullId() {
        softly.assertThat(mapper.get(1L, null)).isNull();
    }

    @Test
    void testGetByLmsId() {
        softly
            .assertThat(mapper.get(1L, new GenericPartnerId(5L, PartnerIdType.LMS)))
            .isEqualTo(PARTNER_ID_LEGAL_INFO);
    }

    @Test
    void testGetByMarketId() {
        softly
            .assertThat(mapper.get(1L, new GenericPartnerId(7L, PartnerIdType.MARKET_ID)))
            .isEqualTo(MARKET_ID_LEGAL_INFO);
    }

    @Test
    void getByTransportationIds() {
        softly
            .assertThat(mapper.getByTransportationIds(List.of(1L)))
            .containsExactlyInAnyOrder(MARKET_ID_LEGAL_INFO, PARTNER_ID_LEGAL_INFO);
    }

    @Test
    void testGet() {
        Map<PartnerMarketKey, TransportationLegalInfo> legalInfoMap = mapper.get(Set.of(
            new PartnerMarketKey(1L, null, 7L),
            new PartnerMarketKey(2L, 10L, 7L)
            )
        );
        assertThat(legalInfoMap, Is.is(
            Map.of(
                new PartnerMarketKey(1L, null, 7L), MARKET_ID_LEGAL_INFO,
                new PartnerMarketKey(2L, 10L, 7L), ANOTHER_LEGAL_INGO
            )
        ));
    }
}
