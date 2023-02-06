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
public class DeleteBannersAdditionsRequest extends BasicDirectRequest {

    public static DeleteBannersAdditionsRequest defaultCalloutsRequest(String ulogin, Callout... callouts) {
        return new DeleteBannersAdditionsRequest()
                .withAdditions(new Additions()
                        .withCallouts(Arrays.asList(callouts)))
                .withUlogin(ulogin);
    }

    public static DeleteBannersAdditionsRequest defaultCalloutsRequest(String ulogin, Long... ids) {
        return new DeleteBannersAdditionsRequest()
                .withAdditions(new Additions()
                        .withCallouts(
                                Arrays.asList(ids)
                                        .stream()
                                        .map(a -> (Callout) new Callout().withAdditionsItemId(a))
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

    public DeleteBannersAdditionsRequest withAdditions(Additions additions) {
        this.additions = additions;
        return this;
    }
}
