package ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class AjaxApplyRejectCorrectionsParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "json_phrases")
    private AjaxApplyRejectCorrectionsPhrasesBean jsonPhrases;
    @JsonPath(requestPath = "is_rejected")
    private String isRejected;
    @JsonPath(requestPath = "correction")
    private String correction;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public AjaxApplyRejectCorrectionsPhrasesBean getJsonPhrases() {
        return jsonPhrases;
    }

    public void setJsonPhrases(AjaxApplyRejectCorrectionsPhrasesBean jsonPhrases) {
        this.jsonPhrases = jsonPhrases;
    }

    public String getIsRejected() {
        return isRejected;
    }

    public void setIsRejected(String isRejected) {
        this.isRejected = isRejected;
    }

    public String getCorrection() {
        return correction;
    }

    public void setCorrection(String correction) {
        this.correction = correction;
    }
}
