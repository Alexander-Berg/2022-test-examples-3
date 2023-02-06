package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

public enum PerformanceTgoMultiplierPctErrors implements ITextResource {

    NOT_AN_INT("50.1"),
    TOO_SHORT("19"),
    TOO_LONG("1301"),
    NOT_AN_INT_TOO_SHORT("1.1"),
    NOT_AN_INT_TOO_LONG("1500.9");

    PerformanceTgoMultiplierPctErrors(String value) {
        this.value = value;
    }

    private String value;

    @Override
    public String toString() {
        return "value='" + value + '\'';
    }

    @Override
    public String getBundle() {
        return "backend.campaigns.PerformanceTgoMultiplierPctErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
