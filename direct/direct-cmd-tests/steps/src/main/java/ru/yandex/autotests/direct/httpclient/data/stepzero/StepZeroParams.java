package ru.yandex.autotests.direct.httpclient.data.stepzero;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * Created by shmykov on 01.04.15.
 */
public class StepZeroParams extends BasicDirectFormParameters {

    @FormParameter("for_agency")
    private String forAgency;

    public String getForAgency() {
        return forAgency;
    }

    public void setForAgency(String forAgency) {
        this.forAgency = forAgency;
    }
}
