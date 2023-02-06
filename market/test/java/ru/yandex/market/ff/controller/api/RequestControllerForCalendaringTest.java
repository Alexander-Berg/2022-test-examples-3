package ru.yandex.market.ff.controller.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.RequestAcceptDTO;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.SUPPLY_ACCEPTED_BY_SERVICE;
import static ru.yandex.market.ff.util.TestUtils.dontCare;

public class RequestControllerForCalendaringTest extends MvcIntegrationTest {

    private static final long VALID_REQ_ID = 1;
    private static final long INVALID_REQ_ID = 2;
    private static final long VALID_SUPPLIER_ID = 1;
    private static final String REQUEST_SLOT = "{\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}";
    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @Autowired
    private SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer;

    @AfterEach
    public void resetCacheAndValidateMocks() {
        lmsClientCachingService.invalidateCache();
        //in all tests we don't care about how many times csClient.getTimezoneByWarehouseId was called
        verify(csClient, dontCare()).getTimezoneByWarehouseId(anyLong());
        verify(csClient, dontCare()).putExternallyBookedSlot(any());

        reset(csClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/on-accept-updating.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/after-accept-updating.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptUpdatingInbound() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>1</id>" +
                        "<destination-warehouse-id>100</destination-warehouse-id>" +
                        "<destination-warehouse-name>test</destination-warehouse-name>" +
                        "<merchandise-receipt-date>06 января</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>11:00</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/on-accept-updating-with-cs-booking.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/" +
            "after-accept-updating-with-cs-booking.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptUpdatingInboundWithCsBooking() throws Exception {
        when(csClient.getSlotByExternalIdentifiers(eq(Set.of("1")), anyString(), isNull())).thenReturn(
                new BookingListResponse(List.of(
                        new BookingResponse(199, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 6, 8, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2018, 1, 6, 8, 30, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.UPDATING, null,
                                100L),
                        new BookingResponse(200, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 5, 7, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2018, 1, 5, 7, 30, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.ACTIVE, null,
                                100L)
                ))
        );
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-updating")
        ).andExpect(status().isOk());
        verify(csClient).acceptUpdate(199);
        verify(csClient, times(2)).getSlotByExternalIdentifiers(eq(Set.of("1")), anyString(), isNull());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/on-accept-updating.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/after-accept-updating.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceOutbound() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/on-accept-updating.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/after-accept-updating.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptUpdatingCallbackOutbound() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/on-accept-updating-move-backward.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-accept-updating-move-backward.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceOutboundMoveBackward() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/" +
            "on-accept-updating-with-cancellation-request.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/" +
            "after-accept-updating-with-cancellation-request.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptUpdatingInboundWithCancellationRequest() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>1</id>" +
                        "<destination-warehouse-id>100</destination-warehouse-id>" +
                        "<destination-warehouse-name>test</destination-warehouse-name>" +
                        "<merchandise-receipt-date>06 января</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>11:00</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/" +
            "on-accept-updating-with-cancellation-request.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-accept-updating-with-cancellation-request.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceOutboundWithCancellationRequest() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/" +
            "on-accept-updating-without-limit-updates.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/" +
            "after-accept-updating-without-limit-updates.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptUpdatingInboundWithoutLimitUpdates() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>1</id>" +
                        "<destination-warehouse-id>100</destination-warehouse-id>" +
                        "<destination-warehouse-name>test</destination-warehouse-name>" +
                        "<merchandise-receipt-date>05 января</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>11:00</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/" +
            "on-accept-updating-without-limit-updates.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-accept-updating-without-limit-updates.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceOutboundWithoutLimitUpdates() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);


    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/on-reject-updating.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/after-reject-updating.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectUpdatingInbound() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());

        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>1</id>" +
                        "<destination-warehouse-id>100</destination-warehouse-id>" +
                        "<destination-warehouse-name>test</destination-warehouse-name>" +
                        "<merchandise-receipt-date>05 января</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>10:00</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/on-reject-updating-with-cs-booking.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/" +
            "after-reject-updating-with-cs-booking.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectUpdatingInboundWithCsBooking() throws Exception {
        when(csClient.getSlotByExternalIdentifiers(eq(Set.of("1")), anyString(), isNull())).thenReturn(
                new BookingListResponse(List.of(
                        new BookingResponse(199, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 6, 8, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2018, 1, 6, 8, 30, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.UPDATING, null, 100L),
                        new BookingResponse(200, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 5, 7, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2018, 1, 5, 7, 30, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.ACTIVE, null, 100L)
                ))
        );
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-updating")
        ).andExpect(status().isOk());
        verify(csClient).rejectUpdate(199);
        verify(csClient).getSlotByExternalIdentifiers(eq(Set.of("1")), anyString(), isNull());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/on-reject-updating.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/after-reject-updating.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectByServiceOutbound() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/on-reject-updating.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/after-reject-updating.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectUpdatingOutbound() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/on-reject-updating-move-backward.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-reject-updating-move-backward.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectByServiceOutboundMoveBackward() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/" +
            "on-reject-updating-without-limit-updates.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/" +
            "after-reject-updating-without-limit-updates.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectUpdatingInboundWithoutLimitUpdates() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-updating")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(SUPPLY_ACCEPTED_BY_SERVICE, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>1</id>" +
                        "<destination-warehouse-id>100</destination-warehouse-id>" +
                        "<destination-warehouse-name>test</destination-warehouse-name>" +
                        "<merchandise-receipt-date>05 января</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>10:00</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/" +
            "on-reject-updating-without-limit-updates.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-reject-updating-without-limit-updates.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectByServiceOutboundWithoutLimitUpdates() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(0)).produceSingle(any());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-select-slot-ok-before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-select-slot-ok-after.xml",
            assertionMode = NON_STRICT)
    void selectSlotHappyPath() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0), LocalTime.of(9, 30)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-select-slot-nok-before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-select-slot-nok-before.xml",
            assertionMode = NON_STRICT)
    void selectSlotIsOutboundPermittedPeriod() throws Exception {
        MvcResult mvcResult = performSelectSlot(
                INVALID_REQ_ID, LocalDate.of(1999, 9, 9), LocalTime.of(9, 0), LocalTime.of(9, 30)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.BAD_REQUEST.value()));
        assertThat(mvcResult.getResponse().getContentAsString(), containsString(
                "Slot outside the permitted period: RequestedSlotDTO{date=1999-09-09, from=09:00, to=09:30"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-update-slot-for-validated-shadow-inbound.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-update-slot-for-validated-shadow-inbound.xml",
            assertionMode = NON_STRICT)
    void updateSlotForShadowInboundInValidatedStatus() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-select-slot-for-validated-shadow-inbound.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-select-slot-for-validated-shadow-inbound.xml",
            assertionMode = NON_STRICT)
    void selectSlotForShadowInboundInValidatedStatus() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-select-slot-for-validated-expendable-materials.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-select-slot-for-validated-expendable-materials.xml",
            assertionMode = NON_STRICT)
    void selectSlotForExpendableMaterialsSupply() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));

    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-select-slot-for-inbound-with-measurement-taken-limits.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-select-slot-for-inbound-with-measurement-taken-limits.xml",
            assertionMode = NON_STRICT)
    void selectSlotWithExistingMeasurementTakenLimits() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-slot-import-before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/select-slot-import-after.xml",
            assertionMode = NON_STRICT)
    void selectSlotWithImport() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0), LocalTime.of(12, 00)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));
    }

    private MvcResult performSelectSlot(long requestId, LocalDate date, LocalTime from, LocalTime to) throws Exception {
        return mockMvc.perform(
                post("/requests/" + requestId + "/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, date, from, to))
        ).andDo(print())
                .andReturn();
    }
}
