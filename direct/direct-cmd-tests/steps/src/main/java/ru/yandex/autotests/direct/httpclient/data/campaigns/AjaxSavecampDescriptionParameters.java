package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : shmykov@yandex-team.ru
 *         Date: 12.11.14
 */
public class AjaxSavecampDescriptionParameters extends AbstractFormParameters {

    @FormParameter("cid")
    private String cid;

    @FormParameter("ulogin")
    private String ulogin;

    @FormParameter("description")
    private String description;

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getUlogin() {
        return ulogin;
    }

    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
