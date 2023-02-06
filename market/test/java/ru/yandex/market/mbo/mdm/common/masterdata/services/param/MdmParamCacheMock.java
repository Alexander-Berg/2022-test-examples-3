package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;

public class MdmParamCacheMock implements MdmParamCache {

    private final Map<Long, MdmParam> storage;

    private static final Set<Long> NOT_WRITTEN_PARAM_IDS = Set.of(KnownMdmParams.DQ_SCORE);

    public MdmParamCacheMock(List<MdmParam> params) {
        storage = params.stream()
            .filter(it -> !NOT_WRITTEN_PARAM_IDS.contains(it.getId()))
            .collect(Collectors.toMap(MdmParam::getId, it -> it));
    }

    @Override
    public List<MdmParam> find(Collection<Long> mdmParamIds) {
        return mdmParamIds.stream()
            .map(this::get)
            .collect(Collectors.toList());
    }

    @Override
    public MdmParam get(long mdmParamId) {
        return storage.get(mdmParamId);
    }

    @Override
    public Optional<MdmParam> getByMboParamId(long id) {
        return storage.values().stream()
            .filter(it -> it.getExternals().getMboParamId() == id)
            .findFirst();
    }

    @Override
    public Collection<MdmParam> getAll() {
        return storage.values();
    }

    public void add(MdmParam param) {
        Preconditions.checkArgument(!storage.containsKey(param.getId()));
        storage.put(param.getId(), param);
    }


    public void addAll(List<MdmParam> params) {
        params.forEach(this::add);
    }

    @Override
    public void refresh() {

    }
}
