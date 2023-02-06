package ru.yandex.autotests.direct.httpclient.data.campaigns.showcontactinfo;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.ContactInfoCmdBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.04.15
 */
public class ShowContactInfoResponseBean {

    @JsonPath(responsePath = "vcard")
    private ContactInfoCmdBean contactInfo;

    public ContactInfoCmdBean getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfoCmdBean contactInfo) {
        this.contactInfo = contactInfo;
    }
}
