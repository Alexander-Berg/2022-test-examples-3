package ru.yandex.autotests.direct.httpclient.data.textresources.firstAid;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.05.15
 */
public enum SendCampaignOptimizingOrderErrors implements ITextResource {

    INCORRECT_BUDGET,
    ONLY_ONE_TIME;

    @Override
    public String getBundle() {
        return "http.firstAid.SendCampaignOptimizingOrderErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
