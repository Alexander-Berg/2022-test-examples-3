package ru.yandex.autotests.innerpochta.imap.responses;

public final class SelectResponse extends ExamineSelectResponse<SelectResponse> {
    public static final String NONSELECTABLE_FOLDER = "[CLIENTBUG] SELECT Nonselectable folder.";

    public SelectResponse() {
        super("READ-WRITE");
    }
}
