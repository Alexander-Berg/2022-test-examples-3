package ru.yandex.market.ff.controller.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class RequestControllerSelectSlotTest extends MvcIntegrationTest {

    private static final long VALID_REQ_ID = 1;
    private static final String REQUEST_SLOT = "{\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}";
    private static final long INVALID_REQ_ID = 2;

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @AfterEach
    public void initMocks() {
        lmsClientCachingService.invalidateCache();
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
    @DatabaseSetup("classpath:controller/request-api/before-select-slot-for-outbound-on-marschroute.xml")
    void selectSlotForUpdateForMarschrouteFail() throws Exception {
        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.NOT_FOUND.value()));
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
    @DatabaseSetup("classpath:controller/request-api/on-select-slot-put-inbound-before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/on-select-slot-put-inbound-after.xml",
            assertionMode = NON_STRICT
    )
    void updateSlotPutInbound() throws Exception {
        environmentParamService.setParam("supply-put-inbound-date-param",
                List.of(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        when(calendaringServiceClient.getSlotByExternalIdentifiers(any(), any(), any()))
                .thenReturn(new BookingListResponse(
                        List.of(new BookingResponse(1, "FFWF", "", null, 1, ZonedDateTime.now(),
                                ZonedDateTime.now(), BookingStatus.ACTIVE, null, 100L))));

        MvcResult mvcResult = performSelectSlot(
                VALID_REQ_ID, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0), LocalTime.of(9, 30)
        );
        assertThat(mvcResult.getResponse().getStatus(), equalTo(HttpStatus.OK.value()));

        ShopRequest request = new ShopRequest();
        request.setId(1L);
        request.setRemainingShelfLifeStartDate(LocalDateTime.of(2018, 1, 6, 1, 1));

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
