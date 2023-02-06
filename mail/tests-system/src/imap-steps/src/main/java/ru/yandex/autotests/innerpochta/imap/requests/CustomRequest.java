package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.Arrays;
import java.util.List;

import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class CustomRequest implements ImapRequestBuilder<GenericResponse> {
    private final List<String> lines;

    private CustomRequest(String... lines) {
        this.lines = Arrays.asList(lines);
    }

    public static CustomRequest custom(String... lines) {
        return new CustomRequest(lines);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        ImapRequest<GenericResponse> request = new ImapRequest<GenericResponse>(GenericResponse.class, tag);
        for (String line : lines) {
            request.add(line + "\r\n");
        }
        return request;
    }
}
