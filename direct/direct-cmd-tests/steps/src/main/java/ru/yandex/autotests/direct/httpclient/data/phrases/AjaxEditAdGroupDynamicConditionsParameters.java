package ru.yandex.autotests.direct.httpclient.data.phrases;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class AjaxEditAdGroupDynamicConditionsParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "json_adgroup_dynamic_conditions")
    private AjaxEditAdGroupDynamicConditionsRequestBean jsonPhrases;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public AjaxEditAdGroupDynamicConditionsRequestBean getJsonPhrases() {
        return jsonPhrases;
    }

    public void setJsonPhrases(AjaxEditAdGroupDynamicConditionsRequestBean jsonPhrases) {
        this.jsonPhrases = jsonPhrases;
    }
}
