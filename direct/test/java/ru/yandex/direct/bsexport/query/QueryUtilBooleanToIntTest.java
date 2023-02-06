package ru.yandex.direct.bsexport.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.bsexport.query.QueryUtil.booleanToInt;

class QueryUtilBooleanToIntTest {
    @Test
    void throwsExceptionForNullValue() {
        assertThrows(NullPointerException.class, () -> booleanToInt(null));
    }

    @Test
    void returnsOneForTrueArgument() {
        assertThat(booleanToInt(Boolean.TRUE)).isEqualTo(1);
    }

    @Test
    void returnsZeroForFalseArgument() {
        assertThat(booleanToInt(Boolean.FALSE)).isEqualTo(0);
    }
}
