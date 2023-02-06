package ru.yandex.market.mbo.db.params.guru;

import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationVendor;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.tovartree.NameToAliasesSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by anmalysh on 23.03.2017.
 */
public class BaseGuruServiceImpl implements BaseGuruService {

    private Map<Long, Map<Long, Long>> categoryToVendorMap = new HashMap<>();
    private Set<Long> groupCategories = new HashSet<>();
    private Map<Long, Long> guruCategoryByHid = new HashMap<>();
    private ParameterLoaderServiceStub parameterLoaderServiceStub = new ParameterLoaderServiceStub();
    private Map<Long, Set<Long>> categoryToGuruVendors = new HashMap<>();
    private Map<Long, NameToAliasesSettings> nameToAliasByCategoryId = new HashMap<>();

    public void addCategory(Long hid, Long guruCategoryId, boolean isGroup) {
        guruCategoryByHid.put(hid, guruCategoryId);
        if (isGroup) {
            groupCategories.add(guruCategoryId);
        }
    }

    public void addVendor(Long globalVendorId, Long localVendorId, Long categoryId, boolean isGuruVendor) {
        Map<Long, Long> globalToLocalVendor = categoryToVendorMap.computeIfAbsent(categoryId, k -> new HashMap<>());
        globalToLocalVendor.put(globalVendorId, localVendorId);
        if (isGuruVendor) {
            categoryToGuruVendors.computeIfAbsent(categoryId, key -> new HashSet<>()).add(globalVendorId);
        }
    }

    @Override
    public long getLocalVendorIdFromGlobal(long categoryId, long globalVendorId) {
        Map<Long, Long> globalToLocalVendor = categoryToVendorMap.get(categoryId);
        if (globalToLocalVendor == null) {
            return 0;
        }
        return globalToLocalVendor.get(globalVendorId);
    }

    @Override
    public boolean isGroupCategory(long guruCategoryId) {
        return groupCategories.contains(guruCategoryId);
    }

    @Override
    public Long getGuruCategoryByHid(long hid) {
        return guruCategoryByHid.get(hid);
    }

    @Override
    public Map<Long, Long> getGlobal2LocalVendorsMap(long hid) {
        return categoryToVendorMap.get(hid);
    }

    public void addAllCategoryEntities(Collection<CategoryEntities> categoryEntities) {
        this.parameterLoaderServiceStub.addAllCategoryEntities(categoryEntities);
    }

    public void addCategoryEntities(CategoryEntities categoryEntities) {
        this.parameterLoaderServiceStub.addCategoryEntities(categoryEntities);
    }

    public void addCategoryParam(CategoryParam param) {
        this.parameterLoaderServiceStub.addCategoryParam(param);
    }

    public void addNameToAliasesSettingByCategory(long categoryId, NameToAliasesSettings nameToAliases) {
        nameToAliasByCategoryId.put(categoryId, nameToAliases);
    }

    @Override
    public List<? extends ThinCategoryParam> getModelThinPropertyTemplatesByHid(long hid) {
        CategoryEntities categoryEntities = parameterLoaderServiceStub.loadCategoryEntitiesByHid(hid);
        return categoryEntities.getParameters()
            .stream()
            .filter(CategoryParam::isUseForGuru)
            .collect(Collectors.toList());
    }

    public Map<Long, ModelValidationVendor> getVendors(long categoryId) {
        Map<Long, Long> globalToLocalVendor = categoryToVendorMap.getOrDefault(categoryId, Collections.emptyMap());
        Set<Long> guruVendors = categoryToGuruVendors.getOrDefault(categoryId, Collections.emptySet());
        return globalToLocalVendor.keySet().stream()
            .collect(Collectors.toMap(Function.identity(),
                id -> new ModelValidationVendor(id, guruVendors.contains(id))));
    }

    @Override
    public Map<Long, NameToAliasesSettings> getNameToAliasParams(List<Long> categoryIds) {
        return nameToAliasByCategoryId;
    }
}
