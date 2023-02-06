package ru.yandex.autotests.direct.httpclient.data.textresources;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 19.04.15
 */
public enum DomainValidationErrors implements ITextResource {


    YANDEX_DOMAIN_ERROR,
    INVALID_DOMAIN_FORMAT,
    ONLY_THIRD_LEVEL_DOMAIN,
    TOO_LONG_DOMAIN;

    @Override
    public String getBundle() {
        return "http.campaigns.CampaignValidationErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
