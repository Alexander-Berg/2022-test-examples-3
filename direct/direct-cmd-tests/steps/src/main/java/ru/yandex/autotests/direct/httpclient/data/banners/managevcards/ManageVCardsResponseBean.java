package ru.yandex.autotests.direct.httpclient.data.banners.managevcards;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.ContactInfoCmdBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

public class ManageVCardsResponseBean extends BasicDirectRequestParameters {

    @JsonPath(responsePath = "vcards")
    private List<ContactInfoCmdBean> vcards;

    public List<ContactInfoCmdBean> getVcards() {
        return vcards;
    }

    public void setVcards(List<ContactInfoCmdBean> vcards) {
        this.vcards = vcards;
    }
}