package ru.yandex.autotests.direct.httpclient.data.textresources.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum SetAutoPriceAjaxErrors implements ITextResource {

    WRONG_PHRASE_TYPE_PARAMETER,
    WRONG_MAX_PRICE_PARAMETER,
    WRONG_PROC_PARAMETER,
    OUT_OF_BOUND_MAX_PRICE_PARAMETER,
    WRONG_POSITION_CTR_CORRECTION_PARAMETER;

    @Override
    public String getBundle() {
        return "http.campaigns.setautopriceajaxerrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
