package ru.yandex.market.mbo.export.client;

import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.modelstorage.params.ParameterViewBuilder;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersConfig;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.param.ParameterView;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 20.02.2018
 */
public class CategoryParametersServiceClientStub implements CategoryParametersServiceClient {
    private final Map<Long, CategoryParametersConfig> configs;

    private static final CategoryParametersConfig EMPTY = new CategoryParametersConfig(
        MboParameters.Category
        .newBuilder()
        .build()
    );

    public CategoryParametersServiceClientStub(Map<Long, CategoryParametersConfig> configs) {
        this.configs = configs;
    }

    @Override
    public CategoryParametersConfig getCategoryParameters(Long hid) {
        return configs.getOrDefault(hid, EMPTY);
    }

    public static CategoryParametersServiceClient empty() {
        return new CategoryParametersServiceClientStub(Collections.emptyMap());
    }

    @Override
    public List<Long> addOptions(long categoryId, long parameterId, Collection<MboParameters.Option> options,
                                 long userId) throws OperationException {
        throw new UnsupportedOperationException("addOptions isn't implemented yet");
    }

    public static CategoryParametersServiceClient ofCategoryParams(long categoryHid,
                                                             Collection<CategoryParam> parameters) {
        return CategoryParametersServiceClientStub.ofCategory(
            categoryHid,
            parameters.stream().map(p -> ParameterViewBuilder.toProto(ParameterView.of(p))).collect(Collectors.toList())
        );
    }

    public static CategoryParametersServiceClient ofCategoryEntities(CategoryEntities... entities) {
        Map<Long, CategoryParametersConfig> paramsMap = new HashMap<>();
        for (CategoryEntities category : entities) {
            CategoryParametersConfig config = new CategoryParametersConfig(MboParameters.Category.newBuilder()
                .setHid(category.getHid())
                .addAllParameter(category.getParameters().stream()
                    .map(ParameterProtoConverter::convertToParam)
                    .collect(Collectors.toList()))
                .build());
            paramsMap.put(category.getHid(), config);
        }
        return new CategoryParametersServiceClientStub(paramsMap);
    }

    public static CategoryParametersServiceClient ofCategory(long categoryHid,
                                                             Collection<MboParameters.Parameter> params) {
        CategoryParametersConfig config = new CategoryParametersConfig(MboParameters.Category.newBuilder()
            .setHid(categoryHid)
            .addAllParameter(params)
            .build());

        return new CategoryParametersServiceClientStub(Collections.singletonMap(categoryHid, config));
    }

    @Override
    public void invalidateCache() {
        return;
    }
}
