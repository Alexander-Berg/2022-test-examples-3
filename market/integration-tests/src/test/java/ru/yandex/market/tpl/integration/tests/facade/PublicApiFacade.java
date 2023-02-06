package ru.yandex.market.tpl.integration.tests.facade;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import io.qameta.allure.Step;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.app.VersionDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderPaidDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.PhotoDto;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.RescheduleDatesDto;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusResponseDto;
import ru.yandex.market.tpl.api.model.partial_return_order.LinkReturnableItemsInstancesWithBoxesRequestDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftPayStatisticsDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.DeliveryPostponeDto;
import ru.yandex.market.tpl.api.model.task.DeliveryRescheduleDto;
import ru.yandex.market.tpl.api.model.task.MultiOrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.call.CallTaskDto;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointReturnReason;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.user.UserDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.integration.tests.client.LmsTplApiClient;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PublicApiClient;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.service.LmsUserPropertyGridView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING;
import static ru.yandex.market.tpl.integration.tests.tests.courier.app.PickingUpParcelsFromScTest.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED;

@Component
@RequiredArgsConstructor
public class PublicApiFacade extends BaseFacade {
    private final PublicApiClient publicApi;
    private final LmsTplApiClient lmsTplApiClient;
    private final ManualApiClient manualApiClient;

    @Step("Кнопка [Обновить данные]")
    public PublicApiFacade.UpdateDataResponse updateDataButton() {
        var version = publicApi.getLatestVersion();
        var currentUserShift = updateUserShift();
        var routePoints = updateRoutePoints();
        var user = publicApi.getUser();
        assertEquals(AutoTestContextHolder.getContext().getUserId(), user.getId());
        return new PublicApiFacade.UpdateDataResponse(version, currentUserShift, routePoints, user);
    }

    @Step("Кнопка [Обновить статус выкупаемых и возвращаемых позиций]")
    public UpdateItemsInstancesPurchaseStatusResponseDto updateItemsInstancesPurchaseStatus(
            UpdateItemsInstancesPurchaseStatusRequestDto requestDto) {
        return publicApi.updateItemsInstancesPurchaseStatus(requestDto);
    }

    @Step("Привязывает возвратные позиции к сейф-пакетам")
    public void createLogisticReturn(LinkReturnableItemsInstancesWithBoxesRequestDto requestDto) {
        publicApi.createLogisticReturn(requestDto);
    }

    private RoutePointListDto updateRoutePoints() {
        var routePoints = publicApi.getRoutePoints();
        AutoTestContextHolder.getContext().setRoutePoints(routePoints);
        return routePoints;
    }

    private UserShiftDto updateUserShift() {
        var currentUserShift = publicApi.getCurrentUserShift();
        AutoTestContextHolder.getContext().setUserShift(currentUserShift);
        return currentUserShift;
    }

    public RoutePointDto updateCurrentRoutePoint() {
        var currentRoutePoint = publicApi.getRoutePoint(
                AutoTestContextHolder.getContext().getUserShift().getCurrentRoutePointId());
        AutoTestContextHolder.getContext().setRoutePoint(currentRoutePoint);
        return currentRoutePoint;
    }

    public RoutePointDto arriveToRoutePoint() {
        var address = AutoTestContextHolder.getContext().getCurrentRoutePoint().getAddress();
        return arriveToRoutePoint(address.getLatitude(), address.getLongitude());
    }

    @Step("Кнопка [Я на месте]")
    public RoutePointDto arriveToRoutePoint(BigDecimal lat, BigDecimal lon) {
        long currentRoutePointId = AutoTestContextHolder.getContext().getCurrentRoutePoint().getId();
        var currentRoutePoint = publicApi.arrive(currentRoutePointId, lat, lon);
        AutoTestContextHolder.getContext().setRoutePoint(currentRoutePoint);
        return currentRoutePoint;
    }

