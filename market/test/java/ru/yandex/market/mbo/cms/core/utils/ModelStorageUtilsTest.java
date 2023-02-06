package ru.yandex.market.mbo.cms.core.utils;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;

/**
 * @author sergtru
 * @since 12.02.2018
 */
public class ModelStorageUtilsTest {

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void getDefaultTitle() {
        final String expectedName = "This one is correct name";
        ModelStorage.ParameterValue wrongParameter = ModelStorage.ParameterValue.newBuilder()
                .setXslName("wrong one")
                .setParamId(1)
                .setOptionId(3)
                .build();
        ModelStorage.LocalizedString uaName = ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("ua")
                .setValue(expectedName)
                .build();
        ModelStorage.LocalizedString ruName = ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(ModelStorageUtils.RU_ISO_CODE)
                .setValue(expectedName)
                .build();
        ModelStorage.ParameterValue rightParameter = ModelStorage.ParameterValue.newBuilder()
                .setParamId(ModelStorageUtils.NAME_PARAMETER_ID)
                .addStrValue(uaName)
                .addStrValue(ruName)
                .build();
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
                .addParameterValues(wrongParameter)
                .addParameterValues(rightParameter)
                .build();
        Assert.assertEquals(expectedName, ModelStorageUtils.getDefaultTitle(model));
    }
}
