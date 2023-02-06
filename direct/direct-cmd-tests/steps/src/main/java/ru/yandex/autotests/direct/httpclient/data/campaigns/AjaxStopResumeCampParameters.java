package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

public class AjaxStopResumeCampParameters extends AbstractFormParameters {

    @FormParameter("ulogin")
    private String ulogin;
    @FormParameter("cid")
    private String cid;
    @FormParameter("do_stop")
    private String doStop;

    public String getUlogin() {
        return ulogin;
    }

    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getDoStop() {
        return doStop;
    }

    public void setDoStop(String doStop) {
        this.doStop = doStop;
    }
}
