package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RequestControllerPriorityTypeTest extends MvcIntegrationTest {

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @Test
    @DatabaseSetup("classpath:controller/priority-type/withdraw-same-time-withdraw.xml")
    void getSlotPriorityTypeWithdrawSameTimeWithdrawTest() throws Exception {


        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 30)),
                        new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                        new TimeSlotResponse(LocalTime.of(10, 30), LocalTime.of(11, 30)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(12, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetSlots(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("withdraw-same-time-withdraw.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/priority-type/withdraw-supply.xml")
    void getSlotPriorityTypeWithdrawSupplyTest() throws Exception {


        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(12, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetSlots(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("withdraw-supply.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/priority-type/supply-withdraw.xml")
    void getSlotPriorityTypeSupplyWithdrawTest() throws Exception {


        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                        new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                        new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetSlots(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("supply-withdraw.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/priority-type/supply-supply.xml")
    void getSlotPriorityTypeSupplySupplyTest() throws Exception {


        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                        new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                        new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetSlots(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("supply-supply.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/priority-type/when-booking-exists.xml")
    void doNotShowPriorityCausesForCurrentBookingTest() throws Exception {


        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "1",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                        new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                        new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetSlots(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("when-booking-exists.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @DatabaseSetup("classpath:controller/priority-type/when-booking-exists-for-services.xml")
    void doNotShowPriorityCausesForCurrentBookingForServicesTest() throws Exception {

        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                                0,
                                "FFWF",
                                "6547757",
                                null,
                                1,
                                ZonedDateTime.of(2018, 1, 5, 0, 30, 0, 0, ZoneId.of("Europe/Moscow")),
                                ZonedDateTime.of(2018, 1, 5, 1, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2018, 1, 2, 10, 0),
                                172
                        ),
                        new BookingResponseV2(
                                0,
                                "FFWF",
                                "6582440",
                                null,
                                1,
                                ZonedDateTime.of(2018, 1, 4, 23, 30, 0, 0, ZoneId.of("Europe/Moscow")),
                                ZonedDateTime.of(2018, 1, 5, 0, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2018, 1, 2, 10, 0),
                                172
                        ),
                        new BookingResponseV2(
                                0,
                                "FFWF",
                                "6587394",
                                null,
                                2,
                                ZonedDateTime.of(2018, 1, 5, 0, 30, 0, 0, ZoneId.of("Europe/Moscow")),
                                ZonedDateTime.of(2018, 1, 5, 1, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2018, 1, 2, 10, 0),
                                172
                        ),
                        new BookingResponseV2(
                                0,
                                "FFWF",
                                "6587519",
                                null,
                                3,
                                ZonedDateTime.of(2018, 1, 5, 0, 30, 0, 0, ZoneId.of("Europe/Moscow")),
                                ZonedDateTime.of(2018, 1, 5, 1, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2018, 1, 2, 10, 0),
                                172
                        )
                )
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(0, 0), LocalTime.of(0, 30)),
                        new TimeSlotResponse(LocalTime.of(1, 0), LocalTime.of(1, 30))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(172L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetFreeTimeSlotsByService(6547757L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("when-booking-exists-for-services.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/priority-type/for-service-supply-supply.xml")
    void getSlotForServicePriorityTypeSupplySupplyTest() throws Exception {

        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 11, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                        new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 30)),
                        new TimeSlotResponse(LocalTime.of(11, 30), LocalTime.of(12, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetFreeTimeSlotsByService(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("for-service-supply-supply.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Не показывать рекомендацию если уже есть противоположный слот
     * 01:30-02:00 - Поставка;
     * 02:00-03:00 - Изъятие;
     *
     * не показывать рекомендацию на 02:00 - 02:30 потому что уже есть слот на изъятие
     */
    @Test
    @DatabaseSetup("classpath:controller/priority-type/exclude-invalid-priority.xml")
    void excludeInvalidPriorityCausesTest() throws Exception {

        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 1, 30, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 2, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ), new BookingResponseV2(
                        0,
                        "FFWF",
                        "3",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 2, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 3, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(1, 0), LocalTime.of(1, 30)),
                        new TimeSlotResponse(LocalTime.of(1, 30), LocalTime.of(2, 0)),
                        new TimeSlotResponse(LocalTime.of(2, 0), LocalTime.of(2, 30)),
                        new TimeSlotResponse(LocalTime.of(2, 30), LocalTime.of(3, 0)),
                        new TimeSlotResponse(LocalTime.of(3, 0), LocalTime.of(3, 30))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetSlots(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("exclude-invalid-priority.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/priority-type/for-service-withdraw-withdraw.xml")
    void withdrawWithdrawMidnightCaseTest() throws Exception {

        BookingListResponseV2 response = new BookingListResponseV2(
                List.of(new BookingResponseV2(
                        0,
                        "FFWF",
                        "2",
                        null,
                        1,
                        ZonedDateTime.of(2018, 1, 5, 5, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        ZonedDateTime.of(2018, 1, 5, 6, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2018, 1, 2, 10, 0),
                        1
                ))
        );

        when(calendaringServiceClient.getBookedSlots(any(), any(), any(), any()))
                .thenReturn(response);

        FreeSlotsForDayResponse freeSlotsForDayResponse = new FreeSlotsForDayResponse(
                LocalDate.of(2018, 1, 5),
                ZoneOffset.of("+0300"),
                List.of(
                        new TimeSlotResponse(LocalTime.of(0, 0), LocalTime.of(1, 0)),
                        new TimeSlotResponse(LocalTime.of(4, 0), LocalTime.of(5, 0)),
                        new TimeSlotResponse(LocalTime.of(5, 0), LocalTime.of(6, 0)),
                        new TimeSlotResponse(LocalTime.of(6, 0), LocalTime.of(7, 0)),
                        new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(12, 0)),
                        new TimeSlotResponse(LocalTime.of(22, 0), LocalTime.of(23, 0)),
                        new TimeSlotResponse(LocalTime.of(23, 0), LocalTime.of(0, 0))
                )
        );

        WarehouseFreeSlotsResponse warehouseFreeSlotsResponse =
                new WarehouseFreeSlotsResponse(1L, List.of(freeSlotsForDayResponse));

        FreeSlotsResponse freeSlotsResponse = new FreeSlotsResponse(List.of(warehouseFreeSlotsResponse));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(
                freeSlotsResponse
        );

        MvcResult mvcResult = performGetFreeTimeSlotsByService(1L);
        JSONAssert.assertEquals(
                getJsonResponseNoFile("for-service-withdraw-withdraw.json"),
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }


    private MvcResult performGetFreeTimeSlotsByService(long requestId) throws Exception {
        return mockMvc.perform(
                get(String.format("/requests/%d/free-time-slots-by-service", requestId))
        ).andDo(print())
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
    }

    private MvcResult performGetSlots(long id) throws Exception {
        return mockMvc.perform(
                get("/requests/" + id + "/getFreeTimeSlots")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }


    private String getJsonResponseNoFile(String name) throws IOException {
        return getJsonNoFile("response/", name);
    }

    private String getJsonNoFile(String prefix, String name) throws IOException {
        return FileContentUtils.getFileContent("controller/priority-type/" + prefix + name);
    }

}
