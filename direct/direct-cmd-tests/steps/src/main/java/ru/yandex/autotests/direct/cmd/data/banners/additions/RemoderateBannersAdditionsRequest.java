package ru.yandex.autotests.direct.cmd.data.banners.additions;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;

import java.util.Arrays;
import java.util.List;

/*
* todo javadoc
*/
public class RemoderateBannersAdditionsRequest extends BasicDirectRequest {
    @SerializeKey("cid")
    private Long cid;

    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    @SerializeKey("adgroup_ids")
    private List<Long> adgroupIds;

    public Long getCid() {
        return cid;
    }

    public RemoderateBannersAdditionsRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public List<Long> getAdgroupIds() {
        return adgroupIds;
    }

    public RemoderateBannersAdditionsRequest withAdgroupIds(List<Long> adgroupIds) {
        this.adgroupIds = adgroupIds;
        return this;
    }

    public RemoderateBannersAdditionsRequest withAdgroupIds(Long... adgroupIds) {
        return withAdgroupIds(Arrays.asList(adgroupIds));
    }
}
