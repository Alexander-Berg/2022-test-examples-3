package ru.yandex.market.tpl.integration.tests.facade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.manual.CreateDeliveryTasksRequest;
import ru.yandex.market.tpl.api.model.manual.CreateDeliveryTasksResponse;
import ru.yandex.market.tpl.api.model.manual.CreateRoutePointRequestDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.ShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.service.CourierPool;

@Component
@RequiredArgsConstructor
public class ManualApiFacade extends BaseFacade {
    private final ManualApiClient manualApi;
    private final CourierPool courierPool;
    private final ManualApiClient manualApiClient;
    private int orderCounter = 0;

    @Step("Удаление курьера, и всех связанных с ним сущностей")
    public synchronized void deleteCourier() {
        try {
            manualApi.deleteCourier(AutoTestContextHolder.getContext().getUserId(), null);
        } finally {
            courierPool.releaseCourier();
        }
        manualApiClient.updateCaches();
    }

    @Step("Создание смены и заказов")
    public UserShiftDto createDefaultShiftWithTasks() {
        UserShiftDto currentUserShift = manualApi.createDefaultShiftWithTasks(
                AutoTestContextHolder.getContext().getUid());
        AutoTestContextHolder.getContext().setUserShift(currentUserShift);
        return currentUserShift;
    }

    @Step("Создание пустого RoutePoint")
    public void createEmptyRoutePoint() {
        CreateRoutePointRequestDto requestDto = new CreateRoutePointRequestDto();
        requestDto.setCity("Москва");
        requestDto.setStreet("Ленинские горы");
        requestDto.setHouse("1");
        requestDto.setLatitude(BigDecimal.valueOf(55.702951));
        requestDto.setLongitude(BigDecimal.valueOf(37.530822));
        requestDto.setExpectedDeliveryTime(Instant.now());
        requestDto.setType(RoutePointType.DELIVERY);
        var routePoint = manualApi.createEmptyRoutePoint(AutoTestContextHolder.getContext().getUid(),
                AutoTestContextHolder.getContext().getUserShiftId(), requestDto);
        AutoTestContextHolder.getContext().setRoutePoint(routePoint);
    }

    @Step("Создание RoutePoint с заданием на доставку")
    public void createRoutePointWithDeliveryTask(boolean isFashion) {
        CreateRoutePointRequestDto requestDto = new CreateRoutePointRequestDto();
        requestDto.setCity("Москва");
        requestDto.setStreet("Ленинские горы");
        requestDto.setHouse("1");
        requestDto.setLatitude(BigDecimal.valueOf(55.702951));
        requestDto.setLongitude(BigDecimal.valueOf(37.530822));
        requestDto.setExpectedDeliveryTime(Instant.now());
        requestDto.setType(RoutePointType.DELIVERY);
        requestDto.setFashion(isFashion);
        var routePoint = manualApi.generateOrderTaskAndAssign(AutoTestContextHolder.getContext().getUid(),
                AutoTestContextHolder.getContext().getUserShiftId(), requestDto);
        AutoTestContextHolder.getContext().setRoutePoint(routePoint);
    }

    @Step("Создание постоматного RoutePoint")
    public void createLockerDeliveryRoutePoint() {
        CreateRoutePointRequestDto requestDto = new CreateRoutePointRequestDto();
        requestDto.setCity("Москва");
        requestDto.setStreet("Ленинские горы");
        requestDto.setHouse("1");
        requestDto.setLatitude(BigDecimal.valueOf(55.702951));
        requestDto.setLongitude(BigDecimal.valueOf(37.530822));
        requestDto.setExpectedDeliveryTime(Instant.now());
        requestDto.setType(RoutePointType.LOCKER_DELIVERY);
        requestDto.setPickupPointType(PartnerSubType.PVZ.name());
        requestDto.setPickPointCode("37");
        var routePoint = manualApi.createSimpleRoutePoint(AutoTestContextHolder.getContext().getUid(),
                AutoTestContextHolder.getContext().getUserShiftId(), requestDto);
        AutoTestContextHolder.getContext().setRoutePoint(routePoint);
    }

