package ru.yandex.autotests.direct.cmd.data.banners.additions;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

import java.util.Arrays;
import java.util.List;

/*
* todo javadoc
*/
public class Additions {
    @SerializeKey("callouts")
    List<Callout> callouts;

    public List<Callout> getCallouts() {
        return callouts;
    }

    public Additions withCallouts(List<Callout> callouts) {
        this.callouts = callouts;
        return this;
    }

    public Additions withCallouts(Callout... callouts) {
        return withCallouts(Arrays.asList(callouts));
    }
}
