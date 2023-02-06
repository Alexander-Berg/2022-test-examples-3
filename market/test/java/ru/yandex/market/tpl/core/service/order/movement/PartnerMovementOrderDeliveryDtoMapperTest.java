package ru.yandex.market.tpl.core.service.order.movement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDeliveryInfoDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDetailsDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementShipper;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftOrderQueryService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReason;
import ru.yandex.market.tpl.core.domain.usershift.dropship.CollectDropshipFailReasonType;
import ru.yandex.market.tpl.core.service.order.PartnerOrderAddressDtoMapper;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.movement.Movement.TAG_DROPOFF_CARGO_RETURN;

@ExtendWith(MockitoExtension.class)
class PartnerMovementOrderDeliveryDtoMapperTest {

    public static final String FAIL_COMMENT = "failComment";
    public static final Source FAIL_SOURCE = Source.CLIENT;
    public static final CollectDropshipFailReasonType FAIL_TYPE = CollectDropshipFailReasonType.COURIER_NEEDS_HELP;
    public static final LocalDateTime TASK_FINISHED_TIME = LocalDateTime.of(2021, 11, 3, 10, 30);
    public static final LocalDate MOVEMENT_DELIVERY_FROM = LocalDate.of(2021, 11, 3);
    public static final LocalDate MOVEMENT_DELIVERY_TO = LocalDate.of(2021, 11, 2);
    public static final ZoneOffset DS_TIMEZONE = ZoneOffset.UTC;
    public static final long USER_ID = 1L;
    public static final long USER_UID = 11L;
    public static final String USER_FULLNAME = "User Full Name";
    public static final BigDecimal MOVEMENT_ADDRESS_LAT = BigDecimal.valueOf(55.123456);
    public static final BigDecimal MOVEMENT_ADDRESS_LON = BigDecimal.valueOf(33.1234567);
    public static final long MOVEMENT_DELIVERY_SERVICE_ID = 10L;
    public static final long MOVEMENT_SC_ID = 12L;
    public static final String MOVEMENT_SC_NAME = "SC_NAME";
    public static final String MOVEMENT_DS_NAME = "MOVEMENT_DS_NAME";
    public static final long ROUTPOINT_ID = 13L;
    public static final int MOVEMENT_PALLETS = 123;
    public static final BigDecimal MOVEMENT_VOLUME = BigDecimal.valueOf(1234);
    public static final long MOVEMENT_ID = 124L;
    public static final String MOVEMENT_EXTERNAL_ID = "12345L";
    public static final String MOVEMENT_ADDRESS_STRING = "MOVEMENT_ADDRESS_STRING";
    public static final String WAREHOUSE_CONTACT = "WAREHOUSE_CONTACT";
    public static final String SHIPPER_COMPANY_NAME = "ShipperCompanyName";
    @Mock
    private SortingCenterService sortingCenterService;
    @Mock
    private UserShiftOrderQueryService userShiftOrderQueryService;
    @Mock
    private PartnerOrderAddressDtoMapper orderAddressDtoMapper;
    @Mock
    private DsZoneOffsetCachingService dsZoneOffsetCachingService;
    @Mock
    private UserShiftReassignManager userShiftReassignManager;
    @InjectMocks
    private PartnerMovementOrderDeliveryDtoMapper mapper;

    @BeforeEach
    void setUp() {
        when(dsZoneOffsetCachingService.getOffsetForDs(any())).thenReturn(DS_TIMEZONE);

    }

    @Test
    void mappingDropshipMovement_oldVersion() {
        //given
        Movement dropshipMovement = buildDropshipMovement();

        CollectDropshipTask mockedTask = buildMockedDropshipTask();

        when(userShiftOrderQueryService.findDropshipTasksByMovement(eq(dropshipMovement)))
                .thenReturn(Optional.of(mockedTask));

        SortingCenter mockedSortingCenter = buildSc();
        when(sortingCenterService.findSortCenterForDs(eq(dropshipMovement.getDeliveryServiceId())))
                .thenReturn(mockedSortingCenter);
        DeliveryService mockedDeliveryService = buildDS();
        when(sortingCenterService.findDsById(eq(dropshipMovement.getDeliveryServiceId())))
                .thenReturn(mockedDeliveryService);

        //when
        PartnerOrderDeliveryDto dto = mapper.toPartnerOrderDeliveryDto(dropshipMovement);

        //then
        assertNotNull(dto);
        asserts(dto.getOrder());
        asserts(dto.getDelivery());
        asserts(dto.getActions());
    }

