package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

/**
 * Created by aleran on 07.08.2015.
 */
public enum RetargetingMultiplierPctErrors implements ITextResource {
    NOT_AN_INT("100.1"),
    TOO_SHORT("-1"),
    TOO_LONG("1301"),
    NOT_AN_INT_TOO_SHORT("49.1"),
    NOT_AN_INT_TOO_LONG("1300.9");

    RetargetingMultiplierPctErrors(String value) {
        this.value = value;
    }

    private String value;

    @Override
    public String toString() {
        return "value='" + value + '\'';
    }

    @Override
    public String getBundle() {
        return "backend.campaigns.DemographyMultiplierPctErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
