package ru.yandex.autotests.direct.cmd.data.transfer;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum TransferDoneErrors implements ITextResource {

    TRANSFER_DISALLOW_CAMPAIGNS_DOESNT_SELECTED,
    TRANSFER_SUM_SHOULD_BE_MORE_THAN,
    CAMPAIGN_SHOULD_NOT_BE_AT_CAMPAIGNS_FROM,
    LESS_THAN_ZERO,
    MANY_TO_MANY;


    @Override
    public String getBundle() {
        return "http.pay.TransferErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale())
                .replace("\\n","\n");
    }
}
