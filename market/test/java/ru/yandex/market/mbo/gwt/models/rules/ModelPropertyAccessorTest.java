package ru.yandex.market.mbo.gwt.models.rules;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ModelPropertyAccessorTest {

    @Test
    public void testPropertyParamsContainsDifferentNegativeParamIds() {
        CommonModel model = new CommonModel();

        List<Long> paramIds = new ArrayList<>();
        for (ModelProperty modelProperty : ModelProperty.values()) {
            ParameterValues property = ModelPropertyAccessor.getProperty(model, modelProperty);
            paramIds.add(property.getParamId());
        }

        List<Long> distinctParamIds = paramIds.stream()
            .distinct()
            .collect(Collectors.toList());

        Assertions.assertThat(paramIds).containsOnlyElementsOf(distinctParamIds);
        Assertions.assertThat(paramIds.stream().filter(id -> id >= 0)).isEmpty();
    }
}
