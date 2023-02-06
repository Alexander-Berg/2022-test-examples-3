package ru.yandex.autotests.direct.httpclient.data.textresources.timeTargeting;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 22.04.15
 */
public enum TimeTargetingValidationErrors implements ITextResource {

    INCORRECT_TIME_TARGETING,
    LESS_THAN_MIN_WORK_HOURS,
    INCORRECT_TIME_ZONE;

    @Override
    public String getBundle() {
        return "http.timeTargeting.TimeTargetingValidationErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
