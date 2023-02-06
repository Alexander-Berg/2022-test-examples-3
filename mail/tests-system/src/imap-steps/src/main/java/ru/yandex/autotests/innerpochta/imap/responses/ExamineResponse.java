package ru.yandex.autotests.innerpochta.imap.responses;

public final class ExamineResponse extends ExamineSelectResponse<ExamineResponse> {
    public static final String NONSELECTABLE_FOLDER = "[CLIENTBUG] EXAMINE Nonselectable folder.";

    public ExamineResponse() {
        super("READ-ONLY");
    }
}
