package ru.yandex.mail.tests.hound;

import io.restassured.response.Response;

public class MessagesUnread extends OperationWithEnvelopes {
    private MessagesUnread(String responseAsString) {
        super(responseAsString);
    }

    public static MessagesUnread messagesUnreadByFolder(Response response) {
        return new MessagesUnread(response.asString());
    }

    public boolean hasMessage(String mid) {
        return envelopes()
                .stream()
                .anyMatch
                        (e -> e
                                .getMid()
                                .equals(mid)
                        );
    }

    public boolean hasMessage(String mid, String fid) {
        return envelopes()
                .stream()
                .filter
                        (e -> e
                                .getMid()
                                .equals(mid)
                        )
                .anyMatch(
                        e -> e
                                .getFid()
                                .equals(fid)
                );
    }

    public int messageCount() {
        return envelopes().size();
    }
}
