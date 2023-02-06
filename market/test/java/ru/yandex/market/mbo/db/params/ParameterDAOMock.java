package ru.yandex.market.mbo.db.params;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.ConcurrentUpdateException;
import ru.yandex.market.mbo.core.kdepot.saver.KdepotDataProvider;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.ParamNode;
import ru.yandex.market.mbo.gwt.models.params.ParameterInfo;
import ru.yandex.market.mbo.gwt.models.params.ParameterOptionsPositions;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;
import ru.yandex.market.mbo.gwt.models.params.dto.Suggest;

public class ParameterDAOMock implements ParameterDAO {

    private static final Logger log = LoggerFactory.getLogger(ParameterDAOMock.class);

    private final IdGenerator kdepotIdGenerator;
    private final ParameterTestStorage parameterTestStorage;
    private final Lock optionsLock = new ReentrantLock();
    private final Lock paramLock = new ReentrantLock();

    public ParameterDAOMock(IdGenerator kdepotIdGenerator, ParameterTestStorage parameterTestStorage) {
        this.kdepotIdGenerator = kdepotIdGenerator;
        this.parameterTestStorage = parameterTestStorage;
    }

    @Override
    public void insertParameter(CategoryParam p) {
        p.setId(kdepotIdGenerator.getId());
        p.setMboDumpId(KdepotDataProvider.templateToId(p));

        CategoryParam savedParam = cloneParam(p);
        savedParam.setTimestamp(Timestamp.from(Instant.now()));
        log.debug("savedParam.getTimestamp() = {}", savedParam.getTimestamp());
        parameterTestStorage.getRealIdToParam().put(savedParam.getRealParamId(), savedParam);
    }

    @Override
    public void updateParameter(CategoryParam savingParam, boolean isInherited) {
        CategoryParam existingParam = parameterTestStorage.getRealIdToParam().get(savingParam.getRealParamId());
        if (existingParam != null) {
            log.debug("existingParam.getTimestamp() = {}", existingParam.getTimestamp());
        }
        log.debug("savingParam.getTimestamp() = " + savingParam.getTimestamp());
        if (existingParam != null && savingParam.getTimestamp() == existingParam.getTimestamp()) {
            CategoryParam savedParam = cloneParam(savingParam);
            savedParam.setTimestamp(Timestamp.from(Instant.now()));
            savedParam.setOptions(existingParam.getOptions());
            parameterTestStorage.getRealIdToParam().put(savedParam.getRealParamId(), savedParam);
            log.debug("Updated param " + savedParam.getRealParamId()
                + "; options count = " + savedParam.getOptions().size()
                + "; TS = " + savedParam.getTimestamp()
            );
        } else {
            throw new ConcurrentUpdateException();
        }
    }

    private CategoryParam cloneParam(CategoryParam param) {
        CategoryParam copiedParam = param.copy();
        if (param instanceof InheritedParameter) {
            ((InheritedParameter) param).getParent().getOptions()
                .forEach(((InheritedParameter) copiedParam).getParent()::addOption);
        }
        copiedParam.setId(param.getId());
        Map<String, Long> paramOptionIds = param.getOptions().stream()
            .collect(Collectors.toMap(Option::getName, Option::getId));
        copiedParam.getOptions().forEach(option -> option.setId(paramOptionIds.getOrDefault(option.getName(), 0L)));
        return copiedParam;
    }

