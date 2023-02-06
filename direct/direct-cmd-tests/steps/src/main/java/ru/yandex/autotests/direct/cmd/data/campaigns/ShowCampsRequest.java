package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ShowCampsRequest extends BasicDirectRequest {

    @SerializeKey("tab")
    private String tab;

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public ShowCampsRequest withTab(String tab) {
        this.tab = tab;
        return this;
    }
}
