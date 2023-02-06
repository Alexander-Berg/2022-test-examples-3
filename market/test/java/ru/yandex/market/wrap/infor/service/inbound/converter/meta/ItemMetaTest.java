package ru.yandex.market.wrap.infor.service.inbound.converter.meta;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ItemMetaTest {
    /**
     * Если item == null, то в конструкторе бросается исключение
     */
    @Test
    void shouldThrowExceptionIfItemIsNull() {
        Assertions.assertThatThrownBy(() -> new ItemMeta(null, null, null))
            .isInstanceOf(NullPointerException.class);
    }
}
