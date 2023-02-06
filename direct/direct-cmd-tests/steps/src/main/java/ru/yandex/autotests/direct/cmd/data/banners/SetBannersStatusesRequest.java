package ru.yandex.autotests.direct.cmd.data.banners;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.Map;

public class SetBannersStatusesRequest extends BasicDirectRequest {
    @SerializeKey("json_statuses")
    @SerializeBy(ValueToJsonSerializer.class)
    public Map<String, BannerStatuses> statuses;

    @SerializeKey("adgroup_id")
    private String adGroupId;

    public Map<String, BannerStatuses> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, BannerStatuses> statuses) {
        this.statuses = statuses;
    }

    public SetBannersStatusesRequest withStatuses(Map<String, BannerStatuses> statuses) {
        this.statuses = statuses;
        return this;
    }

    public String getAdGroupId() {
        return adGroupId;
    }

    public void setAdGroupId(String adGroupId) {
        this.adGroupId = adGroupId;
    }

    public SetBannersStatusesRequest withAdGroupId(String adGroupId) {
        this.adGroupId = adGroupId;
        return this;
    }
}
