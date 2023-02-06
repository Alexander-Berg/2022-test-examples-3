package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections;

/**
 * Created by shmykov on 11.06.15.
 */
public enum CorrectionType {

    STOPWORD_FIXATED("stopword-fixated"),
    UNGLUED("unglued");

    private String value;

    CorrectionType(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }
}
