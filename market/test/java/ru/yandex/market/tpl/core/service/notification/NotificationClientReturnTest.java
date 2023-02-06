package ru.yandex.market.tpl.core.service.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.clientreturn.PartnerClientReturnService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.ClientReturnReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.service.crm.communication.AsyncCommunicationSender;
import ru.yandex.market.tpl.core.service.crm.communication.model.CourierPlatformCommunicationDto;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.GET_MULTI_INTERVAL_BY_ALL_ORDERS;

@RequiredArgsConstructor
public class NotificationClientReturnTest extends TplAbstractTest {

    private static final long UID = 1L;
    private static final String INTERVAL_9_TO_18 = "09:00-18:00";
    private static final String phone = "+79050000401";


    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long YANDEX_BUYER_ID = 1L;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserShiftCommandService commandService;
    private final NotificationService notificationService;
    @SpyBean
    private AsyncCommunicationSender asyncCommunicationSender;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;

    private LocalDate now;
    private Order order;
    private User user;
    private NewDeliveryRoutePointData delivery;
    private NewDeliveryRoutePointData clientReturnDelivery;
    private Shift shift;
    private AddressGenerator.AddressGenerateParam addressGenerateParam;
    private GeoPoint geoPoint;
    private ClientReturn clientReturn;
    private RoutePointAddress myAddress;
    private Instant clientReturnDeliveryTime;