    @Override
    public Map<Long, String> removeParameter(long categoryId, long paramId) {
        Map<Long, String> removedValues = new HashMap<>();
        CategoryParam toRemove = parameterTestStorage.getRealIdToParam().values()
            .stream()
            .filter(p -> p.getId() == paramId && p.getCategoryHid() == categoryId)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "Parameter " + paramId + " not found in category " + categoryId));

        toRemove.getOptions().forEach(o -> removedValues.put(o.getId(), o.getName()));
        parameterTestStorage.getRealIdToParam().remove(toRemove.getRealParamId());
        return removedValues;
    }

    @Override
    public void removeOption(Option option, CategoryParam param) {
        CategoryParam realParam = parameterTestStorage.getRealIdToParam().get(param.getRealParamId());
        optionsLock.lock();
        try {
            realParam.removeOption(option);
        } finally {
            optionsLock.unlock();
        }
        log.debug("Removed option " + option.getId() + " from param " + param.getRealParamId());
    }

    @Override
    public void updateParameterClobs(CategoryParam p) {
        CategoryParam toUpdate = parameterTestStorage.getRealIdToParam().get(p.getRealParamId());
        toUpdate.setComment(p.getComment());
        toUpdate.setCommentForOperator(p.getCommentForOperator());
        toUpdate.setCommentForPartner(p.getCommentForPartner());
        toUpdate.setDescription(p.getDescription());
    }

    @Override
    public void updateParameterFast(CategoryParam savingParam, boolean isInherited) {

    }

    @Override
    public void setParamHid(long paramId, long hid) {
        CategoryParam toUpdate = parameterTestStorage.getRealIdToParam().get(paramId);
        toUpdate.setCategoryHid(hid);
    }

    @Override
    public Long getParamHid(long paramId) {
        return parameterTestStorage.getRealIdToParam().get(paramId).getCategoryHid();
    }

    @Override
    public Map<Long, Long> getHidByParamId(Collection<Long> paramIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long paramId : paramIds) {
            Long paramHid = getParamHid(paramId);
            result.computeIfAbsent(paramId, id -> paramHid);
        }
        return result;
    }

    @Override
    public void touchParam(Long categoryHid, Long paramId) {
        touchParam(categoryHid, paramId, Timestamp.from(Instant.now()));
    }

    public void touchParam(Long categoryHid, Long paramId, Timestamp ts) {
        CategoryParam param = parameterTestStorage.getRealIdToParam().values().stream()
            .filter(p -> p.getCategoryHid() == categoryHid)
            .filter(p -> ParameterTestStorage.getParamId(p) == paramId || p.getId() == paramId)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Missing parameter"));
        param.setTimestamp(ts);
    }

    @Override
    public void touchParam(CategoryParam p) {
        touchParam(p, Timestamp.from(Instant.now()));
    }

    public void touchParam(CategoryParam p, Timestamp ts) {
        touchParam(p.getCategoryHid(), p.getId(), ts);
    }

    @Override
    public void touchParams(Collection<Long> paramIds) {
        // Do nothing for now
    }

    @Override
    public void touch(Collection<Long> hids) {
        // Do nothing for now
    }

    @Override
    public void touch(long hid) {
        // Do nothing for now
    }

    @Override
    public void touchByGlobalParamId(long globalParamId) {
        // Do nothing for now
    }

    @Override
    public void touchByGlobalParamIds(Collection<Long> globalParamIds) {
        for (Long globalParamId : globalParamIds) {
            touchByGlobalParamId(globalParamId);
        }
    }

    @Override
    public List<ParameterInfo> getXslNameDuplication(long hid, String xslName) {
        return parameterTestStorage.getRealIdToParam().values()
            .stream()
            .map(this::getParentParam)
            .filter(param -> param.getCategoryHid() == hid && param.getXslName().equals(xslName))
            .map(param -> new ParameterInfo(param.getId(), xslName, param.getCategoryHid(), "Category"))
            .collect(Collectors.toList());
    }

    private CategoryParam getParentParam(CategoryParam param) {
        if (param instanceof ParameterOverride) {
            ParameterOverride override = (ParameterOverride) param;
            CategoryParam parent = parameterTestStorage.getRealIdToParam().get(override.getOverridenParameterId());
            if (parent == null) {
                throw new RuntimeException("Missing parent parameter!");
            }
            return parent;
        }
        return param;
    }

    @Override
    public void disableParameterGlobalOverrides(Collection<Long> realParamIds) {

    }

    @Override
    public Map<Long, List<Long>> getAllDisabledParameterInheritanceCategoryIds(List<Long> paramId) {
        return new HashMap<>();
    }

    @Override
    public List<Long> getGlobalOverridenRealParamIds(List<Long> categoryIds, List<Long> parameterIds) {
        return parameterIds;
    }

    @Override
    public List<Long> getCategoryIds(List<Long> realParameterIds) {
        return realParameterIds.stream()
            .map(id -> parameterTestStorage.getRealIdToParam().get(id))
            .filter(Objects::nonNull)
            .map(CategoryParam::getCategoryHid)
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> getCategoryIdsWithGlobalOverrides(Collection<Long> paramIds) {
        return getCategoryIds(new ArrayList<>(paramIds));
    }


    @Override
    public void disableParameterInheritance(long categoryId, long paramId) {
        parameterTestStorage.getBreakInheritance().computeIfAbsent(categoryId, (k) -> new HashSet<>())
            .add(paramId);
    }

    @Override
    public void removeDisableParameterInheritance(Collection<Long> categoryIds, long paramId) {
        categoryIds.forEach(categoryId -> {
            parameterTestStorage.getBreakInheritance().computeIfAbsent(categoryId, (k) -> new HashSet<>())
                .remove(paramId);
        });
    }

    @Override
    public void clearMeasureInOverrides(long overrideId) {
        // Do nothing
    }

    @Override
    public Map<Long, List<Long>> getOverrideParamCategoryIdsWithGlobalParamIds(List<Long> paramIds) {
        return new HashMap<>();
    }

    @Override
    public Map<Long, Long> getCategoryIdsWithOverrideParam(long paramId) {
        return parameterTestStorage.getRealIdToParam().values()
            .stream()
            .filter(p -> p.getId() == paramId)
            .filter(p -> p instanceof ParameterOverride)
            .collect(Collectors.toMap(CategoryParam::getCategoryHid, CategoryParam::getRealParamId));
    }

    @Override
    public List<ParameterInfo> getGlobalOverrideInfos(Collection<Long> globalParamIds) {
        return parameterTestStorage.getRealIdToParam().values().stream()
            .map(param -> new ParameterInfo(param.getId(), param.getRealParamId(), param.getCategoryHid()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> getCategoryIdsWithCategoryGlobalParam(long globalParamId) {
        return parameterTestStorage.getRealIdToParam().values()
            .stream()
            .filter(p -> p instanceof ParameterOverride)
            .map(p -> (ParameterOverride) p)
            .filter(p -> p.getOverridenParameterId() == globalParamId && p.isGlobalOverride())
            .map(ParameterOverride::getCategoryHid)
            .collect(Collectors.toList());
    }

    @Override
    public void insertEnumOption(CategoryParam p, OptionImpl v) {
        v.setId(kdepotIdGenerator.getId());
        CategoryParam param = parameterTestStorage.getRealIdToParam().get(p.getRealParamId());
        if (p.getCategoryHid() == KnownIds.GLOBAL_CATEGORY_ID && param instanceof InheritedParameter) {
            param = ((InheritedParameter) param).getParent();
        }
        optionsLock.lock();
        try {
            param.addOption(v);
        } finally {
            optionsLock.unlock();
        }
        log.debug("Added option " + v.getId() + " to param " + param.getId());
    }

    @Override
    public void insertBoolOption(CategoryParam p, Option v) {
        insertEnumOption(p, (OptionImpl) v);
    }

    @Override
    public void updateEnumOption(Option v) {
        List<Option> options =
            parameterTestStorage.getRealIdToParam().get(v.getParamId()).getOptions();
        options.removeIf(o -> o.getId() == v.getId());
        options.add(v);
    }

    @Override
    public void updateBoolOption(Option v) {
        updateEnumOption(v);
    }

    @Override
    public Map<Long, Long> getDirectUniqueOptionIdToCategoryIdMap(Long globalParamId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDirectIncludeOfOption(Long realParamId, Long optionId) {
        parameterTestStorage.getDirectInclude().computeIfAbsent(realParamId, k -> new HashSet<>())
            .add(optionId);
    }

    @Override
    public void addDirectIncludeOfOptions(MapSqlParameterSource[] realParamIdAndOptionId) {
        throw new NotImplementedException();
    }

    @Override
    public int deleteDirectIncludeOfOption(Long realParamId, Long optionId) {
        boolean result = parameterTestStorage.getDirectInclude().computeIfAbsent(realParamId, k -> new HashSet<>())
            .remove(optionId);
        return (result) ? 1 : 0;
    }

    @Override
    public int deleteDirectIncludeOfOption(Collection<Long> realParameterIds, Long optionId) {
        int amount = 0;
        for (Long realParameterId : realParameterIds) {
            amount += deleteDirectIncludeOfOption(realParameterId, optionId);
        }
        return amount;
    }

    @Override
    public void deleteDirectIncludeOfOption(Long optionId) {
        parameterTestStorage.getDirectInclude().values().forEach(optionIds -> optionIds.remove(optionId));
    }

    @Override
    public Map<String, List<Pair<Long, Long>>> getExistingParams(List<Long> categoryIds, List<String> xslNames) {
        return Collections.emptyMap();
    }

    @Override
    public void createFastOverride(long hid, InheritedParameter inherited) {
        ParameterOverride override = inherited.getOverride();
        inherited.setCategoryHid(hid);
        override.setCategoryHid(hid);
        override.setOverridenParameterId(inherited.getId());
        override.setId(kdepotIdGenerator.getId());
        insertParameter(override);
    }

    @Override
    public void createOverride(long hid, InheritedParameter inherited) {
        ParameterOverride override = inherited.getOverride();
        inherited.setCategoryHid(hid);
        override.setCategoryHid(hid);
        override.setOverridenParameterId(inherited.getId());
        override.setId(kdepotIdGenerator.getId());
        insertParameter(override);
    }

    @Override
    public <T> T doWithLock(Collection<Long> parameterIds, Supplier<T> action) {
        return action.get();
    }

    @Override
    public void doInTransaction(Runnable action) {
        action.run();
    }

    @Override
    public void doWithLock(Collection<Long> parameterIds, Runnable action) {
        paramLock.lock();
        try {
            action.run();
        } finally {
            paramLock.unlock();
        }
    }

    @Override
    public void runAfterTransaction(Runnable runnable) {
        runnable.run();
    }

    @Override
    public Timestamp getParameterTimestamp(long categoryId, long paramId) {
        return parameterTestStorage.getRealIdToParam().values().stream()
            .filter(p -> p.getCategoryHid() == categoryId)
            .filter(p -> ParameterTestStorage.getParamId(p) == paramId)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Missing parameter"))
            .getTimestamp();
    }

    @Override
    public List<ParamNode> getParametersTree(long categoryHid, Long parameterId) {
        return Collections.emptyList();
    }

    @Override
    public boolean isExistOption(long optionId) {
        return false;
    }

    @Override
    public Map<Long, Suggest> findOption(String searchString, Long rootParamId, boolean fullEquals) {
        return Collections.emptyMap();
    }

    @Override
    public void removeUncheckedThrough(CategoryParam p) {

    }

    @Override
    public void deleteOptionPositions(Long categoryHid, Long paramId, ParameterOptionsPositions.ListType listType) {

    }

    @Override
    public Collection<Long> getSubOverrides(long paramId, long hid) {
        return Collections.emptyList();
    }

    @Override
    public void saveOrderForOptionIds(Long categoryHid, Long paramId, List<Long> orderedIds,
                                      ParameterOptionsPositions.ListType listType) {
    }

    @Override
    public void saveOrderForOptionIdsFast(Long categoryHid, Long paramId, List<Long> orderedIds,
                                          ParameterOptionsPositions.ListType listType) {
    }
}
