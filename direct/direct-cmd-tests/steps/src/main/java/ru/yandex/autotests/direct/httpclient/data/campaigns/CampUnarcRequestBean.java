package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.04.15
 */
public class CampUnarcRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "tab")
    private String tab;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }
}
