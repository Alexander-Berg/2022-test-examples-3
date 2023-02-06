package ru.yandex.market.pvz.internal.controller.logistics;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.ApiSettingsCreator;
import ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverrideParams;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.schedule.model.PickupPointScheduleDay;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService.API_FORMAT;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService.API_TYPE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.DropOffCreateService.API_VERSION;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LogisticsPickupPointControllerTest extends BaseShallowTest {

    @MockBean
    private ApiSettingsCreator apiSettingsCreator;

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory testOrderFactory;

    private final DropOffCreateService dropOffCreateService;

    private final PickupPointQueryService pickupPointQueryService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final PickupPointDeactivationCommandService deactivationCommandService;

    @Test
    void createDayOff() throws Exception {
        mockMvc.perform(put("/logistics/pickup-point/day-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_day_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("logistics/response_create_day_off.json"), false));
    }

    @Test
    void createSameDayOffTwice() throws Exception {
        mockMvc.perform(put("/logistics/pickup-point/day-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_day_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("logistics/response_create_day_off.json"), false));

        mockMvc.perform(put("/logistics/pickup-point/day-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_day_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("logistics/response_create_day_off.json"), false));
    }

    @Test
    void deleteDayOff() throws Exception {
        mockMvc.perform(put("/logistics/pickup-point/day-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_day_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("logistics/response_create_day_off.json"), false));

        mockMvc.perform(delete("/logistics/pickup-point/day-off")
                .param("courierDeliveryServiceId", "1005477")
                .param("dayOff", "2021-02-09"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(""));
    }

    @Test
    void noDayOffToDelete() throws Exception {
        mockMvc.perform(delete("/logistics/pickup-point/day-off")
                .param("courierDeliveryServiceId", "1005477")
                .param("dayOff", "2021-02-09"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(""));
    }

    @Test
    void createDropOff() throws Exception {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        cancelFirstDeactivation(pickupPoint.getPvzMarketId());

        mockMvc.perform(post("/logistics/pickup-point/" + pickupPoint.getId() + "/dropoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_drop_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(""));

        PickupPointParams dropOffedPickupPoint = pickupPointQueryService.getHeavy(pickupPoint.getId());

        assertThat(dropOffedPickupPoint.getDropOffFeature()).isTrue();
        verify(apiSettingsCreator, times(1))
                .create(
                        SettingsApiDto.newBuilder()
                                .partnerId(deliveryService.getId())
                                .apiType(API_TYPE)
                                .format(API_FORMAT)
                                .version(API_VERSION)
                                .token(deliveryService.getToken())
                                .build(),
                        dropOffCreateService.getApiMethods()
                );

    }

    private void cancelFirstDeactivation(Long pvzMarketId) {
        var deactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
    }

    @Test
    void idempotentCreateDropOff() throws Exception {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());
        cancelFirstDeactivation(pickupPoint.getPvzMarketId());

        mockMvc.perform(post("/logistics/pickup-point/" + pickupPoint.getId() + "/dropoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_drop_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(""));

        mockMvc.perform(post("/logistics/pickup-point/" + pickupPoint.getId() + "/dropoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_create_drop_off.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(""));

        verify(apiSettingsCreator, times(1))
                .create(
                        SettingsApiDto.newBuilder()
                                .partnerId(deliveryService.getId())
                                .apiType(API_TYPE)
                                .format(API_FORMAT)
                                .version(API_VERSION)
                                .token(deliveryService.getToken())
                                .build(),
                        dropOffCreateService.getApiMethods()
                );

    }

    @Test
    void getExpirationDatesNotFound() throws Exception {
        mockMvc.perform(get("/logistics/pickup-point/orders/badId/reschedule-expiration"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpirationDatesWrongStatus() throws Exception {
        var order = testOrderFactory.createOrder();

        mockMvc.perform(get("/logistics/pickup-point/orders/" + order.getExternalId() + "/reschedule-expiration"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("[]"));
    }

    @Test
    void getExpirationDatesStoragePeriodExtended() throws Exception {
        var order = testOrderFactory.createOrder();
        testOrderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.STORAGE_PERIOD_EXTENDED);
        testOrderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        mockMvc.perform(get("/logistics/pickup-point/orders/" + order.getExternalId() + "/reschedule-expiration"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("[]"));
    }

    @Test
    void getExpirationDates() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(LocalDate.of(2021, 8, 2))
                        .build())
                .build());
        testOrderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        mockMvc.perform(get("/logistics/pickup-point/orders/" + order.getExternalId() + "/reschedule-expiration"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("logistics/expiration_dates.json")));
    }

    @Test
    void getExpirationDatesWithHoliday() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(LocalDate.of(2021, 8, 2))
                        .build())
                .build());
        testOrderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        pickupPointFactory.updateCalendarOverrides(
                pickupPoint.getId(),
                false,
                StreamEx.of((pickupPoint.getSchedule().getScheduleDays()))
                        .filter(d -> !d.getIsWorkingDay())
                        .map(PickupPointScheduleDay::getDayOfWeek)
                        .toList(),
                List.of(
                        PickupPointCalendarOverrideParams.builder()
                                .isHoliday(true)
                                .date(order.getExpirationDate().plusDays(1))
                                .build()
                ));

        mockMvc.perform(get("/logistics/pickup-point/orders/" + order.getExternalId() + "/reschedule-expiration"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("logistics/expiration_dates_with_holiday.json")));
    }

    @Test
    void extendStoragePeriodNotFound() throws Exception {
        mockMvc.perform(patch("/logistics/pickup-point/orders/badId/reschedule-expiration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_extend_date.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    void extendStoragePeriodBadRequest() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(LocalDate.of(2020, 8, 2))
                        .build())
                .build());
        testOrderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        mockMvc.perform(patch("/logistics/pickup-point/orders/" + order.getExternalId() +
                "/reschedule-expiration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_extend_date.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extendStoragePeriod() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .expirationDate(LocalDate.of(2021, 8, 2))
                        .build())
                .build());
        testOrderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.ARRIVED_TO_PICKUP_POINT);

        mockMvc.perform(patch("/logistics/pickup-point/orders/" + order.getExternalId() +
                "/reschedule-expiration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/request_extend_date.json")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void disableDropoff() throws Exception {
        deactivationReasonCommandService.createDeactivationReason("Причина", "Описание причины",
                true, false, "UNPROFITABLE");
        var pickupPoint = pickupPointFactory.createPickupPoint();

        mockMvc.perform(post("/logistics/pickup-point/" + pickupPoint.getLmsId() + "/deactivate-dropoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/disable_dropoff_request.json")))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/logistics/pickup-point/" + pickupPoint.getLmsId() + "/deactivate-dropoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/disable_dropoff_request.json")))
                .andExpect(status().isNonAuthoritativeInformation());
    }

    @Test
    void getLogisticsReasons() throws Exception {
        deactivationReasonCommandService.createDeactivationReason("Нерентабельность", "Описание причины",
                true, false, "UNPROFITABLE");
        deactivationReasonCommandService.createDeactivationReason("Рентабельность", "Описание причины",
                true, false, null);

        mockMvc.perform(get("/logistics/pickup-point/deactivation-reasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("logistics/deactivation_reasons.json")))
                .andExpect(status().is2xxSuccessful());
    }
}
