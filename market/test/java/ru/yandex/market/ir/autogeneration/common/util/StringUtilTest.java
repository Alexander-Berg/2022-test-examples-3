package ru.yandex.market.ir.autogeneration.common.util;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class StringUtilTest {

    @Test
    public void splitIntoSet() {
        Assertions.assertThat(
            StringUtil.splitIntoSet("1,2,3,4,5", ",", Integer::parseInt)
        )
            .isInstanceOf(Set.class)
            .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

}
