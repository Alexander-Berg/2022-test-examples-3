package ru.yandex.autotests.direct.httpclient.data.textresources.campaigns;

import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.textresource.ITextResource;
import ru.yandex.autotests.direct.utils.textresource.TextResources;

/**
 * Created by shmykov on 13.11.14.
 */
public enum StopResumeCampErrors implements ITextResource {

    WITHOUT_DO_STOP_ERROR,
    NO_ULOGIN_ERROR;

    @Override
    public String getBundle() {
        return "http.campaigns.stopResumeCampErrors";
    }

    @Override
    public String toString() {
        return TextResources.getText(this, DirectTestRunProperties.getInstance().getDirectCmdLocale());
    }
}
