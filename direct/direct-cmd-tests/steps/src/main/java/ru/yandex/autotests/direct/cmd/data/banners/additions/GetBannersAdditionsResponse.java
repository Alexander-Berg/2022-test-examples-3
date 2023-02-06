package ru.yandex.autotests.direct.cmd.data.banners.additions;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;

import java.util.ArrayList;
import java.util.List;

/*
* todo javadoc
*/
public class GetBannersAdditionsResponse {

    @SerializedName("success")
    private String success;

    @SerializedName("callouts")
    private List<Callout> callouts = new ArrayList<>();

    public List<Callout> getCallouts() {
        return callouts;
    }

    public GetBannersAdditionsResponse withCallouts(List<Callout> callouts) {
        this.callouts = callouts;
        return this;
    }

    public String getSuccess() {
        return success;
    }

    public GetBannersAdditionsResponse withSuccess(String success) {
        this.success = success;
        return this;
    }
}
