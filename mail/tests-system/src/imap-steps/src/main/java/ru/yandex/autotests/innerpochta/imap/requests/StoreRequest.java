package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.FetchStoreResponse;

public class StoreRequest implements ImapRequestBuilder<FetchStoreResponse> {

    public static final String FLAGS = "flags";
    public static final String FLAGS_SILENT = "flags.silent";
    public static final String PLUS_FLAGS = "+flags";
    public static final String PLUS_FLAGS_SILENT = "+flags.silent";
    public static final String MINUS_FLAGS = "-flags";
    public static final String MINUS_FLAGS_SILENT = "-flags.silent";
    private final String sequenceSet;
    private final String itemName;
    private final String value;
    private boolean isUid = false;

    private StoreRequest(String sequenceSet, String itemName, String value) {
        this.sequenceSet = sequenceSet;
        this.itemName = itemName;
        this.value = value;
    }

    public static StoreRequest store(String sequenceSet, String itemName, String value) {
        return new StoreRequest(sequenceSet, itemName, value);
    }

    @Override
    public ImapRequest<FetchStoreResponse> build(String tag) {
        ImapRequest<FetchStoreResponse> request = new ImapRequest(FetchStoreResponse.class, tag);
        if (isUid) {
            request.add(ImapCmd.UID);
        }
        request.add(ImapCmd.STORE, sequenceSet, itemName, value);
        return request;
    }

    public StoreRequest uid(boolean value) {
        isUid = value;
        return this;
    }
}
