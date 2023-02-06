package ru.yandex.market.tpl.integration.tests.client;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.manual.CreateDeliveryTasksRequest;
import ru.yandex.market.tpl.api.model.manual.CreateDeliveryTasksResponse;
import ru.yandex.market.tpl.api.model.manual.CreateRoutePointRequestDto;
import ru.yandex.market.tpl.api.model.order.CreateOrderDto;
import ru.yandex.market.tpl.api.model.order.DetailedOrderDto;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderIdDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.ReassignResultDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.shift.ShiftDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.api.model.user.UserListDto;
import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRoutingPropertiesDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleColorDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDataDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleDto;
import ru.yandex.market.tpl.api.model.user.partner.vehicle.PartnerUserVehicleRegistrationNumberCountry;
import ru.yandex.market.tpl.api.model.vehicle.VehicleInstanceTypeDto;
import ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration;
import ru.yandex.market.tpl.integration.tests.stress.StressStatFilter;

import static ru.yandex.market.tpl.common.util.StringFormatter.sf;
import static ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration.X_YA_SERVICE_TICKET;

@Component
@Slf4j
public class ManualApiClient implements ApiClient {
    private final StressStatFilter stressStatFilter;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(ContentType.JSON)
            .build();
    private final TvmConfiguration.TvmTicketProvider tplTvmTicketProvider;
    @Value("${api.url.manual}")
    private String manualApiURL;

    public ManualApiClient(StressStatFilter stressStatFilter,
                           @Qualifier("tplTvmTicketProvider") TvmConfiguration.TvmTicketProvider tplTvmTicketProvider) {
        this.stressStatFilter = stressStatFilter;
        this.tplTvmTicketProvider = tplTvmTicketProvider;
    }

    RequestSpecification requestSpecification() {
        return RestAssured
                .given()
                .urlEncodingEnabled(true)
                .filters(stressStatFilter, new AllureRestAssured(), new RequestLoggingFilter(),
                        new ResponseLoggingFilter())
                .baseUri(manualApiURL)
                .contentType(ContentType.JSON)
                .header("x-company-id", 1000848111L)
                .header(X_YA_SERVICE_TICKET, tplTvmTicketProvider.provideServiceTicket());
    }

    @Step("Вызов DELETE /manual/users")
    public void deleteCourier(Long userId, String email) {
        Map<String, Object> queryParams = new HashMap<>();
        Optional.ofNullable(userId).ifPresent(v -> queryParams.put("userId", userId));
        Optional.ofNullable(email).ifPresent(v -> queryParams.put("email", email));
        requestSpecification()
                .when()
                .queryParams(queryParams)
                .delete("/users")
                .then().spec(responseSpecification);
    }

    @Step("Вызов DELETE /manual/users")
    public void deleteCouriers(Collection<String> emails) {
        Map<String, Object> queryParams = new HashMap<>();
        requestSpecification()
                .when()
                .queryParam("email", emails)
                .delete("/users")
                .then().spec(responseSpecification);
    }


    @Step("Вызов POST /manual/user-shifts/demo")
    public UserShiftDto createDefaultShiftWithTasks(Long uid) {
        return requestSpecification()
                .when()
                .queryParam("uid", uid)
                .post("/user-shifts/demo")
                .then().spec(responseSpecification)
                .extract().as(UserShiftDto.class);
    }

    @Step("Вызов GET /manual/users")
    public UserListDto findUserByEmail(String email) {
        return requestSpecification()
                .when()
                .queryParam("email", email)
                .get("/users")
                .then().spec(responseSpecification)
                .extract().as(UserListDto.class);
    }

    @Step("Вызов POST /manual/shifts/{date}/create")
    public ShiftDto createOpenShift(LocalDate localDate) {
        return requestSpecification()
                .when()
                .queryParam("open", true)
                .post(sf("/shifts/{}/create", localDate))
                .then().spec(responseSpecification)
                .extract().as(ShiftDto.class);
    }

