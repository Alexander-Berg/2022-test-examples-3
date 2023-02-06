package ru.yandex.autotests.innerpochta.imap.responses;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.06.14
 * Time: 20:15
 */
public class SubscribeResponse extends ImapResponse<GenericResponse> {

    public static final String FOLDER_ENCODING_ERROR = "[CLIENTBUG] SUBSCRIBE Folder encoding error.";

    @Override
    protected void parse(String line) {

    }

    @Override
    protected void validate() {

    }
}
