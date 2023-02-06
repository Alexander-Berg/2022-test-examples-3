package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

import static com.google.common.base.Joiner.on;

public class CopyRequest implements ImapRequestBuilder<GenericResponse> {
    private final String sequenceSet;
    private final String folderName;
    private boolean isUid = false;

    private CopyRequest(String sequenceSet, String folderName) {
        this.sequenceSet = sequenceSet;
        this.folderName = folderName;
    }

    public static CopyRequest copy(String sequenceSet, String folderName) {
        return new CopyRequest(sequenceSet, folderName);
    }

    public static CopyRequest copy(List<String> ids, String folderName) {
        return new CopyRequest(on(",").join(ids), folderName);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        ImapRequest<GenericResponse> request = new ImapRequest(GenericResponse.class, tag);
        if (isUid) {
            request.add(ImapCmd.UID);
        }
        request.add(ImapCmd.COPY, sequenceSet, folderName);
        return request;
    }

    public CopyRequest uid(boolean value) {
        isUid = value;
        return this;
    }
}
