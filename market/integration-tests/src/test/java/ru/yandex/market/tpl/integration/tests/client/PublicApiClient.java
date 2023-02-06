package ru.yandex.market.tpl.integration.tests.client;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.app.VersionDto;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaidDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.PhotoDto;
import ru.yandex.market.tpl.api.model.order.RescheduleDatesDto;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusResponseDto;
import ru.yandex.market.tpl.api.model.partial_return_order.LinkReturnableItemsInstancesWithBoxesRequestDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.scanner.ScannerOrderDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftPayStatisticsDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.api.model.task.CourierNotesDto;
import ru.yandex.market.tpl.api.model.task.DeliveryPostponeDto;
import ru.yandex.market.tpl.api.model.task.DeliveryRescheduleDto;
import ru.yandex.market.tpl.api.model.task.MultiOrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.ScannedPlaceDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.call.CallTaskDto;
import ru.yandex.market.tpl.api.model.task.locker.delivery.LockerDeliveryCancelRequestDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointScanTaskRequestDto;
import ru.yandex.market.tpl.api.model.user.UserDto;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.service.LmsUserPropertyGridView;
import ru.yandex.market.tpl.integration.tests.stress.StressStatFilter;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.currentCourierEmail;
import static ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil.isStressTestEnabled;

@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class PublicApiClient implements ApiClient {
    private final StressStatFilter stressStatFilter;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(ContentType.JSON)
            .build();
    @Value("${api.url.public}")
    private String publicApiUrl;

    RequestSpecification requestSpecification() {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .urlEncodingEnabled(false)
                .filters(stressStatFilter, new AllureRestAssured())
                .baseUri(publicApiUrl)
                .contentType(ContentType.JSON)
                .auth().oauth2(AutoTestContextHolder.getContext().getCourierTkn());
        if (isStressTestEnabled()) {
            requestSpecification.header("Stress-Test-User-Email", currentCourierEmail());
        } else {
            requestSpecification.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        return requestSpecification;
    }

    @Step("Вызов GET /api/app/versions/latest")
    public VersionDto getLatestVersion() {
        return requestSpecification()
                .when()
                .get("/api/app/versions/latest")
                .then().spec(responseSpecification)
                .extract().response().as(VersionDto.class);
    }

    @Step("Вызов GET /api/shifts/current")
    public UserShiftDto getCurrentUserShift() {
        return requestSpecification()
                .when()
                .get("/api/shifts/current")
                .then().spec(responseSpecification)
                .extract().response().as(UserShiftDto.class);
    }

    @Step("Вызов GET /api/route-points")
    public RoutePointListDto getRoutePoints() {
        return requestSpecification()
                .when()
                .get("/api/route-points")
                .then().spec(responseSpecification)
                .extract().response().as(RoutePointListDto.class);
    }

    @Step("Вызов POST /api/shifts/{userShiftId}/checkin")
    public UserShiftDto checkin(Long userShiftId, Double lat, Double lon) {
        return requestSpecification()
                .when()
                .post(sf("/api/shifts/{}/checkin", userShiftId))
                .then().spec(responseSpecification)
                .extract().response().as(UserShiftDto.class);
    }

    @Step("Вызов GET /api/shifts/{id}/statistics")
    public UserShiftStatisticsDto shiftStatistics(Long shiftId) {
        return requestSpecification()
                .when()
                .get(sf("/api/shifts/{}/statistics", shiftId))
                .then().spec(responseSpecification)
                .extract().response().as(UserShiftStatisticsDto.class);
    }

    @Step("Вызов GET /api/shifts/{id}/statistics/pay")
    public UserShiftPayStatisticsDto shiftPayStatistics(Long shiftId) {
        return requestSpecification()
                .when()
                .get(sf("/api/shifts/{}/statistics/pay", shiftId))
                .then().spec(responseSpecification)
                .extract().response().as(UserShiftPayStatisticsDto.class);
    }

    @Step("Вызов GET /api/user")
    public UserDto getUser() {
        return requestSpecification()
                .when().get("/api/user")
                .then().spec(responseSpecification)
                .extract().response().as(UserDto.class);
    }

    @Step("Вызов GET /api/route-points/{id}")
    public RoutePointDto getRoutePoint(Long routePointId) {
        return requestSpecification()
                .when()
                .get("/api/route-points/" + routePointId)
                .then().spec(responseSpecification)
                .extract().response().as(RoutePointDto.class);
    }

    @Step("Вызов POST /api/route-points/{}/arrive")
    public RoutePointDto arrive(Long routePointId, BigDecimal lat, BigDecimal lon) {
        return requestSpecification()
                .when()
                .body(new LocationDto(lon, lat))
                .post(sf("/api/route-points/{}/arrive", routePointId))
                .then().spec(responseSpecification)
                .extract().response().as(RoutePointDto.class);
    }

    @Step("Вызов GET /api/tasks/order-delivery-single/{}")
    public OrderDeliveryTaskDto getOrderDeliveryTask(Long taskId) {
        return requestSpecification()
                .when()
                .get("/api/tasks/order-delivery-single/{taskId}", taskId)
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-pickup/{}/start")
    public OrderPickupTaskDto startOrderPickupTask(Long taskId) {
        return requestSpecification()
                .when()
                .post("/api/tasks/order-pickup/{taskId}/start", taskId)
                .then().spec(responseSpecification)
                .extract().response().as(OrderPickupTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-pickup/{}/finish")
    public OrderPickupTaskDto finishOrderPickupTask(Long taskId, List<String> completedOrderIds,
                                                    List<String> skippedOrderIds, String comment,
                                                    Map<String, List<PlaceForScanDto>> placesForScan,
                                                    List<LmsUserPropertyGridView> properties) {
        List<OrderScanTaskDto.OrderForScanDto> completedOrders = completedOrderIds.stream()
                .map(id -> new OrderScanTaskDto.OrderForScanDto(false, null, id,
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, null, null, null, null,
                        placesForScan.getOrDefault(id, List.of()),
                        OrderFlowStatus.TRANSPORTATION_RECIPIENT, null, null))
                .collect(Collectors.toList());
        List<OrderScanTaskDto.OrderForScanDto> skippedOrders = skippedOrderIds.stream()
                .map(id -> new OrderScanTaskDto.OrderForScanDto(false, null, id,
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, null, null, null, null, null, null, null, null))
                .collect(Collectors.toList());
        OrderScanTaskRequestDto requestBody = new OrderScanTaskRequestDto(
                completedOrders,
                skippedOrders,
                Set.of(),
                Set.of(),
                comment,
                null
        );

        requestBody.setScannedOutsidePlaces(extractOutsidePlaces(completedOrders));

        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-pickup/{}/finish", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderPickupTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-pickup/{}/finish-loading")
    public OrderPickupTaskDto finishLoadingOrderPickupTask(Long taskId,
                                                           List<String> completedOrderIds,
                                                           List<String> skippedOrderIds, String comment) {
        List<OrderScanTaskDto.OrderForScanDto> completedOrders = completedOrderIds.stream()
                .map(id -> new OrderScanTaskDto.OrderForScanDto(false, null, id,
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, null, null, null, null, List.of(),
                        null, null, null))
                .collect(Collectors.toList());
        List<OrderScanTaskDto.OrderForScanDto> skippedOrders = skippedOrderIds.stream()
                .map(id -> new OrderScanTaskDto.OrderForScanDto(false, null, id,
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, null, null, null, null, null, null, null, null))
                .collect(Collectors.toList());
        OrderScanTaskRequestDto requestBody = new OrderScanTaskRequestDto(
                completedOrders,
                skippedOrders,
                Set.of(),
                Set.of(),
                comment,
                null
        );
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-pickup/{}/finish-loading", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderPickupTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery-single/{task-id}/register-cheque")
    public OrderDeliveryTaskDto registerCheque(Long taskId, OrderPaymentType paymentType,
                                               OrderChequeType chequeType) {
        OrderChequeRemoteDto requestBody = new OrderChequeRemoteDto();
        requestBody.setPaymentType(paymentType);
        requestBody.setChequeType(chequeType);
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-delivery-single/{}/register-cheque", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery-single/{task-id}/pay-and-register-cheque")
    public OrderDeliveryTaskDto payAndRegisterCheque(Long taskId, OrderPaymentType paymentType,
                                                     OrderChequeType chequeType) {
        OrderChequeRemoteDto requestBody = new OrderChequeRemoteDto();
        requestBody.setPaymentType(paymentType);
        requestBody.setChequeType(chequeType);
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-delivery-single/{}/pay-and-register-cheque", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery-single/{task-id}/fail")
    public OrderDeliveryTaskDto cancelOrder(Long taskId,
                                            OrderDeliveryTaskFailReasonType failReasonType) {
        OrderDeliveryFailReasonDto requestBody = new OrderDeliveryFailReasonDto();
        requestBody.setReason(failReasonType);
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-delivery-single/{}/fail", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery-single/{task-id}/reopen")
    public OrderDeliveryTaskDto reopenOrder(Long taskId) {
        return requestSpecification()
                .when()
                .body("{}")
                .post(sf("/api/tasks/order-delivery-single/{}/reopen", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery-single/{task-id}/pay")
    public OrderDeliveryTaskDto pay(Long taskId, OrderPaidDto requestBody) {
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-delivery-single/{}/pay", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов GET /api/shifts/{shiftId}/rescheduleDates")
    public RescheduleDatesDto rescheduleDates(Long shiftId) {
        return requestSpecification()
                .when()
                .get(sf("/api/shifts/{}/rescheduleDates", shiftId))
                .then().spec(responseSpecification)
                .extract().response().as(RescheduleDatesDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery-single/{task-id}/reschedule")
    public OrderDeliveryTaskDto rescheduleOrder(Long taskId,
                                                DeliveryRescheduleDto requestBody) {
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-delivery-single/{}/reschedule", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTaskDto.class);
    }

    @Step("Вызов GET /api/tasks/order-delivery")
    public RemainingOrderDeliveryTasksDto getOrderDeliveryTasks() {
        return requestSpecification()
                .when()
                .get("/api/tasks/order-delivery")
                .then().spec(responseSpecification)
                .extract().response().as(RemainingOrderDeliveryTasksDto.class);
    }

    @Step("Вызов POST /api/tasks/order-return/{task-id}/task/finish")
    public OrderReturnTaskDto finishOrderReturnTask(Long taskId) {
        return requestSpecification()
                .when()
                .body("{}")
                .post(sf("/api/tasks/order-return/{}/task/finish", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderReturnTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-return/{task-id}/cash/return")
    public OrderReturnTaskDto cashReturn(Long taskId) {
        return requestSpecification()
                .when()
                .body("{}")
                .post(sf("/api/tasks/order-return/{}/cash/return", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderReturnTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-return/{task-id}/finish")
    public OrderReturnTaskDto finishOrderReturn(Long taskId,
                                                List<OrderScanTaskDto.OrderForScanDto> orderForScanDto,
                                                List<OrderScanTaskDto.OrderForScanDto> skippedOrders) {
        OrderReturnTaskDto requestBody = new OrderReturnTaskDto();
        requestBody.setCompletedOrders(orderForScanDto);
        requestBody.setSkippedOrders(skippedOrders);
        return requestSpecification()
                .when()
                .body(requestBody)
                .post(sf("/api/tasks/order-return/{}/finish", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderReturnTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-return/{task-id}/start")
    public OrderReturnTaskDto startOrderReturnTask(Long taskId) {
        return requestSpecification()
                .when()
                .body("{}")
                .post(sf("/api/tasks/order-return/{}/start", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderReturnTaskDto.class);
    }

    @Step("Вызов POST /api/call-tasks/{id}/attempt-success")
    public CallTaskDto callTaskAttemptSuccess(Long callTaskId) {
        return requestSpecification()
                .when()
                .body(new CourierNotesDto(""))
                .post(sf("/api/call-tasks/{}/attempt-success", callTaskId))
                .then().spec(responseSpecification)
                .extract().response().as(CallTaskDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery/{taskId}/photo/upload")
    public PhotoDto uploadPhoto(Long taskId, File photo) {
        return requestSpecification()
                .when()
                .contentType(org.apache.http.entity.ContentType.MULTIPART_FORM_DATA.getMimeType())
                .multiPart("photoFile", photo)
                .post(sf("/api/tasks/order-delivery/{}/photo/upload", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(PhotoDto.class);
    }

    @Step("Вызов POST /api/location")
    public LocationDto postLocation(LocationDto locationDto) {
        return requestSpecification()
                .when()
                .body(locationDto)
                .post("/api/location")
                .then().spec(responseSpecification)
                .extract().response().as(LocationDto.class);
    }

    @Step("Вызов POST /api/route-points/{}/tasks/locker-delivery/{}/finish-load")
    public LockerDeliveryTaskDto finishLockerLoad(Long routePointId, Long taskId, OrderScanTaskRequestDto request) {
        request.setScannedOutsidePlaces(extractOutsidePlaces(request.getCompletedOrders()));
        return requestSpecification()
                .when()
                .body(request)
                .post(sf("/api/route-points/{}/tasks/locker-delivery/{}/finish-load", routePointId, taskId))
                .then().spec(responseSpecification)
                .extract().response().as(LockerDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/route-points/{}/tasks/locker-delivery/{}/finish-unload")
    public LockerDeliveryTaskDto finishLockerUnload(Long routePointId, Long taskId,
                                                    PickupPointScanTaskRequestDto request) {
        return requestSpecification()
                .when()
                .body(request)
                .post(sf("/api/route-points/{}/tasks/locker-delivery/{}/finish-unload", routePointId, taskId))
                .then().spec(responseSpecification)
                .extract().response().as(LockerDeliveryTaskDto.class);
    }

    @Step("Вызов POST /api/scanner/v2")
    public ScannerOrderDto scanOrder(String externalOrderId) {
        return requestSpecification()
                .when()
                .queryParam("barcodes", externalOrderId)
                .get("/api/scanner/v2")
                .then().spec(responseSpecification)
                .extract().response().as(ScannerOrderDto.class);
    }

    @Step("Вызов POST /api/tasks/locker-delivery/{}/cancel")
    public TaskDto cancelLockerDeliveryTask(Long taskId, OrderDeliveryTaskFailReasonType reasonType, String comment) {
        return requestSpecification()
                .when()
                .body(new LockerDeliveryCancelRequestDto(reasonType, comment, null))
                .post(sf("/api/tasks/locker-delivery/{}/cancel", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(TaskDto.class);
    }

    @Step("Вызов POST /api/tasks/locker-delivery/{}/reopen")
    public TaskDto reopenLockerDeliveryTask(Long taskId) {
        return requestSpecification()
                .when()
                .post(sf("/api/tasks/locker-delivery/{}/reopen", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(TaskDto.class);
    }

    @Step("Вызов GET /api/tasks")
    public OrderDeliveryTasksDto getTasks() {
        return requestSpecification()
                .when()
                .get("/api/tasks")
                .then().spec(responseSpecification)
                .extract().response().as(OrderDeliveryTasksDto.class);
    }

    @Step("Вызов POST /api/tasks/order-delivery/{}/postpone")
    public MultiOrderDeliveryTaskDto postponeMultiOrder(String multiOrderId, DeliveryPostponeDto body) {
        return requestSpecification()
                .when()
                .body(body)
                .post(sf("/api/tasks/order-delivery/{}/postpone", multiOrderId))
                .then().spec(responseSpecification)
                .extract().response().as(MultiOrderDeliveryTaskDto.class);
    }

    @Step("Вызов PUT /api/orders/update-items-instances-purchase-status")
    public UpdateItemsInstancesPurchaseStatusResponseDto updateItemsInstancesPurchaseStatus(
            UpdateItemsInstancesPurchaseStatusRequestDto body) {
        return requestSpecification()
                .when()
                .body(body)
                .put(sf("/api/orders/update-items-instances-purchase-status"))
                .then().spec(responseSpecification)
                .extract().response().as(UpdateItemsInstancesPurchaseStatusResponseDto.class);
    }

    @Step("Вызов POST /api/partial-return-orders/link-returnable-items-with-boxes")
    public void createLogisticReturn(LinkReturnableItemsInstancesWithBoxesRequestDto body) {
        requestSpecification()
                .when()
                .body(body)
                .post(sf("/api/partial-return-orders/link-returnable-items-with-boxes"))
                .then().statusCode(HttpStatus.OK.value());

    }

    @Step("Вызов GET /api/tasks/order-pickup/{task-id}")
    public OrderPickupTaskDto getOrderPickupTask(Long taskId) {
        return requestSpecification()
                .when()
                .get(sf("/api/tasks/order-pickup/{}", taskId))
                .then().spec(responseSpecification)
                .extract().response().as(OrderPickupTaskDto.class);
    }

    private Set<ScannedPlaceDto> extractOutsidePlaces(List<OrderScanTaskDto.OrderForScanDto> completedOrders) {
        return completedOrders.stream().flatMap(order -> {
                    if (order.getPlaces().isEmpty()) {
                        return Stream.of(new ScannedPlaceDto(order.getExternalOrderId(),
                                order.getExternalOrderId()));
                    }
                    return order.getPlaces().stream()
                            .map(place -> new ScannedPlaceDto(order.getExternalOrderId(), place.getBarcode()));
                })
                .collect(Collectors.toSet());
    }
}
