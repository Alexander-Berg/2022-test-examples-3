package ru.yandex.autotests.innerpochta.imap.responses;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.06.14
 * Time: 20:10
 */
public class RenameResponse extends ImapResponse<GenericResponse> {

    public static final String FOLDER_ENCODING_ERROR = "[CLIENTBUG] RENAME Folder encoding error.";
    public static final String UNAVAILABLE_BACKEND_ERROR = "[UNAVAILABLE] RENAME Backend error.";
    public static final String FOLDER_NAME_LARGE = "[CLIENTBUG] RENAME Folder name is too large.";

    @Override
    protected void parse(String line) {

    }

    @Override
    protected void validate() {

    }
}
