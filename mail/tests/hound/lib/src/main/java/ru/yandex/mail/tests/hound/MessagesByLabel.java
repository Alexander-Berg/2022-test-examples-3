package ru.yandex.mail.tests.hound;

import io.restassured.response.Response;

public class MessagesByLabel extends OperationWithEnvelopes {
    private MessagesByLabel(String responseAsString) {
        super(responseAsString);
    }

    public static MessagesByLabel messagesByLabel(Response response) {
        return new MessagesByLabel(response.asString());
    }

    public boolean hasMessageWithLid(String mid) {
        return envelopes()
                .stream()
                .anyMatch(e -> e.getMid().equals(mid));
    }

    public boolean hasMessageWithLidInFolder(String mid, String fid) {
        return envelopes()
                .stream()
                .filter(e -> e.getFid().equals(fid))
                .anyMatch(e -> e.getMid().equals(mid));
    }
}
