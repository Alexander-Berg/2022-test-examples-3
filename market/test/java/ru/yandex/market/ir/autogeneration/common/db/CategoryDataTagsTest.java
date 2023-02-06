package ru.yandex.market.ir.autogeneration.common.db;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.db.ParameterValueComposer;

@RunWith(JUnit4.class)
public class CategoryDataTagsTest {

    @Test
    public void getParamsByTag() {
        var tag1 = "tag1";
        var tag2 = "tag2";
        CategoryData categoryData = CategoryData.build(MboParameters.Category.newBuilder()
                        .setHid(1000)
                        .setLeaf(true)
                        .addParameter(MboParameters.Parameter.newBuilder()
                                .setId(ParameterValueComposer.VENDOR_ID)
                                .setXslName(CategoryData.VENDOR)
                                .setValueType(MboParameters.ValueType.ENUM)
                        )
                        .addParameter(
                                MboParameters.Parameter.newBuilder()
                                        .setId(1L)
                                        .setXslName("param1")
                                        .setValueType(MboParameters.ValueType.STRING)
                                        .addTags(tag1)
                                        .addTags(tag2)
                        )
                        .addParameter(
                                MboParameters.Parameter.newBuilder()
                                        .setId(2L)
                                        .setXslName("param2")
                                        .setValueType(MboParameters.ValueType.STRING)
                                        .addTags(tag1)
                        )
                        .addParameter(
                                MboParameters.Parameter.newBuilder()
                                        .setId(3L)
                                        .setXslName("param3")
                                        .setValueType(MboParameters.ValueType.STRING)
                                        .addTags(tag2)
                        )
                        .build(),
                Set.of("tag1", "tag2"));

        var tag1Params = categoryData.getParamIdsByTag(tag1);
        Assert.assertEquals("tag1 params quantity", 2, tag1Params.size());
        Assert.assertTrue("all tag1 params present", categoryData.getParamIdsByTag(tag1).containsAll(List.of(1L, 2L)));

        var tag2Params = categoryData.getParamIdsByTag(tag2);
        Assert.assertEquals("tag2 params quantity", 2, tag2Params.size());
        Assert.assertTrue("all tag2 params present", categoryData.getParamIdsByTag(tag2).containsAll(List.of(1L, 3L)));

        Assert.assertTrue("param2 has tag1", categoryData.paramHasTag(2L, tag1));
        Assert.assertFalse("param2 does not have tag2", categoryData.paramHasTag(2L, tag2));

        Assert.assertEquals("check param1 tags", Set.of(tag1, tag2), categoryData.getParamTags(1L));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getParamsByTagIsUnmodifiable() {
        var tag1 = "tag1";
        CategoryData categoryData = CategoryData.build(MboParameters.Category.newBuilder()
                        .setHid(1000)
                        .setLeaf(true)
                        .addParameter(MboParameters.Parameter.newBuilder()
                                .setId(ParameterValueComposer.VENDOR_ID)
                                .setXslName(CategoryData.VENDOR)
                                .setValueType(MboParameters.ValueType.ENUM)
                        )
                        .addParameter(
                                MboParameters.Parameter.newBuilder()
                                        .setId(1L)
                                        .setXslName("param1")
                                        .setValueType(MboParameters.ValueType.STRING)
                                        .addTags(tag1)
                        ),
                Set.of("tag1", "tag2"));
        var tag1Params = categoryData.getParamIdsByTag(tag1);
        tag1Params.add(0L);
    }
}
