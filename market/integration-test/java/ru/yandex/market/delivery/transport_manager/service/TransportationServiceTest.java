package ru.yandex.market.delivery.transport_manager.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.filter.InternalTransportationSearchFilter;
import ru.yandex.market.delivery.transport_manager.facade.transportation.TransportationFacade;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;

public class TransportationServiceTest extends AbstractContextualTest {

    private static final Long PARTNER_ID = 123L;

    @Autowired
    TransportationFacade transportationFacade;
    @Autowired
    TransportationService transportationService;

    @Autowired
    TransportationStatusService transportationStatusService;

    @Autowired
    TransportationMapper transportationMapper;

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml"
    })
    @ExpectedDatabase(
        value = "/repository/transportation/after/transportation_new_with_history.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void persist() {
        transportationService.persist(
            new Transportation()
                .setStatus(TransportationStatus.NEW)
                .setMovement(new Movement().setId(1L))
                .setInboundUnit(new TransportationUnit().setId(2L))
                .setOutboundUnit(new TransportationUnit().setId(1L))
                .setTransportationType(TransportationType.ORDERS_OPERATION)
                .setTransportationSource(TransportationSource.LMS_TM_MOVEMENT)
                .setScheme(TransportationScheme.UNKNOWN)
                .setHash("")
        );
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_with_partner_info_and_items.xml"
    })
    void search() {
        List<Transportation> byOutboundUnit = transportationMapper.search(
            InternalTransportationSearchFilter.builder()
                .outboundPartnerIds(Set.of(PARTNER_ID))
                .outboundDateFrom(LocalDate.of(2021, 3, 2))
                .outboundDateTo(LocalDate.of(2021, 3, 4))
                .outboundStatuses(Set.of(TransportationUnitStatus.SENT))
                .outboundLogisticPointIds(Set.of(1L))
                .movementExcludePartnerIds(Set.of(234L))
                .movementStatuses(Set.of(MovementStatus.NEW))
                .inboundLogisticPointIds(Set.of(2L))
                .build(),
            Pageable.unpaged()
        );
        softly.assertThat(byOutboundUnit).extracting(Transportation::getId).containsExactly(1L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_with_partner_info_and_items.xml"
    })
    void getByBarcode() {
        var byBarcode = transportationService.getByOrder("23456");
        softly.assertThat(byBarcode).extracting(Transportation::getId).containsExactly(2L);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportations_for_page_testing.xml"
    })
    void searchSeveralPages() {
        List<Transportation> firstPage = transportationMapper.search(
            InternalTransportationSearchFilter.builder().build(),
            PageRequest.of(0, 10)
        );

        List<Transportation> secondPage = transportationMapper.search(
            InternalTransportationSearchFilter.builder().build(),
            PageRequest.of(1, 10)
        );

        List<Transportation> thirdPage = transportationMapper.search(
            InternalTransportationSearchFilter.builder().build(),
            PageRequest.of(2, 10)
        );

        List<Transportation> fourthPage = transportationMapper.search(
            InternalTransportationSearchFilter.builder().build(),
            PageRequest.of(3, 10)
        );

        long count = transportationMapper.searchCount(InternalTransportationSearchFilter.builder().build());

        var commonList = new ArrayList<>(firstPage);
        commonList.addAll(secondPage);
        commonList.addAll(thirdPage);
        commonList.addAll(fourthPage);

        softly.assertThat(commonList.size()).isEqualTo(count);
        softly.assertThat(commonList
                                .stream().map(Transportation::getId).collect(Collectors.toSet()).size())
                .isEqualTo(count);
        softly.assertThat((int) commonList.stream()
                .filter(transportation -> transportation.getTransportationType() == TransportationType.INTERWAREHOUSE)
                .count())
                .isEqualTo(6);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_shipment_deps.xml",
        "/repository/transportation/transportation_xdoc_transport.xml"
    })
    void findNearestXDocTransport() {
        Transportation transportation = transportationService.findNearestXDocTransport(
            LocalDateTime.of(2020, 7, 10, 14, 0),
            1L,
            2L
        );

        softly.assertThat(transportation).isNotNull();
        softly.assertThat(transportation.getId()).isEqualTo(1L);
    }
}
