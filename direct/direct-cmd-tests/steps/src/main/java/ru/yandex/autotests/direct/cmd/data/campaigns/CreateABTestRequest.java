package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class CreateABTestRequest extends BasicDirectRequest {
    @SerializeKey("primary_cid")
    private Long cid;

    @SerializeKey("secondary_cid")
    private Long secondaryCid;

    @SerializeKey("percent")
    private Integer primaryPercent;

    @SerializeKey("date_from")
    private String dateStart;

    @SerializeKey("date_to")
    private String dateFinish;

    public String getDateStart() {
        return dateStart;
    }

    public CreateABTestRequest withDateStart(String dateStart) {
        this.dateStart = dateStart;
        return this;
    }

    public Integer getPrimaryPercent() {
        return primaryPercent;
    }

    public CreateABTestRequest withPrimaryPercent(Integer primaryPercent) {
        this.primaryPercent = primaryPercent;
        return this;
    }

    public Long getSecondaryCid() {
        return secondaryCid;
    }

    public CreateABTestRequest withSecondaryCid(Long secondaryCid) {
        this.secondaryCid = secondaryCid;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public CreateABTestRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public String getDateFinish() {
        return dateFinish;
    }

    public CreateABTestRequest withDateFinish(String dateFinish) {
        this.dateFinish = dateFinish;
        return this;
    }
}
