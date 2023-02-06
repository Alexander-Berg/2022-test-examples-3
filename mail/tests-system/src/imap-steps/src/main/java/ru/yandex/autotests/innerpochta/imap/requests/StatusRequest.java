package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.StatusResponse;

import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

public class StatusRequest implements ImapRequestBuilder<StatusResponse> {
    private final String folderName;
    private final List<String> items = new ArrayList<>();

    private StatusRequest(String folderName) {
        this.folderName = folderName;
    }

    public static StatusRequest status(String folderName) {
        return new StatusRequest(folderName);
    }

    @Override
    public ImapRequest<StatusResponse> build(String tag) {
        return new ImapRequest(StatusResponse.class, tag).add(ImapCmd.STATUS, folderName, roundBraceList(items));
    }

    public StatusRequest item(String name) {
        items.add(name);
        return this;
    }

    public StatusRequest messages() {
        return item("MESSAGES");
    }

    public StatusRequest recent() {
        return item("RECENT");
    }

    public StatusRequest uidNext() {
        return item("UIDNEXT");
    }

    public StatusRequest uidValidity() {
        return item("UIDVALIDITY");
    }

    public StatusRequest unseen() {
        return item("UNSEEN");
    }
}
