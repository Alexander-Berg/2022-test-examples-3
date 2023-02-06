package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

/**
 * Created by aleran on 12.08.2015.
 */
public enum RetargetingMultiplierErrors implements ITextResource {
    ERROR_NULL,
    ERROR_RETARGETING_NULL,
    GROUP_ERROR_NULL,
    ERROR_COUNT,
    ERROR_RETARGETING_COUNT,
    ERROR_INCORRECT_FIELD_VALUE; // https://st.yandex-team.ru/DIRECT-92143



    private String value;

    @Override
    public String getBundle() {
        return "backend.campaigns.RetargetingMultiplierErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
