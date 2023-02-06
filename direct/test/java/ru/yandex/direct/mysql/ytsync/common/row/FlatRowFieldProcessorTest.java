package ru.yandex.direct.mysql.ytsync.common.row;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.mysql.ytsync.common.model.JsonString;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.misc.lang.number.UnsignedInteger;
import ru.yandex.misc.lang.number.UnsignedLong;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class FlatRowFieldProcessorTest {
    @Parameterized.Parameter
    public Object value;
    @Parameterized.Parameter(1)
    public Object expectNode;

    @Parameterized.Parameters
    public static Object[][] params() {
        return new Object[][]{
                {null, kosherBuildNode(null)},
                {"Привет", kosherBuildNode("Привет")},
                {new byte[]{12, 54, -23}, kosherBuildNode(new byte[]{12, 54, -23})},
                {123124, kosherBuildNode(123124)},
                {-235345, kosherBuildNode(-235345)},
                {234234L, kosherBuildNode(234234L)},
                {-234525L, kosherBuildNode(-234525L)},
                {UnsignedLong.valueOf(234324), kosherBuildNode(UnsignedLong.valueOf(234324))},
                {UnsignedInteger.valueOf(23534262), kosherBuildNode(UnsignedInteger.valueOf(23534262))},
                {12345.34f, kosherBuildNode(12345.34f)},
                {534542.43635d, kosherBuildNode(534542.43635d)},
                {true, kosherBuildNode(true)},
                {false, kosherBuildNode(false)},
                {new JsonString("{\"s\": \"ARCHIVED\"}"), YTree.mapBuilder().key("s").value("ARCHIVED").buildMap()},
                {new JsonString("{}"), YTree.mapBuilder().buildMap()}
        };
    }

    @Test
    public void inlinedVersionOfBuildNodeIsEquivalentToOriginal() {
        assertThat(FlatRowFieldProcessor.buildNode(value))
                .isEqualTo(expectNode);
    }

    static YTreeNode kosherBuildNode(Object value) {
        return YTree.builder().value(value).build();
    }
}
