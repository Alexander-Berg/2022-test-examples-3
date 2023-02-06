package ru.yandex.market.tpl.integration.tests.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.company.PartnerUserCompanyDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerUpdateOrderRequestDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
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
import ru.yandex.market.tpl.integration.tests.stress.StressTestsUtil;

import static ru.yandex.market.tpl.integration.tests.configuration.TvmConfiguration.X_YA_SERVICE_TICKET;

@Component
public class PartnerApiClient implements ApiClient {
    public static final Long SORTING_CENTER_ID = 1L;
    private final StressStatFilter stressStatFilter;
    private final ResponseSpecification responseSpecification = new ResponseSpecBuilder()
            .expectStatusCode(HttpStatus.OK.value())
            .expectContentType(ContentType.JSON)
            .build();
    private final TvmConfiguration.TvmTicketProvider tplTvmTicketProvider;
    @Value("${api.url.partner}")
    private String partnerApiURL;

    public PartnerApiClient(StressStatFilter stressStatFilter,
                            @Qualifier("tplTvmTicketProvider")
                                    TvmConfiguration.TvmTicketProvider tplTvmTicketProvider) {
        this.stressStatFilter = stressStatFilter;
        this.tplTvmTicketProvider = tplTvmTicketProvider;
    }

    RequestSpecification requestSpecification() {
        RequestSpecification requestSpecification = RestAssured
                .given()
                .urlEncodingEnabled(false)
                .filters(stressStatFilter, new AllureRestAssured())
                .baseUri(partnerApiURL)
                .contentType(ContentType.JSON)
                .header("x-company-id", 1000848111L)
                .header(X_YA_SERVICE_TICKET, tplTvmTicketProvider.provideServiceTicket());
        if (!StressTestsUtil.isStressTestEnabled()) {
            requestSpecification.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        }
        return requestSpecification;
    }

    @Step("Вызов POST /internal/partner/users")
    public PartnerUserDto createCourier(String courierEmail, boolean recipientCallEnabled,
                                        PartnerUserRoutingPropertiesDto routingProperties) {
        PartnerUserDto requestBody = PartnerUserDto.builder()
                .email(courierEmail)
                .firstName("first-name")
                .lastName("last-name")
                .role(UserRole.COURIER)
                .company(
                        PartnerUserCompanyDto.builder()
                                .id(2901L)
                                .name("Яндекс.Маркет")
                                .build()
                )
                .recipientCallEnabled(recipientCallEnabled)
                .routingProperties(routingProperties)
                .phone("+79990000001")
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

    @Step("Вызов POST /internal/partner/users/{userId}/schedule-rules")
    public void createCourierSchedule(Long courierId) {
        LocalDate today = LocalDate.now();
        UserScheduleRuleDto requestBody = UserScheduleRuleDto.builder()
                .activeFrom(today)
                .activeTo(today.plusYears(1))
                .applyFrom(today)
                .scheduleType(UserScheduleType.ALWAYS_WORKS)
                .shiftStart(LocalTime.MIN)
                .shiftEnd(LocalTime.MAX)
                .sortingCenterId(SORTING_CENTER_ID)
                .vehicleType(CourierVehicleType.CAR)
                .build();
        requestSpecification()
                .when()
                .body(requestBody)
                .post("/users/{userId}/schedule-rules", courierId)
                .then().spec(responseSpecification)
                .extract().response().as(UserScheduleRuleDto.class);
    }

    public void updateOrder(Long orderId, PartnerUpdateOrderRequestDto request) {
        requestSpecification()
                .when()
                .body(request)
                .patch("/v2/orders/{orderId}", orderId)
                .then().spec(responseSpecification);
    }
}
