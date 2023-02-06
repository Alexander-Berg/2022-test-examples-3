package ru.yandex.direct.ytwrapper.dynamic.dsl;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.jooq.impl.DSL;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.ytwrapper.dynamic.dsl.YtDSL.Case.of;

public class YtDSLTest {

    @Test
    public void testTransform() {
        ImmutableMap<Integer, Integer> map = ImmutableMap.of(1, 2, 3, 4);
        String field = YtDSL.transform(DSL.field("bid", Integer.class), map).toString();
        String expectedField = "transform(bid, (1, 3), (2, 4))";
        assertEquals(expectedField, field);
    }

    @Test
    public void testYtSwitchEmptyCases() {
        String sql = YtDSL.ytSwitch(DSL.val("myField"), List.of(), "default value").toString();
        assertThat(sql).isEqualTo("'default value'");
    }

    @Test
    public void testYtSwitch() {
        var cases = List.of(
                of("case1", DSL.val("val1")),
                of("case2", DSL.val("val2"))
        );

        String sql = YtDSL.ytSwitch(DSL.val("myField"), cases, "default value").toString();

        String expected = "IF('myField' = 'case1', 'val1', IF('myField' = 'case2', 'val2', 'default value'))";
        assertThat(sql).isEqualTo(expected);
    }
}