    @Step("Кнопка [Поехали]")
    public RoutePointDto startUserShift() {
        UserShiftDto currentUserShift = updateUserShift();
        publicApi.checkin(currentUserShift.getId(), null, null);
        currentUserShift = updateUserShift();
        assertThat(currentUserShift.getCurrentRoutePointId()).isNotNull();
        var currentRoutePoint = publicApi.getRoutePoint(currentUserShift.getCurrentRoutePointId());
        AutoTestContextHolder.getContext().setRoutePoint(currentRoutePoint);
        assertThat(currentUserShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        return currentRoutePoint;
    }

    @Step("Отправляем на бэк отсканированные товары")
    public void scanItems() {
        var startedTask = startOrderPickupTask();
        List<OrderScanTaskDto.OrderForScanDto> orders = startedTask.getOrders();
        List<String> ids = orders.stream()
                .map(OrderScanTaskDto.OrderForScanDto::getExternalOrderId)
                .collect(Collectors.toList());
        Map<String, List<PlaceForScanDto>> places =
                orders.stream().collect(Collectors.toMap(OrderScanTaskDto.OrderForScanDto::getExternalOrderId,
                        OrderScanTaskDto.OrderForScanDto::getPlaces));
        scanItems(ids, List.of(), null, places);
    }

    public OrderPickupTaskDto startOrderPickupTask() {
        RoutePointDto currentRoutePoint = AutoTestContextHolder.getContext().getRoutePoint();
        long taskId = currentRoutePoint.getTasks().iterator().next().getId();
        var startedTask = publicApi.startOrderPickupTask(taskId);
        assertThat(startedTask.getStatus()).isEqualTo(OrderPickupTaskStatus.IN_PROGRESS);
        assertThat(startedTask.getCompletedOrders()).isEmpty();
        assertThat(startedTask.getSkippedOrders()).isEmpty();
        return startedTask;
    }

    public void scanItems(List<String> completedOrdersIds, List<String> skippedOrderIds, String comment) {
        RoutePointDto currentRoutePoint = AutoTestContextHolder.getContext().getRoutePoint();
        long taskId = currentRoutePoint.getTasks().iterator().next().getId();
        OrderPickupTaskDto orderPickupTask = publicApi.getOrderPickupTask(taskId);
        List<OrderScanTaskDto.OrderForScanDto> orders = orderPickupTask.getOrders();
        Set<String> ids = new HashSet<>(completedOrdersIds);
        Map<String, List<PlaceForScanDto>> places =
                StreamEx.of(orders)
                        .filter(o -> ids.contains(o.getExternalOrderId()))
                        .toMap(
                                OrderScanTaskDto.OrderForScanDto::getExternalOrderId,
                                OrderScanTaskDto.OrderForScanDto::getPlaces
                        );
        scanItems(completedOrdersIds, skippedOrderIds, comment, places);
    }

    public void scanItems(List<String> completedOrderIds, List<String> skippedOrderIds, String comment,
                          Map<String, List<PlaceForScanDto>> places) {
        List<LmsUserPropertyGridView> properties =
                lmsTplApiClient.getPropertiesForCourier(AutoTestContextHolder.getContext().getUserId());
        RoutePointDto currentRoutePoint = AutoTestContextHolder.getContext().getRoutePoint();
        long taskId = currentRoutePoint.getTasks().iterator().next().getId();
        var loadingTask = publicApi.finishOrderPickupTask(taskId, completedOrderIds,
                skippedOrderIds, comment, places, properties);
        if (properties.stream().anyMatch(property -> property.getName().equals(TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED)
                && property.getValue().equals("true"))) {
            assertThat(loadingTask.getStatus()).isEqualTo(OrderPickupTaskStatus.TRANSFER_ACT_PROCESSING);
            manualApiClient.signTransferAct(AutoTestContextHolder.getContext().getUserId());
            loadingTask = publicApi.getOrderPickupTask(loadingTask.getId());
        }

        assertThat(loadingTask.getStatus()).isEqualTo(OrderPickupTaskStatus.BOX_LOADING);

        var finishedTask = publicApi.finishLoadingOrderPickupTask(taskId,
                completedOrderIds, skippedOrderIds, comment);
        if (skippedOrderIds.isEmpty()) {
            assertThat(finishedTask.getStatus()).isEqualTo(OrderPickupTaskStatus.FINISHED);
        } else {
            assertThat(finishedTask.getStatus()).isEqualTo(OrderPickupTaskStatus.PARTIALLY_FINISHED);
        }
        updateDataButton();
        var routePoint = publicApi.getRoutePoint(AutoTestContextHolder.getContext().getRoutePointId());
        AutoTestContextHolder.getContext().setRoutePoint(routePoint);
    }

    @Step("Кнопка [Отменить заказ]")
    public void cancelOrder() {
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        OrderDeliveryTaskDto deliveryTask = publicApi.cancelOrder(
                task.getId(),
                OrderDeliveryTaskFailReasonType.NO_CONTACT
        );
        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(deliveryTask.getFailReason().getReason()).isEqualTo(OrderDeliveryTaskFailReasonType.NO_CONTACT);
        updateRoutePoints();
        updateUserShift();
    }

    @Step("Кнопка [Перенести заказ]")
    public void rescheduleOrder() {
        DeliveryRescheduleDto deliveryRescheduleDto = getDeliveryRescheduleDto();
        OrderDeliveryTaskDto task = (OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().iterator().next();
        OrderDeliveryTaskDto deliveryTask = publicApi.rescheduleOrder(task.getId(), deliveryRescheduleDto);
        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(deliveryTask.getFailReason().getReason())
                .isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
        assertThat(deliveryTask.getOrder().getDelivery().getIntervalFrom())
                .isEqualTo(deliveryRescheduleDto.getIntervalFrom());
        assertThat(deliveryTask.getOrder().getDelivery().getIntervalTo())
                .isEqualTo(deliveryRescheduleDto.getIntervalTo());
        updateRoutePoints();
        updateUserShift();
    }

    @Step("Кнопка [Возобновить заказ]")
    public void reopenDeliveryTask() {
        var deliveryRoutePointId = getDeliveryRoutePoint().getId();
        var currentRoutePoint = publicApi.getRoutePoint(deliveryRoutePointId);
        OrderDeliveryTaskDto deliveryTask = publicApi.reopenOrder(
                currentRoutePoint.getTasks().iterator().next().getId());
        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        updateRoutePoints();
        updateUserShift();
    }

    @Step("Кнопка [Оплата принята]")
    public void pay() {
        OrderPaidDto orderPaidDto = new OrderPaidDto();
        orderPaidDto.setPaymentType(getConfig().getPaymentType());
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        OrderDeliveryTaskDto deliveryTaskDto = publicApi.pay(task.getId(), orderPaidDto);
        assertThat(deliveryTaskDto.getOrder().getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    @Step("Кнопка [Возврат посылок на склад]")
    public void startReturnOrders() {
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        publicApi.startOrderReturnTask(task.getId());
    }

    @Step("Кнопка [Передать деньги в кассу]")
    public void returnCash() {
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        publicApi.cashReturn(task.getId());
    }

    @Step("Кнопка [Ввести код вручную]")
    public void enterOrderCode() {
        OrderReturnTaskDto task = (OrderReturnTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().iterator().next();
        publicApi.finishOrderReturn(task.getId(), task.getOrders(), List.of());
    }

    @Step("Кнопка [Завершить смену]")
    public void finishUserShift() {
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        publicApi.finishOrderReturnTask(task.getId());
    }

    @Step("Кнопка [Выдать заказ]")
    public void giveParcel() {
        OrderDeliveryTaskDto task = (OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().iterator().next();
        makePhoto();
        publicApi.registerCheque(task.getId(),
                getConfig().getPaymentType(), OrderChequeType.SELL);
    }

    @Step("Кнопка отложить")
    public void postpone() {
        OrderDeliveryTaskDto task = (OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().iterator().next();
        MultiOrderDeliveryTaskDto multiOrderDeliveryTaskDto = publicApi.postponeMultiOrder(
                task.getMultiOrderId(),
                new DeliveryPostponeDto(Duration.ofHours(24))
        );

        multiOrderDeliveryTaskDto.getTasks()
                .forEach(t -> assertThat(t.getPostponed()).isNotNull());
    }

    @Step("Кнопка [Оплата принята]")
    public void payAndGiveParcel() {
        OrderDeliveryTaskDto task = (OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().iterator().next();
        var deliveryTaskDto = publicApi.payAndRegisterCheque(task.getId(),
                getConfig().getPaymentType(), OrderChequeType.SELL);
        assertThat(deliveryTaskDto.getOrder().getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    @Step("Кнопка [Выдать заказ] - мульт")
    public void giveParcel(OrderDeliveryTaskDto task) {
        makePhoto();
        publicApi.registerCheque(task.getId(),
                getConfig().getPaymentType(), OrderChequeType.SELL);
    }


    @Step("Кнопка [Возврат]")
    public void cancelCheque() {
        long deliveryRoutePointId = getDeliveryRoutePoint().getId();
        RoutePointDto deliveryRoutePoint = publicApi.getRoutePoint(deliveryRoutePointId);
        OrderDeliveryTaskDto task = (OrderDeliveryTaskDto) deliveryRoutePoint.getTasks().iterator().next();
        OrderDto order = task.getOrder();
        makePhoto();
        OrderDeliveryTaskDto deliveryTask = publicApi.registerCheque(
                task.getId(),
                order.getPaymentType(), OrderChequeType.RETURN);
        assertThat(deliveryTask.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(deliveryTask.getOrder().getPaymentStatus()).isEqualTo(OrderPaymentStatus.UNPAID);
        updateRoutePoints();
        updateUserShift();
    }

    @Step("Кнопка [Завершить]")
    public void finishLastDeliveryTask() {
        RemainingOrderDeliveryTasksDto orderDeliveryTasks = publicApi.getOrderDeliveryTasks();
        assertThat(orderDeliveryTasks.getOrders()).isEmpty();
        updateRoutePoints();
        updateUserShift();
    }


    @Step("Кнопка [Завершить]")
    public void finishDeliveryTask() {
        publicApi.getOrderDeliveryTasks();
        updateRoutePoints();
        updateUserShift();
    }

    @Step("Успешный дозвон до получателя")
    public void successCallToRecipient() {
        assertThat(AutoTestContextHolder.getContext().getCurrentRoutePoint().getId() ==
                AutoTestContextHolder.getContext().getRoutePoint().getId());
        AutoTestContextHolder.getContext().getRoutePoint().getCallTasks()
                .forEach(callTask -> {
                    CallTaskDto callTaskDto2 = publicApi.callTaskAttemptSuccess(callTask.getId());
                    assertThat(callTaskDto2.getTaskStatus()).isEqualTo(CallToRecipientTaskStatus.SUCCESS);
                });
        updateDataButton();
        updateCurrentRoutePoint();
    }

    @Step("Сфотографировать доставленную посылку")
    @SneakyThrows
    public void makePhoto() {
        TaskDto task = AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next();
        URL pictureUrl = this.getClass().getClassLoader().getResource("some_picture.png");
        BufferedImage img = ImageIO.read(pictureUrl);
        File file = new File("some_picture.png");
        ImageIO.write(img, "png", file);
        PhotoDto photoDto = publicApi.uploadPhoto(task.getId(), file);
        assertThat(photoDto.getUrl()).isNotEmpty();
    }

    public OrderDeliveryTaskDto getDeliveryTask() {
        RoutePointSummaryDto routePointSummaryDto = getDeliveryRoutePoint();
        RoutePointDto routePoint = publicApi.getRoutePoint(routePointSummaryDto.getId());
        return (OrderDeliveryTaskDto) routePoint.getTasks().iterator().next();
    }

    public UserShiftStatisticsDto getShiftStatistics() {
        return publicApi.shiftStatistics(AutoTestContextHolder.getContext().getUserShiftId());
    }

    public UserShiftPayStatisticsDto getShiftPayStatistics() {
        return publicApi.shiftPayStatistics(AutoTestContextHolder.getContext().getUserShiftId());
    }

    private RoutePointSummaryDto getDeliveryRoutePoint() {
        return AutoTestContextHolder.getContext().getRoutePointsMap().values().stream()
                .filter(rp -> rp.getType() == RoutePointType.DELIVERY)
                .findFirst()
                .orElseThrow();
    }

    private DeliveryRescheduleDto getDeliveryRescheduleDto() {
        RescheduleDatesDto rescheduleDates = publicApi.rescheduleDates(getContext().getUserShift().getId());
        LocalDate rescheduleDate = LocalDate.now(DateTimeUtil.DEFAULT_ZONE_ID)
                .plusDays(getConfig().getRescheduleDays());
        Interval interval = rescheduleDates.getDays().stream()
                .filter(day -> LocalDate.parse(day.getDate()).isEqual(rescheduleDate))
                .findFirst()
                .map(RescheduleDatesDto.Day::getIntervals)
                .map(List::iterator)
                .map(Iterator::next)
                .map(intervalDto -> new LocalTimeInterval(LocalTime.of(intervalDto.getFrom(), 0),
                        LocalTime.of(intervalDto.getTo(), 0)))
                .map(timeInterval -> timeInterval.toInterval(rescheduleDate, DateTimeUtil.DEFAULT_ZONE_ID))
                .orElseThrow(() -> new RuntimeException("Не удалось получить интервал для переноса заказа."));
        DeliveryRescheduleDto deliveryRescheduleDto = new DeliveryRescheduleDto();
        deliveryRescheduleDto.setIntervalFrom(interval.getStart());
        deliveryRescheduleDto.setIntervalTo(interval.getEnd());
        deliveryRescheduleDto.setReason(OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        return deliveryRescheduleDto;
    }

    public void update() {
        updateDataButton();
        updateCurrentRoutePoint();
    }

    @Step("Загрузить посылки в Постомат")
    public LockerDeliveryTaskDto loadToLocker() {
        update();
        var currentRoutePoint = getContext().getRoutePoint();
        assertThat(currentRoutePoint.getType() == RoutePointType.LOCKER_DELIVERY);
        LockerDeliveryTaskDto task = (LockerDeliveryTaskDto) currentRoutePoint.getTasks().iterator().next();
        var orders = task.getOrders().stream()
                .map(order -> {
                    var orderForScanDto = new OrderScanTaskDto.OrderForScanDto();
                    orderForScanDto.setExternalOrderId(order.getExternalOrderId());
                    List<PlaceForScanDto> places = order.getPlaces().stream()
                            .map(PlaceDto::getBarcode)
                            .map(PlaceForScanDto::new)
                            .collect(Collectors.toList());
                    orderForScanDto.setPlaces(places);
                    return orderForScanDto;
                })
                .collect(Collectors.toList());
        var request = new OrderScanTaskRequestDto(
                orders,
                List.of(),
                Set.of(),
                Set.of(),
                null,
                null
        );
        return publicApi.finishLockerLoad(currentRoutePoint.getId(), task.getId(), request);
    }

    public void unloadRandomOrderFromLockerSuccessfully() {
        String randomExternalOrderId = UUID.randomUUID().toString();
        unloadFromLocker(randomExternalOrderId, null);
    }

    @Step("Выгрузить посылки из постомата")
    public void unloadFromLocker(String externalOrderId, PickupPointReturnReason returnReason) {
        update();
        // ответ не проверяем, т.к. и приложение не проверяет, главное, чтобы было 200.
        publicApi.scanOrder(externalOrderId);
        var currentRoutePoint = getContext().getRoutePoint();
        assertThat(currentRoutePoint.getType() == RoutePointType.LOCKER_DELIVERY);
        LockerDeliveryTaskDto task = (LockerDeliveryTaskDto) currentRoutePoint.getTasks().iterator().next();
        ScannerOrderDto scannerOrderDto = new ScannerOrderDto();
        scannerOrderDto.setExternalOrderId(externalOrderId);
        scannerOrderDto.setReturnReason(returnReason);
        var request = new PickupPointScanTaskRequestDto(List.of(scannerOrderDto));
        publicApi.finishLockerUnload(currentRoutePoint.getId(), task.getId(), request);
        update();
    }

    @Step("Отменить задание на загрузку в постомат")
    public void cancelLockerTask() {
        update();
        RoutePointDto ldRoutePoint = getContext().getRoutePoint();
        assertThat(ldRoutePoint.getType()).isEqualTo(RoutePointType.LOCKER_DELIVERY);
        assertThat(ldRoutePoint.getTasks().size()).isEqualTo(1);
        TaskDto task = ldRoutePoint.getTasks().iterator().next();
        assertThat(task).isInstanceOf(LockerDeliveryTaskDto.class);
        TaskDto canceledTask = publicApi.cancelLockerDeliveryTask(task.getId(), LOCKER_NOT_WORKING, "ed");
        assertThat(canceledTask).isInstanceOf(LockerDeliveryTaskDto.class);
        LockerDeliveryTaskDto ldCanceledTask = (LockerDeliveryTaskDto) canceledTask;
        assertThat(ldCanceledTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);

        update();
        assertThat(getContext().getRoutePoint().getType()).isNotEqualTo(RoutePointType.LOCKER_DELIVERY);
        assertThat(getContext().findRoutePointById(ldRoutePoint.getId()).getStatus())
                .isEqualTo(RoutePointStatus.FINISHED);
    }

    @Step("Переоткрыть задание на загрузку в ПВЗ")
    public void reopenLockerTask() {
        update();
        assertThat(getContext().getRoutePoint().getType()).isNotEqualTo(RoutePointType.LOCKER_DELIVERY);

        long ldRoutePointId = getContext().findFirstRoutePointByType(RoutePointType.LOCKER_DELIVERY).getId();
        RoutePointDto ldRoutePoint = publicApi.getRoutePoint(ldRoutePointId);
        assertThat(ldRoutePoint.getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        TaskDto taskDto = ldRoutePoint.getTasks().stream().findFirst().orElseThrow();
        assertThat(taskDto).isInstanceOf(LockerDeliveryTaskDto.class);
        LockerDeliveryTaskDto ldTask = (LockerDeliveryTaskDto) taskDto;
        assertThat(ldTask.getStatus()).isIn(LockerDeliveryTaskStatus.CANCELLED, LockerDeliveryTaskStatus.FINISHED);

        publicApi.reopenLockerDeliveryTask(taskDto.getId());

        update();
        RoutePointDto reopenedRoutePoint = getContext().getRoutePoint();
        assertThat(reopenedRoutePoint.getType()).isEqualTo(RoutePointType.LOCKER_DELIVERY);
        assertThat(reopenedRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(reopenedRoutePoint.getTasks().size()).isEqualTo(1);
        TaskDto task = reopenedRoutePoint.getTasks().iterator().next();
        assertThat(task).isInstanceOf(LockerDeliveryTaskDto.class);
        assertThat(task).isInstanceOf(LockerDeliveryTaskDto.class);
        LockerDeliveryTaskDto reopenedTask = (LockerDeliveryTaskDto) task;
        assertThat(reopenedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        assertThat(reopenedTask.getOrders()).isNotEmpty();
    }

    @Data
    @AllArgsConstructor
    public static class UpdateDataResponse {
        VersionDto version;
        UserShiftDto userShift;
        RoutePointListDto routePoints;
        UserDto user;
    }
}
