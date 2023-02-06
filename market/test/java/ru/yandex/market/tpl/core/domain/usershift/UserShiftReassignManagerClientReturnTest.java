package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.LocalDateTimeReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CLIENT_RETURN_NEW_REOPEN_ENABLED;

@RequiredArgsConstructor
public class UserShiftReassignManagerClientReturnTest extends TplAbstractTest {

    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnCommandService clientReturnCommandService;
    private final ClientReturnRepository clientReturnRepository;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TransactionTemplate transactionTemplate;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final UserShiftRepository userShiftRepository;
    private final Clock clock;

    Shift shift;
    User userA;
    User userB;
    Order order;
    Order orderSortingCenterCreated;
    Long deliveryServiceId;
    ClientReturn clientReturnA;
    ClientReturn clientReturnTwo;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        userA = testUserHelper.findOrCreateUser(1L);
        userB = testUserHelper.findOrCreateUser(2L);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .build());
        orderSortingCenterCreated = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                .build());
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);
        deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        clientReturnA = clientReturnGenerator.generateReturnFromClient(deliveryServiceId);
        clientReturnTwo = clientReturnGenerator.generateReturnFromClient(deliveryServiceId);
    }


    @Test
    @DisplayName("Проверка, что возврат не переназначается, если отменен")
    void clientReturnNotReassigned_WhenCancelled() {
        clientReturnCommandService.cancel(
                new ClientReturnCommand.Cancel(
                        clientReturnA.getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                                "comment",
                                List.of(),
                                Source.CLIENT
                        ),
                        Source.CLIENT
                )
        );

        var clientReturns = Set.of(clientReturnA.getId(), clientReturnTwo.getId());

        //возврат отменен
        var clientReturnCanceled = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
        assertThat(clientReturnCanceled.getStatus()).isEqualTo(ClientReturnStatus.CANCELLED);

        var userShiftA = testUserHelper.createEmptyShift(userA, shift);
        userShiftReassignManager.reassignOrders(Set.of(), clientReturns, Set.of(), userA.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    //отмененная таска не переназначена
                    var orderDeliveryTask =
                            userShiftRepository.getById(userShiftA.getId()).streamOrderDeliveryTasks().collect(Collectors.toList());
                    assertThat(orderDeliveryTask).hasSize(1);
                    assertThat(orderDeliveryTask.get(0).getClientReturnId()).isEqualTo(clientReturnTwo.getId());

                    //возврат все еще отменен
                    var clientReturnReassigned = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
                    assertThat(clientReturnReassigned.getStatus()).isEqualTo(ClientReturnStatus.CANCELLED);

                    //второй возврат назнчени
                    var secondClientReturnReassigned =
                            clientReturnRepository.findById(clientReturnTwo.getId()).orElseThrow();
                    assertThat(secondClientReturnReassigned.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                }
        );
    }

    @Test
    @DisplayName("Проверка, что возврат переназначается, если был до этого назначен на другого курьера")
    void reassignClientReturn_WhenAssignedToCourrier() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(userA.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 13, clientReturnA.getId()))
                .build();
        long userShiftId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(userA, userShiftId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(userShiftId));

        //у курьера есть таска на забор клиентского возврата
        var clientReturnStartOfTheDay = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
        assertThat(clientReturnStartOfTheDay.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

        var userShiftB = testUserHelper.createEmptyShift(userB, shift);
        //переназначаем
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userB.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    //возврат названачен на курьера
                    var clientReturnAssignedToCourrier = clientReturnRepository.getById(clientReturnA.getId());
                    assertThat(clientReturnAssignedToCourrier.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

                    var oldShiftDeliveryTask =
                            userShiftRepository.getById(userShiftId).streamOrderDeliveryTasks()
                                    .filter(OrderDeliveryTask::isClientReturn)
                                    .findFirst().orElseThrow();

                    //таска на забор у прошлого курьера зафейлена
                    assertThat(oldShiftDeliveryTask.getStatus().isFailed()).isTrue();

                    var orderDeliveryTask = userShiftRepository.getById(userShiftB.getId()).streamOrderDeliveryTasks()
                            .findFirst().orElseThrow();
                    //новая таска на забор клинтского возврата у нового курьера
                    assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
                    assertThat(orderDeliveryTask.getClientReturnId()).isNotNull();

                    //запись в историю, что возврат был перенесен
                    var clientReturnReassignEvents =
                            clientReturnHistoryEventRepository.findAll().stream()
                                    .filter(evt -> Objects.equals(evt.getClientReturnId(),
                                            clientReturnStartOfTheDay.getId()))
                                    .filter(evt -> evt.getType().equals(ClientReturnHistoryEventType.CLIENT_RETURN_REASSIGNED))
                                    .collect(Collectors.toList());

                    assertThat(clientReturnReassignEvents).hasSize(1);
                }
        );
    }

    private final ClientReturnHistoryEventRepository clientReturnHistoryEventRepository;

    @Test
    @DisplayName("Проверка, что возврат переназначается, если создан и не назначен")
    void reassignClientReturn_WhenCreated() {
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var userShiftA = testUserHelper.createEmptyShift(userA, shift);
        var userShiftId = userShiftA.getId();

        //возврат создан
        var clientReturnStartOfTheDay = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
        assertThat(clientReturnStartOfTheDay.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

        //переназначаем
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userA.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    //возврат названачен на курьера
                    var clientReturnAssignedToCourrier = clientReturnRepository.getById(clientReturnA.getId());
                    assertThat(clientReturnAssignedToCourrier.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

                    var orderDeliveryTask = userShiftRepository.getById(userShiftId).streamOrderDeliveryTasks()
                            .findFirst().orElseThrow();
                    //новая таска на забор клинтского возврата у нового курьера
                    assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
                    assertThat(orderDeliveryTask.getClientReturnId()).isNotNull();
                }
        );
    }

    @Test
    @DisplayName("Проверка, что когда курьер идет сдавать заказы на сц и ему переназначается клиентский возврат," +
            "текущий рут поинт меняется на рут поинт с таской для забора клиентского возврата.")
    void reassignClientReturn_WhenOnReturnTask() {
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(userA.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskPrepaid("addr1", 15, order.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(userA, userShiftId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(userShiftId));

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var odt =
                            userShiftRepository.getById(userShiftId).streamOrderDeliveryTasks().findFirst().orElseThrow();
                    //заканчиваем доставку
                    testUserHelper.finishDelivery(odt.getRoutePoint(), false);

                    assertThat(userShiftRepository.getById(userShiftId).getCurrentRoutePoint().streamReturnTasks().count()).isEqualTo(1);
                }
        );


        //переназначаем
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userA.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var deliveryTask =
                            userShiftRepository.getById(userShiftId).getCurrentRoutePoint().streamOrderDeliveryTasks().findFirst();
                    var returnTask =
                            userShiftRepository.getById(userShiftId).getCurrentRoutePoint().streamReturnTasks().findFirst();
                    assertThat(deliveryTask).isNotEmpty();
                    assertThat(returnTask).isEmpty();
                    assertThat(deliveryTask.get().getClientReturnId()).isEqualTo(clientReturnA.getId());

                    var clientReturnReassigned = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
                    assertThat(clientReturnReassigned.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                }
        );
    }

    @Test
    @DisplayName("Проверка, что возврат переназначается, если был перенесен")
    void reassignClientReturn_WhenReschedule() {
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var userShiftB = testUserHelper.createEmptyShift(userB, shift);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(userA.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr1", 15, clientReturnA.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(userA, userShiftId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(userShiftId));

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    assertThat(clientReturnRepository.findById(clientReturnA.getId()).get().getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                    assertThat(userShiftRepository.findById(userShiftB.getId()).orElseThrow().streamOrderDeliveryTasks().findFirst()).isEmpty();
                }
        );

        clientReturnCommandService.reschedule(
                new ClientReturnCommand.Reschedule(
                        clientReturnA.getId(),
                        new LocalDateTimeReschedule(
                                clientReturnA.getArriveIntervalFrom(),
                                clientReturnA.getArriveIntervalTo(),
                                OrderDeliveryTaskFailReasonType.NO_CONTACT,
                                OrderDeliveryRescheduleReasonType.NO_CONTACT,
                                "comment",
                                Source.CLIENT,
                                clientReturnA.getArriveIntervalFrom().toInstant(ZoneOffset.UTC)
                        )
                )
        );

        assertThat(clientReturnRepository.findById(clientReturnA.getId()).get().getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userB.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    //таска переназначена в юзершифт курьера
                    var orderDeliveryTask = userShiftRepository.getById(userShiftB.getId()).streamOrderDeliveryTasks()
                            .findFirst().orElseThrow();
                    assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

                    var clientReturnReassigned = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
                    assertThat(clientReturnReassigned.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                }
        );
    }


    @Test
    @DisplayName("Проверка, что при повторном переназначении возврата, он переназначается вновь и отменяется в " +
            "прежней смене")
    void testDoubleReassign() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CLIENT_RETURN_NEW_REOPEN_ENABLED)).thenReturn(true);
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        var secondCourrierUs = testUserHelper.createEmptyShift(userB, shift);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(userA.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr1", 15, clientReturnA.getId()))
                .build();

        long firstCourrierUsId = userShiftTestHelper.start(createCommand);

        testUserHelper.openShift(userA, firstCourrierUsId);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftRepository.getById(firstCourrierUsId));

        //возврат в первой смене
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    assertThat(clientReturnRepository.findById(clientReturnA.getId()).get().getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                    assertThat(userShiftRepository.findById(secondCourrierUs.getId()).orElseThrow().streamOrderDeliveryTasks().findFirst()).isEmpty();
                }
        );

        //Переназначаем на второго курьера
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userB.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    //таска переназначена в юзершифт курьера
                    var orderDeliveryTask =
                            userShiftRepository.getById(secondCourrierUs.getId()).streamOrderDeliveryTasks()
                                    .findFirst().orElseThrow();
                    assertThat(orderDeliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

                    var orderDeliveryTasks =
                            userShiftRepository.getById(firstCourrierUsId).streamOrderDeliveryTasks().collect(Collectors.toList());

                    assertThat(orderDeliveryTasks).hasSize(1);
                    assertThat(orderDeliveryTasks.get(0).getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);

                    var clientReturnReassigned = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
                    assertThat(clientReturnReassigned.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                }
        );

        //Переназначаем обратно на первого
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userA.getId());

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    //таска переназначена в юзершифт курьера
                    var orderDeliveryTasks =
                            userShiftRepository.getById(firstCourrierUsId).streamOrderDeliveryTasks().collect(Collectors.toList());

                    assertThat(orderDeliveryTasks).hasSize(1);
                    assertThat(orderDeliveryTasks.get(0).getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

                    //В прежней смене таска зафейлена
                    var orderDeliveryTaskSecondCourrier =
                            userShiftRepository.getById(secondCourrierUs.getId()).streamOrderDeliveryTasks()
                                    .findFirst().orElseThrow();
                    assertThat(orderDeliveryTaskSecondCourrier.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);

                    var clientReturnReassigned = clientReturnRepository.findById(clientReturnA.getId()).orElseThrow();
                    assertThat(clientReturnReassigned.getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);
                }
        );
    }
}
