package ru.yandex.autotests.direct.httpclient.data.textresources.transfer;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.06.15
 */
public enum TransferErrors implements ITextResource {

    MONEY_TRANSFER_DISALLOW_FOR_THIS_CLIENT;


    @Override
    public String getBundle() {
        return "http.pay.TransferErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
