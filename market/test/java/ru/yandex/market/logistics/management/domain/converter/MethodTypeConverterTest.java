package ru.yandex.market.logistics.management.domain.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.type.MethodType;

class MethodTypeConverterTest extends AbstractTest {

    private static final MethodTypeConverter METHOD_TYPE_CONVERTER = new MethodTypeConverter();

    @Test
    void convertFromDbColumn() {
        softly.assertThat("createOrder")
            .isEqualTo(METHOD_TYPE_CONVERTER.convertToDatabaseColumn(MethodType.CREATE_ORDER));
    }

    @Test
    void convertToDbColumn() {
        softly.assertThat(MethodType.CREATE_ORDER)
            .isEqualTo(METHOD_TYPE_CONVERTER.convertToEntityAttribute("createOrder"));
    }
}
