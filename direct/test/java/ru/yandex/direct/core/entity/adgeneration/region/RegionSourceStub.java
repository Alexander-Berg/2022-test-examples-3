package ru.yandex.direct.core.entity.adgeneration.region;

import java.util.Collection;
import java.util.List;

import ru.yandex.direct.core.entity.adgeneration.model.RegionSuggest;
import ru.yandex.direct.result.Result;

public class RegionSourceStub extends AbstractRegionSource {

    private final String sourceName;
    private final List<RegionSuggest> suggests;

    public RegionSourceStub(String sourceName, List<RegionSuggest> suggests) {
        super(null, null, null);
        this.sourceName = sourceName;
        this.suggests = suggests;
    }

    @Override
    protected Result<Collection<RegionSuggest>> generateRegionsInternal(InputContainer input) {
        return Result.successful(suggests);
    }

    @Override
    public String getRegionSourceName() {
        return sourceName;
    }
}
