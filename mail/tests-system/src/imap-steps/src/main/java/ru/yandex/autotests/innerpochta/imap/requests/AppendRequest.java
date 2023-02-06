package ru.yandex.autotests.innerpochta.imap.requests;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.AppendResponse;

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.dateToString;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

public class AppendRequest implements ImapRequestBuilder<AppendResponse> {
    private final String folderName;
    private final Collection<String> flags = new ArrayList<>();
    private final String message;
    private String dateTime = "";

    private AppendRequest(String folderName, String message) {
        this.folderName = folderName;
        this.message = message;
    }

    public static AppendRequest append(String folderName, InputStream inputStream) throws IOException {
        return new AppendRequest(folderName, IOUtils.toString(inputStream));
    }

    public static AppendRequest append(String folderName, String message) {
        return new AppendRequest(folderName, message);
    }

    @Override
    public ImapRequest<AppendResponse> build(String tag) {
        ImapRequest<AppendResponse> request = new ImapRequest<AppendResponse>(AppendResponse.class, tag) {
            @Override
            public String toString() {
                return substringBefore(super.toString(), "\r\n") + " ... [тело в аттаче]";
            }
        };

        request.add(ImapCmd.APPEND).add(folderName);
        if (!flags.isEmpty()) {
            request.add(roundBraceList(flags));
        }
        if (!dateTime.isEmpty()) {
            request.add(dateTime);
        }
        request.add(message);
        return request;
    }

    public AppendRequest flags(Collection<String> values) {
        flags.addAll(values);
        return this;
    }

    public AppendRequest flags(String... values) {
        return flags(Arrays.asList(values));
    }

    public AppendRequest dateTime(String value) {
        dateTime = value;
        return this;
    }

    public AppendRequest dateTime(Date value) {
        return dateTime(dateToString(value));
    }
}
