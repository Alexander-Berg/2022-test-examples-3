package ru.yandex.market.tpl.client.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.client.BaseClientTest;
import ru.yandex.market.tpl.client.billing.dto.BillingCompanyContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingCompanyDto;
import ru.yandex.market.tpl.client.billing.dto.BillingDropOffReturnMovementContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingDropOffReturnMovementDto;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingIntakeDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingOrderDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSortingCenterContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSortingCenterDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSurchargePageDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BillingClientImplTest extends BaseClientTest {
    private static final LocalDate DATE = LocalDate.of(2021, 1, 1);
    private static final int MOSCOW_REG_ID = 213;
    private static final long MOSCOW_OBL_REG_ID = 1;
    private static final int LEN_OBL_VIL_REG_ID = 205959;
    private static final long LEN_OBL_REG_ID = 10174;
    private static final List<Long> SUBJECT_FEDERATION_REGION_IDS = List.of(MOSCOW_OBL_REG_ID, LEN_OBL_REG_ID);

    @Autowired
    private BillingClient billingClient;

    @Test
    void findUsers() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/users?companyId=1"))
                .andRespond(withSuccess(
                        new ClassPathResource("response/billing/response_billing_users.json"),
                        MediaType.APPLICATION_JSON
                ));

        BillingUserContainerDto actual = billingClient.findUsers(1);
        BillingUserContainerDto expected = BillingUserContainerDto.builder()
                .users(List.of(
                        BillingUserDto.builder()
                                .id(1L)
                                .dsmId("1")
                                .type(BillingUserType.PARTNER)
                                .name("name1")
                                .uid(123)
                                .email("321")
                                .build()
                ))
                .build();
        Assertions.assertEquals(expected, actual, "users");
    }

    @Test
    void findUsers_withPaging() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(new StringStartsWith(
                        tplIntUrl + "/billing/users?"
                )))
                .andExpect(queryParam("companyId", "1"))
                .andExpect(queryParam("pageNumber", "0"))
                .andExpect(queryParam("pageSize", "1"))
                .andRespond(withSuccess(
                        new ClassPathResource("response/billing/response_billing_users.json"),
                        MediaType.APPLICATION_JSON
                ));

        BillingUserContainerDto actual = billingClient.findUsers(1, 0, 1);
        BillingUserContainerDto expected = BillingUserContainerDto.builder()
                .users(List.of(
                        BillingUserDto.builder()
                                .id(1L)
                                .dsmId("1")
                                .type(BillingUserType.PARTNER)
                                .name("name1")
                                .uid(123)
                                .email("321")
                                .build()
                ))
                .build();
        Assertions.assertEquals(expected, actual, "users");
    }

    @Test
    void findCompanies() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/companies"))
                .andRespond(withSuccess(
                        new ClassPathResource("response/billing/response_billing_companies.json"),
                        MediaType.APPLICATION_JSON
                ));

        BillingCompanyContainerDto actual = billingClient.findCompanies();
        BillingCompanyContainerDto expected = BillingCompanyContainerDto.builder()
                .companies(List.of(
                        BillingCompanyDto.builder()
                                .id(1L)
                                .dsmId("1")
                                .name("name1")
                                .taxpayerNumber("123")
                                .ogrn("321")
                                .deactivated(false)
                                .isSuperCompany(true)
                                .build()
                ))
                .build();
        Assertions.assertEquals(expected, actual, "companies");
    }

    @Test
    void findCompanies_withPaging() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(new StringStartsWith(
                        tplIntUrl + "/billing/companies?"
                )))
                .andExpect(queryParam("pageNumber", "0"))
                .andExpect(queryParam("pageSize", "1"))
                .andRespond(withSuccess(
                        new ClassPathResource("response/billing/response_billing_companies.json"),
                        MediaType.APPLICATION_JSON
                ));

        BillingCompanyContainerDto actual = billingClient.findCompanies(0, 1);
        BillingCompanyContainerDto expected = BillingCompanyContainerDto.builder()
                .companies(List.of(
                        BillingCompanyDto.builder()
                                .id(1L)
                                .dsmId("1")
                                .name("name1")
                                .taxpayerNumber("123")
                                .ogrn("321")
                                .deactivated(false)
                                .isSuperCompany(true)
                                .build()
                ))
                .build();
        Assertions.assertEquals(expected, actual, "companies");
    }

    @Test
    void findShifts() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/shifts?date={date}",
                        DATE.format(DateTimeFormatter.ISO_DATE)))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_shifts.json"),
                        MediaType.APPLICATION_JSON));

        BillingUserShiftContainerDto actual = billingClient.findShifts(DATE);
        BillingUserShiftContainerDto expected = BillingUserShiftContainerDto.builder()
                .userShifts(List.of(
                        BillingUserShiftDto.builder()
                                .id(2L)
                                .shiftDate(DATE)
                                .sortingCenterId(5L)
                                .companyId(6L)
                                .companyDsmId("6")
                                .userId(7L)
                                .userDsmId("7")
                                .distance(444)
                                .routingVehicleType(RoutingVehicleType.COMMON)
                                .transportTypeId(0L)
                                .build()
                ))
                .build();
        Assertions.assertEquals(expected, actual, "shifts");
    }

    @Test
    void findOrders() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/orders?userShiftIds={shiftId}", 123))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_orders.json"),
                        MediaType.APPLICATION_JSON));

        BillingOrderContainerDto actual = billingClient.findOrders(Set.of(123L));
        BillingOrderContainerDto expected = BillingOrderContainerDto.builder()
                .orders(List.of(BillingOrderDto.builder()
                        .userShiftId(123L)
                        .id(2L)
                        .marketOrderId("234")
                        .longitude(BigDecimal.valueOf(1111.111))
                        .latitude(BigDecimal.valueOf(2222.222))
                        .multiOrderId("2")
                        .recipientPhone("223322")
                        .deliveryTaskStatus("DELIVERED")
                        .pickupSubtaskStatus("FINISHED")
                        .taskId(3L)
                        .routePointId(4L)
                        .taskType("ORDER_DELIVERY")
                        .pickupPointId(5L)
                        .pickupPointType("LOCKER")
                        .placeCount(1)
                        .deliveryIntervalFrom(Instant.parse("1990-01-01T08:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("1990-01-01T12:00:00Z"))
                        .finishedAt(Instant.parse("1990-01-01T10:00:00Z"))
                        .weight(BigDecimal.valueOf(1.2))
                        .width(20)
                        .height(30)
                        .length(10)
                        .dimensionsClass("REGULAR_CARGO")
                        .build()))
                .build();
        Assertions.assertEquals(expected, actual, "orders");
    }

    @Test
    void findIntakes() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/intakes?userShiftIds={shiftId}", 123))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_intakes.json"),
                        MediaType.APPLICATION_JSON));

        BillingIntakeContainerDto actual = billingClient.findIntakes(Set.of(123L));
        BillingIntakeContainerDto expected = BillingIntakeContainerDto.builder()
                .intakes(List.of(BillingIntakeDto.builder()
                        .userShiftId(123L)
                        .longitude(BigDecimal.valueOf(1111.111))
                        .latitude(BigDecimal.valueOf(2222.222))
                        .taskId(3L)
                        .collectDropshipTaskStatus("FINISHED")
                        .movementId(1234L)
                        .movementExternalId("ext")
                        .warehouseYandexId("ya")
                        .routePointId(4L)
                        .taskType("COLLECT_DROPSHIP")
                        .deliveryIntervalFrom(Instant.parse("1990-01-01T08:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("1990-01-01T12:00:00Z"))
                        .finishedAt(Instant.parse("1990-01-01T10:00:00Z"))
                        .build()))
                .build();
        Assertions.assertEquals(expected, actual, "intakes");
    }

    @Test
    void findDropOffReturnMovements() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/dropOff-movements?userShiftIds={shiftId}", 123))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_dropOff_movements.json"),
                        MediaType.APPLICATION_JSON));

        BillingDropOffReturnMovementContainerDto actual = billingClient.findDropOffReturnMovements(Set.of(123L));
        BillingDropOffReturnMovementContainerDto expected = BillingDropOffReturnMovementContainerDto.builder()
                .movements(List.of(BillingDropOffReturnMovementDto.builder()
                        .userShiftId(123L)
                        .longitude(BigDecimal.valueOf(1111.111))
                        .latitude(BigDecimal.valueOf(2222.222))
                        .taskId(3L)
                        .taskStatus("FINISHED")
                        .movementId(1234L)
                        .movementExternalId("ext")
                        .warehouseYandexId("ya")
                        .routePointId(4L)
                        .taskType("LOCKER_DELIVERY")
                        .deliveryIntervalFrom(Instant.parse("1990-01-01T08:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("1990-01-01T12:00:00Z"))
                        .finishedAt(Instant.parse("1990-01-01T10:00:00Z"))
                        .deliveredCargoCount(10L)
                        .build()))
                .build();
        Assertions.assertEquals(expected, actual, "movements");
    }

    @Test
    void findSortingCenters() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/sorting-centers"))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_sorting_centers.json"),
                        MediaType.APPLICATION_JSON));

        BillingSortingCenterContainerDto actual = billingClient.findSortingCenters();
        BillingSortingCenterContainerDto expected = getSortingCenters();

        Assertions.assertEquals(expected, actual, "sorting-centers");
    }

    @Test
    void getRegionIdsOfSubjectFederationType() throws Exception {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestToUriTemplate(tplIntUrl + "/billing/region-ids"))
                .andExpect(content().json(getRequestContent()))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_get_region_ids.json"),
                        MediaType.APPLICATION_JSON));

        List<Long> actual =
                billingClient.getRegionIdsOfSubjectFederationType(getSortingCenters().getSortingCenters());

        Assertions.assertEquals(SUBJECT_FEDERATION_REGION_IDS, actual, "sorting-centers");

    }

    @Test
    void shouldFindUserSurcharges() {
        LocalDate date = LocalDate.of(2021, 01, 01);
        int pageNumber = 1;
        int pageSize = 1;
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(new StringStartsWith(tplIntUrl +
                        "/billing/surcharges?")))
                .andExpect(queryParam("pageNumber", String.valueOf(pageNumber)))
                .andExpect(queryParam("pageSize", String.valueOf(pageSize)))
                .andExpect(queryParam("date", date.format(DateTimeFormatter.ISO_DATE)))
                .andRespond(withSuccess(new ClassPathResource("response/billing/response_billing_surcharges.json"),
                        MediaType.APPLICATION_JSON));

        BillingSurchargePageDto actual = billingClient.findUserSurcharges(date, pageNumber, pageSize);
        Assertions.assertEquals(1, actual.getContent().size());
        Assertions.assertEquals("billing-surcharge-id", actual.getContent().get(0).id);
    }

    private String getRequestContent() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(getSortingCenters());
    }

    private BillingSortingCenterContainerDto getSortingCenters() {
        return BillingSortingCenterContainerDto.builder()
                .sortingCenters(List.of(
                        BillingSortingCenterDto.builder()
                                .id(1L)
                                .name("СЦ ГОРОД Х")
                                .regionId((long) MOSCOW_REG_ID)
                                .build(),
                        BillingSortingCenterDto.builder()
                                .id(2L)
                                .name("СЦ ГОРОД Y")
                                .regionId((long) LEN_OBL_VIL_REG_ID)
                                .build()
                ))
                .build();
    }
}
