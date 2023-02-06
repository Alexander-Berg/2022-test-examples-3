package ru.yandex.autotests.direct.cmd.data.autopayment;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum AutoPaymentSettingsErrorsEnum implements ITextResource {

    AUTOPAYMENT_UNAVAILABLE,
    WALLET_NOT_FOUND,
    WRONG_INPUT_DATA,
    WRONG_CARD,
    PAYMENT_SUM_TOO_SHORT,
    PAYMENT_SUM_TOO_LONG,
    REMAINING_SUM_TOO_LONG,
    PAYMENT_YM_SUM_TOO_LONG,
    REMAINING_SUM_TOO_SHORT,
    DISABLE_WALLET_UNALLOWED;
    @Override
    public String getBundle() {
        return "http.autopayment.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
