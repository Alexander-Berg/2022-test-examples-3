package ru.yandex.autotests.direct.httpclient.data.textresources.banners;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by f1nal on 01.07.15
 * TESTIRT-6117.
 */
public enum DynamicGroupsErrorTexts implements ITextResource {

    SEARCH_PRICE_CAN_NOT_BE_LOWER_THAN,
    DUPLICATE_CONDITION,
    EMPTY_CONDITION_TITLE,
    MAX_CONDITIONS_IN_GROUP,
    NEED_GROUP_NAME,
    ERROR_BANNERS_NOT_FOUND,
    ERROR_BANNER_BODY_NOT_FOUND,
    ERROR_DYNAMIC_CONDITIONS_NOT_FOUND,
    NEED_MAIN_DOMAIN,
    BAD_INPUT_PARAMS,
    ERROR_HREF_PARAMS;

    @Override
    public String getBundle() {
            return "http.dynamicgroups.errors";
    }

    @Override
    public String toString() {
            return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
