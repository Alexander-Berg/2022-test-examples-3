package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.controller.util.MockParametersHelper;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookConsolidatedSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.ConsolidatedBookSlotResponseElement;
import ru.yandex.market.logistics.calendaring.client.dto.DestinationWithTakenLimits;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsWithDestinationsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.controller.util.MockParametersHelper.mockGatesSchedules;

public class RequestControllerForConsolidatedShippingTest extends MvcIntegrationTest {

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    private static final String REQUEST_SLOT = "{\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}";

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/1/setup.xml")
    void getSlotsForConsolidatedSuccess() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        doReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(1, Collections.emptyList()))))
                .when(calendaringServiceClient).getFreeSlots(any());
        doReturn(new BookingListResponse(List.of(new BookingResponse(3, "FFWF", "", null, 1,
                        ZonedDateTime.now(), ZonedDateTime.now(), BookingStatus.CANCELLED, null, 100L),
                new BookingResponse(4, "FFWF", "", null, 1, ZonedDateTime.now(),
                        ZonedDateTime.now(), BookingStatus.CANCELLED, null, 100L))
        )).when(calendaringServiceClient).getSlotByExternalIdentifiers(any(), any(), any());
        mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 2]}"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonNoFile(
                        "free-slots-many-reqs/1",
                        "/response.json")))
                .andReturn();
        ArgumentCaptor<GetFreeSlotsRequest> argumentCaptor = ArgumentCaptor.forClass(GetFreeSlotsRequest.class);
        verify(calendaringServiceClient).getFreeSlots(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getWarehouseIds(), equalTo(Set.of(1L)));
        assertThat(argumentCaptor.getValue().getBookingType(), equalTo(BookingType.SUPPLY));
        assertThat(argumentCaptor.getValue().getSlotDurationMinutes(), equalTo(30));
        assertThat(argumentCaptor.getValue().getTakenItems(), equalTo(6L));
        assertThat(argumentCaptor.getValue().getTakenPallets(), equalTo(1L));
        assertThat(new HashSet<>(argumentCaptor.getValue().getIgnoredBookings()), equalTo(Set.of(3L, 4L)));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/8/setup.xml")
    void getSlotsForXDockConsolidatedSuccess() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        doReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(1, Collections.emptyList()))))
                .when(calendaringServiceClient).getFreeSlots(any());
        doReturn(new BookingListResponse(List.of(new BookingResponse(3, "FFWF", "", null, 1,
                        ZonedDateTime.now(), ZonedDateTime.now(), BookingStatus.CANCELLED, null, 100L),
                new BookingResponse(4, "FFWF", "", null, 1, ZonedDateTime.now(),
                        ZonedDateTime.now(), BookingStatus.CANCELLED, null, 100L))
        )).when(calendaringServiceClient).getSlotByExternalIdentifiers(any(), any(), any());
        mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 2]}"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonNoFile(
                        "free-slots-many-reqs/8",
                        "/response.json")))
                .andReturn();
        ArgumentCaptor<GetFreeSlotsRequest> argumentCaptor = ArgumentCaptor.forClass(GetFreeSlotsRequest.class);
        verify(calendaringServiceClient).getFreeSlots(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getWarehouseIds(), equalTo(Set.of(200L)));
        assertThat(argumentCaptor.getValue().getBookingType(), equalTo(BookingType.SUPPLY));
        assertThat(argumentCaptor.getValue().getSlotDurationMinutes(), equalTo(30));
        assertThat(argumentCaptor.getValue().getTakenItems(), equalTo(6L));
        assertThat(argumentCaptor.getValue().getTakenPallets(), equalTo(1L));
        assertThat(new HashSet<>(argumentCaptor.getValue().getIgnoredBookings()), equalTo(Set.of(3L, 4L)));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/9/setup.xml")
    void getSlotsForXDockConsolidatedWithDifferentDestinationsSuccess() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        doReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(1, Collections.emptyList()))))
                .when(calendaringServiceClient).getFreeSlotsWithDestinations(any());
        doReturn(new BookingListResponse(List.of(new BookingResponse(3, "FFWF", "", null, 1,
                        ZonedDateTime.now(), ZonedDateTime.now(), BookingStatus.CANCELLED, null, 100L),
                new BookingResponse(4, "FFWF", "", null, 1, ZonedDateTime.now(),
                        ZonedDateTime.now(), BookingStatus.CANCELLED, null, 100L))
        )).when(calendaringServiceClient).getSlotByExternalIdentifiers(any(), any(), any());
        mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 2]}"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonNoFile(
                        "free-slots-many-reqs/9",
                        "/response.json")))
                .andReturn();
        ArgumentCaptor<GetFreeSlotsWithDestinationsRequest> argumentCaptor =
                ArgumentCaptor.forClass(GetFreeSlotsWithDestinationsRequest.class);
        verify(calendaringServiceClient).getFreeSlotsWithDestinations(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getWarehouseIds(), equalTo(Set.of(200L)));
        assertThat(argumentCaptor.getValue().getBookingType(), equalTo(BookingType.SUPPLY));
        assertThat(argumentCaptor.getValue().getSlotDurationMinutes(), equalTo(30));
        assertThat(argumentCaptor.getValue().getTakenItems(), equalTo(7L));
        assertThat(argumentCaptor.getValue().getTakenPallets(), equalTo(1L));
        assertThat(new HashSet<>(argumentCaptor.getValue().getIgnoredBookings()), equalTo(Set.of(3L, 4L)));
        assertThat(argumentCaptor.getValue().getDestinationInfoForWarehouses(), equalTo(Map.of(200L, Set.of(
                new DestinationWithTakenLimits(2L, 3, 1),
                new DestinationWithTakenLimits(1L, 4, 1)
        ))));
        verify(calendaringServiceClient, never()).getFreeSlots(any());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/2/setup.xml")
    void getSlotsForConsolidatedNotFullConsolidatedShipping() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        MvcResult mvcResult = mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 3]}"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        JSONAssert.assertEquals(
                "{\"message\":\"Consolidated shipping is not fully presented\"}",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/3/setup.xml")
    void getSlotsForConsolidatedIllegalConsolidatedShipping() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        MvcResult mvcResult = mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 3]}"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        JSONAssert.assertEquals(
                "{\"message\":\"Some supplies сonsist in consolidated shipping with status UPDATING\"}",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/4/setup.xml")
    void getSlotsForConsolidatedDifferentWarehouses() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        MvcResult mvcResult = mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1,2,3,4]}"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        JSONAssert.assertEquals(
                "{\"message\":\"Warehouses are not the same\"}",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/5/setup.xml")
    void getSlotsForConsolidatedIllegalRequestStatuses() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        MvcResult mvcResult = mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 3]}"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        JSONAssert.assertEquals(
                "{\"message\":\"Not all request statuses are WAITING_FOR_CONFIRMATION or VALIDATED\"}",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/6/setup.xml")
    void getSlotsNotShadowSupply() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        MvcResult mvcResult = mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 3]}"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        JSONAssert.assertEquals(
                "{\"message\":\"Not all request types are shadow supplies\"}",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-many-reqs/7/setup.xml")
    void getSlotsPalletsOverflow() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));
        MvcResult mvcResult = mockMvc.perform(
                post("/requests/consolidated-free-time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyIds\" :  [1, 2]}"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        JSONAssert.assertEquals(
                "{\"message\":\"It should be <= 33 pallets\"}",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/1/setup.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/select-consolidated-slot/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void selectSlotsForManyRequestsSuccess() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/8/setup.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/select-consolidated-slot/8/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void selectSlotsSuccessCreateConsolidatedShipping() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/2/setup.xml")
    void selectSlotNotShadowSupply() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        var response = mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"message\":\"Not all shop requests are SHADOW_SUPPLY\"}",
                response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/7/setup.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/select-consolidated-slot/7/setup.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void rollBackWhenCSThrowsException() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doThrow(new HttpTemplateException(409, "FAIL"))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().is5xxServerError());

    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/3/setup.xml")
    void selectSlotIllegalStatus() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        var result = mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"message\":\"Not all shop requests have status VALIDATED or WAITING FOR CONFIRMATION\"}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/4/setup.xml")
    void selectSlotDifferentWarehouse() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        var result = mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"message\":\"Shop requests from different warehouses are not allowed\"}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/5/setup.xml")
    void selectSlotIllegalConsolidatedStatus() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        var result = mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        ).andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"message\":\"One of the consolidated shipping has status UPDATING\"}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/6/setup.xml")
    void selectSlotNotFullConsolidatedShipping() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        var result = mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"message\":\"One of the consolidated shipping is not fully presented\"}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/select-consolidated-slot/9/setup.xml")
    void selectSlotPalletsOverflow() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        doReturn(new BookConsolidatedSlotResponse(List.of(new ConsolidatedBookSlotResponseElement(1, "1", 1, from, to),
                new ConsolidatedBookSlotResponseElement(2, "2", 1, from, to))))
                .when(calendaringServiceClient).bookConsolidatedSlot(any());

        var result = mockMvc.perform(
                post("/requests/select-consolidated-slot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(buildRequestJson(List.of(1L, 2L), LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                "{\"message\":\"It should be <= 33 pallets\"}",
                result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-select-slot-in-consolidated-before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-select-slot-in-consolidated-after.xml",
            assertionMode = NON_STRICT)
    void selectSlotInConsolidatedShipping() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6));
        when(calendaringServiceClient.bookSlot(any())).thenReturn(new BookSlotResponse(1L, 1L,
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 9, 0), ZoneId.of("+03:00")),
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 9, 30), ZoneId.of("+03:00"))));
        mockMvc.perform(
                post("/requests/1/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isOk());
    }


    private String buildRequestJson(List<Long> ids, LocalDate day, LocalTime from, LocalTime to) {
        return String.format("{\"ids\" : [%s], \"slot\" : {\"date\" : \"%s\", \"from\" : \"%s\", \"to\" : \"%s\"}}",
                ids.stream().map(Object::toString).collect(Collectors.joining(",")),
                day,
                from, to);
    }

    private String getJsonNoFile(String prefix, String name) throws IOException {
        return FileContentUtils.getFileContent("controller/request-api/" + prefix + name);
    }

    private void setupLmsGateSchedule(int fromHour, int toHour, LocalDate... workingDays) {
        setupLmsGateSchedule(fromHour, toHour, GateTypeResponse.INBOUND, workingDays);
    }

    private void setupLmsGateSchedule(int fromHour, int toHour, GateTypeResponse gateType, LocalDate... workingDays) {
        when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                anyLong(), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                        MockParametersHelper.mockSingleGateAvailableResponse(1L, gateType),
                        Arrays.stream(workingDays)
                                .map(day -> mockGatesSchedules(day, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0)))
                                .collect(Collectors.toList())
                )
        );
    }
}
