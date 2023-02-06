package ru.yandex.market.pvz.internal.controller.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerDto;
import ru.yandex.market.pvz.client.billing.dto.BillingOrderDto;
import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointDto;
import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointWorkingDaysDto;
import ru.yandex.market.pvz.client.billing.dto.BillingReturnDto;
import ru.yandex.market.pvz.client.model.order.OrderType;
import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.pvz.internal.domain.calendar.PickupPointCalendarBillingService;
import ru.yandex.market.pvz.internal.domain.legal_partner.LegalPartnerBillingService;
import ru.yandex.market.pvz.internal.domain.order.OrderBillingService;
import ru.yandex.market.pvz.internal.domain.pickup_point.PickupPointBillingService;
import ru.yandex.market.pvz.internal.domain.return_request.ReturnBillingService;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DEACTIVATION_WITH_REASONS;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BillingControllerTest extends BaseShallowTest {
    private static final long BALANCE_CLIENT_ID = 135L;
    private static final long PARTNER_ID = 531L;
    private static final String PARTNER_NAME = "партнёр";
    private static final String KPP = "кпп";
    private static final long DELIVERY_SERVICE_ID = 3;
    private static final String CONTRACT_NUMBER = "номер договора";
    private static final String OGRN = "огрн";
    private static final String VIRTUAL_ACCOUNT_NUMBER = "Виртуальный номер счёта в OEBS";
    private static final String PICKUP_POINT_NAME = "точка";
    private static final long PICKUP_POINT_ID = 1234;
    private static final LocalDate WORKING_DAY = LocalDate.of(2020, 12, 12);
    private static final LocalDate OFFER_SIGNED_SINCE = LocalDate.of(2021, 3, 3);

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestBrandRegionFactory brandRegionFactory;
    private final PickupPointQueryService pickupPointQueryService;
    private final LegalPartnerQueryService legalPartnerQueryService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final TestableClock clock;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean
    private OrderBillingService orderBillingService;

    @MockBean
    private ReturnBillingService returnBillingService;

    @MockBean
    private PickupPointBillingService pickupPointBillingService;

    @MockBean
    private LegalPartnerBillingService legalPartnerBillingService;

    @MockBean
    private PickupPointCalendarBillingService pickupPointCalendarBillingService;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.EPOCH, DateTimeUtil.DEFAULT_ZONE_ID);
        configurationGlobalCommandService.setValue(DEACTIVATION_WITH_REASONS, true);
        deactivationReasonCommandService.createDeactivationReason(LegalPartnerTerminationType.DEBT.getDescription(),
                "", false, false, null);
        deactivationReasonCommandService.createDeactivationReason(LegalPartnerTerminationType.CONTRACT_TERMINATED.getDescription(),
                "", false, false, null);
    }

    @Test
    public void getDispatchedReturns() throws Exception {
        when(returnBillingService.getDispatchedReturns(any(), any())).thenReturn(List.of(getReturn()));
        String fileContent = getFileContent("billing/response_dispatched_returns.json");
        ResultActions perform = mockMvc.perform(
                get("/billing/returns")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .param("from", "2020-10-10")
                        .param("to", "2020-10-10"));
        perform
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fileContent));
    }

    @Test
    public void getDeliveredOrders() throws Exception {
        when(orderBillingService.getDeliveredOrdersByDaysPeriod(any(), any(), any(), any()))
                .thenReturn(List.of(getOrder()));
        String fileContent = getFileContent("billing/response_delivery_orders.json");
        ResultActions perform = mockMvc.perform(
                get("/billing/delivered-orders")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .param("from", "2020-10-10")
                        .param("to", "2020-10-10"));
        perform
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fileContent, true));
    }

    @Test
    public void getDeliveredOrdersWithLimit() throws Exception {
        when(orderBillingService.getDeliveredOrdersByDaysPeriod(any(), any(), any(), any()))
                .thenReturn(List.of(getOrder()));
        String fileContent = getFileContent("billing/response_delivery_orders.json");
        ResultActions perform = mockMvc.perform(
                get("/billing/delivered-orders")
                        .contentType(MediaType.APPLICATION_JSON).content("{}")
                        .param("from", "2020-10-10")
                        .param("to", "2020-10-10")
                        .param("limit", "10")
                        .param("offset", "0")
        );
        perform
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fileContent, true));
    }

    @Test
    public void getPickupPoints() throws Exception {
        when(pickupPointBillingService.getAll()).thenReturn(List.of(getPickupPoint()));
        String fileContent = getFileContent("billing/response_pickup_points.json");
        ResultActions perform =
                mockMvc.perform(get("/billing/pickup-points").contentType(MediaType.APPLICATION_JSON).content("{}"));
        perform
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fileContent));
    }

    @Test
    public void getLegalPartners() throws Exception {
        when(legalPartnerBillingService.getAll()).thenReturn(List.of(getLegalPartner()));
        String fileContent = getFileContent("billing/response_legal_partners.json");
        ResultActions perform =
                mockMvc.perform(get("/billing/legal-partners").contentType(MediaType.APPLICATION_JSON).content("{}"));
        perform
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fileContent));
    }

    @Test
    public void getWorkingDays() throws Exception {
        when(pickupPointCalendarBillingService.getWorkingDays(any(), any())).thenReturn(getPointWorkingDaysDto());
        String fileContent = getFileContent("billing/response_working_days.json");
        ResultActions perform =
                mockMvc.perform(get("/billing/pickup-point-working-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .param("from", "2020-10-10")
                        .param("to", "2020-10-10")
                );
        perform
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(fileContent));
    }

    @Test
    public void postLegalPartnerTerminationDebtType() throws Exception {
        setClock();

        LegalPartner partner = createApprovedLegalPartner();
        PickupPoint pickupPoint = createBrandedPickupPoint(partner, "Воронеж");
        PickupPoint pickupPoint2 = createNotBrandedPickupPoint(partner);

        LegalPartner partner2 = createApprovedLegalPartner();

        String inputJson = String.format(
                getJsonFromFile("billing/request_termination_debt_type.json"),
                partner.getId(), clock.instant(), partner2.getId(), clock.instant()
        );
        String expectedJson = String.format(
                getJsonFromFile("billing/response_termination_debt_type.json"),
                partner.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_INSTANT), true,
                partner2.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_INSTANT), true
        );
        mockMvc.perform(post("/billing/legal-partners/termination")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(inputJson)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));

        PickupPointParams params = pickupPointQueryService.getHeavy(pickupPoint.getId());
        checkBrandedPickupPointDisabled(params);

        params = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        checkPickupPointDisabled(params);
    }

    private void checkBrandedPickupPointDisabled(PickupPointParams params) {
        assertThat(params.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
        checkPickupPointDisabled(params);
    }

    private void checkPickupPointDisabled(PickupPointParams params) {
        assertThat(params.getActive()).isTrue();
        assertThat(params.getCardAllowed()).isFalse();
        assertThat(params.getCashAllowed()).isFalse();
        assertThat(params.getPrepayAllowed()).isFalse();
        assertThat(params.getReturnAllowed()).isFalse();
    }

    private void checkBrandedPickupPointEnabled(PickupPointParams params) {
        assertThat(params.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
        checkPickupPointEnabled(params);
    }

    private void checkPickupPointEnabled(PickupPointParams params) {
        assertThat(params.getActive()).isTrue();
        assertThat(params.getCardAllowed()).isTrue();
        assertThat(params.getCashAllowed()).isTrue();
        assertThat(params.getPrepayAllowed()).isTrue();
        assertThat(params.getReturnAllowed()).isTrue();
    }

    @Test
    public void postLegalPartnerTerminationSameType() throws Exception {
        setClock();

        LegalPartner partner = createApprovedLegalPartner();
        PickupPoint pickupPoint = createBrandedPickupPoint(partner, "Москва");

        LegalPartner partner2 = createApprovedLegalPartner();
        PickupPoint pickupPoint2 = createBrandedPickupPoint(partner2, "Питер");

        LegalPartner partner3 = createApprovedLegalPartner();
        PickupPoint pickupPoint3 = createBrandedPickupPoint(partner3, "Ростов");

        String inputJson = String.format(
                getJsonFromFile("billing/request_termination_same_type.json"),
                partner.getId(), clock.instant(), partner2.getId(), clock.instant().plus(Period.ofDays(2)),
                partner2.getId(), clock.instant().plus(Period.ofDays(2)), partner3.getId(), clock.instant()
        );
        String expectedJson = String.format(
                getJsonFromFile("billing/response_termination_same_type.json"),
                partner.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_INSTANT), true,
                partner2.getId(), ZonedDateTime.now(clock).plusDays(2).format(DateTimeFormatter.ISO_INSTANT), true,
                partner3.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_INSTANT), true
        );
        mockMvc.perform(post("/billing/legal-partners/termination")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(inputJson)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));

        PickupPointParams params = pickupPointQueryService.getHeavy(pickupPoint.getId());
        checkBrandedPickupPointDisabled(params);

        params = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        checkBrandedPickupPointDisabled(params);

        params = pickupPointQueryService.getHeavy(pickupPoint3.getId());
        checkBrandedPickupPointDisabled(params);
    }

    @Test
    public void postLegalPartnerTerminationContractTerminatedType() throws Exception {
        setClock();

        LegalPartner partner = createApprovedLegalPartner();
        PickupPoint pickupPoint = createBrandedPickupPoint(partner, "Нижний Новгород");

        String inputJson = String.format(
                getJsonFromFile("billing/request_termination_contract_terminated_type.json"),
                partner.getId(), clock.instant()
        );
        String expectedJson = String.format(
                getJsonFromFile("billing/response_termination_contract_terminated_type.json"),
                partner.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_INSTANT), true
        );
        mockMvc.perform(post("/billing/legal-partners/termination")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(inputJson)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));

        PickupPointParams params = pickupPointQueryService.getHeavy(pickupPoint.getId());
        checkBrandedPickupPointDisabled(params);

        LegalPartnerParams legalPartnerParams = legalPartnerQueryService.get(partner.getId());
        assertThat(legalPartnerParams.getOfferTerminatedSince()).isNotNull();
    }

    @Test
    public void patchLegalPartnerTermination() throws Exception {
        setClock();

        LegalPartner partner = createApprovedLegalPartner();
        PickupPoint pickupPoint = createBrandedPickupPoint(partner, "Санкт-Петербург");

        LegalPartner partner2 = createApprovedLegalPartner();
        PickupPoint pickupPoint2 = createBrandedPickupPoint(partner2, "Оренбург");

        createTermination(LegalPartnerTerminationType.DEBT, pickupPoint, partner.getId());
        createTermination(LegalPartnerTerminationType.DEBT, pickupPoint2, partner2.getId());

        String inputJson = String.format(
                getJsonFromFile("billing/request_enable_legal_partners.json"),
                partner.getId(), LegalPartnerTerminationType.DEBT, partner2.getId(), LegalPartnerTerminationType.DEBT
        );
        String expectedJson = String.format(
                getJsonFromFile("billing/response_termination_debt_type.json"),
                partner.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), false,
                partner2.getId(), ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), false
        );
        mockMvc.perform(patch("/billing/legal-partners/termination")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(inputJson)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));

        PickupPointParams params = pickupPointQueryService.getHeavy(pickupPoint.getId());
        checkBrandedPickupPointEnabled(params);

        params = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        checkBrandedPickupPointEnabled(params);
    }

    @Test
    public void patchAbsentLegalPartnerTermination() throws Exception {
        setClock();

        LegalPartner partner = createApprovedLegalPartner();
        PickupPoint pickupPoint = createBrandedPickupPoint(partner, "Новороссийск");

        LegalPartner partner2 = createApprovedLegalPartner();
        PickupPoint pickupPoint2 = createBrandedPickupPoint(partner2, "Евпатория");

        LegalPartner partner3 = createApprovedLegalPartner();
        PickupPoint pickupPoint3 = createBrandedPickupPoint(partner2, "Ярославль");

        createTermination(LegalPartnerTerminationType.DEBT, pickupPoint, partner.getId());
        createTermination(LegalPartnerTerminationType.DEBT, pickupPoint2, partner2.getId());
        createTermination(LegalPartnerTerminationType.DEBT, pickupPoint3, partner3.getId());

        String inputJson = String.format(
                getJsonFromFile("billing/request_enable_legal_partners.json"),
                partner.getId(), LegalPartnerTerminationType.CONTRACT_TERMINATED,
                partner2.getId(), LegalPartnerTerminationType.CONTRACT_TERMINATED,
                partner3.getId(), LegalPartnerTerminationType.CONTRACT_TERMINATED
        );
        mockMvc.perform(patch("/billing/legal-partners/termination")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(inputJson)
        ).andExpect(status().is2xxSuccessful())
                .andExpect(content().json("[]"));

        PickupPointParams params = pickupPointQueryService.getHeavy(pickupPoint.getId());
        checkBrandedPickupPointDisabled(params);

        params = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        checkBrandedPickupPointDisabled(params);

        params = pickupPointQueryService.getHeavy(pickupPoint3.getId());
        checkBrandedPickupPointDisabled(params);
    }

    private void createTermination(LegalPartnerTerminationType type, PickupPoint pickupPoint, Long legalPartnerId) {
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        terminationFactory.createLegalPartnerTermination(
                        TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                                .params(
                                        TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                                .builder()
                                                .type(type)
                                                .fromTime(clock.instant().atOffset(zone))
                                .legalPartnerId(legalPartnerId)
                                .build()
                )
                .build()
        );
    }

    @Test
    public void createLegalPartnerIndebtedness() throws Exception {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        var json = String.format(getJsonFromFile("billing/request_indebtedness.json"), partner.getId());
        mockMvc.perform(post("/billing/legal-partners/indebtedness")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(json, true));
    }

    private void setClock() {
        clock.setFixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneId.systemDefault());
    }

    private PickupPoint createBrandedPickupPoint(LegalPartner legalPartner, String region) {
        brandRegionFactory.create(TestBrandRegionFactory.BrandRegionTestParams.builder()
                .region(region)
                .dailyTransmissionThreshold(5)
                .build());

        PickupPoint pickupPoint = createPickupPointWithLegalPartner(legalPartner);
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(LocalDate.now(clock).plusDays(2))
                        .brandRegion(region)
                        .cardAllowed(true)
                        .cashAllowed(true)
                        .returnAllowed(true)
                        .prepayAllowed(true)
                        .active(true)
                        .build());
    }

    private PickupPoint createNotBrandedPickupPoint(LegalPartner legalPartner) {
        PickupPoint pickupPoint = createPickupPointWithLegalPartner(legalPartner);
        return pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(true)
                        .cashAllowed(true)
                        .returnAllowed(true)
                        .prepayAllowed(true)
                        .active(true)
                        .build());
    }

    private PickupPoint createPickupPointWithLegalPartner(LegalPartner legalPartner) {
        return pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .name(RandomStringUtils.randomAlphabetic(10))
                                .build())
                        .legalPartner(legalPartner)
                        .build());
    }

    private LegalPartner createApprovedLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        return legalPartnerFactory.forceApprove(legalPartner.getId(), LocalDate.now(clock));
    }

    public static BillingLegalPartnerDto getLegalPartner() {
        return BillingLegalPartnerDto.builder()
                .balanceClientId(BALANCE_CLIENT_ID)
                .legalPartnerId(PARTNER_ID)
                .name(PARTNER_NAME)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .contractNumber(CONTRACT_NUMBER)
                .virtualAccountNumber(VIRTUAL_ACCOUNT_NUMBER)
                .kpp(KPP)
                .ogrn(OGRN)
                .legalType(LegalPartnerType.LEGAL_PERSON.name())
                .legalForm(LegalForm.AO.name())
                .offerDate(OFFER_SIGNED_SINCE)
                .build();
    }

    public static BillingPickupPointWorkingDaysDto getPointWorkingDaysDto() {
        Map<Long, Set<LocalDate>> pickupPointWorkingDays = new HashMap<>();
        pickupPointWorkingDays.put(PICKUP_POINT_ID, Set.of(WORKING_DAY));
        return BillingPickupPointWorkingDaysDto.builder()
                .pickupPointWorkingDays(pickupPointWorkingDays)
                .build();
    }


    public static BillingPickupPointDto getPickupPoint() {
        return BillingPickupPointDto.builder()
                .id(1)
                .deliveryServiceId(2)
                .legalPartnerId(PARTNER_ID)
                .name(PICKUP_POINT_NAME)
                .transmissionReward(BigDecimal.valueOf(3.0))
                .cashOrderCompensationRate(BigDecimal.valueOf(4.0))
                .cardOrderCompensationRate(BigDecimal.valueOf(5.0))
                .active(true)
                .returnAllowed(false)
                .brandingType("FULL")
                .brandRegionId(135L)
                .build();
    }

    public static BillingReturnDto getReturn() {
        return BillingReturnDto.builder()
                .returnId("123")
                .externalOrderId("321")
                .pickupPointId(11L)
                .dispatchedAt(OffsetDateTime.parse("2020-11-13T16:08:10.364804Z"))
                .build();
    }

    public static BillingOrderDto getOrder() {
        return
                BillingOrderDto.builder()
                        .id(1)
                        .externalId("2")
                        .deliveryServiceId(3)
                        .pickupPointId(4)
                        .paymentType("CASH")
                        .paymentStatus("PAID")
                        .deliveredAt(OffsetDateTime.parse("2020-11-13T16:08:10.364804Z"))
                        .orderType(OrderType.CLIENT)
                        .paymentSum(BigDecimal.TEN)
                        .yandexDelivery(false)
                        .build();
    }

    private String getJsonFromFile(String filePath) {
        return getFileContent(filePath);
    }
}
