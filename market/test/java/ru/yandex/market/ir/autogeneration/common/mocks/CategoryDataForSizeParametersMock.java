package ru.yandex.market.ir.autogeneration.common.mocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.mbo.export.MboParameters;

public class CategoryDataForSizeParametersMock extends CategoryData {

    private final Map<Long, MboParameters.Parameter> sizeParameterMap;
    private final Map<Long, Pair<Long, Long>> sizeToNumericRange;
    private final LongSet sizeParamIds;
    private final LongSet sizeRangeParamIds;
    private final LongSet skuDefiningParamIds = new LongOpenHashSet();
    private final LongSet mandatoryForPartnerParamIds = new LongOpenHashSet();
    private final boolean isLeaf;
    private final Map<String, MboParameters.Parameter> paramByXslName;

    public CategoryDataForSizeParametersMock(
            Map<Long, Pair<Long, Long>> sizeToNumericRange,
            boolean isLeaf,
            MboParameters.Parameter... parameters
    ) {
        this.sizeToNumericRange = sizeToNumericRange;
        this.isLeaf = isLeaf;
        this.sizeParameterMap = Arrays.stream(parameters).collect(Collectors.toMap(
                MboParameters.Parameter::getId,
                Function.identity()
        ));
        this.sizeParamIds = new LongOpenHashSet(sizeToNumericRange.keySet());
        this.sizeRangeParamIds = new LongOpenHashSet();
        sizeToNumericRange.values().forEach(range -> {
            sizeRangeParamIds.add(range.getLeft());
            sizeRangeParamIds.add(range.getRight());
        });
        this.paramByXslName = new HashMap<>();
        for (MboParameters.Parameter parameter : parameters) {
            if (parameter.getSkuMode() == MboParameters.SKUParameterMode.SKU_DEFINING) {
                skuDefiningParamIds.add(parameter.getId());
            }
            if (parameter.getMandatoryForPartner()) {
                this.mandatoryForPartnerParamIds.add(parameter.getId());
            }
            if (!parameter.getXslName().isEmpty()) {
                this.paramByXslName.put(parameter.getXslName(), parameter);
            }
        }
    }



    public CategoryDataForSizeParametersMock(
            Map<Long, Pair<Long, Long>> sizeToNumericRange,
            MboParameters.Parameter... parameters
    ) {
        this(sizeToNumericRange, false, parameters);
    }

    @Override
    public MboParameters.Parameter getParamById(long id) {
        return sizeParameterMap.get(id);
    }

    @Override
    public LongSet getSizeParamIds() {
        return LongSets.unmodifiable(sizeParamIds);
    }

    @Override
    public Map<Long, Pair<Long, Long>> getSizeToNumericRange() {
        return Collections.unmodifiableMap(sizeToNumericRange);
    }

    @Override
    public LongSet getSizeNumericRangeParamIds() {
        return sizeRangeParamIds;
    }

    @Override
    public boolean isSkuDefiningParameter(long paramId) {
        return skuDefiningParamIds.contains(paramId);
    }

    @Override
    public LongSet getSkuDefiningParamIds() {
        return skuDefiningParamIds;
    }

    @Override
    public LongSet getMandatoryForPartnerParamIds() {
        return LongSets.unmodifiable(mandatoryForPartnerParamIds);
    }

    @Override
    public boolean isMandatoryForPartnerParameter(long paramId) {
        return mandatoryForPartnerParamIds.contains(paramId);
    }

    @Override
    public boolean isLeaf() {
        return isLeaf;
    }

    @Override
    public MboParameters.Parameter getParamByXslName(String xslName) {
        return paramByXslName.get(xslName);
    }
}
