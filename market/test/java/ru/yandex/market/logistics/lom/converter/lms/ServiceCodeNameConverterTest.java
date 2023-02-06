package ru.yandex.market.logistics.lom.converter.lms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;

@DisplayName("Конвертация enum'а ServiceCodeName LMS -> LOM")
public class ServiceCodeNameConverterTest extends AbstractTest {
    private final EnumConverter enumConverter = new EnumConverter();

    /**
     * Если упал этот тест, это означает, что в LMS добавился новый элемент enum'а.
     * Его нужно добавить и в enum в LOM'е.
     * LOM должен обязательно релизиться раньше, чем LMS.
     */
    @ParameterizedTest
    @EnumSource(ru.yandex.market.logistics.management.entity.type.ServiceCodeName.class)
    void allEnumConstantsConvertCorrectly(
        ru.yandex.market.logistics.management.entity.type.ServiceCodeName serviceCodeName
    ) {
        softly.assertThat(enumConverter.convert(serviceCodeName, ServiceCodeName.class))
            .extracting(Enum::name)
            .isEqualTo(serviceCodeName.name());
    }
}
