package ru.yandex.market.tpl.core.domain.shift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleMetaType;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleStatus;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyRole;
import ru.yandex.market.tpl.core.domain.company.CompanyRoleRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyType;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceCreateDto;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryServiceRoleEnum;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterUtil;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTag;
import ru.yandex.market.tpl.core.domain.routing.tag.RoutingOrderTagRepository;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCommonRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.location.ReceptionService;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.service.lms.deliveryservice.DeliveryServiceCommandService;
import ru.yandex.market.tpl.core.service.order.TaskErrorSaver;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.core.service.user.transport.Transport;
import ru.yandex.market.tpl.core.service.user.transport.TransportRepository;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;
import ru.yandex.market.tpl.core.task.flow.EmptyPayload;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.service.TaskService;

import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.ON_TASK;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.SHIFT_OPEN;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_CAMPAIGN_ID;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;

/**
 * @author ungomma
 */
@Component
@RequiredArgsConstructor
@Transactional
public class TestUserHelper {

    public static final String DEFAULT_TRANSPORT_NAME = "Машинка №1";
    public static final String DEFUALT_COMPANY_DRAFT_LOGIN = "LOGIN";

    public static final String DBS_COMPANY_NAME = "DBS_COMPANY_NAME_NOT_OVERWRITE!";
    public static final Long DBS_USER_UID = 930529748L;
    public static final Long DBS_COMPANY_CAMPAIGN_ID = 23534535L;
    public static final String DBS_COMPANY_L0GIN = "dbsLogin@yandex.ru";

    public static final Long DBS_DELIVERY_SERVICE_ID = 24345345L;
    public static final String DBS_DELIVERY_SERVICE_NAME = "DBS_DELIVERY_SERVICE_NAME_NOT_OVERWRITE!";

    public static final Long DBS_SORTING_CENTER_ID = 2342543234L;

    public static final long TEST_SERVICE_CENTER_ID = 47819L;

    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final UserShiftRepository userShiftRepository;
    private final CompanyRepository companyRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftCommandDataHelper helper;
    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final PartnerRepository<DeliveryService> dsRepository;
    private final PartnerRepository<SortingCenter> scRepository;
    private final Clock clock;
    private final ReceptionService receptionService;
    private final TaskErrorSaver taskErrorSaver;
    private final TransportRepository transportRepository;
    private final TransportTypeRepository transportTypeRepository;
    private final RoutingOrderTagRepository routingOrderTagRepository;
    private final UserPropertyService userPropertyService;
    private final CompanyDraftRepository companyDraftRepository;
    private final DeliveryServiceCommandService deliveryServiceCommandService;
    private final CompanyRoleRepository companyRoleRepository;
    private final PartnerRepository<DeliveryService> deliveryServiceRepository;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final ClientReturnService clientReturnService;
    private final ClientReturnRepository clientReturnRepository;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final UserShiftCommandService userShiftCommandService;
    private final ShiftManager shiftManager;
    private final TaskService taskService;


    @Autowired
    @Qualifier("oneMinuteCacheManager")
    private CacheManager cacheManager;

    public UserShift createEmptyShift(User user, LocalDate date) {
        return createEmptyShift(user, findOrCreateOpenShift(date));
    }

    public UserShift createEmptyShift(User user, Shift shift) {
        long userShiftId = commandService.createUserShift(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .scheduleData(UserScheduleTestHelper.createScheduleData(CourierVehicleType.CAR))
                .build()
        );

        commandService.switchActiveUserShift(user, userShiftId);

        return userShiftRepository.findById(userShiftId).orElseThrow();
    }

    public CompanyDraft getCompanyDraft() {
        return companyDraftRepository.findByLogin(DEFUALT_COMPANY_DRAFT_LOGIN).orElse(getNewCompanyDraft());
    }

    private CompanyDraft getNewCompanyDraft() {
        CompanyDraft companyDraft = new CompanyDraft();
        companyDraft.setId(1L);
        companyDraft.setBusinessId(1L);
        companyDraft.setLogin(DEFUALT_COMPANY_DRAFT_LOGIN);
        companyDraft.setScLatitude(454.45);
        companyDraft.setScLongitude(345.53);
        companyDraft.setOgrn("123456");
        return companyDraft;
    }

    public UserShift createOpenedShift(User user, Order order, LocalDate date) {
        return createOpenedShift(user, order, date, TEST_SERVICE_CENTER_ID);
    }

    public UserShift createOpenedShift(User user, Order order, LocalDate date, long scId) {
        return createShiftWithDeliveryTask(user, ON_TASK, findOrCreateOpenShiftForSc(date, scId), order);
    }

    public DeliveryService createOrFindDbsDeliveryService() {
        Long id = deliveryServiceCommandService.createDeliveryService(LmsDeliveryServiceCreateDto.builder()
                .deliveryServiceId(DBS_DELIVERY_SERVICE_ID)
                .deliveryAreaMarginWidth(0L)
                .name(DBS_DELIVERY_SERVICE_NAME)
                .build());
        DeliveryService deliveryService = deliveryServiceRepository.findByIdOrThrow(id);
        deliveryService.setSortingCenter(sortingCenter(DBS_SORTING_CENTER_ID));
        deliveryService.setDeliveryServiceRoleEnum(DeliveryServiceRoleEnum.DBS);
        deliveryServiceRepository.save(deliveryService);
        return deliveryService;
    }


