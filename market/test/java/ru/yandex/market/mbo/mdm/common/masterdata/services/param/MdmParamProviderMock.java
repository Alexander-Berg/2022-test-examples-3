package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;

public class MdmParamProviderMock implements MdmParamProvider {

    private final Map<MdmParamIoType, List<Long>> allowedParams = new HashMap<>();
    private final Function<Long, MdmParam> paramGetter;

    public MdmParamProviderMock(MdmParamCache mdmParamCache) {
        this.paramGetter = mdmParamCache::get;
    }

    public void markParamAllowed(MdmParamIoType useCase, long paramId) {
        allowedParams.computeIfAbsent(useCase, k -> new ArrayList<>()).add(paramId);
    }

    public void markParamsAllowed(MdmParamIoType useCase, long... paramIds) {
        for (long paramId : paramIds) {
            markParamAllowed(useCase, paramId);
        }
    }

    public void markParamsAllowed(MdmParamIoType useCase, Collection<Long> paramIds) {
        for (long paramId : paramIds) {
            markParamAllowed(useCase, paramId);
        }
    }

    @Override
    public LinkedHashSet<Long> getParamIdsForIoType(MdmParamIoType type, boolean onlyEnabled) {
        return new LinkedHashSet<>(allowedParams.getOrDefault(type, List.of()));
    }

    @Override
    public Map<Long, MdmParam> getParamsMapForIoType(MdmParamIoType type, boolean onlyEnabled) {
        Set<Long> ids = getParamIdsForIoType(type, onlyEnabled);
        Map<Long, MdmParam> mdmParams = ids.stream()
            .map(paramGetter)
            .collect(Collectors.toMap(MdmParam::getId, Function.identity()));
        return Map.copyOf(mdmParams);
    }

}
