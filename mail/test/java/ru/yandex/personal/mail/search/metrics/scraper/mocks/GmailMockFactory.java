package ru.yandex.personal.mail.search.metrics.scraper.mocks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GmailMockFactory {

    public Gmail mockGmail(int serpSize) {
        try {
            return deepMock(serpSize);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Gmail deepMock(int serpSize) throws IOException {
        Gmail gm = mock(Gmail.class, Mockito.RETURNS_DEEP_STUBS);

        when(gm.users().messages().list(anyString()).setQ(anyString()).execute()).thenAnswer(
                (Answer<ListMessagesResponse>) invocation -> createResponse(serpSize));

        when(gm.users().messages().get(anyString(), anyString()).execute()).thenAnswer(
                (Answer<Message>) invocation -> createMessage());

        return gm;
    }

    private ListMessagesResponse createResponse(int size) {
        ListMessagesResponse result = new ListMessagesResponse();
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            messages.add(createMessage());
        }
        result.setMessages(messages);
        result.setResultSizeEstimate((long) size);
        return result;
    }

    private Message createMessage() {
        Message result = new Message();

        MessagePart payload = createEmptyMessagePart();

        result.setPayload(payload);
        result.setSnippet("Snippet");
        result.setId("");

        return result;
    }

    private MessagePart createEmptyMessagePart() {
        List<MessagePartHeader> headers = new ArrayList<>();

        MessagePartHeader from = new MessagePartHeader();
        from.setName("From");
        from.setValue("example@example.com");
        headers.add(from);

        MessagePartHeader subject = new MessagePartHeader();
        subject.setName("Subject");
        subject.setValue("Email subject");
        headers.add(subject);

        MessagePartHeader date = new MessagePartHeader();
        date.setName("Date");
        date.setValue("Sun, 17 Feb 2019 00:41:00 +0300");
        headers.add(date);

        MessagePart result = new MessagePart();
        result.setHeaders(headers);

        return result;
    }
}
