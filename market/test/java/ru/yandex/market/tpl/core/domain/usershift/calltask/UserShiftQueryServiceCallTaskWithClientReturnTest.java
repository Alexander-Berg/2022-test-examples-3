package ru.yandex.market.tpl.core.domain.usershift.calltask;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.call.CallTaskSummaryDto;
import ru.yandex.market.tpl.api.model.task.call.CallTasksDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ClientReturnReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@RequiredArgsConstructor
public class UserShiftQueryServiceCallTaskWithClientReturnTest extends TplAbstractTest {

    private final ClientReturnGenerator clientReturnGenerator;
    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserPropertyService userPropertyService;
    private final UserShiftRepository repository;
    private final UserShiftQueryService userShiftQueryService;
    private final Clock clock;
    private final static long YANDEX_BUYER_ID = 1L;
    private final static String INTERVAL_9_TO_18 = "09:00-18:00";
    private final static String PHONE = "+79050000401";

    private User user;
    private Shift shift;
    private ClientReturn clientReturn;
    private Order order;
    private RoutePointAddress myAddress;
    private GeoPoint geoPoint;

    @BeforeEach
    void init() {
        geoPoint = GeoPointGenerator.generateLonLat();
        myAddress = new RoutePointAddress("my_address", geoPoint);

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        clientReturn =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(PHONE)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0)))
                        .build());

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_9_TO_18))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .recipientPhone(PHONE)
                .buyerYandexUid(YANDEX_BUYER_ID)
                .build());

        transactionTemplate.executeWithoutResult(cmd -> {
            userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });
    }


    @Test
    @DisplayName("Проверка, что возврат и заказ в мульте правильно маппятся")
    public void clientReturnAndOrderReturnCallTask() {
        var clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        var deliveryMerge = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order, false, false)
                .build();
        var crMerge = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturn.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryMerge)
                .routePoint(crMerge)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);

        transactionTemplate.executeWithoutResult(cmd -> {
            UserShift userShift = repository.findById(id).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            Hibernate.initialize(userShift.streamCallTasks().toList());
            List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
            assertThat(callToRecipientTasks).hasSize(1);
        });

        CallTasksDto callTasks = userShiftQueryService.getCallTasks(user, Boolean.FALSE);

        assertThat(callTasks.getTasks()).hasSize(1);
        var task = callTasks.getTasks().get(0);

        assertThat(task.getExternalReturnIds()).hasSize(1);
        assertThat(task.getExternalOrderIds()).hasSize(1);

        assertThat(task.getExternalReturnIds().get(0)).isEqualTo(clientReturn.getExternalReturnId());
        assertThat(task.getExternalOrderIds().get(0)).isEqualTo(order.getExternalOrderId());
    }

    @Test
    @DisplayName("Проверка, что раздельные возвраты правильно маппятся")
    public void separateClientReturnsReturnCallTask() {
        var clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();
        ClientReturn anotherClientReturn = clientReturnGenerator.generateReturnFromClient();
        var anotherCrDelivery = helper.clientReturn("addr1", 14, anotherClientReturn.getId());
        var crMerge = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturn.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(crMerge)
                .routePoint(anotherCrDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);

        transactionTemplate.executeWithoutResult(cmd -> {
            UserShift userShift = repository.findById(id).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            Hibernate.initialize(userShift.streamCallTasks().toList());
            List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
            assertThat(callToRecipientTasks).hasSize(2);
        });

        CallTasksDto callTasks = userShiftQueryService.getCallTasks(user, Boolean.FALSE);

        assertThat(callTasks.getTasks()).hasSize(2);

        List<String> crExternalIds =
                callTasks.getTasks().stream().flatMap(t -> t.getExternalReturnIds().stream()).collect(Collectors.toList());

        assertThat(crExternalIds).containsExactlyInAnyOrder(
                clientReturn.getExternalReturnId(),
                anotherClientReturn.getExternalReturnId()
        );

        List<String> orderExternalIds =
                callTasks.getTasks().stream().flatMap(t -> t.getExternalOrderIds().stream()).collect(Collectors.toList());

        assertThat(orderExternalIds).hasSize(0);

        CallTaskSummaryDto callTaskSummaryDto = callTasks.getTasks().get(0);

        assertThat(callTaskSummaryDto.getRecipientPhone()).isNotNull();
        assertThat(callTaskSummaryDto.getRecipientFullName()).isNotNull();
    }

    @Test
    @DisplayName("Проверка, что раздельные возвра и заказ правильно маппятся")
    public void separateClientReturnSeparateOrderReturnCallTask() {
        var clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();
        ClientReturn anotherClientReturn = clientReturnGenerator.generateReturnFromClient();
        var anotherCrDelivery = helper.clientReturn("addr1", 14, anotherClientReturn.getId());

        var deliveryMerge = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryMerge)
                .routePoint(anotherCrDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);

        transactionTemplate.executeWithoutResult(cmd -> {
            UserShift userShift = repository.findById(id).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            Hibernate.initialize(userShift.streamCallTasks().toList());
            List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
            assertThat(callToRecipientTasks).hasSize(2);
        });

        CallTasksDto callTasks = userShiftQueryService.getCallTasks(user, Boolean.FALSE);

        assertThat(callTasks.getTasks()).hasSize(2);

        List<String> crExternalIds =
                callTasks.getTasks().stream().flatMap(t -> t.getExternalReturnIds().stream()).collect(Collectors.toList());

        assertThat(crExternalIds).containsExactlyInAnyOrder(
                anotherClientReturn.getExternalReturnId()
        );

        List<String> orderExternalIds =
                callTasks.getTasks().stream().flatMap(t -> t.getExternalOrderIds().stream()).collect(Collectors.toList());

        assertThat(orderExternalIds).containsExactlyInAnyOrder(order.getExternalOrderId());

        CallTaskSummaryDto callTaskSummaryDto =
                callTasks.getTasks().stream().filter(t -> t.getExternalOrderIds().isEmpty()).findFirst().orElseThrow();

        assertThat(callTaskSummaryDto.getRecipientPhone()).isNotNull();
        assertThat(callTaskSummaryDto.getRecipientFullName()).isNotNull();
    }

    @Test
    @DisplayName("Проверка, что мульт из возвратов правильно маппится")
    public void multiClientReturnReturnDto() {
        var clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        ClientReturn anotherClientReturn =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(PHONE)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0)))
                        .build());

        var clientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturn.getId()).build())
                .build();

        var anotherClientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(anotherClientReturn.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(clientReturnDelivery)
                .routePoint(anotherClientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);

        transactionTemplate.executeWithoutResult(cmd -> {
            UserShift userShift = repository.findById(id).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            Hibernate.initialize(userShift.streamCallTasks().toList());
            List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
            assertThat(callToRecipientTasks).hasSize(1);
        });

        CallTasksDto callTasks = userShiftQueryService.getCallTasks(user, Boolean.FALSE);

        assertThat(callTasks.getTasks()).hasSize(1);

        List<String> crExternalIds = callTasks.getTasks().get(0).getExternalReturnIds();

        assertThat(crExternalIds).containsExactlyInAnyOrder(
                anotherClientReturn.getExternalReturnId(),
                clientReturn.getExternalReturnId()
        );

        CallTaskSummaryDto callTaskSummaryDto =
                callTasks.getTasks().stream().filter(t -> t.getExternalOrderIds().isEmpty()).findFirst().orElseThrow();

        assertThat(callTaskSummaryDto.getRecipientPhone()).isNotNull();
        assertThat(callTaskSummaryDto.getRecipientFullName()).isNotNull();
    }

    @Test
    @DisplayName("Проверка, что мульт из заказов правильно маппится")
    public void multiOrderReturnCallDto() {
        var clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        Order anotherOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_9_TO_18))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .recipientPhone(PHONE)
                .buyerYandexUid(YANDEX_BUYER_ID)
                .build());

        var orderDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order, false, false)
                .build();

        var anotherOrderDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(anotherOrder, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(orderDelivery)
                .routePoint(anotherOrderDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long id = commandService.createUserShift(createCommand);

        transactionTemplate.executeWithoutResult(cmd -> {
            UserShift userShift = repository.findById(id).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            Hibernate.initialize(userShift.streamCallTasks().toList());
            List<CallToRecipientTask> callToRecipientTasks = userShift.streamCallTasks().toList();
            assertThat(callToRecipientTasks).hasSize(1);
        });

        CallTasksDto callTasks = userShiftQueryService.getCallTasks(user, Boolean.FALSE);

        assertThat(callTasks.getTasks()).hasSize(1);

        List<String> crExternalIds = callTasks.getTasks().get(0).getExternalOrderIds();

        assertThat(crExternalIds).containsExactlyInAnyOrder(
                order.getExternalOrderId(),
                anotherOrder.getExternalOrderId()
        );
    }
}
