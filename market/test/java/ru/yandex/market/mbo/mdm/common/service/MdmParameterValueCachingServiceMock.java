package ru.yandex.market.mbo.mdm.common.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.export.MboParameters;

/**
 * @author danfertev
 * @since 06.07.2020
 */
public class MdmParameterValueCachingServiceMock implements MdmParameterValueCachingService {
    private Map<Long, Map<Long, MboParameters.ParameterValue>> categoryParameterValues = new HashMap<>();

    @Override
    public Map<Long, MboParameters.ParameterValue> getCategoryParameterValues(long categoryId) {
        Map<Long, MboParameters.ParameterValue> parameterValues = categoryParameterValues.get(categoryId);
        return parameterValues == null ? Map.of() : parameterValues;
    }

    @Override
    public Optional<MboParameters.ParameterValue> getCategoryParameterValue(long categoryId, long paramId) {
        return Optional.ofNullable(getCategoryParameterValues(categoryId).get(paramId));
    }

    public MdmParameterValueCachingServiceMock addCategoryParameterValues(long categoryId,
                                                                          MboParameters.ParameterValue... parameterValues) {
        categoryParameterValues.computeIfAbsent(categoryId, key -> new HashMap<>())
            .putAll(Arrays.stream(parameterValues)
                .collect(Collectors.toMap(MboParameters.ParameterValue::getParamId, Function.identity())));
        return this;
    }

    public MdmParameterValueCachingServiceMock addCategoryParameterValues(long categoryId,
                                                                          List<MboParameters.ParameterValue> parameterValues) {
        categoryParameterValues.computeIfAbsent(categoryId, key -> new HashMap<>())
            .putAll(parameterValues.stream()
                .collect(Collectors.toMap(MboParameters.ParameterValue::getParamId, Function.identity())));
        return this;
    }
}