    @BeforeEach
    void init() {
        now = LocalDate.now();
        geoPoint = GeoPointGenerator.generateLonLat();
        shift = testUserHelper.findOrCreateOpenShift(now);
        user = testUserHelper.findOrCreateUser(UID);

        addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(geoPoint)
                .street("Колотушкина")
                .house("1")
                .build();

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .deliveryInterval(LocalTimeInterval.valueOf(INTERVAL_9_TO_18))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .recipientPhone(phone)
                .buyerYandexUid(YANDEX_BUYER_ID)
                .build());
        clientReturn =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(phone)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0)))
                        .build());

        clientReturnDeliveryTime = order.getDelivery().getDeliveryIntervalFrom();
        myAddress = new RoutePointAddress("my_address", geoPoint);

        delivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order, false, false)
                .build();

        clientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturn.getId()).build())
                .build();
    }

    @Test
    @DisplayName("Проверка, что событие отправляется в триггерную при заказе и возврате в одной смене")
    void testClientReturnAndOrder() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER, true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery)
                .routePoint(clientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        verify(asyncCommunicationSender).send(any());

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
        assertThat(event.getExternalReturnIds()).isEqualTo(List.of(clientReturn.getCheckouterReturnId()));
    }

    @Test
    @DisplayName("Проверка, что событие не отправляется в триггерную при возврате в одной смене и отключенном флаге")
    void testClientReturnNotSent_WhenToggleOff() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER,
                    false);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery)
                .routePoint(clientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        verify(asyncCommunicationSender).send(any());

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
        assertThat(event.getExternalReturnIds()).isNull();
    }

    @Test
    @DisplayName("Проверка, что когда в мульте только возвраты, и флаг включен, событие отправляется в триггерную")
    void sentToTrigger_WhenClientReturnsOnlyMultiOrder() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var clientReturnMerge =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(phone)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(22, 0)))
                        .build());

        var mergeClientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturnMerge.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(mergeClientReturnDelivery)
                .routePoint(clientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        verify(asyncCommunicationSender).send(any());

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
        assertThat(event.getExternalReturnIds()).containsExactlyInAnyOrderElementsOf(List.of(clientReturn.getCheckouterReturnId(),
                clientReturnMerge.getCheckouterReturnId()));

    }

    private final ClientReturnCommandService clientReturnCommandService;
    private final PartnerClientReturnService partnerClientReturnService;

    @Test
    @DisplayName("Проверка, что когда в мульте только возвраты, и флаг включен, и один возврат отменен, событие с " +
            "отмененным возвратом не отправляется в триггерную")
    void dontSend_WhenClientReturnCancelledFromLES() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var clientReturnMerge =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(phone)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(22, 0)))
                        .build());

        var mergeClientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturnMerge.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(mergeClientReturnDelivery)
                .routePoint(clientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        OrderDeliveryFailReason reason = new OrderDeliveryFailReason(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED, "Не вышло");
        clientReturnCommandService.cancel(new ClientReturnCommand.Cancel(clientReturnMerge.getId(), reason,
                Source.CLIENT));
        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        verify(asyncCommunicationSender).send(any());

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
        assertThat(event.getExternalReturnIds()).containsExactlyInAnyOrderElementsOf(List.of(clientReturn.getCheckouterReturnId()));

    }

    @Test
    @DisplayName("Проверка, что когда в мульте только возвраты, и флаг включен, и один возврат отменен, событие с " +
            "отмененным возвратом не отправляется в триггерную")
    void dontSend_WhenClientReturnCancelledFromPI() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var clientReturnMerge =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .recipientPhone(phone)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(22, 0)))
                        .build());

        var mergeClientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturnMerge.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(mergeClientReturnDelivery)
                .routePoint(clientReturnDelivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        OrderDeliveryFailReasonDto failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED, "some comment", Source.OPERATOR
        );
        partnerClientReturnService.cancelClientReturn(clientReturn.getExternalReturnId(), failReason);

        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        verify(asyncCommunicationSender).send(any());

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender).send(eventCaptor.capture());
        var event = (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) eventCaptor.getValue();
        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
        assertThat(event.getExternalReturnIds()).containsExactlyInAnyOrderElementsOf(List.of(clientReturnMerge.getCheckouterReturnId()));

    }


    @Test
    @DisplayName("Проверка, что при наличии мульта с только возвратами и возврат + заказ, отправляются оба ивента как" +
            " надо")
    void sentToTrigger_WhenClientRetunsOnlyAndClientReturnAndOrder() {
        transactionTemplate.executeWithoutResult(cmd -> {
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.insertValue(GET_MULTI_INTERVAL_BY_ALL_ORDERS, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                    true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                    OrderGenerateService.DEFAULT_PHONE);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_STARTED_DELIVERY_EVENT, true);
            configurationServiceAdapter.mergeValue(ConfigurationProperties.ENABLED_CLIENT_RETURN_SEND_TO_TRIGGER,
                    true);
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);
        });

        var newGeoPoint = GeoPointGenerator.generateLonLat();
        var anotherPhone = "+79257646188";


        var clientReturnMerge =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(newGeoPoint)
                                .build())
                        .recipientPhone(anotherPhone)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(22, 0)))
                        .build());

        var clientReturnMerge2 =
                clientReturnGenerator.createClientReturn(ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(newGeoPoint)
                                .build())
                        .recipientPhone(anotherPhone)
                        .itemCount(10L)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .arriveIntervalFrom(LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)))
                        .arriveIntervalTo(LocalDateTime.of(LocalDate.now(), LocalTime.of(22, 0)))
                        .build());

        var mergeClientReturnDelivery = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturnMerge.getId()).build())
                .build();

        var mergeClientReturnDelivery2 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(clientReturnDeliveryTime)
                .expectedDeliveryTime(clientReturnDeliveryTime)
                .name("my_name")
                .clientReturnReference(ClientReturnReference.builder().clientReturnId(clientReturnMerge2.getId()).build())
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(mergeClientReturnDelivery)
                .routePoint(mergeClientReturnDelivery2)
                .routePoint(clientReturnDelivery)
                .routePoint(delivery)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        notificationService.sendDeliverySmsForUserShiftId(userShiftId);

        verify(asyncCommunicationSender, times(2)).send(any());

        var eventCaptor = ArgumentCaptor.forClass(CourierPlatformCommunicationDto.class);
        verify(asyncCommunicationSender, times(2)).send(eventCaptor.capture());
        var events = eventCaptor.getAllValues();
        List<CourierPlatformCommunicationDto.OrderStartedDeliveryEvent> triggerEvents =
                events.stream().map(event -> (CourierPlatformCommunicationDto.OrderStartedDeliveryEvent) event).collect(Collectors.toList());
        var checkouterIds =
                triggerEvents.stream().flatMap(e -> e.getExternalReturnIds().stream()).collect(Collectors.toList());
        var orderIds = triggerEvents.stream().flatMap(e -> e.getYandexOrderIds().stream()).collect(Collectors.toList());

        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
        assertThat(clientReturnMerge.getCheckouterReturnId()).isNotNull();
        assertThat(clientReturnMerge2.getCheckouterReturnId()).isNotNull();

        assertThat(checkouterIds).hasSize(3);
        assertThat(orderIds).hasSize(1);
        assertThat(checkouterIds).containsExactlyInAnyOrderElementsOf(List.of(
                clientReturn.getCheckouterReturnId(),
                clientReturnMerge.getCheckouterReturnId(),
                clientReturnMerge2.getCheckouterReturnId())
        );
        assertThat(orderIds).containsExactlyInAnyOrderElementsOf(List.of(order.getExternalOrderId()));
    }

}
