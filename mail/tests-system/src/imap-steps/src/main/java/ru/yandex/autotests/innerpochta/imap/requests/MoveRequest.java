package ru.yandex.autotests.innerpochta.imap.requests;

import java.util.List;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.MoveResponse;

import static com.google.common.base.Joiner.on;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.08.14
 * Time: 20:04
 */
public class MoveRequest implements ImapRequestBuilder<MoveResponse> {

    private String sequenceSet;
    private String folderToMove;

    private boolean isUid = false;

    private MoveRequest(String sequenceSet, String folderToMove) {
        this.sequenceSet = sequenceSet;
        this.folderToMove = folderToMove;
    }

    public static MoveRequest move(List<String> ids, String folder) {
        return new MoveRequest(on(",").join(ids), folder);
    }

    public static MoveRequest move(String sequenceSet, String folder) {
        return new MoveRequest(sequenceSet, folder);
    }

    @Override
    public ImapRequest<MoveResponse> build(String tag) {
        ImapRequest<MoveResponse> request = new ImapRequest(MoveResponse.class, tag);
        if (isUid) {
            request.add(ImapCmd.UID);
        }
        return request.add(ImapCmd.MOVE, sequenceSet, folderToMove);
    }

    public MoveRequest uid(boolean value) {
        isUid = value;
        return this;
    }
}
