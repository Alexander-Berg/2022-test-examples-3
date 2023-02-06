package ru.yandex.autotests.direct.cmd.data.phrases;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AjaxTestPhrasesRequest extends BasicDirectRequest {
    @SerializeKey("phrases")
    private String phrases;

    public String getPhrases() {
        return phrases;
    }

    public AjaxTestPhrasesRequest withPhrases(String phrases) {
        this.phrases = phrases;
        return this;
    }
}
