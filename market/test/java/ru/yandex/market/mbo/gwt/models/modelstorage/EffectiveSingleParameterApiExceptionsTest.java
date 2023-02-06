package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 23.03.2017
 */
@RunWith(Parameterized.class)
public class EffectiveSingleParameterApiExceptionsTest {
    private CommonModel model;

    public EffectiveSingleParameterApiExceptionsTest(CommonModel model) {
        this.model = model;
    }

    @Parameterized.Parameters
    public static List<Object[]> makeData() {
        return Arrays.asList(
            new Object[][]{
                {DataFactory.modificationEmptyModelNotEmpty()},
                {DataFactory.modificationNotEmptyModelEmpty()},
                {DataFactory.modificationNotEmptyModelNotEmpty()}
            }
        );
    }

    @Test(expected = IllegalStateException.class)
    public void getEffectiveSingleValueOnManyValuesShouldThrowException() {
        model.getEffectiveSingleParameterValue(DataFactory.MULTI_PARAM);
    }

    @Test(expected = IllegalStateException.class)
    public void getEffectiveSingleValueByParamOnManyValuesShouldThrowException() {
        model.getEffectiveSingleParameterValue(DataFactory.MULTI_PARAM);
    }
}
