package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author s-ermakov
 */
public class EffectiveParameterApiTest {

    @Test
    public void testNotNullReturnIfNoParamValuesIsFound() {
        CommonModel model = new CommonModel();
        ParameterValues values = model.getEffectiveParameterValuesOrEmptyIfNotExist(DataFactory.SINGLE_PARAM);
        Assert.assertNotNull(values);
        Assert.assertTrue(values.getValues().isEmpty());
    }
}
