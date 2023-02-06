package ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.gapi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GMessageToMailSearchMessageSnippetConverterTest {
    private static final String KEY = "key";
    private static final String VALUE = "value";

    @Test
    void convert() {
    }

    @Test
    void findInMessageHeaders() {
        MessagePartHeader header = new MessagePartHeader();

        header.setName(KEY);
        header.setValue(VALUE);

        List<MessagePartHeader> headers = new ArrayList<>();
        headers.add(header);

        MessagePart payload = new MessagePart();
        payload.setHeaders(headers);

        Message msg = new Message();
        msg.setPayload(payload);

        GApiMessageToMessageSearchSnippetConverter converter = new GApiMessageToMessageSearchSnippetConverter();
        assertEquals(VALUE, converter.findInMessageHeaders(msg, KEY));
    }

    @Test
    void parseMessageDate() {
        MessagePartHeader header = new MessagePartHeader();

        header.setName("Date");
        header.setValue("Fri, 31 May 2019 14:53:28 +0000 (UTC)");

        List<MessagePartHeader> headers = new ArrayList<>();
        headers.add(header);

        MessagePart payload = new MessagePart();
        payload.setHeaders(headers);

        Message msg = new Message();
        msg.setPayload(payload);

        GApiMessageToMessageSearchSnippetConverter converter = new GApiMessageToMessageSearchSnippetConverter();

        Instant ldt = LocalDateTime.of(2019, 5, 31, 14, 53, 28).toInstant(ZoneOffset.UTC);
        assertEquals(ldt, converter.parseMessageDate(msg));
    }
}
