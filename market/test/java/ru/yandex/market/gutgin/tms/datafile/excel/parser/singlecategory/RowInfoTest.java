package ru.yandex.market.gutgin.tms.datafile.excel.parser.singlecategory;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.partner.content.common.entity.ParameterType;
import ru.yandex.market.partner.content.common.entity.ParameterValue;

import java.util.Arrays;
import java.util.Collection;

public class RowInfoTest {
    @Test
    public void getModelNameWithVendorAndParams() {
        long paramId1 = 1;
        long paramId2 = 2;
        long paramId3 = 3;
        long paramId4 = 4;

        ParameterValue v1 = new ParameterValue();
        v1.setParamId(paramId1);
        v1.setType(ParameterType.BOOL);
        v1.setBoolValue(true);

        ParameterValue v31 = new ParameterValue();
        v31.setParamId(paramId3);
        v31.setType(ParameterType.ENUM);
        v31.setOptionId(1);

        ParameterValue v32 = new ParameterValue();
        v32.setParamId(paramId3);
        v32.setType(ParameterType.ENUM);
        v32.setOptionId(2);

        ParameterValue v4 = new ParameterValue();
        v4.setParamId(paramId4);
        v4.setType(ParameterType.STRING);
        v4.setStringValue("string val");

        RowInfo ri = RowInfo.newBuilder(1)
            .setShopSku("shop.sku")
            .setModelName("model.name")
            .setModelId(null)
            .setPictures(Arrays.asList("url1", "url2"))
            .setVendor("vendor")
            .setPickerImageUrl(null)
            .build();
        ri.addParamValue(1, v4);
        ri.addParamValue(2, v32);
        ri.addParamValue(2, v31);
        ri.addParamValue(3, v1);

        LongSet paramIds = new LongOpenHashSet(Arrays.asList(paramId3, paramId1, paramId2));

        String expected = "vendor model.name (1, BOOL, true) (3, ENUM, 1) (3, ENUM, 2)";

        Assert.assertEquals(expected, ri.getModelNameWithVendorAndParams(paramIds));
    }

    @Test
    public void shouldDeduplicateParamValues() {
        long paramId = 1L;
        ParameterType type = ParameterType.ENUM;
        int optionId = 2;

        ParameterValue v1 = new ParameterValue();
        v1.setParamId(paramId);
        v1.setType(type);
        v1.setOptionId(optionId);

        ParameterValue v2 = new ParameterValue();
        v2.setParamId(paramId);
        v2.setType(type);
        v2.setOptionId(optionId);


        RowInfo ri = RowInfo.newBuilder(1)
            .setShopSku("shop.sku")
            .setModelName("model.name")
            .setModelId(null)
            .setPictures(Arrays.asList("url1", "url2"))
            .setVendor("vendor")
            .setPickerImageUrl(null)
            .build();
        ri.addParamValue(1, v1);
        ri.addParamValue(2, v2);

        Collection<ParamValue> parameterValues = ri.getParameterValues();
        Assert.assertEquals(1, parameterValues.size());
        ParamValue value = parameterValues.iterator().next();
        Assert.assertEquals(1, value.getCellIndex());
        Assert.assertEquals(paramId, value.getParamId());
        Assert.assertEquals(v1, value.getInnerParameterValue());
    }
}