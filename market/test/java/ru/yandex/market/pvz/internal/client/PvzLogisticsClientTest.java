package ru.yandex.market.pvz.internal.client;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClientImpl;
import ru.yandex.market.pvz.client.logistics.dto.CourierDsDayOffDto;
import ru.yandex.market.pvz.client.logistics.dto.CreateDropOffDto;
import ru.yandex.market.pvz.client.logistics.dto.DeactivateDropoffDto;
import ru.yandex.market.pvz.client.logistics.dto.ExtendStoragePeriodDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestCreateDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestResponseDto;
import ru.yandex.market.pvz.client.logistics.dto.hub.HubFeaturesDto;
import ru.yandex.market.pvz.client.logistics.dto.hub.HubInfoDto;
import ru.yandex.market.pvz.client.logistics.model.ReturnClientType;
import ru.yandex.market.pvz.client.logistics.model.ReturnStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_DROP_OFF_FEATURE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParams.DEFAULT_COURIER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_ACTIVE;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointTestParams.DEFAULT_RETURN_ALLOWED;

public class PvzLogisticsClientTest extends ClientTest {

    private final PvzLogisticsClient pvzLogisticsClient = new PvzLogisticsClientImpl(URL, REST_TEMPLATE);

    @Test
    void getHubInfo() {
        long id = 1;
        long campaignId = 21997585;
        mock.expect(method(HttpMethod.GET))
                .andExpect(
                        requestTo(URL + "/v1/pi/hub?campaignId=" + campaignId))
                .andRespond(
                        withSuccess(
                                String.format(
                                        getFileContent(
                                                "hub/response_get_hub_by_campaign_id.json"), id, campaignId),
                                APPLICATION_JSON));

        HubInfoDto actual = pvzLogisticsClient.getHubInfoByCampaignId(campaignId);

        HubInfoDto expected = HubInfoDto.builder()
                .id(id)
                .mbiCampaignId(campaignId)
                .name(DEFAULT_NAME)
                .active(DEFAULT_ACTIVE)
                .features(HubFeaturesDto.builder()
                        .delivery(true)
                        .refund(DEFAULT_RETURN_ALLOWED)
                        .sorting(false)
                        .dropOff(DEFAULT_DROP_OFF_FEATURE)
                        .build())
                .build();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createDayOff() {
        LocalDate date = LocalDate.of(2021, 2, 9);
        mock.expect(method(HttpMethod.PUT))
                .andExpect(
                        requestTo(URL + "/logistics/pickup-point/day-off"))
                .andRespond(
                        withSuccess(getFileContent("logistics/response_create_day_off.json"), APPLICATION_JSON));

        CourierDsDayOffDto actual = pvzLogisticsClient.createDayOffForPartner(DEFAULT_COURIER_DELIVERY_SERVICE_ID,
                date);

        CourierDsDayOffDto expected = new CourierDsDayOffDto(DEFAULT_COURIER_DELIVERY_SERVICE_ID, date);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createReturnRequest() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + "/logistics/return-request"))
                .andRespond(
                        withSuccess(getFileContent(
                                "return_request/return_request_create_response.json"), APPLICATION_JSON));

