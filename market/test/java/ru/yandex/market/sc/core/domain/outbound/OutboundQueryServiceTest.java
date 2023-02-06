package ru.yandex.market.sc.core.domain.outbound;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.route.model.TransferActDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
class OutboundQueryServiceTest {
    @Autowired
    TestFactory testFactory;
    @Autowired
    OutboundQueryService outboundQueryService;
    @Autowired
    OutboundRepository outboundRepository;
    @Autowired
    OutboundFacade outboundFacade;
    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
    }

    @DisplayName("АПП для отгрузки от дропоффа на СЦ")
    @Test
    @SneakyThrows
    void getOutboundTransferActFromDropoffToSc() {
        var dropoff = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(12387263L); // random number
        var uid = user.getUid();
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                dropoff.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                dropoff.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.setSortingCenterProperty(
                dropoff.getId(),
                SortingCenterPropertiesKey.IS_DROPOFF,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        var deliveryService2 = testFactory.storedDeliveryService("827301234102382"); // random
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(dropoff.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var order = testFactory.createForToday(
                order(dropoff, "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(dropoff)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .partnerToExternalId(String.valueOf(sortingCenterToId))
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(dropoff, cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, user);
        testFactory.sortToLot(order, "12", lot, user);
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp", user);
        testFactory.bindLotToOutbound(scToScOutbound.getExternalId(), lot.getBarcode(),
                testFactory.getRouteIdForSortableFlow(route), user);
        var order2 = testFactory.createForToday(
                order(dropoff, "2").places("21", "22")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        testFactory.shipPlace(order2, "21");
        testFactory.shipPlace(order2, "22");
        var order3 = testFactory.createForToday(
                order(dropoff, "3").places("31", "32")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("31", "32").sortPlaces("31", "32").get();
        testFactory.shipPlace(order3, "31");
        var order4 = testFactory.createForToday(
                order(dropoff, "4").places("41", "42")
                        .deliveryService(deliveryService2).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("41", "42").sortPlaces("41", "42").shipPlaces("41", "42").get(); // to another courier
        var outboundExternalId = outbound.getExternalId();
        outboundFacade.shipOutboundByPIAdmin(outboundExternalId, null);

        var actual = outboundQueryService.getTransferAct(outbound.getExternalId());

        assertThat(actual.getOrders()).containsExactlyInAnyOrder(
                TransferActDto.Order.builder()
                        .externalId("1")
                        .placeMainPartnerCode("11")
                        .totalSum(new BigDecimal("336.00"))
                        .lotName(lot.getBarcode())
                        .build(),
                TransferActDto.Order.builder()
                        .externalId("1")
                        .placeMainPartnerCode("12")
                        .totalSum(new BigDecimal("336.00"))
                        .lotName(lot.getBarcode())
                        .build(),
                TransferActDto.Order.builder()
                        .externalId("2")
                        .placeMainPartnerCode("21")
                        .totalSum(new BigDecimal("336.00"))
                        .build(),
                TransferActDto.Order.builder()
                        .externalId("2")
                        .placeMainPartnerCode("22")
                        .totalSum(new BigDecimal("336.00"))
                        .build(),
                TransferActDto.Order.builder()
                        .externalId("3")
                        .placeMainPartnerCode("31")
                        .totalSum(new BigDecimal("336.00"))
                        .build()
        );
        assertThat(actual).isEqualToIgnoringGivenFields(TransferActDto.builder()
                        .number("О-outbound1")
                        .date(LocalDate.now(clock))
                        .sender("ООО Яндекс.Маркет")
                        .executor("legalName")
                        .recipient("ООО Яндекс.Маркет")
                        .senderScName(dropoff.getScName())
                        .recipientScName(sortingCenterTo.getScName())
                        .courier("name")
                        .totalPlaces(5)
                        .build(),
                "orders");
    }

    @Test
    @SneakyThrows
    @DisplayName("АПП для отгрузки от СЦ на СЦ")
    void getOutboundTransferActFromScToSc() {
        var scFrom = testFactory.storedSortingCenter();
        var user = testFactory.storedUser(12387263L); // random number
        var uid = user.getUid();
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                scFrom.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                scFrom.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        var deliveryService2 = testFactory.storedDeliveryService("827301234102382"); // random
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(scFrom.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var order = testFactory.createForToday(
                order(scFrom, "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(scFrom)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(scFrom, cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, user);
        testFactory.sortToLot(order, "12", lot, user);
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp", user);
        testFactory.bindLotToOutbound(scToScOutbound.getExternalId(), lot.getBarcode(),
                testFactory.getRouteIdForSortableFlow(route), user);
        var order2 = testFactory.createForToday(
                order(scFrom, "2").places("21", "22")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        testFactory.shipPlace(order2, "21");
        testFactory.shipPlace(order2, "22");
        var order4 = testFactory.createForToday(
                order(scFrom, "4").places("41", "42")
                        .deliveryService(deliveryService2).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("41", "42").sortPlaces("41", "42").shipPlaces("41", "42").get(); // to another courier
        var outboundExternalId = outbound.getExternalId();
        outboundFacade.shipOutboundByPIAdmin(outboundExternalId, null);

        var actual = outboundQueryService.getTransferAct(outbound.getExternalId());

        assertThat(actual.getOrders()).containsExactlyInAnyOrder(
                TransferActDto.Order.builder()
                        .externalId("1")
                        .placeMainPartnerCode("11")
                        .totalSum(new BigDecimal("336.00"))
                        .lotName(lot.getBarcode())
                        .build(),
                TransferActDto.Order.builder()
                        .externalId("1")
                        .placeMainPartnerCode("12")
                        .totalSum(new BigDecimal("336.00"))
                        .lotName(lot.getBarcode())
                        .build()
        );
        assertThat(actual).isEqualToIgnoringGivenFields(TransferActDto.builder()
                        .number("О-outbound1")
                        .date(LocalDate.now(clock))
                        .sender("ООО Яндекс.Маркет")
                        .executor("legalName")
                        .recipient("ООО Яндекс.Маркет")
                        .senderScName(scFrom.getScName())
                        .recipientScName(sortingCenterTo.getScName())
                        .courier("name")
                        .totalPlaces(2)
                        .build(),
                "orders");
    }

    @Test
    @SneakyThrows
    @Transactional
    void getOutboundTransferAct() {
        var sortingCenter = testFactory.storedSortingCenter();
        Outbound outbound = testFactory.createOutbound(sortingCenter);
        outbound.setStatus(OutboundStatus.SHIPPED);
        outbound.setType(OutboundType.DS_SC);
        var sortingCenterTo = testFactory.storedSortingCenter(112L);
        outbound.setLogisticPointToExternalId(sortingCenterTo.getYandexId());
        Registry outboundRegistry = testFactory.bindRegistry(outbound.getExternalId(), RegistryType.FACTUAL);

        var place = testFactory.createForToday(order(sortingCenter, "single-place-order").build())
                .accept().sort().getPlace();
        var cell = place.getCell();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);

        testFactory.bindOrder(
                outboundRegistry,
                place.getExternalId(),
                place.getMainPartnerCode(),
                lot.getBarcode()
        );

        var multiPlaceOrder1 = testFactory.createForToday(
            order(sortingCenter, "multi-place-order").places("place-1", "p_duplicate").build()
        ).acceptPlaces().sortPlaces().get();
        var multiPlaceOrder2 = testFactory.createForToday(
            order(sortingCenter, "multi-place-order2").places("place-2", "p_duplicate").build()
        ).acceptPlaces().sortPlaces().get();
        var orderPlaces = testFactory.orderPlaces(multiPlaceOrder1.getId());
        orderPlaces.forEach(p -> testFactory.bindOrder(
                outboundRegistry,
                multiPlaceOrder1.getExternalId(),
                p.getMainPartnerCode(),
                lot.getBarcode()
        ));
        testFactory.orderPlaces(multiPlaceOrder2.getId()).forEach(p -> testFactory.bindOrder(
                outboundRegistry,
                multiPlaceOrder2.getExternalId(),
                p.getMainPartnerCode(),
                lot.getBarcode()
        ));

        var actual = outboundQueryService.getTransferAct(outbound.getExternalId());

        assertThat(actual).isEqualTo(
                TransferActDto.builder()
                        .number("О-111")
                        .date(LocalDate.of(2021, 4, 22))
                        .sender("ООО Яндекс.Маркет")
                        .executor("legalName")
                        .recipient("ООО Яндекс.Маркет")
                        .senderScName(sortingCenter.getScName())
                        .recipientScName(sortingCenterTo.getScName())
                        .orders(
                                List.of(
                                        TransferActDto.Order.builder()
                                                .externalId("multi-place-order")
                                                .placeMainPartnerCode("place-1")
                                                .totalSum(new BigDecimal("336.00"))
                                                .lotName(lot.getBarcode())
                                                .build(),
                                        TransferActDto.Order.builder()
                                                .externalId("multi-place-order")
                                                .placeMainPartnerCode("p_duplicate")
                                                .totalSum(new BigDecimal("336.00"))
                                                .lotName(lot.getBarcode())
                                                .build(),
                                        TransferActDto.Order.builder()
                                                .externalId("multi-place-order2")
                                                .placeMainPartnerCode("place-2")
                                                .totalSum(new BigDecimal("336.00"))
                                                .lotName(lot.getBarcode())
                                                .build(),
                                        TransferActDto.Order.builder()
                                                .externalId("multi-place-order2")
                                                .placeMainPartnerCode("p_duplicate")
                                                .totalSum(new BigDecimal("336.00"))
                                                .lotName(lot.getBarcode())
                                                .build(),
                                        TransferActDto.Order.builder()
                                                .externalId("single-place-order")
                                                .placeMainPartnerCode("single-place-order")
                                                .totalSum(new BigDecimal("336.00"))
                                                .lotName(lot.getBarcode())
                                                .build()
                                )
                        )
                        .courier("name")
                        .totalPlaces(5)
                        .build()
        );
    }

}
