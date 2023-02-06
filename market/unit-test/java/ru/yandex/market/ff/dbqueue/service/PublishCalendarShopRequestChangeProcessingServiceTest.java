package ru.yandex.market.ff.dbqueue.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ru.yandex.market.ff.model.dbqueue.PublishCalendaringChangePayload;
import ru.yandex.market.ff.model.dto.CalendaringChangeDTO;
import ru.yandex.market.ff.model.entity.CalendarBookingEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.LogbrokerPublishingService;
import ru.yandex.market.ff.service.enums.LogbrokerTopic;
import ru.yandex.market.ff.service.timeslot.CalendarBookingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PublishCalendarShopRequestChangeProcessingServiceTest {


    private final PublishCalendarShopRequestChangeProcessingService service;

    private final CalendarBookingService calendarBookingService;
    private final LogbrokerPublishingService logbrokerPublishingService;

    public PublishCalendarShopRequestChangeProcessingServiceTest() {

        ObjectMapper logbrokerObjectMapper = new ObjectMapper();
        logbrokerObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        logbrokerPublishingService = mock(LogbrokerPublishingService.class);
        calendarBookingService = mock(CalendarBookingService.class);

        this.service = new PublishCalendarShopRequestChangeProcessingService(
                logbrokerObjectMapper,
                logbrokerPublishingService,
                calendarBookingService
        );
    }


    @Test
    void addBookingIdByRequestIdTest() {

        LocalDateTime localDateTime = LocalDateTime.of(2021, 1, 1, 1, 0);

        long requestId = 1L;

        PublishCalendaringChangePayload payload = new PublishCalendaringChangePayload(
                requestId,
                new CalendaringChangeDTO(),
                new CalendaringChangeDTO(),
                localDateTime,
                LogbrokerTopic.CALENDARING_META_INFO_CHANGE_EVENTS,
                "FFWF"
        );

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);

        when(calendarBookingService.findByRequestId(requestId))
                .thenReturn(Optional.of(new CalendarBookingEntity(100L, shopRequest, null, true)));

        service.processPayload(payload);

        ArgumentCaptor<LogbrokerTopic> topicCaptor = ArgumentCaptor.forClass(LogbrokerTopic.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(logbrokerPublishingService).publish(topicCaptor.capture(), bodyCaptor.capture());

        String eventBodyJson = new String(bodyCaptor.getValue());
        String expectedJson = "{\"externalId\":\"1\",\"oldMeta\":{\"ffwfId\":0},\"newMeta\":{\"ffwfId\":0}," +
                "\"updatedTime\":{\"nano\":0,\"year\":2021,\"monthValue\":1,\"dayOfMonth\":1,\"hour\":1,\"minute\":0," +
                "\"second\":0,\"month\":\"JANUARY\",\"dayOfWeek\":\"FRIDAY\",\"dayOfYear\":1,\"chronology\":" +
                "{\"calendarType\":\"iso8601\",\"id\":\"ISO\"}}," +
                "\"source\":\"FFWF\",\"bookingId\":100}";

        JSONAssert.assertEquals(expectedJson, eventBodyJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void doNotAddBookingIdByRequestIdTest() {

        LocalDateTime localDateTime = LocalDateTime.of(2021, 1, 1, 1, 0);

        long requestId = 1L;

        PublishCalendaringChangePayload payload = new PublishCalendaringChangePayload(
                requestId,
                new CalendaringChangeDTO(),
                new CalendaringChangeDTO(),
                localDateTime,
                LogbrokerTopic.CALENDARING_META_INFO_CHANGE_EVENTS,
                "FFWF"
        );

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);

        when(calendarBookingService.findByRequestId(requestId))
                .thenReturn(Optional.of(new CalendarBookingEntity(100L, shopRequest, null, false)));

        service.processPayload(payload);

        ArgumentCaptor<LogbrokerTopic> topicCaptor = ArgumentCaptor.forClass(LogbrokerTopic.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(logbrokerPublishingService).publish(topicCaptor.capture(), bodyCaptor.capture());

        String eventBodyJson = new String(bodyCaptor.getValue());
        String expectedJson = "{\"externalId\":\"1\",\"oldMeta\":{\"ffwfId\":0},\"newMeta\":{\"ffwfId\":0}," +
                "\"updatedTime\":{\"nano\":0,\"year\":2021,\"monthValue\":1,\"dayOfMonth\":1,\"hour\":1,\"minute\":0," +
                "\"second\":0,\"month\":\"JANUARY\",\"dayOfWeek\":\"FRIDAY\",\"dayOfYear\":1,\"chronology\":" +
                "{\"calendarType\":\"iso8601\",\"id\":\"ISO\"}}," +
                "\"source\":\"FFWF\"}";

        JSONAssert.assertEquals(expectedJson, eventBodyJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void whenNoBookingId() {

        LocalDateTime localDateTime = LocalDateTime.of(2021, 1, 1, 1, 0);

        long requestId = 1L;

        PublishCalendaringChangePayload payload = new PublishCalendaringChangePayload(
                requestId,
                new CalendaringChangeDTO(),
                new CalendaringChangeDTO(),
                localDateTime,
                LogbrokerTopic.CALENDARING_META_INFO_CHANGE_EVENTS,
                "FFWF"
        );

        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(requestId);

        when(calendarBookingService.findByRequestId(requestId))
                .thenReturn(Optional.empty());

        service.processPayload(payload);

        ArgumentCaptor<LogbrokerTopic> topicCaptor = ArgumentCaptor.forClass(LogbrokerTopic.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(logbrokerPublishingService).publish(topicCaptor.capture(), bodyCaptor.capture());

        String eventBodyJson = new String(bodyCaptor.getValue());
        String expectedJson = "{\"externalId\":\"1\",\"oldMeta\":{\"ffwfId\":0},\"newMeta\":{\"ffwfId\":0}," +
                "\"updatedTime\":{\"nano\":0,\"year\":2021,\"monthValue\":1,\"dayOfMonth\":1,\"hour\":1,\"minute\":0," +
                "\"second\":0,\"month\":\"JANUARY\",\"dayOfWeek\":\"FRIDAY\",\"dayOfYear\":1,\"chronology\":" +
                "{\"calendarType\":\"iso8601\",\"id\":\"ISO\"}}," +
                "\"source\":\"FFWF\"}";

        JSONAssert.assertEquals(expectedJson, eventBodyJson, JSONCompareMode.NON_EXTENSIBLE);

    }

}