    @Step("Вызов POST /manual/user-shifts")
    public UserShiftDto createUserShift(Long uid, Long shiftId) {
        return requestSpecification()
                .when()
                .queryParam("uid", uid)
                .queryParam("shiftId", shiftId)
                .queryParams("forceCreateAndActivate", true)
                .post("/user-shifts")
                .then().spec(responseSpecification)
                .extract().as(UserShiftDto.class);
    }

    @Step("Вызов POST /manual/user-shifts/demoFromCsv")
    public ReassignResultDto createOrdersFromCsv(Long uid, String csv) {
        return requestSpecification()
                .when()
                .contentType(ContentType.TEXT)
                .queryParam("uid", uid)
                .body(csv)
                .post("/user-shifts/demoFromCsv")
                .then().spec(responseSpecification)
                .extract().as(ReassignResultDto.class);
    }

    @Step("Вызов POST /manual/orders/create")
    public OrderDto createOrder(CreateOrderDto createOrderDto) {
        return requestSpecification()
                .when()
                .contentType(ContentType.JSON)
                .body(createOrderDto)
                .post("/orders/create")
                .then().spec(responseSpecification)
                .extract().as(OrderDto.class);
    }

    @Step("Вызов POST /manual/route-points/new-empty")
    public RoutePointDto createEmptyRoutePoint(Long uid, Long userShiftId, CreateRoutePointRequestDto requestDto) {
        return requestSpecification()
                .when()
                .queryParam("uid", uid)
                .queryParam("userShiftId", userShiftId)
                .body(requestDto)
                .post("/route-points/new-empty")
                .then().spec(responseSpecification)
                .extract().as(RoutePointDto.class);
    }

    @Step("Вызов POST /manual/route-points/new-simple")
    public RoutePointDto generateOrderTaskAndAssign(Long uid, Long userShiftId, CreateRoutePointRequestDto requestDto) {
        return requestSpecification()
                .when()
                .queryParam("uid", uid)
                .queryParam("userShiftId", userShiftId)
                .body(requestDto)
                .post("/route-points/new-simple")
                .then().spec(responseSpecification)
                .extract().as(RoutePointDto.class);
    }

    @Step("Вызов POST /manual/route-points/{id}/tasks/order-delivery")
    public RoutePointDto addDeliveryTask(Long routePointId, Long uid, Long userShiftId,
                                         OrderPaymentStatus paymentStatus, OrderPaymentType paymentType,
                                         String recipientNotes) {
        return requestSpecification()
                .when()
                .queryParam("uid", uid)
                .queryParam("userShiftId", userShiftId)
                .queryParam("paymentStatus", paymentStatus)
                .queryParam("paymentType", paymentType)
                .queryParam("recipientNotes", recipientNotes)
                .post(sf("/route-points/{}/tasks/order-delivery", routePointId))
                .then().spec(responseSpecification)
                .extract().as(RoutePointDto.class);
    }

    @Step("Вызов DELETE /manual/orders/{id}")
    public void deleteOrder(String orderId) {
        requestSpecification()
                .when()
                .delete(sf("/orders/{}", orderId))
                .then().spec(responseSpecification);
    }

    @Step("Вызов DELETE /manual/orders")
    public void deleteOrders(List<String> externalOrderIds) {
        List<List<String>> partition = Lists.partition(externalOrderIds, 20);
        partition.forEach(part -> requestSpecification()
                .when()
                .queryParam("externalOrderIds", part)
                .delete("/orders")
                .then().spec(responseSpecification));
    }

