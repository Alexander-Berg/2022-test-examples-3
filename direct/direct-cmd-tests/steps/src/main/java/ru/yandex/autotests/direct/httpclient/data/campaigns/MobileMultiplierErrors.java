package ru.yandex.autotests.direct.httpclient.data.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;

/**
 * Created by aleran on 12.08.2015.
 */
public enum MobileMultiplierErrors  implements ITextResource {
    BANNER_MULTI_SAVE_ERROR();



    private String value;

    @Override
    public String getBundle() {
        return "backend.campaigns.MobileMultiplierErrors";
    }

    public String getValue() {
        return value;
    }

    public String getErrorText() {
        return TextResourceFormatter.resource(this).
                locale(DirectTestRunProperties.getInstance().getDirectCmdLocale()).toString();
    }
}
