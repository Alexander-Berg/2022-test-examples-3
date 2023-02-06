package ru.yandex.autotests.direct.httpclient.data.textresources.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 18.04.15
 */
public enum CampaignValidationErrors implements ITextResource {

    EMPTY_CAMPAIGN_NAME,
    EMPTY_EMAIL,
    TOO_LONG_EMAIL,
    INCORRECT_EMAIL,
    TOO_LONG_FIO,
    INCORRECT_START_DATE,
    EMPTY_START_DATE,
    INCORRECT_FINISH_DATE,
    FINISH_DATE_LESS_THAN_CURRENT,
    FINISH_DATE_LESS_THAN_START_DATE,
    BACKSPACES_CAMPAIGN_NAME,
    INCORRECT_SYMBOLS_CAMPAIGN_NAME,
    INCORRECT_CID,
    EMPTY_CID,
    INCORRECT_BROAD_MATCH_LIMIT,
    INCORRECT_BROAD_MATCH_RATE_VALUE,
    INCORRECT_CID_OR_LOGIN,
    THIS_CAMPAIGN_DOESNT_BELONG_TO_USER,
    MAX_IP_ADDRESS_COUNT_EXCEEDED,
    INCORRECT_NETWORK_TARGETING,
    INCORRECT_DEVICE_TYPE_TARGETING;

    @Override
    public String getBundle() {
        return "http.campaigns.CampaignValidationErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
