package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryPair;
import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelsConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ModelTransferList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author danfertev
 * @since 05.10.2018
 */
public class ListOfModelsConfigBuilder {
    private List<ModelTransferList> lists = new ArrayList<>();

    public static ListOfModelsConfigBuilder newBuilder() {
        return new ListOfModelsConfigBuilder();
    }

    public ListOfModelsConfigBuilder models(long sourceCategoryId, long targetCategoryId, long... modelIds) {
        lists.add(new ModelTransferList(new CategoryPair(sourceCategoryId, targetCategoryId),
            Arrays.stream(modelIds).boxed().collect(Collectors.toList()), null));
        return this;
    }

    public ListOfModelsConfig build() {
        return new ListOfModelsConfig(lists);
    }
}
