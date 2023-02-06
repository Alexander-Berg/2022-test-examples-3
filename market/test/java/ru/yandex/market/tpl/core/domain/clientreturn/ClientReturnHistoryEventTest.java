package ru.yandex.market.tpl.core.domain.clientreturn;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType;
import ru.yandex.market.tpl.api.model.order.clientreturn.PartnerClientReturnEventDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType.CLIENT_RETURN_CREATED;
import static ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType.CLIENT_RETURN_DELIVERED_TO_SC;
import static ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType.CLIENT_RETURN_READY_FOR_RECEIVED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class ClientReturnHistoryEventTest {
    private final static Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "date"));

    private static final String CLIENT_RETURN_EXTERNAL_ID_1 = "EXTERNAL_RETURN_ID_1";

    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ClientReturnService clientReturnService;
    private final ClientReturnQueryService clientReturnQueryService;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private PickupPoint pickupPoint;
    private Order order;
    private LockerDeliveryTask lockerDeliveryTask;
    private RoutePoint routePoint;
    private String clientReturnBarcodeExternalCreated1;

    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, DeliveryService.DEFAULT_DS_ID));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("111")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .pickupPoint(pickupPoint)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        userShiftReassignManager.assign(userShift, order);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(1);
    }

    @Test
    void testCreatedClientReturn() {
        ClientReturnCreateDto clientReturnCreateDto = new ClientReturnCreateDto();
        clientReturnCreateDto.setReturnId(CLIENT_RETURN_EXTERNAL_ID_1);
        clientReturnCreateDto.setBarcode(clientReturnBarcodeExternalCreated1);
        clientReturnCreateDto.setPickupPointId(pickupPoint.getId());
        clientReturnCreateDto.setLogisticPointId(pickupPoint.getLogisticPointId());
        clientReturnService.create(clientReturnCreateDto);

        List<PartnerClientReturnEventDto> events =
                clientReturnQueryService.getEvents(CLIENT_RETURN_EXTERNAL_ID_1, pageable).getContent();

        assertThat(events).hasSize(1);
        PartnerClientReturnEventDto partnerClientReturnEventDto = events.stream().findFirst().orElseThrow();
        checkPartnerClientReturnEventDto(partnerClientReturnEventDto, CLIENT_RETURN_CREATED);
    }

    @Test
    void testReceiveOnPvz() {
        testCreatedClientReturn();

        clientReturnService.receiveOnPvz(CLIENT_RETURN_EXTERNAL_ID_1);

        List<PartnerClientReturnEventDto> events =
                clientReturnQueryService.getEvents(CLIENT_RETURN_EXTERNAL_ID_1, pageable).getContent();

        assertThat(events).hasSize(2);
        checkPartnerClientReturnEventDto(events.get(0), CLIENT_RETURN_READY_FOR_RECEIVED);
    }

    @Test
    void idempotencyDeliverToScTest() {
        testCreatedClientReturn();

        clientReturnService.receiveOnPvz(CLIENT_RETURN_EXTERNAL_ID_1);
        clientReturnService.deliveredToSc(CLIENT_RETURN_EXTERNAL_ID_1);
        clientReturnService.deliveredToSc(CLIENT_RETURN_EXTERNAL_ID_1);

        List<PartnerClientReturnEventDto> events =
                clientReturnQueryService.getEvents(CLIENT_RETURN_EXTERNAL_ID_1, pageable).getContent();

        // inverted order
        checkPartnerClientReturnEventDto(events.get(1), CLIENT_RETURN_DELIVERED_TO_SC);
        checkPartnerClientReturnEventDto(events.get(0), CLIENT_RETURN_DELIVERED_TO_SC);
    }

    private void checkPartnerClientReturnEventDto(PartnerClientReturnEventDto partnerClientReturnEventDto,
                                                  ClientReturnHistoryEventType clientReturnHistoryEventType) {
        if (partnerClientReturnEventDto.getType() == CLIENT_RETURN_READY_FOR_RECEIVED
                || partnerClientReturnEventDto.getType() == CLIENT_RETURN_DELIVERED_TO_SC) {
            assertThat(partnerClientReturnEventDto.getContext()).isBlank();
        } else {
            assertThat(partnerClientReturnEventDto.getContext()).isNotBlank();
        }
        assertThat(partnerClientReturnEventDto.getType()).isEqualTo(clientReturnHistoryEventType);
        assertThat(partnerClientReturnEventDto.getTypeName()).isNotBlank();
    }
}