    @Test
    void mappingDropshipMovement_newVersion() {
        //given
        Movement dropshipMovement = buildDropshipMovement();

        when(userShiftOrderQueryService.findDropshipTasksByMovement(eq(dropshipMovement)))
                .thenReturn(Optional.empty());

        LockerSubtask subtask =  buildSubtask();
        when(userShiftOrderQueryService.findDropOffTaskByMovement(dropshipMovement))
                .thenReturn(Optional.of(subtask));

        SortingCenter mockedSortingCenter = buildSc();
        when(sortingCenterService.findSortCenterForDs(eq(dropshipMovement.getDeliveryServiceId())))
                .thenReturn(mockedSortingCenter);
        DeliveryService mockedDeliveryService = buildDS();
        when(sortingCenterService.findDsById(eq(dropshipMovement.getDeliveryServiceId())))
                .thenReturn(mockedDeliveryService);

        //when
        PartnerOrderDeliveryDto dto = mapper.toPartnerOrderDeliveryDto(dropshipMovement);

        //then
        assertNotNull(dto);
        asserts(dto.getOrder());
        asserts(dto.getDelivery());
        asserts(dto.getActions());
    }

    @Test
    void mappingDropOffReturnMovement() {
        //given
        Movement dropOffReturnMovement = buildDropOffReturnMovement();

        LockerSubtask subtask =  buildSubtask();
        when(userShiftOrderQueryService.findDropOffTaskByMovement(dropOffReturnMovement))
                .thenReturn(Optional.of(subtask));

        SortingCenter mockedSortingCenter = buildSc();
        when(sortingCenterService.findSortCenterForDs(eq(dropOffReturnMovement.getDeliveryServiceId())))
                .thenReturn(mockedSortingCenter);
        DeliveryService mockedDeliveryService = buildDS();
        when(sortingCenterService.findDsById(eq(dropOffReturnMovement.getDeliveryServiceId())))
                .thenReturn(mockedDeliveryService);

        //when
        PartnerOrderDeliveryDto dto = mapper.toPartnerOrderDeliveryDto(dropOffReturnMovement);

        //then
        assertNotNull(dto);
        asserts(dto.getOrder());
        asserts(dto.getDelivery());
        asserts(dto.getActions());
    }

    private LockerSubtask buildSubtask() {
        LockerDeliveryTask lockerDeliveryTask = buildMockedDropOffSubTask();
        LockerSubtask lockerSubtask = mock(LockerSubtask.class);

        when(lockerSubtask.getTask()).thenReturn(lockerDeliveryTask);
        return lockerSubtask;
    }

    private void asserts(PartnerOrderDetailsDto order) {
        assertEquals(MOVEMENT_ID, order.getId());
        assertEquals(MOVEMENT_EXTERNAL_ID, order.getOrderId());

        assertEquals(MOVEMENT_DELIVERY_SERVICE_ID, order.getDeliveryServiceId());
        assertEquals(MOVEMENT_DS_NAME, order.getDeliveryServiceName());

        assertEquals(MOVEMENT_SC_ID, order.getSortingCenterId());
        assertEquals(MOVEMENT_SC_NAME, order.getSortingCenterName());

        assertEquals(PartnerOrderType.DROPSHIP, order.getOrderType());

        assertEquals(MOVEMENT_ADDRESS_STRING, order.getAddress());

        assertNotNull(order.getRecipient());
        assertEquals(WAREHOUSE_CONTACT, order.getRecipient().getName());


        assertNotNull(order.getShipperCompanyName());
        assertEquals(SHIPPER_COMPANY_NAME, order.getShipperCompanyName());

        //todo complex asserts for order.getAddressDetails() and others;
    }

    private void asserts(List<OrderDeliveryTaskDto.Action> actions) {
        List<OrderDeliveryTaskDto.ActionType> actionTypes =
                actions.stream().map(OrderDeliveryTaskDto.Action::getType).collect(Collectors.toList());
        assertThat(actionTypes).containsExactlyInAnyOrderElementsOf(
                Set.of(OrderDeliveryTaskDto.ActionType.MOVEMENT_CANCEL,
                OrderDeliveryTaskDto.ActionType.DROPSHIP_TASK_REOPEN)
        );
    }

    private void asserts(PartnerOrderDeliveryInfoDto delivery) {
        assertEquals(USER_ID, delivery.getCourierId());
        assertEquals(USER_UID, delivery.getCourierUid());
        assertEquals(USER_FULLNAME, delivery.getCourierName());

        assertEquals(LocalTime.of(5,30), delivery.getDeliveryIntervalFrom());
        assertEquals(LocalTime.of(6,30), delivery.getDeliveryIntervalTo());

        assertEquals(MOVEMENT_DELIVERY_FROM, delivery.getDeliveryDate());
        assertEquals(TASK_FINISHED_TIME, delivery.getDeliveryTime());

        assertEquals(ROUTPOINT_ID, delivery.getRoutePointId());

        assertEquals(MOVEMENT_ADDRESS_LAT, delivery.getLatitude());
        assertEquals(MOVEMENT_ADDRESS_LON, delivery.getLongitude());

        assertEquals(MOVEMENT_PALLETS, delivery.getPallets());
        assertEquals(MOVEMENT_VOLUME, delivery.getVolume());

        //todo  complex assert lastOrderDeliveryDto
    }

