package ru.yandex.market.delivery.transport_manager.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class FormatUtilTest {

    @Test
    void nulls() {
        var core = "core";
        var result = FormatUtil.join(
            "core",
            null,
            null,
            null,
            null
        );
        Assertions.assertThat(result).isEqualTo(core);
    }

    @Test
    void nullDelimiter() {
        var core = "core element element2";
        var result = FormatUtil.join(
            "core",
            null,
            "element",
            "element2"
        );
        Assertions.assertThat(result).isEqualTo(core);
    }

    @Test
    void ok() {
        String result = FormatUtil.join("core", ", ", "element1", "element2");
        Assertions.assertThat(result).isEqualTo("core, element1, element2");
    }
}
