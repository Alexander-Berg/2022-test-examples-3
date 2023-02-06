package ru.yandex.autotests.direct.httpclient.data.textresources.groups;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 18.12.14.
 */
public enum EditGroupsErrors implements ITextResource {

    NO_GROUPS_FOUND,
    BANNERS_UNAVAILABLE_FOR_EDIT,
    NO_GROUP_NUMBER,
    NO_GROUP_NAME,
    NO_HREF,
    NO_PHRASES,
    NO_GEO,
    EASY_GROUP_CAN_CONTAIN_ONLY_ONE_BANNER,
    NO_BANNERS_IN_GROUP,
    BANNERS_SHOULD_BE_ADDED,
    TITLE_CONTAINS_WRONG_SYMBOLS,
    BODY_CONTAINS_WRONG_SYMBOLS,
    VERY_LONG_TITLE,
    VERY_LONG_BODY,
    VERY_LONG_CONTINUOUS_WORD_IN_TITLE,
    VERY_LONG_CONTINUOUS_WORD_IN_BODY,
    ARCHIVED_CAMPAIGN,
    SAVE_ARCHIVED_CAMPAIGN,
    SAVE_ARCHIVED_CAMPAIGN2,
    ARCHIVED_BANNER,
    NO_APP_HREF_IN_GROUP,
    NO_MOBILE_CONTENT_IN_GROUP,
    INCORRECT_NETWORK_TARGETING,
    INCORRECT_DEVICE_TYPE_TARGETING,
    VIDEO_ADDITION_NOT_FOUND;

    @Override
    public String getBundle() {
        return "http.groups.editGroupsErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
