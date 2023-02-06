package ru.yandex.market.tpl.internal.controller.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
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
import ru.yandex.market.tpl.client.billing.dto.BillingSurchargeDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserShiftDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserType;
import ru.yandex.market.tpl.core.domain.order.CargoType;
import ru.yandex.market.tpl.core.domain.surcharge.entity.SurchargeResolution;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.billing.BillingService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(BillingController.class)
class BillingControllerTest extends BaseShallowTest {
    private static final LocalDate DATE = LocalDate.of(2021, 01, 01);
    private static final long MOSCOW_REG_ID = 213;
    private static final long MOSCOW_OBL_REG_ID = 1;
    private static final long LEN_OBL_VIL_REG_ID = 205959;
    private static final long LEN_OBL_REG_ID = 10174;
    private static final long OTHER_OBL_REG_ID = 10904;
    private static final List<Long> SUBJECT_TYPE_REGION_IDS = List.of(MOSCOW_OBL_REG_ID, LEN_OBL_REG_ID);

    @MockBean
    private BillingService billingService;

    @Test
    void findUsers() throws Exception {
        when(billingService.findUsers(1, null, null)).thenReturn(BillingUserContainerDto.builder()
                .users(List.of(
                        BillingUserDto.builder()
                                .id(1L)
                                .dsmId("a")
                                .type(BillingUserType.PARTNER)
                                .name("name1")
                                .uid(123)
                                .email("321")
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/users?companyId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_users.json")));
    }

    @Test
    void findUsers_withPaging() throws Exception {
        when(billingService.findUsers(1, 0, 1)).thenReturn(BillingUserContainerDto.builder()
                .users(List.of(
                        BillingUserDto.builder()
                                .id(1L)
                                .dsmId("a")
                                .type(BillingUserType.PARTNER)
                                .name("name1")
                                .uid(123)
                                .email("321")
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/users?companyId=1&pageNumber=0&pageSize=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_users.json")));
    }

    @Test
    void findCompanies() throws Exception {
        when(billingService.findCompanies(null, null)).thenReturn(BillingCompanyContainerDto.builder()
                .companies(List.of(
                        BillingCompanyDto.builder()
                                .id(1L)
                                .dsmId("a")
                                .name("name1")
                                .taxpayerNumber("123")
                                .ogrn("321")
                                .deactivated(false)
                                .isSuperCompany(true)
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/companies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_companies.json")));
    }

    @Test
    void findCompanies_withPaging() throws Exception {
        when(billingService.findCompanies(0, 1)).thenReturn(BillingCompanyContainerDto.builder()
                .companies(List.of(
                        BillingCompanyDto.builder()
                                .id(1L)
                                .dsmId("a")
                                .name("name1")
                                .taxpayerNumber("123")
                                .ogrn("321")
                                .deactivated(false)
                                .isSuperCompany(true)
                                .build()
                ))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/companies?pageNumber=0&pageSize=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_companies.json")));
    }

    @Test
    void findShifts() throws Exception {
        when(billingService.findShifts(DATE)).thenReturn(
                BillingUserShiftContainerDto.builder()
                        .userShifts(List.of(
                                BillingUserShiftDto.builder()
                                        .id(2L)
                                        .shiftDate(DATE)
                                        .sortingCenterId(5L)
                                        .distance(444)
                                        .companyId(6L)
                                        .companyDsmId("a")
                                        .userId(7L)
                                        .userDsmId("b")
                                        .routingVehicleType(RoutingVehicleType.COMMON)
                                        .transportTypeId(0L)
                                        .build()
                        ))
                        .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/shifts?date={date}", DATE.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_shifts.json")));
    }

    @Test
    void findUserSurcharges() throws Exception {
        when(billingService.findUserSurcharges(DATE, 1, 1)).thenReturn(
                new PageImpl(Stream.of(BillingSurchargeDto.builder()
                        .id("billing-surcharge-id")
                        .createdAt(DATE.atStartOfDay().toInstant(ZoneOffset.UTC))
                        .eventDate(DATE)
                        .resolution(SurchargeResolution.COMMIT.name())
                        .type("test-surcharge")
                        .cargoType(CargoType.ADULT.name())
                        .companyId(123L)
                        .companyDsmId("company-dsm-id")
                        .scId(124L)
                        .userId(111L)
                        .userShiftId(112L)
                        .userDsmId("user-dsm-id")
                        .amount(BigDecimal.TEN)
                        .multiplier(2)
                        .build()
                ).collect(Collectors.toList()))
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/surcharges?date={date}&pageSize=1&pageNumber=1", DATE.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_surcharges.json")));
    }

    @Test
    void findOrders() throws Exception {
        when(billingService.findOrders(Set.of(123L))).thenReturn(BillingOrderContainerDto.builder()
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
                        .pickupPointType("PVZ")
                        .placeCount(1)
                        .deliveryIntervalFrom(Instant.parse("1990-01-01T08:00:00Z"))
                        .deliveryIntervalTo(Instant.parse("1990-01-01T12:00:00Z"))
                        .finishedAt(Instant.parse("1990-01-01T10:00:00Z"))
                        .weight(BigDecimal.valueOf(1.2))
                        .width(20)
                        .height(30)
                        .length(10)
                        .dimensionsClass("REGULAR_CARGO")
                        .pickupPointSubtype("LAVKA")
                        .build()))
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/orders?userShiftIds={shiftId}", 123)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_orders.json")));
    }

    @Test
    void findIntakes() throws Exception {
        when(billingService.findIntakes(Set.of(123L))).thenReturn(BillingIntakeContainerDto.builder()
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
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/intakes?userShiftIds={shiftId}", 123)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_intakes.json")));
    }

    @Test
    void findDropOffReturnMovements() throws Exception {
        when(billingService.findDropOffReturnMovements(Set.of(123L))).thenReturn(BillingDropOffReturnMovementContainerDto.builder()
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
                .build());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/dropOff-movements?userShiftIds={shiftId}", 123)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_dropoffReturn_movements.json")));
    }

    @Test
    void findSortingCenters() throws Exception {
        when(billingService.findSortingCenters()).thenReturn(getSortingCenters());

        mockMvc.perform(MockMvcRequestBuilders.get("/billing/sorting-centers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_sorting_centers.json")));
    }

    @Test
    void getSubjectFederationRegionIds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String sortingCentersJson = mapper.writeValueAsString(getSortingCenters());
        when(billingService.getRegionIdsByType(eq(getSortingCenters()), eq(RegionType.SUBJECT_FEDERATION)))
                .thenReturn(SUBJECT_TYPE_REGION_IDS);
        mockMvc.perform(MockMvcRequestBuilders.post("/billing/region-ids")
                        .content(sortingCentersJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("billing/response_billing_get_region_ids.json")));
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
