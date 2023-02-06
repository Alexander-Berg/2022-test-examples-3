package ru.yandex.travel.commons.logging.masking;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.FormattedMessage;
import org.asynchttpclient.RequestBuilder;
import org.junit.Test;

import ru.yandex.travel.commons.logging.LogEventRequest;
import ru.yandex.travel.commons.logging.LogEventResponse;
import ru.yandex.travel.commons.logging.LoggingMarkers;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMaskingConverterTest {
    LogMaskingConverter converter = LogMaskingConverter.create();
    ObjectMapper objectMapper = LogMaskingConverter.getObjectMapperForLogEvents();

    @Test
    public void testMaskPdfBody() {
        var logResponse = new LogEventResponse();
        logResponse.setResponseHeaders(Map.of(
                "Content-Length", "68862",
                "Content-Type", "application/pdf"
        ));
        logResponse.setResponseBody("%PDF-1.4 pdf-binary-data...");
        LogEvent event = createLogEvent(logResponse);

        var output = new StringBuilder();
        converter.format(event, output);

        var result = output.toString();
        assertThat(result).doesNotContain("pdf-binary-data");
        assertThat(result).contains("\"response_body\":\"**PDF**\"");
    }

    @Test
    public void testMaskPdfAttachment() {
        var logRequest = new LogEventRequest();
        logRequest.setRequestHeaders(Map.of());
        RequestBuilder requestBuilder = new RequestBuilder();
        requestBuilder.addFormParam("args", "{\"user\":\"evgen\"}");
        requestBuilder.addFormParam("attachments", "[" +
                "{\"filename\":\"1.pdf\",\"mime_type\":\"application/pdf\",\"data\":\"%PDF-1.4 pdf-binary-data...\"}," +
                "{\"filename\":\"2.txt\",\"data\":\"text-data-1\"}," +
                "{\"filename\":\"3.txt\",\"mime_type\":\"text\",\"data\":\"text-data-2\"}" +
                "]");
        logRequest.setRequestBody(requestBuilder.build().getFormParams());
        LogEvent event = createLogEvent(logRequest);

        var output = new StringBuilder();
        converter.format(event, output);

        var result = output.toString();
        assertThat(result).doesNotContain("pdf-binary-data");
        assertThat(result).contains(
                "{\"filename\":\"1.pdf\",\"mime_type\":\"application/pdf\",\"data\":\"**PDF**\"}"
        );
        assertThat(result).contains("{\"filename\":\"2.txt\",\"data\":\"text-data-1\"}");
        assertThat(result).contains("{\"filename\":\"3.txt\",\"mime_type\":\"text\",\"data\":\"text-data-2\"}");
    }

    @SneakyThrows
    private LogEvent createLogEvent(Object messageObject) {
        var event = new MutableLogEvent();
        event.setMarker(new MarkerManager.Log4jMarker(LoggingMarkers.HTTP_REQUEST_RESPONSE_MARKER.getName()));
        event.setMessage(new FormattedMessage(objectMapper.writeValueAsString(messageObject)));
        return event;
    }

}
