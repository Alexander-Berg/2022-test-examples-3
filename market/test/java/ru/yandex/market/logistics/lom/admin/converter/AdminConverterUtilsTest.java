package ru.yandex.market.logistics.lom.admin.converter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.logistics.lom.admin.utils.AdminConverterUtils.normalize;

public class AdminConverterUtilsTest {

    @Test
    void normalizeToNullTest() {
        Assertions.assertThat(normalize("")).isEqualTo(null);
        Assertions.assertThat(normalize("  \n\r   \r   \t ")).isEqualTo(null);
        Assertions.assertThat(normalize("  \n\r  hello \r   \t ")).isEqualTo("hello");
        Assertions.assertThat(normalize("  \n\r  hello  \t \r world \r   \t ")).isEqualTo("hello world");
    }
}
