package ru.yandex.market.tpl.core.domain.clientreturn;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.les.dto.TplRequestIntervalDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressChangeReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressChangedEvent;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.LocalDateTimeReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.sqs.SendClientReturnEventToSqsService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.EXTRA_RESCHEDULING_ALL_SOURCES;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;

@RequiredArgsConstructor
public class ClientReturnRescheduleTest extends TplAbstractTest {


    private final UserShiftRepository repository;
    private final OrderGenerateService orderGenerateService;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftCommandService commandService;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper helper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandService userShiftCommandService;
    private final TestDataFactory testDataFactory;
    private final UserShiftTestHelper userShiftTestHelper;
    private final ClientReturnCommandService clientReturnCommandService;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;
    @MockBean
    private final SendClientReturnEventToSqsService sendClientReturnEventToSqsService;

    private ClientReturn clientReturn;
    private User user;
    private List<Instant> instantsForEvent;


    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0)));
        user = testUserHelper.findOrCreateUser(1L);
        clientReturn = clientReturnGenerator.generateReturnFromClient(198L);
        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_CLIENT_RETURN_RESCHEDULE_LIMITED, true);
        configurationServiceAdapter.insertValue(ConfigurationProperties.ENABLE_CLIENT_RETURN_RESCHEDULE_EVENT_TO_LES,
                true);
        instantsForEvent = IntStreamEx.range(0, 10)
                .boxed()
                .map(it -> Instant.now(clock).plus(it, ChronoUnit.DAYS))
                .toList();
    }

    @Test
    @DisplayName("Клиентский возврат отменяется после превышеия лимита в 3 переноса")
    void clientReturnCancelled_AfterThreeReschedules() {
        //делаем 3 отмены, которые результируют в перенос на следующий день
        OrderDeliveryTaskFailReasonType failReason = OrderDeliveryTaskFailReasonType.WRONG_COORDINATES;
        failClientReturn(instantsForEvent.get(0), failReason, user.getId());
        failClientReturn(instantsForEvent.get(1), failReason, user.getId());
        failClientReturn(instantsForEvent.get(2), failReason, user.getId());

        //подготавливаем перенос на сегодня
        Instant today = instantsForEvent.get(3);
        Interval todayIntervalForDelivery = new Interval(today,
                today.plus(2, ChronoUnit.HOURS));

        var userShift = prepareShift(LocalDate.ofInstant(today, ZoneId.systemDefault()),
                clientReturn, user);
        var userShiftId = userShift.getId();
        rescheduleClientReturn(userShift, todayIntervalForDelivery, today, user);

        //в результатте возврат подготовливается к отмене
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var clientReturnOdt = getOrderDeliveryTasksClintReturns(userShift.getId());
                    var rescheduledClientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
                    assertThat(rescheduledClientReturn.getStatus()).isEqualTo(ClientReturnStatus.PREPARED_FOR_CANCEL);
                    assertThat(clientReturnOdt).hasSize(1);
                    assertThat(clientReturnOdt.get(0).getFailReason().getType()).isEqualTo(EXTRA_RESCHEDULING_ALL_SOURCES);
                }
        );


        //окончание шифта, убедиться, что у нас закрылся возврат
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    finishUserShiftAfterRescheduling(userShiftId);
                    var cr = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
                    assertThat(cr.getStatus()).isEqualTo(ClientReturnStatus.CANCELLED);
                    var clientReturnOdt = getOrderDeliveryTasksClintReturns(userShiftId);
                    assertThat(clientReturnOdt).hasSize(1);
                    assertThat(clientReturnOdt.get(0).getFailReason().getType()).isEqualTo(EXTRA_RESCHEDULING_ALL_SOURCES);
                    assertThat(clientReturnOdt.get(0).getFinishedAt()).isEqualTo(
                            LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0))
                                    .atZone(DEFAULT_ZONE_ID)
                                    .toInstant()
                    );
                }
        );
    }

    @Test
    @DisplayName("Клиентский возврат не отменяется если не превысить лимит в 3 переноса")
    void clientReturnNotCancelled_AfterTwoReschedules() {
        //делаем 2 переноса в истории
        OrderDeliveryTaskFailReasonType failReasonType = OrderDeliveryTaskFailReasonType.WRONG_COORDINATES;
        failClientReturn(instantsForEvent.get(0), failReasonType, user.getId());
        failClientReturn(instantsForEvent.get(1), failReasonType, user.getId());

        //подготавливаем перенос на сегодня
        Instant today = instantsForEvent.get(2);
        Interval todayInterval = new Interval(today,
                today.plus(2, ChronoUnit.HOURS));

        var userShift = prepareShift(LocalDate.ofInstant(today, ZoneId.systemDefault()).plusDays(3),
                clientReturn, user);
        rescheduleClientReturn(userShift, todayInterval, Instant.now(clock).plus(2, ChronoUnit.DAYS), user);

        //возврат снова в состоянии создан, таска со статусом изменеа дата доставки
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var cr = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
                    assertThat(cr.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                    var userShiftAfterReschedule = userShiftRepository.findByIdOrThrow(userShift.getId());
                    var clientReturnOdt = getOrderDeliveryTasksClintReturns(userShiftAfterReschedule.getId());
                    assertThat(clientReturnOdt).hasSize(1);
                    assertThat(clientReturnOdt.get(0).getFailReason().getType()).isEqualTo(DELIVERY_DATE_UPDATED);
                }
        );
    }

    @Test
    @DisplayName("Клиентский возврат не отменяется, если один из превышающих лимит связан с переназначением ")
    void clientReturnNotRescheduled_AfterOneRescheduleReopenedWithThreeReschedules() {
        //делаем 2 переноса
        OrderDeliveryTaskFailReasonType failReasonType = OrderDeliveryTaskFailReasonType.WRONG_COORDINATES;
        failClientReturn(instantsForEvent.get(0), failReasonType, 1L);
        failClientReturn(instantsForEvent.get(1), failReasonType, 1L);

        //подготавливаем перенос
        Instant instant = instantsForEvent.get(2);
        var shiftDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        var shift = testUserHelper.findOrCreateOpenShift(shiftDate);
        user = testUserHelper.findOrCreateUser(2L);

        var userShift = testUserHelper.createEmptyShift(user, shift);
        var crRoutePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = crRoutePoint.getId();
        Instant deliveryTime = Instant.now(clock);
        userShiftCommandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        var user2 = testUserHelper.findOrCreateUser(3L);
        var userShift2 = testUserHelper.createEmptyShift(user2, shift);
        userShiftReassignManager.reassignOrdersV2(Set.of(), Set.of(clientReturn.getId()), Set.of(),
                user2.getId(),
                "some reason");


        //подготавливаем перенос на сегодня
        Instant now = instantsForEvent.get(1);
        Interval deliveryInterval = new Interval(now,
                now.plus(2, ChronoUnit.HOURS));
        testUserHelper.checkinAndFinishPickup(userShift2);
        rescheduleClientReturn(userShift2, deliveryInterval, now, user2);

        //возврат снова в состоянии создан, таска со статусом изменеа дата доставки
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var cr = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
                    assertThat(cr.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                    var userShiftUpdated = userShiftRepository.findByIdOrThrow(userShift2.getId());
                    var clientReturnOdt = getOrderDeliveryTasksClintReturns(userShiftUpdated.getId());
                    assertThat(clientReturnOdt.get(0).getFailReason().getType()).isEqualTo(DELIVERY_DATE_UPDATED);
                }
        );
    }

    @Test
    @DisplayName("Проверяет отправку верного ивента о переносе клиентского возврата")
    void sendEventToLes_WhenRescheduled_ByReschedule() {
        //given
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var offset = dsZoneOffsetCachingService.getOffsetForDs(clientReturn.getDeliveryServiceId());
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr1", 15, clientReturn.getId()))
                .build();
        long userShiftId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(user, userShiftId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(userShiftId));

        LocalDateTimeReschedule localDateTimeReschedule = new LocalDateTimeReschedule(
                clientReturn.getArriveIntervalFrom().plus(1, ChronoUnit.DAYS),
                clientReturn.getArriveIntervalTo().plus(1, ChronoUnit.DAYS),
                null,
                OrderDeliveryRescheduleReasonType.NO_CONTACT,
                "some comment filler",
                Source.CLIENT,
                clientReturn.getArriveIntervalFrom().plus(1, ChronoUnit.DAYS).toInstant(offset)
        );

        //when
        clientReturnCommandService.reschedule(
                new ClientReturnCommand.Reschedule(
                        clientReturn.getId(), localDateTimeReschedule)
        );

        var captor = ArgumentCaptor.forClass(TplReturnAtClientAddressChangedEvent.class);
        Mockito.verify(sendClientReturnEventToSqsService).sendSynchronously(
                Mockito.anyString(),
                Mockito.anyLong(),
                captor.capture(),
                anyString()
        );

        var expectedInterval = new TplRequestIntervalDto(
                localDateTimeReschedule.getArrivalIntervalFrom().toLocalDate(),
                localDateTimeReschedule.getArrivalIntervalFrom().toLocalTime(),
                localDateTimeReschedule.getArrivalIntervalTo().toLocalDate(),
                localDateTimeReschedule.getArrivalIntervalTo().toLocalTime()
        );

        //then
        TplReturnAtClientAddressChangedEvent captorValue = captor.getValue();
        assertThat(captorValue.getReturnId()).isEqualTo(clientReturn.getExternalReturnId());
        assertThat(captorValue.getSource()).isEqualTo(TplReturnAtClientAddressModificationSource.CLIENT);
        assertThat(captorValue.getReason()).isEqualTo(TplReturnAtClientAddressChangeReason.NO_CONTACT);
        assertThat(captorValue.getNewInterval()).isEqualTo(expectedInterval);
    }

    @Test
    @DisplayName("Проверяет, что при ошибочной причине отмены и переноса, пробрасывается ошибка")
    void throwsException_WhenWrongParamProvided() {
        //given
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var offset = dsZoneOffsetCachingService.getOffsetForDs(clientReturn.getDeliveryServiceId());
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr1", 15, clientReturn.getId()))
                .build();
        long userShiftId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(user, userShiftId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(userShiftId));

        //when
        LocalDateTimeReschedule localDateTimeRescheduleWrongFailReasonAndReschedule =
                createLocalDateTimeReschedule(offset,
                        OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                        OrderDeliveryRescheduleReasonType.DELIVERY_DELAY, Source.COURIER);

        //throws
        assertThatThrownBy(() -> clientReturnCommandService.reschedule(
                new ClientReturnCommand.Reschedule(
                        clientReturn.getId(), localDateTimeRescheduleWrongFailReasonAndReschedule)
        )).isInstanceOf(TplIllegalArgumentException.class)
                .hasMessageContaining(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED.name())
                .hasMessageContaining(OrderDeliveryRescheduleReasonType.DELIVERY_DELAY.name());

        //when
        LocalDateTimeReschedule localDateTimeRescheduleWrongSource = createLocalDateTimeReschedule(offset, null,
                OrderDeliveryRescheduleReasonType.NO_CONTACT, Source.SORT_CENTER);

        //throws
        assertThatThrownBy(() -> clientReturnCommandService.reschedule(
                new ClientReturnCommand.Reschedule(
                        clientReturn.getId(), localDateTimeRescheduleWrongSource)
        )).isInstanceOf(TplIllegalArgumentException.class).hasMessageContaining(Source.SORT_CENTER.name());
    }

    @Test
    @DisplayName("Проверяет отправку верного ивента о переносе клиентского возврата при отмене")
    void sendEventToLes_WhenRescheduled_ByCancel() {
        //given
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var offset = dsZoneOffsetCachingService.getOffsetForDs(clientReturn.getDeliveryServiceId());
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr1", 15, clientReturn.getId()))
                .build();
        long userShiftId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(user, userShiftId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(userShiftId));

        LocalDateTimeReschedule localDateTimeReschedule = new LocalDateTimeReschedule(
                clientReturn.getArriveIntervalFrom().plus(1, ChronoUnit.DAYS),
                clientReturn.getArriveIntervalTo().plus(1, ChronoUnit.DAYS),
                OrderDeliveryTaskFailReasonType.WRONG_COORDINATES,
                OrderDeliveryRescheduleReasonType.COORDINATES_UPDATED,
                "some comment filler",
                Source.CLIENT,
                clientReturn.getArriveIntervalFrom().plus(1, ChronoUnit.DAYS).toInstant(offset)
        );

        //when
        clientReturnCommandService.reschedule(
                new ClientReturnCommand.Reschedule(
                        clientReturn.getId(), localDateTimeReschedule)
        );

        var captor = ArgumentCaptor.forClass(TplReturnAtClientAddressChangedEvent.class);
        Mockito.verify(sendClientReturnEventToSqsService).sendSynchronously(
                Mockito.anyString(),
                Mockito.anyLong(),
                captor.capture(),
                anyString()
        );

        var expectedInterval = new TplRequestIntervalDto(
                localDateTimeReschedule.getArrivalIntervalFrom().toLocalDate(),
                localDateTimeReschedule.getArrivalIntervalFrom().toLocalTime(),
                localDateTimeReschedule.getArrivalIntervalTo().toLocalDate(),
                localDateTimeReschedule.getArrivalIntervalTo().toLocalTime()
        );

        //then
        TplReturnAtClientAddressChangedEvent captorValue = captor.getValue();
        assertThat(captorValue.getReturnId()).isEqualTo(clientReturn.getExternalReturnId());
        assertThat(captorValue.getSource()).isEqualTo(TplReturnAtClientAddressModificationSource.CLIENT);
        assertThat(captorValue.getReason()).isEqualTo(TplReturnAtClientAddressChangeReason.WRONG_ADDRESS);
        assertThat(captorValue.getNewInterval()).isEqualTo(expectedInterval);
    }


    private LocalDateTimeReschedule createLocalDateTimeReschedule(ZoneOffset offset,
                                                                  OrderDeliveryTaskFailReasonType failReasonType,
                                                                  OrderDeliveryRescheduleReasonType rescheduleReasonType,
                                                                  Source source) {
        return new LocalDateTimeReschedule(
                clientReturn.getArriveIntervalFrom().plus(1, ChronoUnit.DAYS),
                clientReturn.getArriveIntervalTo().plus(1, ChronoUnit.DAYS),
                failReasonType,
                rescheduleReasonType,
                "some comment filler",
                source,
                clientReturn.getArriveIntervalFrom().plus(1, ChronoUnit.DAYS).toInstant(offset)
        );
    }

    private void finishUserShiftAfterRescheduling(Long userShiftId) {
        var orderOdt = getOrderDeliveryTasksOrders(userShiftId);
        assertThat(orderOdt).hasSize(1);
        var task = orderOdt.get(0);

        userShiftCommandService.forceSwitchToNextRoutePoint(new UserShiftCommand.ForceSwitchToNextRoutePoint(
                userShiftId, List.of()
        ));
        userShiftCommandService.finishOrderDeliveryTask(user,
                UserShiftCommand.FinishOrderDeliveryTask.builder()
                        .taskId(task.getId())
                        .userShiftId(userShiftId)
                        .finishedAt(Instant.ofEpochMilli(1644454800))
                        .build());
        testUserHelper.finishFullReturnAtEnd(userShiftId);
        testUserHelper.finishUserShift(userShiftId);
    }

    private List<OrderDeliveryTask> getOrderDeliveryTasksOrders(Long userShiftId) {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        return userShift.streamOrderDeliveryTasks().remove(OrderDeliveryTask::isClientReturn).collect(Collectors.toList());
    }

    private List<OrderDeliveryTask> getOrderDeliveryTasksClintReturns(Long userShiftId) {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        return userShift.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).collect(Collectors.toList());
    }

    private void rescheduleClientReturn(UserShift us, Interval deliveryInterval, Instant deliveryIntervalInstant,
                                        User user) {
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var tempUs = userShiftRepository.findById(us.getId()).orElseThrow();
                    var task =
                            tempUs.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).findFirst().orElseThrow();
                    var rp = task.getRoutePoint();

                    userShiftCommandService.rescheduleDeliveryTask(user,
                            new UserShiftCommand.RescheduleOrderDeliveryTask(
                                    us.getId(), rp.getId(), task.getId(),
                                    new DeliveryReschedule(deliveryInterval,
                                            ORDER_NOT_ACCEPTED,
                                            OrderDeliveryRescheduleReasonType.NO_CONTACT, "some comment",
                                            Source.COURIER, deliveryInterval.getStart()), deliveryIntervalInstant,
                                    us.getZoneId())
                    );
                }
        );
    }

    private void failClientReturn(Instant instant, OrderDeliveryTaskFailReasonType reason, Long userId) {
        var shiftDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        OrderDeliveryFailReason failReason = new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(reason, "comment", Source.COURIER),
                Source.COURIER
        );
        var user = testUserHelper.findOrCreateUser(userId);
        var shift = testUserHelper.findOrCreateOpenShift(shiftDate);
        var userShift = testUserHelper.createEmptyShift(user, shift);
        var crRoutePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = crRoutePoint.getId();

        var tod = userShiftCommandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), instant
                )
        );
        testUserHelper.openShiftAndFinishPickupAtStartOfDay(user, userShift.getId());
        commandService.failDeliveryTask(user,
                new UserShiftCommand.FailOrderDeliveryTask(userShift.getId(), routePointId, tod.getId(),
                        failReason));
    }

    private UserShift prepareShift(LocalDate date, ClientReturn clientReturn, User user) {
        Shift shift = testUserHelper.findOrCreateOpenShift(date);
        var order = orderGenerateService.createOrder();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.clientReturn("addr1", 15, clientReturn.getId()))
                .routePoint(helper.taskUnpaid("Add2", 17, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        UserShift userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        return userShift;
    }
}
