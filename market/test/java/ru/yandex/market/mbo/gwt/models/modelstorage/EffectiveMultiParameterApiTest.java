package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 23.03.2017
 */
@RunWith(Parameterized.class)
public class EffectiveMultiParameterApiTest {
    private CommonModel model;
    private ParameterValues expected;

    public EffectiveMultiParameterApiTest(CommonModel model, ParameterValues expected) {
        this.model = model;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static List<Object[]> makeData() {
        return Arrays.asList(new Object[][] {
            {
                DataFactory.modificationEmptyModelEmpty(),
                ParameterValues.of(DataFactory.MULTI_PARAM)
            },
            {
                DataFactory.modificationEmptyModelNotEmpty(),
                DataFactory.multiModelValues()
            },
            {
                DataFactory.modificationNotEmptyModelEmpty(),
                DataFactory.multiModificationValues()
            },
            {
                DataFactory.modificationNotEmptyModelNotEmpty(),
                DataFactory.multiModificationValues()
            }
        });
    }

    @Test
    public void getEffectiveOnMultiValueByParamShouldReturnCorrectValue() {
        ParameterValues values = model.getEffectiveParameterValuesOrEmptyIfNotExist(DataFactory.MULTI_PARAM);
        Assert.assertEquals(expected, values);
    }

    @Test
    public void getEffectiveOnMultiValueByParamCollectionShouldReturnCorrectValue() {
        Map<Long, ParameterValues> values =
            model.getEffectiveParameterValuesMap(Collections.singletonList(DataFactory.MULTI_PARAM));
        Assert.assertEquals(1, values.size());
        Assert.assertEquals(expected, values.get(DataFactory.MULTI_PARAM.getId()));
    }
}
