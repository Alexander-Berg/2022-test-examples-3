package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

/**
 * Created by aleran on 12.08.2015.
 */
public enum DemographyMultiplierErrors implements ITextResource {
    BANNER_MULTI_SAVE_ERROR,
    ERROR_CONDITIONS_COUNT_TEXT,
    ERROR_INTERSECT_CONDITIONS_TEXT,
    ERROR_INCORRECT_DATA,
    ERROR_NULL_DATA,
    ERROR_INCORRECT_FIELD_VALUE; // https://st.yandex-team.ru/DIRECT-92143



    private String value;

    @Override
    public String getBundle() {
        return "backend.campaigns.DemographyMultiplierErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
