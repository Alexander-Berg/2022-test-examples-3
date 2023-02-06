package ru.yandex.autotests.direct.httpclient.data.textresources.firstAid;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 12.05.15
 */
public enum  AcceptOptimizeErrors implements ITextResource {

    INCORRECT_CAMPAIGN_STATUS,
    SOMEONE_ELSE_CAMPAIGN,
    ;

    @Override
    public String getBundle() {
        return "http.firstAid.AcceptOptimizeErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
