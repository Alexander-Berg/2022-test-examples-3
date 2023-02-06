package ru.yandex.autotests.direct.cmd.data.wallet;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

public enum DayBudgetErrorsEnum implements ITextResource {
    MAX_DAY_BUDGET,
    WRONG_DAY_DUBGET_SUM;

    @Override
    public String getBundle() {
        return "cmd.daybudget.errors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
