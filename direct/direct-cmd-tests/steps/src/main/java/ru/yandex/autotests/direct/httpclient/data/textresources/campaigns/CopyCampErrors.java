package ru.yandex.autotests.direct.httpclient.data.textresources.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 24.04.15
 */
public enum  CopyCampErrors implements ITextResource {

    EMTY_CID,
    NOT_ALL_FIELDS_FILLED,
    NOT_EXIST_CLIENT,
    NOT_EXIST_CID,
    OTHER_CLIENT_CID,
    ARCHIVE_CAMPAIGN,
    EMPTY_CAMPAIGN,
    CAN_NOT_HAVE_CAMPAIGN,
    NOT_YOUR_CLIENT,
    NOT_COPY_CAMPAIGNS;

    @Override
    public String getBundle() {
        return "http.campaigns.CopyCampErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
