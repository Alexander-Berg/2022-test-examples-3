package ru.yandex.autotests.innerpochta.imap.responses;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 16.06.14
 * Time: 18:04
 */
public class CopyResponse extends ImapResponse<GenericResponse> {

    public static final String WRONG_SESSION_STATE = "[CLIENTBUG] COPY Wrong session state for command";
    public static final String UID_COPY_WRONG_SESSION_STATE = "[CLIENTBUG] UID COPY Wrong session state for command";
    public static final String FOLDER_ENCODING_ERROR = "[CLIENTBUG] COPY Folder encoding error.";
    public static final String UID_COPY_FOLDER_ENCODING_ERROR = "[CLIENTBUG] UID COPY Folder encoding error.";
    public static final String NO_MESSAGES = "[CLIENTBUG] COPY Failed (no messages).";
    public static final String NO_SUCH_FOLDER = "[TRYCREATE] COPY No such folder.";

    @Override
    protected void parse(String line) {
    }

    @Override
    protected void validate() {
    }
}
