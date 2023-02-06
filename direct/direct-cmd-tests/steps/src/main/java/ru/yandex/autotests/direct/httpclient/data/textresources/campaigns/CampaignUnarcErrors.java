package ru.yandex.autotests.direct.httpclient.data.textresources.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.04.15
 */
public enum CampaignUnarcErrors implements ITextResource {

    MAX_ACTIVE_CAMPAIGN_COUNT_EXCEED;

    @Override
    public String getBundle() {
        return "http.campaigns.CampaignUnarcErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
