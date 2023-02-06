package ru.yandex.market.ff.controller.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.http.entity.ContentType;
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
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.TryBookSlotResultStatus;
import ru.yandex.market.logistics.calendaring.client.dto.exceptions.BookSlotException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;

public class SelectSlotViaCalendaringServiceTests extends MvcIntegrationTest {

    private static final String REQUEST_SLOT = "{\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}";
    private static final long VALID_REQ_ID = 1;

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
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/when-on-call-client/before.xml")
    void whenFeatureIsOnThenCallClientTest() throws Exception {
        performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
        Mockito.verify(calendaringServiceClient).bookSlot(any());
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/request-built-correctly/before.xml")
    void requestBuiltCorrectlyTest() throws Exception {
        performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
        ArgumentCaptor<BookSlotRequest> captor = ArgumentCaptor.forClass(BookSlotRequest.class);
        Mockito.verify(calendaringServiceClient).bookSlot(captor.capture());

        BookSlotRequest value = captor.getValue();
        assertions.assertThat(value.getExternalId()).describedAs("externalId").isEqualTo("1");
        assertions.assertThat(value.getSource()).describedAs("source").isEqualTo("FFWF-test");
        assertions.assertThat(value.getMeta().get("ffwfId")).describedAs("meta->ffwfId").isEqualTo(1L);
        assertions.assertThat(value.getMeta().get("serviceRequestId")).describedAs("meta->serviceRequestId")
                .isEqualTo("101");
        assertions.assertThat(value.getMeta().get("externalRequestId")).describedAs("meta->externalRequestId")
                .isEqualTo("external-id");
        assertions.assertThat(value.getMeta().get("requestType")).describedAs("meta->requestType").isEqualTo("SUPPLY");
        assertions.assertThat(value.getMeta().get("readableRequestType")).describedAs("meta->readableRequestType")
                .isEqualTo("Поставка");
        assertions.assertThat(value.getMeta().get("status")).describedAs("meta->status").isEqualTo("VALIDATED");
        assertions.assertThat(value.getMeta().get("supplierId")).describedAs("meta->supplierId").isEqualTo("1");
        assertions.assertThat(value.getMeta().get("supplierName")).describedAs("meta->supplierName")
                .isEqualTo("supplier1");
        assertions.assertThat(value.getMeta().get("supplierType")).describedAs("meta->supplierType")
                .isEqualTo("FIRST_PARTY");
        assertions.assertThat(value.getMeta().get("totalCost")).describedAs("meta->totalCost")
                .isEqualTo(new BigDecimal("151.50"));
        assertions.assertThat(value.getMeta().get("itemsCount")).describedAs("meta->itemsCount").isEqualTo(3L);
        assertions.assertThat(value.getMeta().get("palletsCount")).describedAs("meta->palletsCount").isEqualTo(1L);
        assertions.assertThat(value.getMeta().get("approximateVolume")).describedAs("meta->approximateVolume")
                .isEqualTo(new BigDecimal("150"));
        assertions.assertThat(value.getMeta().get("requestCreator")).describedAs("meta->requestCreator")
                .isEqualTo("Tester");
        assertions.assertThat(value.getMeta().get("edo")).describedAs("meta->edo").isEqualTo(true);
        String urls = (String) value.getMeta().get("urls");
        assertions.assertThat(urls).describedAs("meta->urls").isEqualTo("http://localhost:80/");
        assertions.assertThat(value.getMeta().get("comment")).describedAs("meta->comment").isEqualTo("hello");
        assertions.assertThat(value.getMeta().get("vetis")).describedAs("meta->vetis").isEqualTo(true);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/calendar-booking-created/before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/book-via-cs/calendar-booking-created/after.xml",
            assertionMode = NON_STRICT)
    void calendarBookingCreatedTest() throws Exception {

        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));

        BookSlotResponse bookSlotResponse = new BookSlotResponse(1, 1, from, to);
        when(calendaringServiceClient.bookSlot(any())).thenReturn(bookSlotResponse);

        performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/calendar-booking-created/before-xdoc.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/book-via-cs/calendar-booking-created/after-xdoc.xml",
            assertionMode = NON_STRICT)
    void calendarBookingForXDocShadowSupplyCreatedTest() throws Exception {

        ZonedDateTime from = ZonedDateTime.of(2018, 1, 6, 9, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        ZonedDateTime to = ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"));

        BookSlotResponse bookSlotResponse = new BookSlotResponse(1, 1, from, to);
        when(calendaringServiceClient.bookSlot(any())).thenReturn(bookSlotResponse);

        performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );

        ArgumentCaptor<BookSlotRequest> captor = ArgumentCaptor.forClass(BookSlotRequest.class);
        Mockito.verify(calendaringServiceClient).bookSlot(captor.capture());
        assertions.assertThat(captor.getValue().getWarehouseId()).isEqualTo(2L);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/already-booked/before.xml")
    void whenAlreadyBookedResponseTest() throws Exception {

        Mockito.when(calendaringServiceClient.bookSlot(any())).thenThrow(new BookSlotException(
                TryBookSlotResultStatus.SLOT_ALREADY_BOOKED, "Oh no! The slot has been booked already"));

        MvcResult result = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );

        String content = getFileContent("controller/request-api/book-via-cs/already-booked/response.json");
        JSONAssert.assertEquals(content, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/quota-exceeded/before.xml")
    void whenQuotaExceededResponseTest() throws Exception {
        Mockito.when(calendaringServiceClient.bookSlot(any())).thenThrow(new BookSlotException(
                TryBookSlotResultStatus.QUOTA_EXCEEDED, "Oh no! Quota exceeded"));

        MvcResult result = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );

        String content = getFileContent("controller/request-api/book-via-cs/quota-exceeded/response.json");
        JSONAssert.assertEquals(content, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/unexpected-exception/before.xml")
    void whenUnexpectedExceptionResponseTest() throws Exception {
        Mockito.when(calendaringServiceClient.bookSlot(any())).thenThrow(new BookSlotException(
                TryBookSlotResultStatus.EXCEPTION, "Oh no! There was exception"));

        MvcResult result = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );

        String content = getFileContent("controller/request-api/book-via-cs/unexpected-exception/response.json");
        JSONAssert.assertEquals(content, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/out-of-rating-period/before.xml")
    void badRequestWhenOutOfRatingPeriodTest() throws Exception {
        MvcResult result = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );

        String content = getFileContent("controller/request-api/book-via-cs/out-of-rating-period/response.json");
        JSONAssert.assertEquals(content, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/book-via-cs/get-real-supplier/before.xml")
    void realSupplierFromItemsTest() throws Exception {
        performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 30), LocalTime.of(10, 0)
        );
        ArgumentCaptor<BookSlotRequest> captor = ArgumentCaptor.forClass(BookSlotRequest.class);
        Mockito.verify(calendaringServiceClient).bookSlot(captor.capture());

        BookSlotRequest value = captor.getValue();
        assertions.assertThat(value.getSupplierId()).describedAs("real supplier id").isEqualTo("King Kong");
    }

    private MvcResult performSelectSlot(long requestId, LocalDate date, LocalTime from, LocalTime to) throws Exception {

        return mockMvc.perform(
                post("/requests/" + requestId + "/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, date, from, to))
        ).andReturn();
    }

}
