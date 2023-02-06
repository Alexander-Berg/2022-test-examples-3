package ru.yandex.autotests.direct.cmd.data.banners.additions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannersAdditionsType;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/*
* todo javadoc
*/
public class GetBannersAdditionsRequest extends BasicDirectRequest {

    public static GetBannersAdditionsRequest getDefaultCalloutsRequest(String ulogin) {
        return new GetBannersAdditionsRequest()
                .withAdditionsType(BannersAdditionsType.CALLOUT)
                .withLimit(10)
                .withOffset(0)
                .withUlogin(ulogin);
    }

    public static GetBannersAdditionsRequest getDefaultDisclaimerRequest(String ulogin) {
        return new GetBannersAdditionsRequest()
                .withAdditionsType(BannersAdditionsType.DISCLAIMER)
                .withLimit(10)
                .withOffset(0)
                .withUlogin(ulogin);
    }

    @SerializeKey("limit")
    protected Integer limit;

    @SerializeKey("offset")
    protected Integer offset;

    @SerializeKey("additions_type")
    protected BannersAdditionsType additionsType;

    public Integer getLimit() {
        return limit;
    }

    public GetBannersAdditionsRequest withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public GetBannersAdditionsRequest withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public BannersAdditionsType getAdditionsType() {
        return additionsType;
    }

    public GetBannersAdditionsRequest withAdditionsType(BannersAdditionsType additionsType) {
        this.additionsType = additionsType;
        return this;
    }
}
