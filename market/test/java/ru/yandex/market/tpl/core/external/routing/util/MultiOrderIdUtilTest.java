package ru.yandex.market.tpl.core.external.routing.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MultiOrderIdUtilTest {

    @Test
    void parseSubTaskIds_multiOrder() {
        //then
        Assertions.assertThat(MultiOrderIdUtil.parseSubTaskIds("m_1_2_3"))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void parseSubTaskIds_multiClientReturn() {
        //then
        Assertions.assertThat(MultiOrderIdUtil.parseSubTaskIds("cr_1_2_3"))
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void parseSubTaskIds_other() {
        //then
        Assertions.assertThat(MultiOrderIdUtil.parseSubTaskIds("123"))
                .containsExactlyInAnyOrder(123L);
    }
}
