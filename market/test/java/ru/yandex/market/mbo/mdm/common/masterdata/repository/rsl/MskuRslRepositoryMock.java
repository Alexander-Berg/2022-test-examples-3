package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;

public class MskuRslRepositoryMock extends GenericMapperRepositoryMock<MskuRsl, MskuRsl.Key>
    implements MskuRslRepository {

    public MskuRslRepositoryMock() {
        super(null, MskuRsl::getKey);
    }

    @Override
    protected MskuRsl.Key nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, List<MskuRsl>> findByMskuIds(Collection<Long> ids) {
        Set<Long> uniqueKeys = new HashSet<>(ids);
        return findAll().stream()
            .filter(i -> uniqueKeys.contains(i.getMskuId()))
            .collect(Collectors.groupingBy(MskuRsl::getMskuId));
    }
}
