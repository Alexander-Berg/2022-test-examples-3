package ru.yandex.autotests.direct.cmd.data.banners.additions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.Arrays;
import java.util.stream.Collectors;

/*
* todo javadoc
*/
public class SaveBannersAdditionsRequest extends BasicDirectRequest {

    public static SaveBannersAdditionsRequest defaultCalloutsRequest(String ulogin, String... callouts) {
        return new SaveBannersAdditionsRequest()
                .withAdditions(new Additions()
                        .withCallouts(
                                Arrays.asList(callouts)
                                        .stream()
                                        .map(a -> new Callout().withCalloutText(a))
                                        .collect(Collectors.toList())
                        ))
                .withUlogin(ulogin);
    }

    @SerializeKey("json_banner_additions")
    @SerializeBy(ValueToJsonSerializer.class)
    private Additions additions;

    public Additions getAdditions() {
        return additions;
    }

    public SaveBannersAdditionsRequest withAdditions(Additions additions) {
        this.additions = additions;
        return this;
    }
}
