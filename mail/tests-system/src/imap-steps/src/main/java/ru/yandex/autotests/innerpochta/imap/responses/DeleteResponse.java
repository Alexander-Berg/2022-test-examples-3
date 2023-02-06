package ru.yandex.autotests.innerpochta.imap.responses;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 04.06.14
 * Time: 18:50
 */
public class DeleteResponse extends ImapResponse<GenericResponse> {

    public static final String FOLDER_ENCODING_ERROR = "[CLIENTBUG] DELETE Folder encoding error.";

    @Override
    protected void parse(String line) {
    }

    @Override
    protected void validate() {
    }
}