    public User createOrFindDbsUser() {
        Optional<User> optionalUser = userRepository.findByUid(DBS_USER_UID);
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        }
        Company dbsCompany = createOrFindDbsCompany();
        return createUser(DBS_USER_UID, LocalDate.now(clock),
                UserScheduleType.ALWAYS_WORKS, RelativeTimeInterval.valueOf("09:00-19:00"),
                SortingCenter.DEFAULT_SC_ID, dbsCompany);
    }

    public Company createOrFindDbsCompany() {
        return companyRepository.findCompanyByName(DBS_COMPANY_NAME).orElseGet(this::createDbsCompany);
    }

    private Company createDbsCompany() {
        CompanyRole companyRole = companyRoleRepository.findByName(CompanyRoleEnum.DBS).orElseThrow();
        Company company = createCompany(Set.of(sortingCenter(DBS_SORTING_CENTER_ID)), DBS_COMPANY_CAMPAIGN_ID,
                DBS_COMPANY_NAME, DBS_COMPANY_L0GIN, false, null, companyRole);
        return company;
    }

    /**
     * Создание открытой смены, включающей заказы, за определенную дату.
     */
    public UserShift createOpenedShift(User user, @NonNull List<Order> orders, LocalDate date) {
        Set<Long> orderDsIds = orders
                .stream()
                .map(Order::getDeliveryServiceId)
                .collect(Collectors.toSet());
        if (orderDsIds.size() != 1) {
            throw new TplIllegalArgumentException("Различные DsId у заказов. Нет однозначности для создания UserShift");
        }
        long scId = dsRepository.findById(orderDsIds.iterator().next()).get().getSortingCenter().getId();
        return createShiftWithDeliveryTask(user, ON_TASK, findOrCreateOpenShiftForSc(date, scId), orders, null);
    }

    public UserShift createOpenedShift(User user, List<Order> orders, LocalDate date, long scId) {
        return createShiftWithDeliveryTask(user, ON_TASK, findOrCreateOpenShiftForSc(date, scId), orders, null);
    }

    public UserShift createShiftWithDeliveryTask(User user, UserShiftStatus shiftStatus, Order order) {
        return createShiftWithDeliveryTask(user, shiftStatus, order, null);
    }

    public UserShift createShiftWithDeliveryTask(User user, UserShiftStatus shiftStatus, Order order,
                                                 AtomicLong taskId) {
        return createShiftWithDeliveryTask(user, shiftStatus, findOrCreateOpenShift(LocalDate.now()), order, taskId);
    }

    public UserShift createShiftWithDeliveryTask(User user, UserShiftStatus shiftStatus, Shift shift, Order order) {
        return createShiftWithDeliveryTask(user, shiftStatus, shift, order, null);
    }

    public UserShift createShiftWithDeliveryTask(User user, UserShiftStatus shiftStatus, Shift shift, Order order,
                                                 AtomicLong taskIdBox) {
        UserShift userShift = createEmptyShift(user, shift);

        DeliveryTask orderDeliveryTask = commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                helper.taskPrepaid("addr1", 12, order.getId()),
                SimpleStrategies.NO_MERGE,
                GeoPoint.GEO_POINT_SCALE
        ));
        taskErrorSaver.saveNoPhotoComment(orderDeliveryTask.getId(), "some-comment", UUID.randomUUID());
        if (taskIdBox != null) {
            taskIdBox.set(orderDeliveryTask.getId());
        }


        if (shiftStatus == SHIFT_OPEN || shiftStatus == ON_TASK) {
            openShift(user, userShift.getId());
        }

        return userShift;
    }

    public UserShift createShiftWithDeliveryTask(User user, UserShiftStatus shiftStatus, Shift shift,
                                                 List<Order> orders,
                                                 AtomicLong taskIdBox) {
        UserShift userShift = createEmptyShift(user, shift);


        orders.forEach(order -> {
            DeliveryTask orderDeliveryTask = commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                    userShift.getId(),
                    helper.task("addr1", order.getId(), DateTimeUtil.todayAtHour(12, clock),
                            false, GeoPointGenerator.generateLonLat(), order.getPaymentType()

                    ),
                    SimpleStrategies.NO_MERGE,
                    GeoPoint.GEO_POINT_SCALE
            ));
            taskErrorSaver.saveNoPhotoComment(orderDeliveryTask.getId(), "some-comment", UUID.randomUUID());
            if (taskIdBox != null) {
                taskIdBox.set(orderDeliveryTask.getId());
            }
        });


        if (shiftStatus == SHIFT_OPEN || shiftStatus == ON_TASK) {
            openShift(user, userShift.getId());
        }

        return userShift;
    }

    public void rescheduleNextDay(RoutePoint routePoint) {
        rescheduleNextDay(routePoint, Source.COURIER, OrderDeliveryRescheduleReasonType.DELIVERY_DELAY);
    }

    public void reopen(RoutePoint routePoint) {
        commandService.reopenDeliveryTask(routePoint.getUserShift().getUser(),
                new UserShiftCommand.ReopenOrderDeliveryTask(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        routePoint.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow().getId(),
                        Source.COURIER
                ));
    }

    public void rescheduleNextDayMultiOrder(RoutePoint routePoint, OrderDeliveryRescheduleReasonType reasonType) {
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();
        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);
        String multiOrderId = userShift.streamCallTasks().findFirst().get().getId().toString();
        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));
        commandService.rescheduleMultiOrder(user,
                new UserShiftCommand.RescheduleMultiOrderCommand(
                        userShift.getId(),
                        routePoint.getId(),
                        multiOrderId,
                        DeliveryReschedule.fromCourier(user, from, to, reasonType),
                        clock.instant(),
                        userShift.getZoneId()), intervals);
    }

    public void rescheduleNextDay(RoutePoint routePoint, Source source, OrderDeliveryRescheduleReasonType reasonType) {
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();
        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);
        commandService.rescheduleDeliveryTask(user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(),
                        routePoint.getId(),
                        routePoint.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow().getId(),
                        source == Source.COURIER ?
                                DeliveryReschedule.fromCourier(user, from, to, reasonType) :
                                new DeliveryReschedule(new Interval(from, to), reasonType, null, source, from),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock), LocalTime.of(18, 20))),
                        userShift.getZoneId()
                ));
    }

    public void arriveAtRoutePoint(UserShift userShift, long routePointId) {
        var user = userShift.getUser();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePointId,
                        helper.getLocationDto(userShift.getId())
                ));
    }

    public void arriveAtRoutePoint(RoutePoint routePoint) {
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
    }

    public void finishDelivery(RoutePoint routePoint, boolean fail) {
        finishDelivery(routePoint, fail ? OrderDeliveryTaskFailReasonType.CLIENT_REFUSED : null);
    }

    public void arriveAtRpAndfinishOrderDeliveryTask(User user, Long userShiftId, Long routePointId,
                                                     DeliveryTask task, Order order) {
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId,
                        routePointId,
                        helper.getLocationDto(userShiftId)
                ));
        commandService.printCheque(user,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShiftId, routePointId, task.getId(),
                        helper.getChequeDto(
                                order.isPrepaid() ? OrderPaymentType.PREPAID :
                                        order.getPaymentType() != null ? order.getPaymentType() :
                                                OrderPaymentType.CASH
                        ), task.getExpectedDeliveryTime(), false, null, Optional.empty()
                ));
    }

    public void finishNextLockerInventory(long userShiftId) {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        var task = userShift.streamFlowTasks()
                .filter(t -> t.getFlowType() == TaskFlowType.LOCKER_INVENTORY)
                .findFirst()
                .orElseThrow();
        var routePointId = task.getRoutePoint().getId();
        var currentRoutePointId = userShift.getCurrentRoutePoint() == null ? null : userShift.getCurrentRoutePoint().getId();

        if (!Objects.equals(routePointId, currentRoutePointId)) {
            commandService.switchOpenRoutePoint(userShift.getUser(),
                    new UserShiftCommand.SwitchOpenRoutePoint(userShiftId, routePointId));
        }

        commandService.arriveAtRoutePoint(userShift.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId,
                        routePointId,
                        helper.getLocationDto(userShift.getId())
                ));
        taskService.executeAction(task.getId(), task.getCurrentActionId(), new EmptyPayload(), userShift.getUser());
    }

    public void finishDelivery(RoutePoint routePoint, @Nullable OrderDeliveryTaskFailReasonType fail,
                               @Nullable OrderPaymentType orderPaymentType, boolean finishCallTasks) {
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        routePoint.streamTasks(OrderDeliveryTask.class)
                .forEach(task -> {
                    if (fail != null) {
                        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(), routePoint.getId(), task.getId(),
                                new OrderDeliveryFailReason(fail, "my comment!")
                        ));
                    } else if (task.isClientReturn()) {
                        var cr = clientReturnRepository.findByIdOrThrow(task.getClientReturnId());
                        clientReturnService.assignBarcodeAndFinishTask(
                                List.of(ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456"),
                                Map.of(), cr.getExternalReturnId(), user, task.getId()
                        );
                    } else {
                        if (!task.isPrepaidOrder()) {
                            commandService.payOrder(user, new UserShiftCommand.PayOrder(
                                    userShift.getId(), routePoint.getId(), task.getId(),
                                    orderPaymentType != null ? orderPaymentType : OrderPaymentType.CASH,
                                    null
                            ));
                        }
                        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                                userShift.getId(), routePoint.getId(), task.getId(),
                                helper.getChequeDto(
                                        task.isPrepaidOrder() ? OrderPaymentType.PREPAID :
                                                orderPaymentType != null ? orderPaymentType : OrderPaymentType.CASH

                                ),
                                task.getExpectedDeliveryTime(),
                                false,
                                null,
                                partialReturnOrderRepository.findByTaskOrderDelivery(task.getId())
                        ));
                    }
                });
        routePoint.streamTasks(LockerDeliveryTask.class)
                .forEach(task -> {
                    if (fail != null) {
                        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(), routePoint.getId(), task.getId(),
                                new OrderDeliveryFailReason(fail, "my comment!")
                        ));
                    } else {
                        commandService.finishLoadingLocker(user,
                                new UserShiftCommand.FinishLoadingLocker(userShift.getId(), routePoint.getId(),
                                        task.getId(), null, ScanRequest.builder()
                                        .successfullyScannedOrders(new ArrayList<>(task.getOrderIds()))
                                        .build()));
                        commandService.finishUnloadingLocker(user, new UserShiftCommand.FinishUnloadingLocker(
                                userShift.getId(), routePoint.getId(), task.getId(), Set.of()
                        ));
                    }
                });
        if (finishCallTasks) {
            finishCallTasksAtRoutePoint(routePoint);
        }
    }

    public void finishAllOrderDeliveryTasks(RoutePoint routePoint, @Nullable OrderDeliveryTaskFailReasonType fail,
                                            boolean finishCallTasks,
                                            Map<Long, OrderPaymentType> paymentTypesByOrderId) {
        var userShift = routePoint.getUserShift();
        var user = userShift.getUser();
        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        routePoint.streamTasks(OrderDeliveryTask.class)
                .filter(task -> task.getOrderId() != null)
                .forEach(task -> {

                    OrderPaymentType orderPaymentType = paymentTypesByOrderId.get(task.getOrderId());

                    if (fail != null) {
                        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                                userShift.getId(), routePoint.getId(), task.getId(),
                                new OrderDeliveryFailReason(fail, "my comment!")
                        ));
                    } else {
                        if (!task.isPrepaidOrder()) {
                            commandService.payOrder(user, new UserShiftCommand.PayOrder(
                                    userShift.getId(), routePoint.getId(), task.getId(),
                                    orderPaymentType != null ? orderPaymentType : OrderPaymentType.CASH,
                                    null
                            ));
                        }
                        commandService.printCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                                userShift.getId(), routePoint.getId(), task.getId(),
                                helper.getChequeDto(
                                        task.isPrepaidOrder() ? OrderPaymentType.PREPAID :
                                                orderPaymentType != null ? orderPaymentType : OrderPaymentType.CASH

                                ),
                                task.getExpectedDeliveryTime(),
                                false,
                                null,
                                partialReturnOrderRepository.findByTaskOrderDelivery(task.getId())
                        ));
                    }
                });

        if (finishCallTasks) {
            finishCallTasksAtRoutePoint(routePoint);
        }
    }

    public void finishDelivery(RoutePoint routePoint, @Nullable OrderDeliveryTaskFailReasonType fail) {
        finishDelivery(routePoint, fail, OrderPaymentType.CASH, true);
    }

    @Transactional
    public void finishAllDeliveryTasks(Long userShiftId) {
        UserShift userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        finishAllDeliveryTasks(userShift);
    }

    public void finishAllDeliveryTasks(UserShift userShift) {
        userShift.streamDeliveryRoutePoints()
                .remove(rp -> rp.getStatus().isTerminal())
                .forEach(rp -> finishDelivery(rp, null));
    }

    @Transactional
    public void finishFullReturnAtEnd(Long userShiftId) {
        UserShift userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        finishFullReturnAtEnd(userShift);
    }

    public void finishFullReturnAtEnd(UserShift userShift) {
        var routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamReturnTasks().findFirst().orElseThrow();
        arriveAtRoutePoint(routePoint);
        if (task.getStatus() == OrderReturnTaskStatus.NOT_STARTED) {
            startAndFinishScanAllForReturn(userShift);
        }
        if (task.getStatus() == OrderReturnTaskStatus.AWAIT_CASH_RETURN) {
            commandService.finishReturnCash(userShift.getUser(), new UserShiftCommand.ReturnCash(userShift.getId(),
                    routePoint.getId(), task.getId()));
        }
        commandService.finishReturnTask(userShift.getUser(), new UserShiftCommand.FinishReturnTask(userShift.getId(),
                routePoint.getId(), task.getId()));
    }

    public void finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(UserShift userShift, boolean ordersAccepted) {
        List<Long> shiftOrderIds = userShift.streamDeliveryTasks()
                .remove(DeliveryTask::isFinishedSuccessfully)
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift,
                ordersAccepted ? shiftOrderIds : List.of(),
                ordersAccepted ? List.of() : shiftOrderIds);
    }

    public void finishReturnAtEndOfTheDayWithOrderWithoutArriving(UserShift userShift, boolean ordersAccepted) {
        List<Long> shiftOrderIds = userShift.streamDeliveryTasks()
                .remove(DeliveryTask::isFinishedSuccessfully)
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        startAndFinishScanOrderForReturn(userShift,
                ordersAccepted ? shiftOrderIds : List.of(),
                ordersAccepted ? List.of() : shiftOrderIds);
    }

    public void startAndFinishScanOrderForReturn(UserShift userShift, List<Long> accepted,
                                                 List<Long> skipped) {
        var routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamReturnTasks().findFirst().orElseThrow();
        commandService.startOrderReturn(userShift.getUser(),
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        routePoint.getId(),
                        task.getId()
                ));

        var returnOrdersCommand = new UserShiftCommand.FinishScan(
                userShift.getId(),
                routePoint.getId(),
                task.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(accepted)
                        .skippedOrders(skipped)
                        .finishedAt(Instant.now(clock))
                        .build()
        );
        commandService.finishReturnOrders(userShift.getUser(), returnOrdersCommand);
    }

    public void startAndFinishScanClientReturnsForReturn(UserShift userShift, List<Long> accepted, List<Long> skipped) {
        var routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamReturnTasks().findFirst().orElseThrow();
        commandService.startOrderReturn(userShift.getUser(),
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        routePoint.getId(),
                        task.getId()
                ));

        var returnOrdersCommand = new UserShiftCommand.FinishScan(
                userShift.getId(),
                routePoint.getId(),
                task.getId(),
                ScanRequest.builder()
                        .successfullyScannedClientReturns(accepted)
                        .skippedClientReturns(skipped)
                        .finishedAt(Instant.now(clock))
                        .build()
        );
        commandService.finishReturnOrders(userShift.getUser(), returnOrdersCommand);
    }

    private void startAndFinishScanAllForReturn(UserShift userShift) {
        List<Long> shiftOrderIds = userShift.streamDeliveryTasks()
                .remove(DeliveryTask::isFinishedSuccessfully)
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<Long> shiftClientReturnIds = userShift.streamOrderDeliveryTasks()
                .remove(deliveryTask -> !deliveryTask.isFinishedSuccessfully())
                .filter(OrderDeliveryTask::isClientReturn)
                .map(OrderDeliveryTask::getClientReturnId)
                .collect(Collectors.toList());


        var routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamReturnTasks().findFirst().orElseThrow();
        commandService.startOrderReturn(userShift.getUser(),
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        routePoint.getId(),
                        task.getId()
                ));

        var returnOrdersCommand = new UserShiftCommand.FinishScan(
                userShift.getId(),
                routePoint.getId(),
                task.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(shiftOrderIds)
                        .skippedOrders(List.of())
                        .successfullyScannedClientReturns(shiftClientReturnIds)
                        .skippedClientReturns(List.of())
                        .finishedAt(Instant.now(clock))
                        .build()
        );
        commandService.finishReturnOrders(userShift.getUser(), returnOrdersCommand);
    }

    public void finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(UserShift userShift, List<Long> accepted,
                                                                       List<Long> skipped) {
        var routePoint = userShift.streamReturnRoutePoints().findFirst().orElseThrow();
        var returnTask = userShift.streamReturnRoutePoints().findFirst().orElseThrow().getOrderReturnTask();
        commandService.arriveAtRoutePoint(userShift.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        routePoint.getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        if (returnTask.getStatus() == OrderReturnTaskStatus.NOT_STARTED) {
            startAndFinishScanOrderForReturn(userShift, accepted, skipped);
        }
    }

    public void checkin(UserShift userShift) {
        shiftManager.activateUserShift(userShift);
        commandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
    }

    public void checkinAndFinishPickup(UserShift userShift) {
        checkin(userShift);
        finishPickupAtStartOfTheDay(userShift);
    }

    public void checkinAndFinishPickup(UserShift userShift, List<Long> accepted, List<Long> skipped,
                                       boolean finishLoading, boolean finishCallTask) {
        commandService.checkin(userShift.getUser(), new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(userShift.getUser(), new UserShiftCommand.Start(userShift.getId()));
        finishPickupAtStartOfTheDay(userShift, accepted, skipped, finishLoading, finishCallTask);
    }

    public void finishPickupAtStartOfTheDay(UserShift userShift) {
        finishPickupAtStartOfTheDay(userShift, true);
    }

    public void finishPickupAtStartOfTheDayWithoutFinishCallTasks(UserShift userShift) {
        finishPickupAtStartOfTheDay(userShift, true, true, false);
    }

    public void finishPickupAtStartOfTheDayAndSelectNext(UserShift userShift) {
        //TODO: выпилить после разделения UserShiftFailCancelTest
        userShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        finishPickupAtStartOfTheDay(userShift, true);
        commandService.switchOpenRoutePoint(userShift.getUser(),
                new UserShiftCommand.SwitchOpenRoutePoint(userShift.getId(),
                        userShift.streamDeliveryRoutePoints().findFirst().orElseThrow().getId()));
    }

    public void finishPickupAtStartOfTheDay(Long userShiftId, boolean ordersAccepted) {
        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();

        finishPickupAtStartOfTheDay(userShift, ordersAccepted);
    }

    public void finishPickupAtStartOfTheDay(UserShift userShift, boolean ordersAccepted) {
        //TODO: выпилить после разделения UserShiftFailCancelTest
        finishPickupAtStartOfTheDay(userShift, ordersAccepted, true);
    }

    public void finishPickupAtStartOfTheDay(Long userShiftId, boolean ordersAccepted, boolean finishLoading) {
        UserShift userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        finishPickupAtStartOfTheDay(userShift, ordersAccepted, finishLoading);
    }

    public void finishPickupAtStartOfTheDay(UserShift userShift, boolean ordersAccepted, boolean finishLoading) {
        finishPickupAtStartOfTheDay(userShift, ordersAccepted, finishLoading, true);
    }

    @Transactional
    public void finishPickupAtStartOfTheDay(
            UserShift userShift,
            boolean ordersAccepted,
            boolean finishLoading,
            boolean finishCallTasks
    ) {
        //TODO: выпилить после разделения UserShiftFailCancelTest
        userShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        List<Long> shiftOrderIds = userShift.streamDeliveryTasks()
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        finishPickupAtStartOfTheDay(userShift,
                ordersAccepted ? shiftOrderIds : List.of(),
                ordersAccepted ? List.of() : shiftOrderIds, finishLoading, finishCallTasks);

    }

    public void finishPickupAtStartOfTheDay(
            UserShift userShift,
            boolean ordersAccepted,
            boolean finishLoading,
            boolean finishCallTasks,
            Duration duration
    ) {
        userShift = userShiftRepository.findById(userShift.getId()).orElseThrow();
        List<Long> shiftOrderIds = userShift.streamDeliveryTasks()
                .map(DeliveryTask::getOrderIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        finishPickupAtStartOfTheDay(userShift,
                ordersAccepted ? shiftOrderIds : List.of(),
                ordersAccepted ? List.of() : shiftOrderIds, finishLoading, finishCallTasks, duration);

    }

    public void finishPickupAtStartOfTheDay(UserShift userShift, List<Long> accepted, List<Long> skipped) {
        finishPickupAtStartOfTheDay(userShift, accepted, skipped, true, true);
    }

    public void finishPickupAtStartOfTheDay(
            UserShift userShift, List<Long> accepted, List<Long> skipped,
            boolean finishLoading, boolean finishCallTasks
    ) {
        finishPickupAtStartOfTheDay(userShift, accepted, skipped, finishLoading, finishCallTasks, null);
    }

    public void finishPickupAtStartOfTheDay(UserShift userShift, List<Long> accepted, List<Long> skipped,
                                            boolean finishLoading, boolean finishCallTask, Duration scanDuration) {
        commandService.arriveAtRoutePoint(userShift.getUser(),
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShift.getId(),
                        userShift.streamPickupRoutePoints().findFirst().orElseThrow().getId(),
                        helper.getLocationDto(userShift.getId())
                ));
        var routePoint = userShift.streamPickupRoutePoints().findFirst().orElseThrow();
        var task = routePoint.streamPickupTasks().findFirst().orElseThrow();
        commandService.startOrderPickup(userShift.getUser(),
                new UserShiftCommand.StartScan(
                        userShift.getId(),
                        routePoint.getId(),
                        task.getId()
                ));

        var pickupOrdersCommand = new UserShiftCommand.FinishScan(
                userShift.getId(),
                routePoint.getId(),
                task.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(accepted)
                        .skippedOrders(skipped)
                        .finishedAt(routePoint.getExpectedDateTime())
                        .scanStartAt(scanDuration == null ? null : routePoint.getExpectedDateTime().minus(scanDuration))
                        .build()
        );
        commandService.pickupOrders(userShift.getUser(), pickupOrdersCommand);
        if (finishLoading) {
            commandService.finishLoading(
                    userShift.getUser(),
                    new UserShiftCommand.FinishLoading(
                            userShift.getId(),
                            routePoint.getId(),
                            task.getId()));
        }

        if (finishCallTask) {
            finishCallTasksAtRoutePoint(routePoint);
        }
    }

    public void finishCallTasksAtRoutePoint(RoutePoint routePoint) {
        UserShift userShift = routePoint.getUserShift();
        User user = userShift.getUser();
        for (CallToRecipientTask callTask : routePoint.getUnfinishedCallTasks()) {
            commandService.successAttemptCall(
                    user,
                    new UserShiftCommand.AttemptCallToRecipient(
                            userShift.getId(),
                            routePoint.getId(),
                            callTask.getId(),
                            ""
                    )
            );
        }

    }

    public void finishUserShift(Long userShiftId) {
        commandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));
    }

    public void finishUserShift(UserShift userShift) {
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
    }

    /**
     * USER CREATION BELOW
     */

    public User getOrCreateUser(UserGenerateParam param) {
        return findByUid(param.userId).orElseGet(() -> createUser(param));
    }

    private User createUser(long uid, LocalDate date) {
        return createUser(uid, date, UserScheduleType.ALWAYS_WORKS,
                RelativeTimeInterval.valueOf("09:00-19:00"), SortingCenter.DEFAULT_SC_ID, DEFAULT_COMPANY_NAME);
    }

    @Transactional
    public User createUserWithoutSchedule(long uid) {
        return createUserWithoutSchedule(uid, DEFAULT_COMPANY_NAME);
    }

    public User createUserWithoutSchedule(long uid, String companyName) {
        Company company = findOrCreateCompany(companyName);
        return createUserWithoutSchedule(uid, company);
    }

    public User createUserWithoutSchedule(long uid, Company company) {
        return UserUtil.createUserWithoutSchedule(uid, transportTypeRepository.getDefault(), company);
    }

    public User createUserWithoutSchedule(UserGenerateParam param) {
        Company company = findOrCreateCompany(param.company);
        return UserUtil.createUserWithoutSchedule(param.userId, transportTypeRepository.getDefault(), company,
                param.getFirstName(), param.getSecondName(), param.getEmail(), param.yaProId);
    }

    public User createUserWithTransportTags(long uid, List<String> transportTags) {
        Company company = findOrCreateCompany(DEFAULT_COMPANY_NAME);
        Set<String> existsTags = routingOrderTagRepository.findByNameIn(Set.copyOf(transportTags))
                .stream()
                .map(RoutingOrderTag::getName)
                .collect(Collectors.toSet());

        Set<RoutingOrderTag> routingOrderTags = StreamEx.of(transportTags)
                .remove(existsTags::contains)
                .map(tagName -> new RoutingOrderTag(
                        tagName,
                        tagName,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        RoutingOrderTagType.ORDER_TYPE,
                        Set.of()
                ))
                .toSet();

        routingOrderTagRepository.saveAll(routingOrderTags);

        TransportType transportType = transportTypeRepository.save(
                new TransportType("Машинка", BigDecimal.TEN,
                        routingOrderTagRepository.findByNameIn(Set.copyOf(transportTags)),
                        100, RoutingVehicleType.COMMON, 0,
                        null)
        );
        User userWithoutSchedule = UserUtil.createUserWithoutSchedule(uid, transportType, company);
        return userRepository.save(userWithoutSchedule);
    }

    public User createUser(UserGenerateParam param) {
        User userWithoutSchedule = createUserWithoutSchedule(param);

        User user = userRepository.save(userWithoutSchedule);
        userPropertyService.addPropertyToUser(user, UserProperties.FEATURE_ALLOW_SWITCH, true);
        userPropertyService.addPropertyToUser(user,
                UserProperties.TRAVEL_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.TRAVEL_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));

        UserScheduleRule rule = new UserScheduleRule();
        SortingCenter sc = new SortingCenter();
        sc.setId(param.sortingCenter.id);
        sc.setRegionId(param.sortingCenter.regionId);
        rule.init(user,
                param.scheduleType,
                sc,
                param.workdate,
                param.scheduleType.getMetaType() == UserScheduleMetaType.BASIC ? null : param.workdate,
                param.workdate,
                new UserScheduleData(CourierVehicleType.NONE, param.localTimeInterval),
                UserScheduleStatus.READY,
                UserScheduleType.ALWAYS_WORKS.getMaskWorkDays());

        scheduleRuleRepository.save(rule);
        return user;
    }

    private User createUser(long uid, LocalDate activeFrom, UserScheduleType scheduleType,
                            RelativeTimeInterval schedule, long scId, String companyName) {
        Company company = findOrCreateCompany(companyName);
        return createUser(uid, activeFrom, scheduleType, schedule, scId, company);
    }

    private User createUser(long uid, LocalDate activeFrom, UserScheduleType scheduleType,
                            RelativeTimeInterval schedule, long scId, Company company) {
        User userWithoutSchedule = createUserWithoutSchedule(uid, company);
        User user = userRepository.save(userWithoutSchedule);
        user = userRepository.findByIdOrThrow(user.getId());
        userPropertyService.addPropertyToUser(user, UserProperties.FEATURE_ALLOW_SWITCH, true);
        userPropertyService.addPropertyToUser(user,
                UserProperties.TRAVEL_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.TRAVEL_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));
        userPropertyService.addPropertyToUser(user,
                UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_CAR, new BigDecimal(3L));

        UserScheduleRule rule = new UserScheduleRule();
        SortingCenter sc = new SortingCenter();
        sc.setId(scId);
        rule.init(user, scheduleType, sc, activeFrom,
                scheduleType.getMetaType() == UserScheduleMetaType.BASIC ? null : activeFrom,
                activeFrom, new UserScheduleData(CourierVehicleType.NONE, schedule),
                UserScheduleStatus.READY,
                scheduleType.getMaskWorkDays()
        );

        scheduleRuleRepository.save(rule);
        return user;
    }

    public void doCollectDropshipTask(CollectDropshipTask collectDropshipTask, UserShift userShift) {
        User user = userShift.getUser();
        arriveAtRoutePoint(collectDropshipTask.getRoutePoint());
        UserShiftCommand.CollectDropships command = new UserShiftCommand.CollectDropships(userShift.getId(),
                collectDropshipTask.getRoutePoint().getId(), collectDropshipTask.getId());
        commandService.collectDropships(user, command);
    }

    public User findOrCreateUser(long uid) {
        return findByUid(uid).orElseGet(() -> createUser(uid, LocalDate.now(clock)));
    }

    public User findOrCreateUser(long uid, LocalDate workday) {
        return findByUid(uid).orElseGet(() -> createUser(uid, workday, UserScheduleType.SIX_ONE,
                RelativeTimeInterval.valueOf("09:00-19:00"), SortingCenter.DEFAULT_SC_ID, DEFAULT_COMPANY_NAME)
        );
    }

    public User findOrCreateUser(long uid, LocalDate workday, String companyName) {
        return findByUid(uid).orElseGet(() -> createUser(uid, workday, UserScheduleType.SIX_ONE,
                RelativeTimeInterval.valueOf("09:00-19:00"), SortingCenter.DEFAULT_SC_ID, companyName)
        );
    }

    public User findOrCreateUserForSc(long uid, LocalDate workday, long scId) {
        return findByUid(uid).orElseGet(() -> createUser(uid, workday, UserScheduleType.SIX_ONE,
                RelativeTimeInterval.valueOf("09:00-19:00"), scId, DEFAULT_COMPANY_NAME)
        );
    }

    public User findOrCreateUser(long uid, LocalDate workday, RelativeTimeInterval scheduleTime) {
        return findByUid(uid).orElseGet(() -> createUser(uid, workday, UserScheduleType.SIX_ONE, scheduleTime,
                SortingCenter.DEFAULT_SC_ID, DEFAULT_COMPANY_NAME)
        );
    }

    public User findOrCreateUserWithoutSchedule(long uid) {
        return findOrCreateUserWithoutSchedule(uid, DEFAULT_COMPANY_NAME);
    }

    public User findOrCreateUserWithoutSchedule(long uid, String companyName) {
        return findByUid(uid).orElseGet(() -> userRepository.save(createUserWithoutSchedule(uid, companyName)));
    }

    public User findOrCreateUserWithoutSchedule(long uid, Company company) {
        return findByUid(uid).orElseGet(() -> userRepository.save(createUserWithoutSchedule(uid, company)));
    }

    @Transactional
    public Shift findOrCreateOpenShift(LocalDate date) {
        long scId = SortingCenter.DEFAULT_SC_ID;
        return findOrCreateOpenShiftForSc(date, scId);
    }

    public Shift findOrCreateOpenShiftForSc(LocalDate date, long scId) {
        return findOrCreateShiftForScWithStatus(date, scId, ShiftStatus.OPEN);
    }

    public Shift findOrCreateShiftForScWithStatus(LocalDate date, long scId, ShiftStatus status) {
        return shiftRepository.findByShiftDateAndSortingCenterId(date, scId)
                .orElseGet(() -> {
                    var shift = new Shift();
                    var sc = sortingCenter(scId);
                    shift.init(date, sc);
                    shift.setStatus(status);
                    return shiftRepository.save(shift);
                });
    }

    public SortingCenter sortingCenter(long id) {
        var sc = scRepository.findById(id)
                .orElseGet(() -> scRepository.save(SortingCenterUtil.sortingCenter(id)));
        if (sc.getToken() == null) {
            sc.setToken("SC token " + id);
        }
        return sc;
    }

    public SortingCenter sortingCenter(long id, int zoneOffset) {
        return scRepository.findById(id)
                .orElseGet(() -> scRepository.save(SortingCenterUtil.sortingCenter(id, zoneOffset)));
    }

    public SortingCenter sortingCenter(long id, Set<Company> companies) {
        return scRepository.findById(id)
                .orElseGet(() -> scRepository.save(SortingCenterUtil.sortingCenter(id, companies)));
    }

    public SortingCenter sortingCenterWithDs(long id, long dsId) {
        return scRepository.findById(id)
                .orElseGet(() -> scRepository.save(
                        SortingCenterUtil.sortingCenter(id,
                                dsRepository.findByIdOrThrow(dsId))
                ));
    }

    private Optional<User> findByUid(long uid) {
        return userRepository.findByUid(uid);
    }

    public void openShift(User user, long userShiftId) {
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
    }

    public void openShiftAndFinishPickupAtStartOfDay(User user, Long userShiftId) {
        openShift(user, userShiftId);
        finishPickupAtStartOfTheDay(userShiftId, true);
    }

    public DeliveryTask addDeliveryTaskToShift(User user, UserShift userShift, Order order) {
        OrderDelivery orderDelivery = order.getDelivery();
        boolean orderCanBeLeavedAtReception = receptionService.isReceptionPresentAt(
                orderDelivery.getDeliveryAddress().getGeoPoint()
        );

        boolean isPrepaid = order.isPrepaid();
        NewDeliveryRoutePointData prepaidTask = helper.taskPrepaid(
                "WOW SUCH ADDRESS",
                12, order.getId(),
                orderCanBeLeavedAtReception
        );
        NewDeliveryRoutePointData unpaidTask = helper.taskUnpaid(
                "WOW SUCH ADDRESS",
                12,
                order.getId(),
                orderCanBeLeavedAtReception
        );


        return commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                isPrepaid ? prepaidTask : unpaidTask,
                SimpleStrategies.NO_MERGE,
                GeoPoint.GEO_POINT_SCALE
        ));
    }

    public CallToRecipientTask addCallTask(
            UserShiftCommand.CreateCallToRecipientTask command,
            UserShift userShift,
            OrderDeliveryTask task,
            CallToRecipientTaskStatus status
    ) {
        return commandService.createCallToRecipientTask(
                command,
                userShift,
                task,
                status,
                clock
        );
    }

    public FlowTaskEntity addLockerInventoryTask(User user, UserShift userShift, PickupPoint pickupPoint) {
        var specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .type(SpecialRequestType.LOCKER_INVENTORY)
                        .pickupPointId(pickupPoint.getId())
                        .build()
        );

        return userShiftCommandService.addFlowTask(user, new UserShiftCommand.AddFlowTask(
                userShift.getId(),
                TaskFlowType.LOCKER_INVENTORY,
                NewCommonRoutePointData.builder()
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .withLogisticRequests(List.of(specialRequest))
                        .address(new RoutePointAddress("some_address", GeoPointGenerator.generateLonLat()))
                        .expectedArrivalTime(Instant.now(clock))
                        .name("Тест инвентаризации постамата")
                        .pickupPointId(pickupPoint.getId())
                        .build()
        ));
    }

    public AddDeliveryTaskFactory createDeliveryTaskFactory(User user, UserShift userShift, Order order) {
        return new AddDeliveryTaskFactory(user, userShift, order);
    }

    public LockerDeliveryTask addLockerDeliveryTaskToShift(User user, UserShift userShift, Order order) {
        NewDeliveryRoutePointData taskData = NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(12, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(12, clock))
                .name("Доставка в постамат")
                .address(new RoutePointAddress(
                        "ул. Пушкина, дом Колотушкина", GeoPointGenerator.generateLonLat()
                ))
                .orderReference(new OrderReference(
                        order.getId(),
                        order.getPlaces(),
                        OrderPaymentType.PREPAID,
                        OrderPaymentStatus.PAID,
                        true
                ))
                .type(RoutePointType.LOCKER_DELIVERY)
                .pickupPointId(Optional.ofNullable(order.getPickupPoint()).map(BaseJpaEntity.LongGen::getId)
                        .orElse(null))
                .updateSc(true)
                .build();


        return (LockerDeliveryTask) commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                taskData,
                SimpleStrategies.NO_MERGE,
                GeoPoint.GEO_POINT_SCALE
        ));
    }

    public Company findOrCreateCompany(String name) {
        return companyRepository.findCompanyByName(name)
                .orElseGet(() -> createCompany(
                        Set.of(sortingCenter(SortingCenter.DEFAULT_SC_ID)),
                        Company.DEFAULT_CAMPAIGN_ID,
                        name,
                        null));
    }

    public Company findOrCreateCompany(CompanyGenerateParam param) {
        return companyRepository.findCompanyByName(param.companyName)
                .orElseGet(() -> createCompany(
                        param.sortingCenterIds.stream().map(this::sortingCenter).collect(Collectors.toSet()),
                        param.campaignId,
                        param.companyName,
                        param.login,
                        param.businessId));
    }

    public Company findOrCreateCompany(String name, String login) {
        return companyRepository.findCompanyByName(name)
                .orElseGet(() -> createCompany(
                        Set.of(sortingCenter(SortingCenter.DEFAULT_SC_ID)),
                        Company.DEFAULT_CAMPAIGN_ID,
                        name,
                        login));
    }

    public Company addCouriersToCompany(String name, Set<User> couriers) {
        Company company = companyRepository.findCompanyByName(name).orElseThrow();
        company.getUsers().addAll(couriers);
        return company;
    }

    public Company findOrCreateSuperCompany() {
        return findOrCreateSuperCompany(DEFAULT_CAMPAIGN_ID, null);

    }

    public Company findOrCreateSuperCompany(Long campaignId, String login) {
        Set<SortingCenter> sc = new HashSet<>();
        sc.add(sortingCenter(SortingCenter.DEFAULT_SC_ID));
        return companyRepository.findCompanyByCampaignId(campaignId)
                .orElseGet(() -> createCompany(
                        sc,
                        campaignId,
                        "SuperCompany",
                        login,
                        true,
                        null,
                        null));
    }

    private Company createCompany(Set<SortingCenter> sortingCenters, Long campaignId, String name, String login,
                                  @Nullable Long businessId) {
        return createCompany(sortingCenters, campaignId, name, login, false, businessId, null);
    }

    public Company createCompany(Set<SortingCenter> sortingCenters, Long campaignId, String name, String login) {
        CompanyRole companyRole = companyRoleRepository.findByName(CompanyRoleEnum.PARTNER).orElseThrow();
        return createCompany(sortingCenters, campaignId, name, login, false, null, companyRole);
    }

    private Company createCompany(
            Set<SortingCenter> sortingCenters,
            Long campaignId,
            String name,
            String login,
            boolean isSuperCompany,
            @Nullable Long businessId,
            CompanyRole companyRole
    ) {
        Company company = new Company();
        company.setCampaignId(campaignId);
        company.setName(name);
        company.setLogin(login == null ? Optional.ofNullable(name).orElse("stasiyan") + "-yndx@yandex.ru" : login);
        company.setPhoneNumber("89175704071");
        company.setTaxpayerNumber("12345678901");
        company.setJuridicalAddress("г. Москва, Каширское шоссе, д. 31");
        company.setNaturalAddress("г. Москва, ул. Льва Толстого, д. 16");
        company.setDeactivated(false);
        company.setSortingCenters(sortingCenters);
        company.setSuperCompany(isSuperCompany);
        company.setType(CompanyType.SUPPLY);
        company.setBusinessId(businessId);
        company.setCompanyRole(companyRole);
        company.setDsmExternalId(name + "-test-employer-id");
        companyRepository.save(company);
        return company;
    }

    public Transport findOrCreateTransport() {
        return findOrCreateTransport(DEFAULT_TRANSPORT_NAME, DEFAULT_COMPANY_NAME);
    }

    public Transport findOrCreateTransport(String transportName, String companyName) {
        Company company = findOrCreateCompany(companyName);
        return transportRepository.findByNameAndCompanyId(transportName, company.getId()).orElseGet(() -> createTransport(transportName, company));
    }

    public Transport createTransport(String transportName, Company company) {
        TransportType transportType =
                transportTypeRepository.findByNameAndCompanyId(transportName, company.getId()).orElseGet(() -> transportTypeRepository.save(
                        new TransportType(
                                "Машинка",
                                BigDecimal.TEN,
                                Set.of(),
                                100,
                                RoutingVehicleType.COMMON,
                                0,
                                company
                        )
                ));

        Transport transport = new Transport();
        transport.setCompany(company);
        transport.setTransportType(transportType);
        transport.setName(transportName);
        transport.setNumber("в921сн");
        transport.setBrand("ВАЗ");
        transport.setModel("2114");
        return transportRepository.save(transport);
    }

    public void clearUserPropertiesCache() {
        cacheManager.getCache("userPropertiesCache").clear();
    }

    @Getter
    @Builder(toBuilder = true)
    public static class UserGenerateParam {

        public static final Long DEFAULT_USER_ID = 1000L;

        @Builder.Default
        private final Long userId = DEFAULT_USER_ID;

        @Nullable
        private final String yaProId;

        @NonNull
        private final LocalDate workdate;

        @Nullable
        private final String firstName;

        @Nullable
        private final String secondName;

        @Nullable
        private final String email;

        @Builder.Default
        private final UserScheduleType scheduleType = UserScheduleType.SIX_ONE;

        @Builder.Default
        private final RelativeTimeInterval localTimeInterval = RelativeTimeInterval.valueOf("09:00-19:00");

        @Builder.Default
        private final SortCenterGenerateParam sortingCenter = SortCenterGenerateParam.builder().build();

        @Builder.Default
        private final CompanyGenerateParam company = CompanyGenerateParam.builder().build();

    }

    @Getter
    @Builder(toBuilder = true)
    public static class CompanyGenerateParam {
        @Builder.Default
        private final String companyName = DEFAULT_COMPANY_NAME;

        @Builder.Default
        private final Set<Long> sortingCenterIds = Set.of(SortingCenter.DEFAULT_SC_ID);

        @Builder.Default
        private final long campaignId = DEFAULT_CAMPAIGN_ID;

        @Builder.Default
        private final String login = null;

        @Builder.Default
        private final Long businessId = null;
    }

    @Getter
    @Builder(toBuilder = true)
    public static class SortCenterGenerateParam {

        public static final Long DEFAULT_REGION_ID = 213L;

        @Builder.Default
        private final Long id = SortingCenter.DEFAULT_SC_ID;

        @Builder.Default
        private final Long regionId = DEFAULT_REGION_ID;
    }

    @Accessors(chain = true)
    public class AddDeliveryTaskFactory {

        private final User user;
        private final UserShift userShift;
        private final Order order;

        public AddDeliveryTaskFactory(User user, UserShift userShift, Order order) {
            this.user = user;
            this.userShift = userShift;
            this.order = order;
        }

        public void create() {
            addDeliveryTaskToShift(user, userShift, order);
        }
    }

}