    private Movement buildMovement(List<String> tags) {
        Movement movement = new Movement();
        movement.setStatus(MovementStatus.CREATED);
        movement.setDeliveryIntervalFrom(MOVEMENT_DELIVERY_FROM.atTime(5, 30).toInstant(DS_TIMEZONE));
        movement.setDeliveryIntervalTo(MOVEMENT_DELIVERY_TO.atTime(6, 30).toInstant(DS_TIMEZONE));
        movement.setDeliveryServiceId(MOVEMENT_DELIVERY_SERVICE_ID);
        movement.setPallets(MOVEMENT_PALLETS);
        movement.setVolume(MOVEMENT_VOLUME);
        movement.setId(MOVEMENT_ID);
        movement.setExternalId(MOVEMENT_EXTERNAL_ID);
        movement.setTags(tags);

        movement.setShipper(
                MovementShipper.builder()
                        .companyName(SHIPPER_COMPANY_NAME)
                        .build()
        );

        OrderWarehouseAddress mockedAddress = mock(OrderWarehouseAddress.class);
        when(mockedAddress.getLatitude()).thenReturn(MOVEMENT_ADDRESS_LAT);
        when(mockedAddress.getLongitude()).thenReturn(MOVEMENT_ADDRESS_LON);
        when(mockedAddress.getAddress()).thenReturn(MOVEMENT_ADDRESS_STRING);

        OrderWarehouse mockedWarehouse = mock(OrderWarehouse.class);
        when(mockedWarehouse.getAddress()).thenReturn(mockedAddress);
        when(mockedWarehouse.getContact()).thenReturn(WAREHOUSE_CONTACT);
        movement.setWarehouse(mockedWarehouse);
        movement.setWarehouseTo(mockedWarehouse);
        return movement;
    }

    private Movement buildDropshipMovement() {
        return buildMovement(List.of());
    }

    private Movement buildDropOffReturnMovement() {
        return buildMovement(List.of(TAG_DROPOFF_CARGO_RETURN));
    }

    private CollectDropshipTask buildMockedDropshipTask() {
        RoutePoint mockedRP = buildRPWithUserShift();

        CollectDropshipTask mockedDropshipTask = mock(CollectDropshipTask.class);

        when(mockedDropshipTask.getRoutePoint()).thenReturn(mockedRP);
        when(mockedDropshipTask.getStatus()).thenReturn(CollectDropshipTaskStatus.CANCELLED);

        CollectDropshipFailReason failReason = new CollectDropshipFailReason();
        failReason.setComment(FAIL_COMMENT);
        failReason.setSource(FAIL_SOURCE);
        failReason.setType(FAIL_TYPE);

        when(mockedDropshipTask.getFailReason()).thenReturn(failReason);

        when(mockedDropshipTask.getFinishedAt()).thenReturn(TASK_FINISHED_TIME.toInstant(DS_TIMEZONE));
        when(mockedDropshipTask.getExpectedTime()).thenReturn(TASK_FINISHED_TIME.toInstant(DS_TIMEZONE));
        return mockedDropshipTask;
    }

    private LockerDeliveryTask buildMockedDropOffSubTask() {
        RoutePoint mockedRP = buildRPWithUserShift();

        LockerDeliveryTask mockedDropOffTask = mock(LockerDeliveryTask.class);

        when(mockedDropOffTask.getRoutePoint()).thenReturn(mockedRP);
        when(mockedDropOffTask.getStatus()).thenReturn(LockerDeliveryTaskStatus.CANCELLED);

        OrderDeliveryFailReason failReason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                FAIL_COMMENT,
                List.of(),
                FAIL_SOURCE
                );

        when(mockedDropOffTask.getFailReason()).thenReturn(failReason);

        when(mockedDropOffTask.getFinishedAt()).thenReturn(TASK_FINISHED_TIME.toInstant(DS_TIMEZONE));
        when(mockedDropOffTask.getExpectedTime()).thenReturn(TASK_FINISHED_TIME.toInstant(DS_TIMEZONE));
        return mockedDropOffTask;
    }

    @NotNull
    private RoutePoint buildRPWithUserShift() {
        UserShift mockedUserShift = mock(UserShift.class);
        User mockedUser = buildUser();
        when(mockedUserShift.getUser()).thenReturn(mockedUser);

        RoutePoint mockedRP = mock(RoutePoint.class);
        when(mockedRP.getUserShift()).thenReturn(mockedUserShift);
        when(mockedRP.getId()).thenReturn(ROUTPOINT_ID);
        return mockedRP;
    }

    private DeliveryService buildDS() {
        DeliveryService mockedDeliveryService = mock(DeliveryService.class);
        when(mockedDeliveryService.getName()).thenReturn(MOVEMENT_DS_NAME);
        return mockedDeliveryService;
    }

    private SortingCenter buildSc() {
        SortingCenter mockedSortingCenter = mock(SortingCenter.class);
        when(mockedSortingCenter.getId()).thenReturn(MOVEMENT_SC_ID);
        when(mockedSortingCenter.getName()).thenReturn(MOVEMENT_SC_NAME);
        return mockedSortingCenter;
    }

    private User buildUser() {
        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(USER_ID);
        when(mockedUser.getUid()).thenReturn(USER_UID);
        when(mockedUser.getFullName()).thenReturn(USER_FULLNAME);
        return mockedUser;
    }
}
