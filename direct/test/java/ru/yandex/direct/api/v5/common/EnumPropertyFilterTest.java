package ru.yandex.direct.api.v5.common;

import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.common.util.PropertyFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

public class EnumPropertyFilterTest {

    @Test
    public void test_from() {
        Map<TestEnum, String> actualMap =
                EnumPropertyFilter.from(TestEnum.class, mock(PropertyFilter.class)).getEnumToFieldMap();

        assertThat(actualMap).containsExactly(
                entry(TestEnum.IS_AVAILABLE, "isAvailable"),
                entry(TestEnum.V_CARD_ID, "VCardId")
        );
    }

    private enum TestEnum {
        IS_AVAILABLE,
        V_CARD_ID
    }
}
