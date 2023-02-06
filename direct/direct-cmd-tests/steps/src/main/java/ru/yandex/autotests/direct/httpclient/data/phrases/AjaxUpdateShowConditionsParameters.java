package ru.yandex.autotests.direct.httpclient.data.phrases;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class AjaxUpdateShowConditionsParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "json_phrases")
    private AjaxUpdateShowConditionsRequestBean jsonPhrases;
    @JsonPath(requestPath = "json_adgroup_retargetings")
    private String jsonRetargetings;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public AjaxUpdateShowConditionsRequestBean getJsonPhrases() {
        return jsonPhrases;
    }

    public void setJsonPhrases(AjaxUpdateShowConditionsRequestBean jsonPhrases) {
        this.jsonPhrases = jsonPhrases;
    }

    public String getJsonRetargetings() {
        return jsonRetargetings;
    }

    public void setJsonRetargetings(String jsonRetargetings) {
        this.jsonRetargetings = jsonRetargetings;
    }
}