        ReturnRequestCreateDto returnRequestCreateDto = ReturnRequestCreateDto.builder().build();
        PvzLogisticsClient pvzLogisticsClient = new PvzLogisticsClientImpl(URL, REST_TEMPLATE);
        ReturnRequestResponseDto returnRequest = pvzLogisticsClient.createReturnRequest(returnRequestCreateDto);
        assertThat(returnRequest).isNotNull();
        assertThat(returnRequest.getReturnRequest().getClientType()).isEqualTo(ReturnClientType.CLIENT);
        assertThat(returnRequest.getReturnRequest().getBuyerName()).isEqualTo("Райгородский Андрей Михайлович");
    }

    @Test
    void getReturnRequest() {
        var returnId = "fake-123";
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + "/logistics/return-request/" + returnId))
                .andRespond(
                        withSuccess(getFileContent(
                                "return_request/return_request_get_response.json"), APPLICATION_JSON));

        var pvzLogisticsClient = new PvzLogisticsClientImpl(URL, REST_TEMPLATE);
        var returnDto = pvzLogisticsClient.getReturnRequest(returnId);

        assertThat(returnDto).isNotNull();
        assertThat(returnDto.getReturnRequest()).isNotNull();
        assertThat(returnDto.getReturnRequest().getClientType()).isEqualTo(ReturnClientType.CLIENT);
        assertThat(returnDto.getReturnRequest().getStatus()).isEqualTo(ReturnStatus.NEW);
        assertThat(returnDto.getReturnRequest().getBuyerName()).isEqualTo("Райгородский Андрей Михайлович");
    }

    @Test
    void getReturnRequests() {
        mock.expect(method(HttpMethod.PUT))
                .andExpect(requestTo(URL + "/logistics/return-requests"))
                .andRespond(
                        withSuccess(getFileContent(
                                "return_request/return_requests_get_response.json"), APPLICATION_JSON));

        var pvzLogisticsClient = new PvzLogisticsClientImpl(URL, REST_TEMPLATE);
        var returnDto = pvzLogisticsClient.getReturnRequests(List.of("ret1", "ret2"));

        assertThat(returnDto).isNotNull();
        assertThat(returnDto.getReturnRequests()).isNotNull();
        assertThat(returnDto.getReturnRequests().size()).isEqualTo(2);
        assertThat(returnDto.getReturnRequests().get(0).getReturnId()).isEqualTo("ret1");
        assertThat(returnDto.getReturnRequests().get(0).getClientType()).isEqualTo(ReturnClientType.CLIENT);
        assertThat(returnDto.getReturnRequests().get(0).getStatus()).isEqualTo(ReturnStatus.NEW);
        assertThat(returnDto.getReturnRequests().get(0).getBuyerName()).isEqualTo("Райгородский Андрей Михайлович");
    }

    @Test
    void cancelReturnRequest() {
        var returnId = "fake-123";
        mock.expect(method(HttpMethod.PATCH))
                .andExpect(requestTo(URL + "/logistics/return-request/cancel/" + returnId))
                .andRespond(
                        withSuccess(getFileContent(
                                "return_request/return_request_cancel_response.json"), APPLICATION_JSON));

        var pvzLogisticsClient = new PvzLogisticsClientImpl(URL, REST_TEMPLATE);
        var requestResponseDto = pvzLogisticsClient.cancelReturnRequest(returnId);

        assertThat(requestResponseDto).isNotNull();
        assertThat(requestResponseDto.getReturnRequest().getStatus()).isEqualTo(ReturnStatus.CANCELLED);
        assertThat(requestResponseDto.getErrorCodes()).isEmpty();
    }

    @Test
    void deleteDayOff() {
        LocalDate date = LocalDate.of(2021, 2, 9);
        mock.expect(method(HttpMethod.DELETE))
                .andExpect(
                        requestTo(String.format(URL + "/logistics/pickup-point/day-off?courierDeliveryServiceId=%s" +
                                        "&dayOff=%s",
                                DEFAULT_COURIER_DELIVERY_SERVICE_ID, date)))
                .andRespond(withNoContent());

        pvzLogisticsClient.deleteDayOffForPartner(DEFAULT_COURIER_DELIVERY_SERVICE_ID, date);
    }

    @Test
    void createDropOff() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + "/logistics/pickup-point/1/dropoff"))
                .andExpect(
                        content().json(getFileContent("logistics/request_create_drop_off.json")))
                .andRespond(withSuccess());

        pvzLogisticsClient.createDropOff(1, CreateDropOffDto.builder().sortingCenterPartnerId(89484L).build());
    }

    @Test
    void processRegionMonitoring() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + "/logistics/pickup-point/region-monitoring/3123?orders=%5Bfake-5200483%5D"))
                .andRespond(withNoContent());

        pvzLogisticsClient.processRegionMonitoring("3123", List.of("fake-5200483"));
    }

    @Test
    void getExpirationDates() {

        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + "/logistics/pickup-point/orders/fake-5200483/reschedule-expiration"))
                .andRespond(withSuccess(new ClassPathResource("logistics/expiration_dates.json"), APPLICATION_JSON));

        pvzLogisticsClient.getExpirationDates("fake-5200483");
    }

    @Test
    void extendStoragePeriod() {

        mock.expect(method(HttpMethod.PATCH))
                .andExpect(requestTo(URL + "/logistics/pickup-point/orders/fake-5200483/reschedule-expiration"))
                .andRespond(withNoContent());

        pvzLogisticsClient.extendStoragePeriod(
            "fake-5200483",
            ExtendStoragePeriodDto.builder().date(LocalDate.of(2021, 1, 1)).build()
        );
    }

    @Test
    void deactivateDropoff() {
        var request = DeactivateDropoffDto.builder()
                .date(LocalDate.of(2021, 12, 16))
                .reason("UNPROFITABLE")
                .build();

        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + "/logistics/pickup-point/123/deactivate-dropoff"))
                .andExpect(content().json(getFileContent("logistics/disable_dropoff_request.json")))
                .andRespond(withNoContent());

        pvzLogisticsClient.deactivateDropoff(123, request);
    }

    @Test
    void getDeactivationReasons() {
        mock.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + "/logistics/pickup-point/deactivation-reasons"))
                .andRespond(withSuccess(new ClassPathResource("logistics/deactivation_reasons.json"),
                        APPLICATION_JSON));

        pvzLogisticsClient.getLogisticsDeactivationReasons();
    }
}
