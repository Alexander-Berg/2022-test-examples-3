package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 23.03.2017
 */
@RunWith(Parameterized.class)
public class EffectiveSingleParameterApiTest {
    private CommonModel model;
    private ParameterValue expectedById;
    private ParameterValue expectedByParam;

    public EffectiveSingleParameterApiTest(CommonModel model,
                                           ParameterValue expectedById,
                                           ParameterValue expectedByParam) {
        this.model = model;
        this.expectedById = expectedById;
        this.expectedByParam = expectedByParam;
    }

    @Parameterized.Parameters
    public static List<Object[]> makeData() {
        return Arrays.asList(new Object[][]{
            {
                DataFactory.modificationEmptyModelEmpty(),
                null,
                new ParameterValue(DataFactory.SINGLE_PARAM, new ArrayList<>())
            },
            {
                DataFactory.modificationEmptyModelNotEmpty(),
                DataFactory.singleModelValue(),
                DataFactory.singleModelValue()
            },
            {
                DataFactory.modificationNotEmptyModelEmpty(),
                DataFactory.singleModificationValue(),
                DataFactory.singleModificationValue()
            },
            {
                DataFactory.modificationNotEmptyModelNotEmpty(),
                DataFactory.singleModificationValue(),
                DataFactory.singleModificationValue()
            }
        });
    }

    @Test
    public void getEffectiveOnSingleValueByIdShouldReturnCorrectValue() {
        Optional<ParameterValue> value = model.getEffectiveSingleParameterValue(DataFactory.SINGLE_PARAM.getId());

        Assert.assertEquals(expectedById != null, value.isPresent());
        Assert.assertEquals(expectedById, value.orElse(null));
    }

    @Test
    public void getEffectiveOnSingleValueByParamShouldReturnCorrectValue() {
        ParameterValue value = model.getEffectiveSingleParameterValue(DataFactory.SINGLE_PARAM);
        Assert.assertEquals(expectedByParam, value);
    }
}
