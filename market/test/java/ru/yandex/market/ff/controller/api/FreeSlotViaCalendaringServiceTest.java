package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeIntervalReason;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeSlotDTO;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeSlotGateDTO;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseNotFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.NotFreeIntervalReasonType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FreeSlotViaCalendaringServiceTest extends MvcIntegrationTest {


    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @Autowired
    private EnvironmentParamService environmentParamService;

    @BeforeEach
    void init() {
        reset(calendaringServiceClient);
    }

    @AfterEach
    void after() {
        environmentParamService.clearCache();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/get-slots/before.xml")
    void getFreeTimeSlotsSuccessfully() throws Exception {

        LocalDate date = LocalDate.of(2018, 1, 5);

        List<TimeSlotResponse> slots = List.of(
                new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 30)),
                new TimeSlotResponse(LocalTime.of(10, 30), LocalTime.of(11, 0)),
                new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
        );

        List<FreeSlotsForDayResponse> freeSlotsForDayResponses =
                List.of(new FreeSlotsForDayResponse(date, ZoneOffset.of("+03:00"), slots));

        List<WarehouseFreeSlotsResponse> warehouseFreeSlotsResponses =
                List.of(new WarehouseFreeSlotsResponse(1, freeSlotsForDayResponses));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(warehouseFreeSlotsResponses);

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);

        MvcResult mvcResult = performGetSlots(1);

        verify(calendaringServiceClient).getFreeSlots(any());

        String responseFile = "controller/request-api/free-slots-via-cs/get-slots/response.json";
        assertJsonResponseCorrect(responseFile, mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/get-slots/before-xdoc.xml")
    void getFreeTimeSlotsForXDocShadowSupplySuccessfully() throws Exception {

        LocalDate date = LocalDate.of(2018, 1, 5);

        List<TimeSlotResponse> slots = List.of(
                new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 30)),
                new TimeSlotResponse(LocalTime.of(10, 30), LocalTime.of(11, 0)),
                new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
        );

        List<FreeSlotsForDayResponse> freeSlotsForDayResponses =
                List.of(new FreeSlotsForDayResponse(date, ZoneOffset.of("+03:00"), slots));

        List<WarehouseFreeSlotsResponse> warehouseFreeSlotsResponses =
                List.of(new WarehouseFreeSlotsResponse(1, freeSlotsForDayResponses));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(warehouseFreeSlotsResponses);

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);

        MvcResult mvcResult = performGetSlots(1);

        ArgumentCaptor<GetFreeSlotsRequest> captor = ArgumentCaptor.forClass(GetFreeSlotsRequest.class);
        verify(calendaringServiceClient).getFreeSlots(captor.capture());

        assertions.assertThat(captor.getValue().getWarehouseIds()).containsExactlyInAnyOrder(2L);

        String responseFile = "controller/request-api/free-slots-via-cs/get-slots/response.json";
        assertJsonResponseCorrect(responseFile, mvcResult);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/get-slots/before.xml")
    void getFreeTimeSlotsRequestToCalendaringServiceCorrect() throws Exception {

        List<WarehouseFreeSlotsResponse> warehouseFreeSlotsResponses =
                List.of(new WarehouseFreeSlotsResponse(1, Collections.emptyList()));
        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(warehouseFreeSlotsResponses);

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);

        performGetSlots(1);

        ArgumentCaptor<GetFreeSlotsRequest> captor = ArgumentCaptor.forClass(GetFreeSlotsRequest.class);
        Mockito.verify(calendaringServiceClient).getFreeSlots(captor.capture());

        GetFreeSlotsRequest value = captor.getValue();

        assertions.assertThat(value).isNotNull();
        assertions.assertThat(value.getWarehouseIds()).contains(1L);
        assertions.assertThat(value.getBookingType()).isEqualTo(BookingType.SUPPLY);
        assertions.assertThat(value.getSlotDurationMinutes()).isEqualTo(30);
        assertions.assertThat(value.getBookingId()).isNull();
        assertions.assertThat(value.getFrom())
                .isEqualTo(LocalDateTime.of(2018, 1, 1, 10, 30, 0));
        assertions.assertThat(value.getTo())
                .isEqualTo(LocalDateTime.of(2018, 1, 15, 0, 0, 0));
        assertions.assertThat(value.getSupplierType()).isEqualTo(SupplierType.FIRST_PARTY);
        assertions.assertThat(value.getSupplierId()).isNull();
        assertions.assertThat(value.getTakenItems()).isEqualTo(3);
        assertions.assertThat(value.getTakenPallets()).isEqualTo(1);

    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/get-slots-by-service/before.xml")
    void performGetFreeTimeSlotsByServiceSuccessfully() throws Exception {

        LocalDate date1 = LocalDate.of(2018, 1, 5);
        LocalDate date2 = LocalDate.of(2018, 1, 6);

        List<TimeSlotResponse> slots = List.of(
                new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0))
        );

        List<FreeSlotsForDayResponse> freeSlotsForDayResponses =
                List.of(new FreeSlotsForDayResponse(date1, ZoneOffset.of("+03:00"), slots),
                        new FreeSlotsForDayResponse(date2, ZoneOffset.of("+03:00"), slots));

        List<WarehouseFreeSlotsResponse> warehouseFreeSlotsResponses =
                List.of(new WarehouseFreeSlotsResponse(1, freeSlotsForDayResponses),
                        new WarehouseFreeSlotsResponse(2, freeSlotsForDayResponses));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(warehouseFreeSlotsResponses);

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);

        MvcResult mvcResult = performGetFreeTimeSlotsByService(1);

        verify(calendaringServiceClient).getFreeSlots(any());

        String responseFile = "controller/request-api/free-slots-via-cs/get-slots-by-service/response.json";
        assertJsonResponseCorrect(responseFile, mvcResult);
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/get-slots-by-service/before.xml")
    void getNotFreeTimeSlotsByServiceSuccessfully() throws Exception {

        List<NotFreeIntervalReason> reasons =
                List.of(new NotFreeIntervalReason(NotFreeIntervalReasonType.NOT_IN_SCHEDULE, null));

        List<NotFreeSlotGateDTO> notFreeSlotGateDTOS = List.of(new NotFreeSlotGateDTO(1L, "1", reasons));

        List<NotFreeSlotDTO> notFreeSlotDTOS = List.of(new NotFreeSlotDTO(
                LocalDateTime.of(2021, 1, 10, 10, 0),
                LocalDateTime.of(2021, 1, 10, 10, 30),
                notFreeSlotGateDTOS
        ));

        List<NotFreeSlotDTO> notFreeSlotDTOS2 = List.of(new NotFreeSlotDTO(
                LocalDateTime.of(2021, 1, 11, 9, 0),
                LocalDateTime.of(2021, 1, 11, 9, 30),
                notFreeSlotGateDTOS
        ));

        NotFreeSlotsResponse notFreeSlotsResponse =
                new NotFreeSlotsResponse(List.of(new WarehouseNotFreeSlotsResponse(1L, notFreeSlotDTOS),
                        new WarehouseNotFreeSlotsResponse(2L, notFreeSlotDTOS2)
                        ));

        when(calendaringServiceClient.getNotFreeSlotsReasons(any())).thenReturn(notFreeSlotsResponse);

        MvcResult mvcResult = performGetNotFreeTimeSlotsReasonsByService(1);

        String responseFile = "controller/request-api/free-slots-via-cs/get-not-free-slots-by-service/response.json";
        assertJsonResponseCorrect(responseFile, mvcResult);
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/get-slots-by-service/before.xml")
    void performGetFreeTimeSlotsByServiceRequestToCalendaringServiceCorrect() throws Exception {

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(Collections.emptyList());

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);

        performGetFreeTimeSlotsByService(1);

        ArgumentCaptor<GetFreeSlotsRequest> captor = ArgumentCaptor.forClass(GetFreeSlotsRequest.class);
        Mockito.verify(calendaringServiceClient).getFreeSlots(captor.capture());

        GetFreeSlotsRequest value = captor.getValue();

        assertions.assertThat(value).isNotNull();
        assertions.assertThat(value.getWarehouseIds()).contains(1L);
        assertions.assertThat(value.getWarehouseIds()).contains(2L);
        assertions.assertThat(value.getBookingType()).isEqualTo(BookingType.SUPPLY);
        assertions.assertThat(value.getSlotDurationMinutes()).isEqualTo(30);
        assertions.assertThat(value.getBookingId()).isNull();
        assertions.assertThat(value.getFrom())
                .isEqualTo(LocalDateTime.of(2018, 1, 1, 10, 30, 0));
        assertions.assertThat(value.getTo())
                .isEqualTo(LocalDateTime.of(2018, 1, 16, 0, 0, 0));
        assertions.assertThat(value.getSupplierType()).isEqualTo(SupplierType.THIRD_PARTY);
        assertions.assertThat(value.getSupplierId()).isEqualTo("1");
        assertions.assertThat(value.getTakenItems()).isEqualTo(3);
        assertions.assertThat(value.getTakenPallets()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/free-slots-via-cs/with-rating-filter-by-service/before.xml")
    void getAvailableOptionsWithRatingFilter() throws Exception {

        LocalDate date1 = LocalDate.of(2018, 1, 5);
        LocalDate date2 = LocalDate.of(2018, 1, 6);

        List<TimeSlotResponse> slots = List.of(
                new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0))
        );

        List<FreeSlotsForDayResponse> freeSlotsForDayResponses =
                List.of(new FreeSlotsForDayResponse(date1, ZoneOffset.of("+03:00"), slots),
                        new FreeSlotsForDayResponse(date2, ZoneOffset.of("+03:00"), slots));

        List<WarehouseFreeSlotsResponse> warehouseFreeSlotsResponses =
                List.of(new WarehouseFreeSlotsResponse(1, freeSlotsForDayResponses),
                        new WarehouseFreeSlotsResponse(2, freeSlotsForDayResponses));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(warehouseFreeSlotsResponses);

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(freeSlotsResponse);

        MvcResult mvcResult = performGetFreeTimeSlotsByService(1);

        String responseFile = "controller/request-api/free-slots-via-cs/with-rating-filter-by-service/response.json";
        assertJsonResponseCorrect(responseFile, mvcResult);
    }

    private MvcResult performGetSlots(long id) throws Exception {
        return mockMvc.perform(
                get("/requests/" + id + "/getFreeTimeSlots")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult performGetFreeTimeSlotsByService(long requestId) throws Exception {
        return mockMvc.perform(
                get(String.format("/requests/%d/free-time-slots-by-service", requestId))
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult performGetNotFreeTimeSlotsReasonsByService(long requestId) throws Exception {
        return mockMvc.perform(
                get(String.format("/requests/%d/not-free-time-slots-by-service", requestId))
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        String fileContent = FileContentUtils.getFileContent(filename);
        JSONAssert.assertEquals(fileContent, response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }


}
