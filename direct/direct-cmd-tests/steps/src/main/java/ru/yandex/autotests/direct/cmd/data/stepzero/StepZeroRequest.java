package ru.yandex.autotests.direct.cmd.data.stepzero;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class StepZeroRequest extends BasicDirectRequest {

    @SerializeKey("for_agency")
    private String forAgency;

    public String getForAgency() {
        return forAgency;
    }

    public void setForAgency(String forAgency) {
        this.forAgency = forAgency;
    }

    public StepZeroRequest withForAgency(String forAgency) {
        this.forAgency = forAgency;
        return this;
    }
}
