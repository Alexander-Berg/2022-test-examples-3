package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.ArrayList;
import java.util.Collections;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.FetchStoreResponse;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

public class FetchRequest implements ImapRequestBuilder<FetchStoreResponse> {
    public static final String LAST = "*";
    private final String sequenceSet;
    private final ArrayList<String> dataItems = new ArrayList<>();
    private boolean isUid = false;

    private FetchRequest(String sequenceSet) {
        this.sequenceSet = sequenceSet;
    }

    public static FetchRequest fetch(String sequenceSet) {
        return new FetchRequest(sequenceSet);
    }

    @Override
    public ImapRequest<FetchStoreResponse> build(String tag) {
        ImapRequest<FetchStoreResponse> request = new ImapRequest(FetchStoreResponse.class, tag);
        if (isUid) {
            request.add(ImapCmd.UID);
        }
        request.add(ImapCmd.FETCH, sequenceSet, dataItems.size() == 1 ? dataItems.get(0) : roundBraceList(dataItems));
        return request;
    }

    public FetchRequest uid(boolean value) {
        isUid = value;
        return this;
    }

    /* МАКРО */

    public FetchRequest dataItem(String... parameters) {
        Collections.addAll(dataItems, parameters);
        return this;
    }

    public FetchRequest all() {
        return dataItem("ALL");
    }

    public FetchRequest fast() {
        return dataItem("FAST");
    }

    /* СПИСОК КОМАНД */

    public FetchRequest full() {
        return dataItem("FULL");
    }

    public FetchRequest envelope() {
        return dataItem("ENVELOPE");
    }

    public FetchRequest flags() {
        return dataItem("FLAGS");
    }

    public FetchRequest bodystructure() {
        return dataItem("BODYSTRUCTURE");
    }

    public FetchRequest internaldate() {
        return dataItem("INTERNALDATE");
    }

    public FetchRequest rfc822() {
        return dataItem("RFC822");
    }

    public FetchRequest rfc822size() {
        return dataItem("RFC822.SIZE");
    }

    public FetchRequest rfc822Header() {
        return dataItem("RFC822.HEADER");
    }

    public FetchRequest rfc822Text() {
        return dataItem("RFC822.TEXT");
    }

    /* КОМАНДЫ С ВОЗМОЖНЫМИ ДОПОЛНИТЕЛЬНЫМИ АРГУМЕНТАМИ */

    public FetchRequest uid() {
        return dataItem("UID");
    }

    public FetchRequest body() {
        return dataItem("BODY");
    }

    public FetchRequest body(String part) {
        return dataItem(format("BODY[%s]", part));
    }

    public FetchRequest body(String part, String substring) {
        return dataItem(format("BODY[%s]<%s>", part, substring));
    }

    public FetchRequest bodyPeek() {
        return dataItem("BODY.PEEK");
    }

    public FetchRequest bodyPeek(String part) {
        return dataItem(format("BODY.PEEK[%s]", part));
    }

    public FetchRequest bodyPeek(String part, String substring) {
        return dataItem(format("BODY.PEEK[%s]<%s>", part, substring));
    }

    @Override
    public String toString() {
        String tag = "BUILD_TAG";
        return this.build(tag).toString().replaceFirst(tag + "\\s", "");
    }
}
