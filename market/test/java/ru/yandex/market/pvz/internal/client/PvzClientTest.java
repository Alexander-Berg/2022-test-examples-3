package ru.yandex.market.pvz.internal.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerDto;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerIndebtednessDto;
import ru.yandex.market.pvz.client.billing.dto.BillingLegalPartnerTerminationDto;
import ru.yandex.market.pvz.client.billing.dto.BillingOrderDto;
import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointDto;
import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointWorkingDaysDto;
import ru.yandex.market.pvz.client.billing.dto.BillingReturnDto;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.internal.controller.billing.BillingControllerTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

class PvzClientTest extends ClientTest {

    @Test
    void getDispatchedReturns() {
        LocalDate date = LocalDate.of(2020, 10, 10);
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(String.format(URL + "/billing/returns?from=%s&to=%s", date, date)))
                .andRespond(withSuccess(new ClassPathResource("billing/response_dispatched_returns.json"),
                        MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        List<BillingReturnDto> deliveredOrders = pvzClient.getDispatchedReturns(date, date);
        assertEquals(1, deliveredOrders.size(), "size");
        assertEquals(BillingControllerTest.getReturn(), deliveredOrders.get(0), "return");
    }

    @Test
    void getDeliveredOrders() {
        LocalDate date = LocalDate.of(2020, 10, 10);
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(String.format(URL + "/billing/delivered-orders?from=%s&to=%s", date, date)))
                .andRespond(withSuccess(new ClassPathResource("billing/response_delivery_orders.json"),
                        MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        List<BillingOrderDto> deliveredOrders = pvzClient.getDeliveredOrders(date, date);
        assertEquals(1, deliveredOrders.size(), "size");
        assertEquals(BillingControllerTest.getOrder(), deliveredOrders.get(0), "order");
    }

    @Test
    void getDeliveredOrdersWithOffset() {
        LocalDate date = LocalDate.of(2020, 10, 10);
        Long limit = 100L;
        Long offset = 0L;
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(
                        String.format(
                                URL + "/billing/delivered-orders?from=%s&to=%s&limit=%d&offset=%d",
                                date, date, limit, offset
                        )
                ))
                .andRespond(withSuccess(new ClassPathResource("billing/response_delivery_orders.json"),
                        MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        List<BillingOrderDto> deliveredOrders = pvzClient.getDeliveredOrders(date, date, limit, offset);
        assertEquals(1, deliveredOrders.size(), "size");
        assertEquals(BillingControllerTest.getOrder(), deliveredOrders.get(0), "order");
    }

    @Test
    void getPickupPoints() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + "/billing/pickup-points"))
                .andRespond(withSuccess(new ClassPathResource("billing/response_pickup_points.json"),
                        MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        List<BillingPickupPointDto> pickupPoints = pvzClient.getPickupPoints();
        assertEquals(1, pickupPoints.size(), "size");
        assertEquals(BillingControllerTest.getPickupPoint(), pickupPoints.get(0), "pickup point");
    }

    @Test
    void getLegalPartners() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + "/billing/legal-partners"))
                .andRespond(withSuccess(new ClassPathResource("billing/response_legal_partners.json"),
                        MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        List<BillingLegalPartnerDto> legalPartners = pvzClient.getLegalPartners();
        assertEquals(1, legalPartners.size(), "size");
        assertEquals(BillingControllerTest.getLegalPartner(), legalPartners.get(0), "legal partner");
    }

    @Test
    void getPickupPointWorkingDays() {
        LocalDate date = LocalDate.of(2020, 10, 10);
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(String.format(URL + "/billing/pickup-point-working-days?from=%s&to=%s", date,
                        date)))
                .andRespond(withSuccess(new ClassPathResource("billing/response_working_days.json"),
                        MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        BillingPickupPointWorkingDaysDto pointWorkingDays = pvzClient.getPickupPointWorkingDays(date, date);
        assertEquals(BillingControllerTest.getPointWorkingDaysDto(), pointWorkingDays, "working day");
    }

    @Test
    void createLegalPartnerIndebtedness() throws IOException {
        var json = String.format(getFileContent("billing/request_indebtedness.json"), 1L);
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + "/billing/legal-partners/indebtedness"))
                .andRespond(withSuccess(String.format(json, 1L), MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        BillingLegalPartnerIndebtednessDto debt = BillingLegalPartnerIndebtednessDto.builder()
                .legalPartnerId(1)
                .debtSum(BigDecimal.valueOf(40000)).build();
        List<BillingLegalPartnerIndebtednessDto> actual =
                pvzClient.createLegalPartnerIndebtedness(List.of(debt));
        assertEquals(debt, actual.get(0), "debt");
    }

    @Test
    void createLegalPartnerTermination() throws IOException {
        var json = getFileContent("billing/response_terminated_partners.json");
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + "/billing/legal-partners/termination"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        BillingLegalPartnerTerminationDto partnerTerminationDto = BillingLegalPartnerTerminationDto.builder()
                .legalPartnerId(100)
                .active(true)
                .fromTime(OffsetDateTime.parse("2021-10-11T12:00:00Z"))
                .type(LegalPartnerTerminationType.DEBT)
                .build();

        List<BillingLegalPartnerTerminationDto> actual =
                pvzClient.createLegalPartnerTermination(List.of(partnerTerminationDto));
        assertEquals(partnerTerminationDto, actual.get(0), "terminatedPartners");
    }

    @Test
    void enableLegalPartners() throws IOException {
        var json = getFileContent("billing/response_enabled_partners.json");
        mock.expect(method(HttpMethod.PATCH))
                .andExpect(requestTo(URL + "/billing/legal-partners/termination"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        PvzClient pvzClient = new PvzClient(URL, REST_TEMPLATE);
        BillingLegalPartnerTerminationDto partnerTerminationDto = BillingLegalPartnerTerminationDto.builder()
                .legalPartnerId(100)
                .active(false)
                .type(LegalPartnerTerminationType.DEBT)
                .build();

        List<BillingLegalPartnerTerminationDto> actual =
                pvzClient.enableLegalPartners(List.of(partnerTerminationDto));
        assertEquals(partnerTerminationDto, actual.get(0), "terminatedPartners");
    }

}
