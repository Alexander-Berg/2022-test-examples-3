package ru.yandex.autotests.direct.httpclient.data.textresources.pay;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public enum PayErrors implements ITextResource {

    CAMPAIGN_DOES_NOT_MODERATE;


    @Override
    public String getBundle() {
        return "http.pay.PayErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
