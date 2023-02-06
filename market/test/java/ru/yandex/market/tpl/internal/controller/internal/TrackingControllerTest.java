package ru.yandex.market.tpl.internal.controller.internal;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.tracking.DeliveryDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.core.domain.tvm.service.ServiceTicketRequest;
import ru.yandex.market.tpl.core.mvc.ServiceTicketRequestHandler;
import ru.yandex.market.tpl.core.service.rover.RoverService;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.service.tracking.confirmation.previously.PreviouslyConfirmationService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(TrackingController.class)
class TrackingControllerTest extends BaseShallowTest {

    private static final String TRACKING_ID = "tracking123";

    @MockBean
    private TrackingService trackingService;
    @MockBean
    private RoverService roverService;
    @MockBean
    private PreviouslyConfirmationService previouslyConfirmationService;

    @Test
    void getCancelledWithNoContact() throws Exception {
        TrackingDto mock = getMockCancelledWithNoContactTrackingDto(TRACKING_ID);
        when(trackingService.getTrackingSecureDto(eq(TRACKING_ID), anyBoolean(), any(ServiceTicketRequest.class)))
                .thenReturn(mock);

        mockMvc.perform(get("/internal/tracking/{id}", TRACKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header(ServiceTicketRequestHandler.SERVICE_TICKET_HEADER, 100)
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_tracking_cancelled_with_no_contact.json")));
    }

    private TrackingDto getMockCancelledWithNoContactTrackingDto(String id) {
        DeliveryDto delivery = new DeliveryDto();
        delivery.setStatus(TrackingDeliveryStatus.CANCELLED_AFTER_NO_CONTACT);

        TrackingDto result = new TrackingDto();
        result.setId(id);
        result.setDelivery(delivery);

        return result;
    }

    @Test
    void getRescheduledWithNoContact() throws Exception {
        TrackingDto mock = getMockRescheduledWithNoContactTrackingDto(TRACKING_ID);

        when(trackingService.getTrackingSecureDto(eq(TRACKING_ID), anyBoolean(), any(ServiceTicketRequest.class)))
                .thenReturn(mock);

        mockMvc.perform(get("/internal/tracking/{id}", TRACKING_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_tracking_rescheduled_with_no_contact.json")));
    }

    private TrackingDto getMockRescheduledWithNoContactTrackingDto(String id) {
        DeliveryDto delivery = new DeliveryDto();
        delivery.setStatus(TrackingDeliveryStatus.RESCHEDULED_AFTER_NO_CONTACT);
        delivery.setIntervalFrom(Instant.parse("2020-03-17T14:00:00Z"));
        delivery.setIntervalTo(Instant.parse("2020-03-17T18:00:00Z"));
        TrackingDto result = new TrackingDto();
        result.setId(id);
        result.setDelivery(delivery);

        return result;
    }
}
