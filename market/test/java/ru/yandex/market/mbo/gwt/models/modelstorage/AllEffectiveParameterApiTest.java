package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 03.04.2017
 */
@RunWith(Parameterized.class)
public class AllEffectiveParameterApiTest {

    private CommonModel model;
    private Collection<ParameterValues> expected;

    public AllEffectiveParameterApiTest(CommonModel model, Collection<ParameterValues> expected) {
        this.model = model;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static List<Object[]> makeData() {
        return Arrays.asList(new Object[][]{
            {
                DataFactory.notEmptyModelOnly(),
                Arrays.asList(
                    ParameterValues.of(DataFactory.singleModelValue()),
                    DataFactory.multiModelValues()
                )
            },
            {
                DataFactory.modificationEmptyModelEmpty(),
                Collections.emptyList()
            },
            {
                DataFactory.modificationEmptyModelNotEmpty(),
                Arrays.asList(
                    ParameterValues.of(DataFactory.singleModelValue()),
                    DataFactory.multiModelValues()
                )
            },
            {
                DataFactory.modificationNotEmptyModelEmpty(),
                Arrays.asList(
                    ParameterValues.of(DataFactory.singleModificationValue()),
                    DataFactory.multiModificationValues()
                )
            },
            {
                DataFactory.modificationNotEmptyModelNotEmpty(),
                Arrays.asList(
                    ParameterValues.of(DataFactory.singleModificationValue()),
                    DataFactory.multiModificationValues()
                )
            }
        });
    }

    @Test
    public void getEffectiveParametersValuesShouldReturnCorrectValues() {
        Collection<ParameterValues> values = model.getEffectiveParameterValues();
        Assert.assertEquals(expected.size(), values.size());
        Assert.assertTrue(expected.containsAll(values));
    }
}
