package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.market.mbo.gwt.utils.ParametersInheritanceUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author tokhmanva
 * @since 10/26/2020
 */
@RunWith(Parameterized.class)
public class AllEffectiveParamsAndHypotheses {

    private CommonModel model;
    private Collection<ParameterValues> expected;

    public AllEffectiveParamsAndHypotheses(CommonModel model,
                                           Collection<ParameterValues> expected) {
        this.model = model;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static List<Object[]> makeData() {
        return Arrays.asList(new Object[][]{
            {
                DataFactory.modificationNotEmptyModelNotEmptyWithHypothesis(),
                Collections.singletonList(
                    DataFactory.multiModificationValues()
                )
            },
            {
                DataFactory.modificationNotEmptyModelNotEmptyWithAnotherHypothesis(),
                Arrays.asList(
                    ParameterValues.of(DataFactory.singleModelValue()),
                    DataFactory.multiModificationValues()
                )
            }
        });
    }

    @Test
    public void getEffectiveParametersValuesAndHypothesisShouldReturnCorrectValues() {
        ParametersInheritanceUtil.inheritParametersAndHypotheses(model);
        Collection<ParameterValues> values = model.getEffectiveParameterValues();

        Assert.assertEquals(expected.size(), values.size());
        Assert.assertTrue(expected.containsAll(values));
    }
}
