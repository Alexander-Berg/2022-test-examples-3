package ru.yandex.market.mbo.db;

import ru.yandex.market.mbo.db.params.OptionsLoader;
import ru.yandex.market.mbo.db.params.ParameterLoader;
import ru.yandex.market.mbo.db.params.ParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntitiesBase;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.GuruParamFilter;
import ru.yandex.market.mbo.gwt.models.params.ParameterOptionsPositions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author padme
 */
public class ParameterLoaderServiceStub extends ParameterLoaderService {

    private final Map<Long, CategoryEntities> categoryEntitiesMap;

    public ParameterLoaderServiceStub() {
        this(Collections.emptyList());
    }

    public ParameterLoaderServiceStub(CategoryEntities... categoryEntities) {
        this(Arrays.asList(categoryEntities));
    }

    public ParameterLoaderServiceStub(Collection<CategoryEntities> categoryEntities) {
        super(null, null, null, null, null, null, -1);
        this.parameterLoader = new ParameterLoaderStub(this);
        this.optionsLoader = new OptionsLoader(
            null,
                     this.parameterLoader);
        this.categoryEntitiesMap = categoryEntities.stream()
            .collect(Collectors.toMap(CategoryEntitiesBase::getHid, Function.identity()));
    }

    public void addAllCategoryEntities(Collection<CategoryEntities> categoryEntities) {
        categoryEntities.forEach(this::addCategoryEntities);
    }

    public void addCategoryEntities(CategoryEntities categoryEntities) {
        this.categoryEntitiesMap.put(categoryEntities.getHid(), categoryEntities);
    }

    public void addCategoryParam(CategoryParam categoryParam) {
        CategoryEntities entities = categoryEntitiesMap.computeIfAbsent(categoryParam.getCategoryHid(),
            categoryId -> new CategoryEntities(categoryId, Collections.emptyList()));

        long id = categoryParam.getId();
        entities.removeParameter(id);
        entities.addParameter(categoryParam);
    }

    public void addAllCategoryParams(Collection<CategoryParam> categoryParams) {
        categoryParams.forEach(this::addCategoryParam);
    }

    public void addAllCategoryParams(CategoryParam... categoryParams) {
        addAllCategoryParams(Arrays.asList(categoryParams));
    }

    @Override
    public CategoryEntities loadCategoryEntitiesByHid(long hid) {
        return categoryEntitiesMap.getOrDefault(hid, new CategoryEntities(hid, Collections.emptyList()));
    }

    @Override
    public CategoryParam loadParameter(long id, long hid) {
        return loadCategoryEntitiesByHid(hid).getParameterById(id);
    }

    @Override
    public List<CategoryParam> loadLocalParamsForCategory(long hid) {
        return loadCategoryEntitiesByHid(hid).getParameters()
            .stream().filter(x -> !x.isGlobal())
            .collect(Collectors.toList());
    }

    @Override
    public List<CategoryParam> loadGlobalParamsForCategory(long hid) {
        return loadCategoryEntitiesByHid(hid).getParameters()
            .stream().filter(x -> x.isGlobal())
            .collect(Collectors.toList());
    }

    @Override
    public List<CategoryParam> loadLocalAndGlobalParamsForCategory(long hid) {
        return loadCategoryEntitiesByHid(hid).getParameters();
    }

    @Override
    public Map<Long, ParameterOptionsPositions> loadOptionPositionsBatch(Long categoryHid, Collection<Long> paramIds) {
        return Collections.emptyMap();
    }

    @Override
    public CategoryEntities loadGlobalEntitiesWithoutValues() {
        return categoryEntitiesMap.getOrDefault(GLOBAL_ENTITIES_HID,
            new CategoryEntities(GLOBAL_ENTITIES_HID, Collections.emptyList()));
    }

    @Override
    public List<CategoryParam> loadFilteredParameters(long hid, GuruParamFilter filter) {
        CategoryEntities categoryEntities = loadCategoryEntitiesByHid(hid);
        List<CategoryParam> list = Stream.concat(
            categoryEntities.getParameters().stream(),
            categoryEntities.getDeletedParameters().stream()).collect(Collectors.toList());

        // Filter params
        return list.stream().
            filter(param -> isMatch(param, filter)).
            collect(Collectors.toList());
    }

    @Override
    public long getCategoryHid(long parameterId) {
        for (CategoryEntities categoryEntities : categoryEntitiesMap.values()) {
            long categoryId = categoryEntities.getHid();
            for (CategoryParam parameter : categoryEntities.getParameters()) {
                if (parameter.getRealParamId() == parameterId) {
                    return categoryId;
                }
            }
        }
        throw new RuntimeException("Not found parameter '" + parameterId + "'.");
    }


    public Map<Long, CategoryEntities> getCategoryEntitiesMap() {
        return categoryEntitiesMap;
    }

    @Override
    public Set<Long> loadBreakInheritanceParameters(long categoryId) {
        return Collections.emptySet();
    }

    @Override
    public Map<Long, Set<Long>> loadBreakInheritanceParameters(Collection<Long> categoryIds) {
        return Collections.emptyMap();
    }

    private class ParameterLoaderStub extends ParameterLoader {

        private final ParameterLoaderServiceStub stub;

        private ParameterLoaderStub(ParameterLoaderServiceStub stub) {
            super(null,
                null,
                null,
                null,
                null,
                null,
                -1);
            this.stub = stub;
        }

        @Override
        public void loadValuesForGlobalParameters(List<CategoryParam> result) {
        }

        @Override
        public long getCategoryHid(long parameterId) {
            return stub.getCategoryHid(parameterId);
        }

        @Override
        public CategoryEntities loadCategoryEntitiesByHid(long hid) {
            return stub.loadCategoryEntitiesByHid(hid);
        }

    }

    @Override
    public List<CategoryParam> loadGlobalParameters(List<Long> paramIds) {
        return loadCategoryEntitiesByHid(GLOBAL_ENTITIES_HID).getParameters()
            .stream()
            .filter(categoryParam -> paramIds.contains(categoryParam.getId()))
            .collect(Collectors.toList());
    }

}