    @Step("Создание задачи на доставку")
    public void createDeliveryTask() {
        TestConfiguration configuration = getConfig();
        manualApi.addDeliveryTask(AutoTestContextHolder.getContext().getRoutePointId(),
                AutoTestContextHolder.getContext().getUid(), AutoTestContextHolder.getContext().getUserShiftId(),
                configuration.getPaymentStatus(), configuration.getPaymentType(),
                configuration.getOrderRecipientNotes());
    }

    @Step("Создание задачи на доставку мультизаказа")
    public void createDeliveryMultiTask() {
        TestConfiguration configuration = getConfig();
        manualApi.addDeliveryTask(AutoTestContextHolder.getContext().getRoutePointId(),
                AutoTestContextHolder.getContext().getUid(), AutoTestContextHolder.getContext().getUserShiftId(),
                configuration.getPaymentStatus(), configuration.getPaymentType(),
                configuration.getOrderRecipientNotes());

        manualApi.addDeliveryTask(AutoTestContextHolder.getContext().getRoutePointId(),
                AutoTestContextHolder.getContext().getUid(), AutoTestContextHolder.getContext().getUserShiftId(),
                configuration.getPaymentStatus(), configuration.getPaymentType(),
                configuration.getOrderRecipientNotes());
    }

    @Step("Создание смены доставочной службы")
    public void createShift() {
        ShiftDto openShift = manualApi.createOpenShift(LocalDate.now());
        getContext().setShift(openShift);
    }

    @Step("Создание курьерской смены")
    public void createUserShift() {
        UserShiftDto userShiftDto = manualApi.createUserShift(getContext().getUid(), getContext().getShiftId());
        getContext().setUserShift(userShiftDto);
    }

    @Step("Удаление заказа")
    public void deleteOrder() {
        Optional.ofNullable(getContext().getOrderId()).ifPresent(manualApi::deleteOrder);
    }

    @Step("Создать курьеру 40 разных заказов")
    public void createManyDeliveryTasks() {
        createManyDeliveryTasks(40);
    }

    @Step("Создать курьеру много разных заказов")
    public CreateDeliveryTasksResponse createManyDeliveryTasks(int count) {
        List<CreateDeliveryTasksRequest.CreateDeliveryTask> createDeliveryTasks = generateCreateDeliveryTasks(count);
        return createManyDeliveryTasks(createDeliveryTasks);
    }

    public CreateDeliveryTasksResponse createManyDeliveryTasks(
            List<CreateDeliveryTasksRequest.CreateDeliveryTask> createDeliveryTasks) {
        var partitions = Lists.partition(createDeliveryTasks, 100);
        List<String> ids = partitions.stream()
                .map(part -> manualApi.addDeliveryTasks(new CreateDeliveryTasksRequest(part)))
                .flatMap(r -> r.getExternalOrderIds().stream())
                .collect(Collectors.toList());
        return new CreateDeliveryTasksResponse(ids);
    }

    public List<CreateDeliveryTasksRequest.CreateDeliveryTask> generateCreateDeliveryTasks(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    if (i % 10 == 0) {
                        createEmptyRoutePoint();
                    }
                    OrderPaymentType paymentType;
                    if (i < 28) {
                        paymentType = OrderPaymentType.PREPAID;
                    } else if (i == 39) {
                        paymentType = OrderPaymentType.CARD;
                    } else {
                        paymentType = OrderPaymentType.CASH;
                    }
                    OrderPaymentStatus paymentStatus = (paymentType == OrderPaymentType.PREPAID) ?
                            OrderPaymentStatus.PAID
                            : OrderPaymentStatus.UNPAID;
                    String recipientNotes = (i == 20) ? "Оставить у двери" : null;
                    return CreateDeliveryTasksRequest.CreateDeliveryTask.builder()
                            .uid(AutoTestContextHolder.getContext().getUid())
                            .routePointId(AutoTestContextHolder.getContext().getRoutePointId())
                            .userShiftId(AutoTestContextHolder.getContext().getUserShiftId())
                            .paymentStatus(paymentStatus)
                            .paymentType(paymentType)
                            .recipientNotes(recipientNotes)
                            .externalOrderId("auto-test-order-" + orderCounter++)
                            .build();
                }).collect(Collectors.toList());
    }
}