    @Step("Вызов POST /manual/users")
    public PartnerUserDto createCourier(String courierEmail,
                                        long uid,
                                        boolean recipientCallEnabled,
                                        PartnerUserRoutingPropertiesDto routingProperties,
                                        String lastPhoneNumber) {
        String phone = "+79991010000";
        PartnerUserDto requestBody = PartnerUserDto.builder()
                .uid(uid)
                .email(courierEmail)
                .firstName("first-name")
                .lastName("last-name")
                .role(UserRole.COURIER)
                .companyName("Яндекс.Маркет")
                .phone(phone.substring(0, phone.length() - lastPhoneNumber.length()) + lastPhoneNumber)
                .recipientCallEnabled(recipientCallEnabled)
                .routingProperties(routingProperties)
                .vehicles(List.of(
                        PartnerUserVehicleDto.builder()
                                .registrationNumber("A111AA")
                                .registrationNumberRegion("077")
                                .registrationNumberCountry(PartnerUserVehicleRegistrationNumberCountry.RUS)
                                .color(PartnerUserVehicleColorDto.builder()
                                        .id(41L)
                                        .build())
                                .type(VehicleInstanceTypeDto.PERSONAL)
                                .vehicleData(PartnerUserVehicleDataDto.builder()
                                        .id(1294L)
                                        .build())
                                .build()
                ))
                .build();
        return requestSpecification()
                .when()
                .body(requestBody)
                .post("/users")
                .then().spec(responseSpecification)
                .extract().response().as(PartnerUserDto.class);
    }

    @Step("Вызов POST /manual/create-delivery-tasks")
    public CreateDeliveryTasksResponse addDeliveryTasks(CreateDeliveryTasksRequest request) {
        return requestSpecification()
                .when()
                .body(request)
                .post("/create-delivery-tasks")
                .then().spec(responseSpecification)
                .extract().as(CreateDeliveryTasksResponse.class);
    }

    @Step("Вызов GET /manual/orders/detailed")
    public DetailedOrderDto getDetailedOrderInfo(String orderId) {
        return requestSpecification()
                .when()
                .queryParam("orderId", orderId)
                .get("/orders/detailed")
                .then().spec(responseSpecification)
                .extract().as(DetailedOrderDto.class);
    }

    @Step("Вызов GET /manual/orders/detailed")
    public DetailedOrderDto getDetailedOrderInfoByExtId(String externalOrderId) {
        return requestSpecification()
                .when()
                .queryParam("externalOrderId", externalOrderId)
                .get("/orders/detailed")
                .then().spec(responseSpecification)
                .extract().as(DetailedOrderDto.class);
    }

    @Step("Вызов GET /manual/orders/id")
    public OrderIdDto getOrderId(String externalOrderId) {
        return requestSpecification()
                .when()
                .queryParam("externalOrderId", externalOrderId)
                .get("/orders/id")
                .then().spec(responseSpecification)
                .extract().response().as(OrderIdDto.class);
    }

    @Step("Вызов POST /manual/route-points/new-simple")
    public RoutePointDto createSimpleRoutePoint(Long uid, Long userShiftId, CreateRoutePointRequestDto requestDto) {
        return requestSpecification()
                .when()
                .queryParam("uid", uid)
                .queryParam("userShiftId", userShiftId)
                .body(requestDto)
                .post("/route-points/new-simple")
                .then().spec(responseSpecification)
                .extract().as(RoutePointDto.class);
    }

    @Step("Вызов POST /manual/update-cache")
    public void updateCaches() {
        requestSpecification()
                .when()
                .post("/update-cache")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Step("Вызов POST /manual/transfer-act/sign")
    public void signTransferAct(Long userId) {

        int statusCode = 0;
        int attemptCount = 0;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info(e.getMessage());
                attemptCount++;
            }
            statusCode = requestSpecification()
                    .when()
                    .queryParam("userId", userId)
                    .post("/transfer-act/sign")
                    .statusCode();
        } while (statusCode != 200 && attemptCount < 30);
        if (statusCode != 200) {
            throw new IllegalStateException("Transfer act can't be signed after " + attemptCount + " attempts");
        }

    }

}
